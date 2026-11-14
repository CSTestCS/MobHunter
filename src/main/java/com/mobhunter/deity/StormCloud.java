package com.mobhunter.deity;

import com.mobhunter.ability.HunterDamage;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * One of the storm clouds orbiting the Ram God. They hurl the god's lightning and hail and can be
 * destroyed (a breakable part): every cloud lost weakens the storm, but enrages the god.
 */
public class StormCloud extends Mob {
    private static final EntityDataAccessor<Float> CHARGE = SynchedEntityData.defineId(StormCloud.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(StormCloud.class, EntityDataSerializers.INT);

    /** The Ram Deity's five weapon-clouds. */
    public enum Kind {
        LIGHTNING(0xD8D27A), HAIL(0xE8F4FF), WIND(0xCFEFE0), THUNDER(0x55506A), TORNADO(0x8A8A90);
        public final int tint;

        Kind(int tint) {
            this.tint = tint;
        }
    }
    private @Nullable UUID owner;
    private int index;
    private @Nullable Vec3 station;

    public StormCloud(EntityType<? extends StormCloud> type, Level level) {
        super(type, level);
        setNoGravity(true);
        addTag(HunterDamage.MINION_TAG);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 140).add(Attributes.ARMOR, 6).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public void bind(RamGod god, int index) {
        this.owner = god.getUUID();
        this.index = index;
        entityData.set(TYPE, index % Kind.values().length);
    }

    public Kind kind() {
        return Kind.values()[Math.floorMod(entityData.get(TYPE), Kind.values().length)];
    }

    public int index() {
        return index;
    }

    /** Glow intensity used by the renderer to telegraph an incoming strike. */
    public float charge() {
        return entityData.get(CHARGE);
    }

    public void setCharge(float c) {
        entityData.set(CHARGE, c);
    }

    /** Temporarily park the cloud over a spot (to aim lightning), or null to resume orbiting. */
    public void setStation(@Nullable Vec3 v) {
        this.station = v;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGE, 0f);
        builder.define(TYPE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.CLOUD, getRandomX(1.2), getRandomY(), getRandomZ(1.2), 0, -0.02, 0);
            switch (kind()) {
                case HAIL -> { if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.SNOWFLAKE, getRandomX(1), getY(), getRandomZ(1), 0, -0.2, 0); }
                case WIND -> { if (random.nextInt(4) == 0) level().addParticle(ParticleTypes.SMALL_GUST, getRandomX(1.4), getRandomY(), getRandomZ(1.4), 0, 0, 0); }
                case THUNDER -> { if (random.nextInt(10) == 0) level().addParticle(ParticleTypes.SONIC_BOOM, getX(), getY() + 0.5, getZ(), 0, 0, 0); }
                case TORNADO -> {
                    for (int i = 0; i < 2; i++) {
                        double a = tickCount * 0.6 + i * Math.PI, h = random.nextDouble() * 9, r = 0.6 + h * 0.18;
                        level().addParticle(ParticleTypes.CLOUD, getX() + Math.cos(a) * r, getY() - h, getZ() + Math.sin(a) * r, 0, 0.05, 0);
                    }
                }
                default -> { }
            }
            if (charge() > 0.3f && random.nextInt(2) == 0) level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(1), getY(), getRandomZ(1), 0, -0.2, 0);
            return;
        }
        ServerLevel server = (ServerLevel) level();
        RamGod god = owner == null ? null : server.getEntity(owner) instanceof RamGod g ? g : null;
        if (god == null || !god.isAlive()) {
            if (tickCount > 40) discard();
            return;
        }
        Vec3 goal;
        if (station != null) goal = station;
        else {
            double a = (tickCount * 0.035) + index * Math.PI * 2 / 5;
            double r = god.getBbWidth() * 0.9 + 5;
            goal = god.position().add(Math.cos(a) * r, god.getBbHeight() * 0.4 + Math.sin(tickCount * 0.05 + index) * 1.5, Math.sin(a) * r);
        }
        Vec3 d = goal.subtract(position());
        double len = d.length();
        Vec3 v = len > 0.05 ? d.scale(Math.min(0.9, len * 0.15) / len) : Vec3.ZERO;
        setDeltaMovement(v);
        move(net.minecraft.world.entity.MoverType.SELF, v);
        if (charge() > 0) setCharge(Math.max(0, charge() - 0.02f));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof RamGod) return false;
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("index", index);
        output.putInt("kind", entityData.get(TYPE));
        if (owner != null) output.putString("owner", owner.toString());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        index = input.getIntOr("index", 0);
        entityData.set(TYPE, input.getIntOr("kind", index % 5));
        input.getString("owner").ifPresent(s -> owner = UUID.fromString(s));
    }
}
