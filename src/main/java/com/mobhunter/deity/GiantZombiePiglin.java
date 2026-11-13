package com.mobhunter.deity;

import net.minecraft.world.entity.EntityTypes;
import com.mobhunter.core.CombatEngine;
import com.mobhunter.core.CombatState;
import com.mobhunter.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Giant Zombie Piglin: the Deity of zombified piglins. Only its upper half has emerged from a nether
 * portal lying flat in the ground, so it cannot move, but it smashes, slices, grabs and throws.
 */
public class GiantZombiePiglin extends Monster implements DeityBoss, CombatEngine.MovesetProvider {
    /** Overall render scale relative to a regular zombified piglin. */
    public static final float SCALE = 9f;
    private BlockPos home = BlockPos.ZERO;
    private @Nullable UUID held;

    public GiantZombiePiglin(EntityType<? extends GiantZombiePiglin> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 600;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 2000)
                .add(Attributes.ARMOR, 18)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    public void setHome(BlockPos pos) {
        this.home = pos;
    }

    @Override
    public void onEntranceFormed(ServerLevel level) {}

    @Override
    public List<String> moveset() {
        List<String> list = new ArrayList<>(List.of("giant_smash", "giant_slice", "giant_grab", "giant_boulder", "giant_horde", "roar"));
        CombatState s = getExistingDataOrNull(ModAttachments.COMBAT);
        if (s != null && s.enraged) list.add("giant_eruption");
        return list;
    }

    public void setHeld(@Nullable Entity e) {
        held = e == null ? null : e.getUUID();
    }

    public @Nullable UUID held() {
        return held;
    }

    /** World position of the right hand, used to carry grabbed prey. */
    public Vec3 handPos() {
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
        return position().add(fwd.scale(4.5)).add(right.scale(-2.5)).add(0, 9.5, 0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            if (random.nextInt(2) == 0) level().addParticle(ParticleTypes.PORTAL, getRandomX(1.2), getY(), getRandomZ(1.2), 0, 0.5, 0);
            return;
        }
        CombatState s = getExistingDataOrNull(ModAttachments.COMBAT);
        if (isNoAi() || (s != null && s.entrance != null)) return;
        // Anchored in the portal: it can turn, but never move.
        Vec3 anchor = Vec3.atBottomCenterOf(home);
        if (home != BlockPos.ZERO && position().distanceToSqr(anchor) > 0.01) setPos(anchor);
        setDeltaMovement(Vec3.ZERO);
        LivingEntity t = getTarget();
        boolean busy = s != null && s.current != null;
        if (t != null && !busy) {
            Vec3 d = t.position().subtract(position());
            float want = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
            float yaw = Mth.approachDegrees(getYRot(), want, 4f);
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(Mth.approachDegrees(getYHeadRot(), want, 10f));
        }
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
        // immovable
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {}

    @Override
    protected void tickDeath() {
        // Sinks back into the portal.
        deathTime++;
        setPos(getX(), getY() - 0.12, getZ());
        if (level() instanceof ServerLevel server && deathTime % 4 == 0) {
            server.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 8, getZ(), 40, 3, 6, 3, 0.5);
        }
        if (deathTime >= 100 && !level().isClientSide()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() != null && source.getEntity().getType() == EntityTypes.ZOMBIFIED_PIGLIN) return false;
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIFIED_PIGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIFIED_PIGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIFIED_PIGLIN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.25f;
    }

    @Override
    protected float getSoundVolume() {
        return 5f;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("home", BlockPos.CODEC, home);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        home = input.read("home", BlockPos.CODEC).orElse(blockPosition());
    }
}
