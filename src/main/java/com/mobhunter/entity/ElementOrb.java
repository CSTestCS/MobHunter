package com.mobhunter.entity;

import com.mobhunter.ability.HunterDamage;
import com.mobhunter.core.Element;
import com.mobhunter.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A glob of elemental energy (fire, frost, venom, void...) used by monster volleys and Deity meteor rains.
 * It is drawn entirely with particles on the client.
 */
public class ElementOrb extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> ELEMENT = SynchedEntityData.defineId(ElementOrb.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(ElementOrb.class, EntityDataSerializers.FLOAT);
    private float damage = 4f;
    private float splash = 0f;
    private int potency = 1;
    private double gravity = 0.0;
    private int life = 200;

    public ElementOrb(EntityType<? extends ElementOrb> type, Level level) {
        super(type, level);
    }

    public static ElementOrb create(ServerLevel level, LivingEntity owner, Vec3 pos, Element element, float damage, float splash, int potency, float size) {
        ElementOrb orb = new ElementOrb(ModEntities.ELEMENT_ORB.get(), level);
        orb.setOwner(owner);
        orb.setPos(pos);
        orb.entityData.set(ELEMENT, element.ordinal());
        orb.entityData.set(SIZE, size);
        orb.damage = damage;
        orb.splash = splash;
        orb.potency = potency;
        return orb;
    }

    public ElementOrb gravity(double g) {
        this.gravity = g;
        return this;
    }

    public Element element() {
        return Element.values()[Math.max(0, Math.min(Element.values().length - 1, entityData.get(ELEMENT)))];
    }

    public float size() {
        return entityData.get(SIZE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ELEMENT, 0);
        builder.define(SIZE, 0.5f);
    }

    @Override
    protected double getDefaultGravity() {
        return gravity;
    }

    @Override
    protected float getAirDrag() {
        return 1.0f;
    }

    @Override
    public void tick() {
        super.tick();
        Level level = level();
        if (level.isClientSide()) {
            Element e = element();
            float s = size();
            for (int i = 0; i < 2 + (int) (s * 3); i++) {
                level.addParticle(e.dust(Math.min(4f, 1f + s)), getX() + (random.nextDouble() - 0.5) * s, getY() + (random.nextDouble() - 0.5) * s,
                        getZ() + (random.nextDouble() - 0.5) * s, 0, 0, 0);
            }
            level.addParticle(e.particle(), getX(), getY(), getZ(), 0, 0, 0);
        } else if (--life <= 0) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity owner = getOwner();
        if (owner != null && (entity.getType() == owner.getType() || entity.entityTags().contains(HunterDamage.MINION_TAG))) return false;
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level() instanceof ServerLevel server && result.getEntity() instanceof LivingEntity victim && splash <= 0) {
            LivingEntity owner = getOwner() instanceof LivingEntity l ? l : null;
            if (owner != null) HunterDamage.apply(server, owner, victim, damage, element(), potency, 0.4);
            else victim.hurtServer(server, damageSources().magic(), damage);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel server) {
            Element e = element();
            if (splash > 0) {
                LivingEntity owner = getOwner() instanceof LivingEntity l ? l : null;
                for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(splash),
                        v -> owner == null || HunterDamage.isFoe(owner, v))) {
                    if (owner != null) HunterDamage.apply(server, owner, victim, damage, e, potency, 0.6);
                }
                server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 2, splash * 0.3, 0.2, splash * 0.3, 0);
            }
            server.sendParticles(e.dust(2f), true, false, getX(), getY(), getZ(), 24, 0.6 + splash * 0.3, 0.4, 0.6 + splash * 0.3, 0.05);
            server.sendParticles(e.particle(), true, false, getX(), getY(), getZ(), 10, 0.5, 0.3, 0.5, 0.05);
            server.playSound(null, getX(), getY(), getZ(), e == Element.FIRE ? SoundEvents.FIRECHARGE_USE : SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.HOSTILE, 0.4f + splash * 0.1f, 1.2f + random.nextFloat() * 0.3f);
            discard();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("damage", damage);
        output.putInt("element", entityData.get(ELEMENT));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        damage = input.getFloatOr("damage", 4f);
        entityData.set(ELEMENT, input.getIntOr("element", 0));
    }
}
