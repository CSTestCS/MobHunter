package com.mobhunter.deity;

import com.mobhunter.core.CombatEngine;
import com.mobhunter.core.CombatState;
import com.mobhunter.core.Tier;
import com.mobhunter.deity.form.DeityForm;
import com.mobhunter.deity.form.DeityForms;
import com.mobhunter.profile.CreatureProfile;
import com.mobhunter.profile.CreatureProfiles;
import com.mobhunter.registry.ModAttachments;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Deity of a creature: one entity type whose body is assembled from that creature's
 * {@link DeityForm}, so every creature gets its own eldritch god.
 */
public class EldritchDeity extends Monster implements DeityBoss, CombatEngine.MovesetProvider {
    private static final EntityDataAccessor<String> CREATURE = SynchedEntityData.defineId(EldritchDeity.class, EntityDataSerializers.STRING);
    private static final Set<String> NO_LEAPS = Set.of("leap_slam", "pounce", "body_slam", "burrow_ambush", "dive_bomb", "stinger_dive");
    private static final Set<String> NO_MOVES = Set.of("charge", "lunge", "stampede", "rampage", "roll_charge", "torpedo", "leap_slam", "pounce",
            "body_slam", "burrow_ambush", "dive_bomb", "stinger_dive", "blink_strike");
    private BlockPos home = BlockPos.ZERO;
    private @Nullable DeityForm cachedForm;
    private String cachedFor = "";

    public EldritchDeity(EntityType<? extends EldritchDeity> type, Level level) {
        super(type, level);
        xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1800)
                .add(Attributes.ARMOR, 16)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 80)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.STEP_HEIGHT, 4.0)
                .add(Attributes.SAFE_FALL_DISTANCE, 60);
    }

    public void setCreature(Identifier id) {
        entityData.set(CREATURE, id.toString());
        refreshDimensions();
    }

    public Identifier creature() {
        Identifier id = Identifier.tryParse(entityData.get(CREATURE));
        return id == null ? Identifier.withDefaultNamespace("pig") : id;
    }

    public DeityForm form() {
        String key = entityData.get(CREATURE);
        if (cachedForm == null || !cachedFor.equals(key)) {
            Identifier id = creature();
            DeityForm f = DeityForms.get(id);
            if (f == null) f = DeityForms.fallback(BuiltInRegistries.ENTITY_TYPE.getValue(id));
            cachedForm = f;
            cachedFor = key;
        }
        return cachedForm;
    }

    public @Nullable CreatureProfile profile() {
        return CreatureProfiles.get(creature());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CREATURE, "minecraft:pig");
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (CREATURE.equals(accessor)) refreshDimensions();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        DeityForm f = form();
        return EntityDimensions.scalable(f.width(), f.height());
    }

    @Override
    public void setHome(BlockPos pos) {
        home = pos;
    }

    @Override
    public void onEntranceFormed(ServerLevel level) {}

    @Override
    public List<String> moveset() {
        CreatureProfile p = profile();
        if (p == null) return List.of("strike", "roar", "meteor_rain", "void_pull", "eldritch_gaze");
        List<String> list = new java.util.ArrayList<>(p.moveset(Tier.DEITY));
        DeityForm.Movement m = form().movement();
        if (m == DeityForm.Movement.FLOAT) list.removeIf(NO_LEAPS::contains);
        if (m == DeityForm.Movement.ANCHORED) list.removeIf(NO_MOVES::contains);
        if (!list.contains("strike")) list.add("strike");
        return list;
    }

    @Override
    public void aiStep() {
        DeityForm.Movement m = form().movement();
        setNoGravity(m != DeityForm.Movement.GROUND);
        super.aiStep();
        if (level().isClientSide()) {
            if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.REVERSE_PORTAL, getRandomX(1.2), getRandomY(), getRandomZ(1.2), 0, 0.02, 0);
            return;
        }
        CombatState s = getExistingDataOrNull(ModAttachments.COMBAT);
        if (isNoAi() || (s != null && (s.entrance != null || s.current != null))) return;
        LivingEntity t = getTarget();
        if (t != null) {
            Vec3 d = t.position().subtract(position());
            float want = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
            float yaw = Mth.approachDegrees(getYRot(), want, m == DeityForm.Movement.ANCHORED ? 3f : 6f);
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        }
        switch (m) {
            case ANCHORED -> {
                if (!home.equals(BlockPos.ZERO)) setPos(Vec3.atBottomCenterOf(home));
                setDeltaMovement(Vec3.ZERO);
            }
            case FLOAT -> {
                Vec3 goal = t != null ? t.position().add(Math.cos(tickCount * 0.01) * 9, 6 + getBbHeight() * 0.3, Math.sin(tickCount * 0.01) * 9)
                        : Vec3.atCenterOf(home).add(0, 8, 0);
                Vec3 d = goal.subtract(position());
                Vec3 v = getDeltaMovement().scale(0.85);
                if (d.length() > 1) v = v.add(d.normalize().scale(0.035));
                if (v.length() > 0.5) v = v.normalize().scale(0.5);
                setDeltaMovement(v);
            }
            case GROUND -> {
                if (t == null) return;
                Vec3 d = t.position().subtract(position());
                double dist = Math.sqrt(d.x * d.x + d.z * d.z);
                if (dist > getBbWidth() * 0.6 + 3) {
                    Vec3 v = new Vec3(d.x / dist, 0, d.z / dist).scale(0.16);
                    double vy = getDeltaMovement().y;
                    if (horizontalCollision && onGround()) vy = 0.6;
                    setDeltaMovement(v.x, vy, v.z);
                }
                if (tickCount % 4 == 0 && level() instanceof ServerLevel server && server.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING)) {
                    var box = getBoundingBox().inflate(0.5, 0, 0.5);
                    for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY + 1, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
                        var st = server.getBlockState(p);
                        if (st.is(net.minecraft.tags.BlockTags.LEAVES) || st.is(net.minecraft.tags.BlockTags.LOGS) || st.canBeReplaced()) {
                            if (!st.isAir()) server.destroyBlock(p, false, this);
                        }
                    }
                }
            }
        }
    }

    /** Keeps floating Deities from sinking into terrain. */
    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && form().movement() == DeityForm.Movement.FLOAT) {
            int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ());
            if (getY() < ground + 2) setDeltaMovement(getDeltaMovement().add(0, 0.12, 0));
        }
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    private static final java.util.Map<Identifier, java.util.Optional<SoundEvent>> AMBIENT = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return AMBIENT.computeIfAbsent(creature(), id -> {
            EntityType<?> t = BuiltInRegistries.ENTITY_TYPE.getValue(id);
            return java.util.Optional.ofNullable(t.create(level(), net.minecraft.world.entity.EntitySpawnReason.LOAD) instanceof net.minecraft.world.entity.Mob mob ? ambientOf(mob) : null);
        }).orElse(null);
    }

    private static @Nullable SoundEvent ambientOf(net.minecraft.world.entity.Mob mob) {
        try {
            var m = net.minecraft.world.entity.Mob.class.getDeclaredMethod("getAmbientSound");
            m.setAccessible(true);
            return (SoundEvent) m.invoke(mob);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    @Override
    public float getVoicePitch() {
        return 0.3f;
    }

    @Override
    protected float getSoundVolume() {
        return 5f;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("creature", entityData.get(CREATURE));
        output.store("home", BlockPos.CODEC, home);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(CREATURE, input.getStringOr("creature", "minecraft:pig"));
        home = input.read("home", BlockPos.CODEC).orElse(blockPosition());
        refreshDimensions();
    }
}
