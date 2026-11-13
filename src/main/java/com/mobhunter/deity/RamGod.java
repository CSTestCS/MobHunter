package com.mobhunter.deity;

import com.mobhunter.core.CombatEngine;
import com.mobhunter.core.CombatState;
import com.mobhunter.registry.ModAttachments;
import com.mobhunter.registry.ModEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Ram God: the Deity of sheep. A living thunderhead with the face of a ram, orbited by storm clouds
 * that strike with lightning, hail and wind.
 */
public class RamGod extends Monster implements DeityBoss, CombatEngine.MovesetProvider {
    private BlockPos home = BlockPos.ZERO;
    private boolean cloudsSpawned;

    public RamGod(EntityType<? extends RamGod> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1800)
                .add(Attributes.ARMOR, 14)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 80)
                .add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    public void setHome(BlockPos pos) {
        this.home = pos;
    }

    @Override
    public void onEntranceFormed(ServerLevel level) {
        spawnClouds(level);
    }

    private void spawnClouds(ServerLevel level) {
        if (cloudsSpawned) return;
        cloudsSpawned = true;
        for (int i = 0; i < 4; i++) {
            StormCloud c = ModEntities.STORM_CLOUD.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (c == null) continue;
            c.bind(this, i);
            double a = i * Math.PI / 2;
            c.snapTo(getX() + Math.cos(a) * 8, getY() + 2, getZ() + Math.sin(a) * 8, 0, 0);
            level.addFreshEntity(c);
        }
    }

    public List<StormCloud> clouds() {
        return level().getEntitiesOfClass(StormCloud.class, getBoundingBox().inflate(40), StormCloud::isAlive);
    }

    @Override
    public List<String> moveset() {
        List<String> list = new ArrayList<>(List.of("ram_lightning", "ram_hail", "ram_gale", "ram_dive", "roar"));
        CombatState s = getExistingDataOrNull(ModAttachments.COMBAT);
        if (s != null && s.enraged) list.add("ram_surge");
        if (clouds().size() < 2) list.add("summon_alphas");
        return list;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            for (int i = 0; i < 3; i++) level().addParticle(ParticleTypes.CLOUD, getRandomX(1.0), getRandomY(), getRandomZ(1.0), 0, -0.03, 0);
            if (random.nextInt(8) == 0) level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(1.0), getY(), getRandomZ(1.0), 0, -0.3, 0);
            if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.RAIN, getRandomX(1.0), getY(), getRandomZ(1.0), 0, -0.5, 0);
            return;
        }
        if (isNoAi()) return;
        if (tickCount > 40 && !cloudsSpawned) spawnClouds((ServerLevel) level());
        CombatState s = getExistingDataOrNull(ModAttachments.COMBAT);
        boolean busy = s != null && s.current != null;
        if (busy) return;
        LivingEntity t = getTarget();
        Vec3 goal;
        if (t != null) {
            double a = tickCount * 0.012;
            goal = t.position().add(Math.cos(a) * 11, 10, Math.sin(a) * 11);
            getLookControl().setLookAt(t, 30, 30);
            Vec3 d = t.position().subtract(position());
            float yaw = (float) (Math.atan2(d.z, d.x) * 180 / Math.PI) - 90f;
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        } else {
            goal = Vec3.atCenterOf(home).add(0, 14, 0);
        }
        Vec3 d = goal.subtract(position());
        double len = d.length();
        Vec3 v = getDeltaMovement().scale(0.85);
        if (len > 0.5) v = v.add(d.scale(0.04 / Math.max(1, len * 0.1)));
        double max = 0.45;
        if (v.length() > max) v = v.normalize().scale(max);
        setDeltaMovement(v);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // The storm shields its heart while clouds still orbit it.
        int clouds = clouds().size();
        return super.hurtServer(level, source, amount * (1f - clouds * 0.1f));
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server) {
            for (StormCloud c : clouds()) c.kill(server);
            server.getServer().setWeatherParameters(6000, 0, false, false);
        }
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SHEEP_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.3f;
    }

    @Override
    protected float getSoundVolume() {
        return 4f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GOAT_SCREAMING_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GOAT_SCREAMING_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("home", BlockPos.CODEC, home);
        output.putBoolean("clouds", cloudsSpawned);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        home = input.read("home", BlockPos.CODEC).orElse(blockPosition());
        cloudsSpawned = input.getBooleanOr("clouds", false);
    }
}
