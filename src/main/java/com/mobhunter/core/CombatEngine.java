package com.mobhunter.core;

import com.mobhunter.ability.Abilities;
import com.mobhunter.ability.Ability;
import com.mobhunter.ability.Ctx;
import com.mobhunter.profile.CreatureProfile;
import com.mobhunter.profile.CreatureProfiles;
import com.mobhunter.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Server-side brain that runs a tiered creature's moveset every tick: picks telegraphed abilities,
 * drives their phases, keeps the anticipation animation synced, manages enrage and the boss bar.
 */
public final class CombatEngine {

    public static void tick(ServerLevel level, Mob mob, Tier tier) {
        CombatState s = mob.getData(ModAttachments.COMBAT);
        CreatureProfile profile = CreatureProfiles.get(mob);
        if (profile == null) return;
        if (!s.initialized) {
            s.initialized = true;
            s.globalCooldown = 30;
        }
        if (profile.movement() == CreatureProfile.Movement.SWIMMING && tier != Tier.NONE) {
            mob.setAirSupply(mob.getMaxAirSupply());
            if (!mob.isInWater() && tier.isBoss()) airSwim(mob);
        }
        if (tier.isBoss()) updateBossBar(level, mob, s, tier);
        if (s.entrance != null) {
            if (s.entrance.tick(level, mob)) s.entrance = null;
            return;
        }
        if (!mob.isAlive() || mob.isNoAi()) return;

        LivingEntity target = acquireTarget(level, mob, tier, s);

        if (tier.isBoss() && !s.enraged && mob.getHealth() < mob.getMaxHealth() * 0.4f) {
            s.enraged = true;
            mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED, 20 * 600, 0, false, false));
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 4f, 0.5f);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER, true, false, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 12,
                    mob.getBbWidth() * 0.5, 0.5, mob.getBbWidth() * 0.5, 0);
            if (target != null) forceAbility(level, mob, tier, profile, s, target, "roar");
        }

        if (s.current != null) {
            runPhase(level, mob, s);
            return;
        }

        for (var e : s.cooldowns.object2IntEntrySet()) if (e.getIntValue() > 0) e.setValue(e.getIntValue() - 1);
        if (s.globalCooldown > 0) s.globalCooldown--;
        if (target == null) return;

        double dist = mob.distanceTo(target) - target.getBbWidth() * 0.5;
        boolean custom = mob instanceof com.mobhunter.deity.DeityBoss;
        if (!custom) approach(level, mob, target, dist, s, tier);
        if (!custom && tier.isBoss() && mob.horizontalCollision) breakObstacles(level, mob);

        if (s.globalCooldown <= 0 && (custom || dist < 10 || mob.hasLineOfSight(target))) {
            Ability chosen = choose(level, mob, tier, profile, s, target, dist);
            if (chosen != null) start(level, mob, tier, profile, s, target, chosen);
            else s.globalCooldown = 5;
        }
    }

    private static float damageMult(Mob mob, Tier tier, CombatState s) {
        float m = tier.damageMult * (s.enraged ? 1.25f : 1f);
        // Naturally strong creatures hit a little harder.
        double atk = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != null
                ? mob.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) : 2.0;
        return m * (float) Math.max(0.85, Math.min(1.6, 0.8 + atk * 0.06));
    }

    private static @Nullable Ability choose(ServerLevel level, Mob mob, Tier tier, CreatureProfile profile, CombatState s, LivingEntity target, double dist) {
        List<Ability> options = new ArrayList<>();
        int total = 0;
        Ctx probe = new Ctx(level, mob, target, tier, profile, 1f);
        for (String id : movesetFor(mob, profile, tier)) {
            Ability a = Abilities.get(id);
            if (a == null || s.cooldowns.getInt(id) > 0 || !a.canUse(probe, dist)) continue;
            options.add(a);
            total += a.weight;
        }
        if (options.isEmpty()) return null;
        int r = mob.getRandom().nextInt(total);
        for (Ability a : options) {
            r -= a.weight;
            if (r < 0) return a;
        }
        return options.get(0);
    }

    /** Custom bosses provide their own moveset through this hook. */
    public interface MovesetProvider {
        List<String> moveset();
    }

    private static List<String> movesetFor(Mob mob, CreatureProfile profile, Tier tier) {
        if (mob instanceof MovesetProvider p) return p.moveset();
        return profile.moveset(tier);
    }

    public static void forceAbility(ServerLevel level, Mob mob, Tier tier, CreatureProfile profile, CombatState s, LivingEntity target, String id) {
        Ability a = Abilities.get(id);
        if (a == null) return;
        if (s.current != null && s.ctx != null) s.current.end(s.ctx);
        start(level, mob, tier, profile, s, target, a);
    }

    private static void start(ServerLevel level, Mob mob, Tier tier, CreatureProfile profile, CombatState s, LivingEntity target, Ability a) {
        com.mobhunter.MobHunter.LOGGER.debug("[MobHunter] {} {} uses {}", tier.getSerializedName(), mob.getType().getDescriptionId(), a.id);
        s.current = a;
        s.ctx = new Ctx(level, mob, target, tier, profile, damageMult(mob, tier, s));
        s.phase = AnimState.WINDUP;
        s.phaseTick = 0;
        mob.getNavigation().stop();
        setAnim(level, mob, a, AnimState.WINDUP, windup(a, s));
    }

    private static int windup(Ability a, CombatState s) {
        // Enraged monsters are faster, but every attack still telegraphs.
        return s.enraged ? Math.max(8, (int) (a.windup * 0.8f)) : a.windup;
    }

    private static void runPhase(ServerLevel level, Mob mob, CombatState s) {
        Ability a = s.current;
        Ctx c = s.ctx;
        if (a == null || c == null) return;
        if (c.target == null || !c.target.isAlive()) {
            if (s.phase == AnimState.WINDUP) {
                finish(level, mob, s, a);
                return;
            }
        }
        int tick = s.phaseTick++;
        switch (s.phase) {
            case AnimState.WINDUP -> {
                int len = windup(a, s);
                float progress = tick / (float) len;
                if (!c.aimLocked && c.target != null) c.aim = c.target.position();
                if (progress >= a.lockAt) c.aimLocked = true;
                if (!a.moveDuringWindup) freeze(mob);
                a.telegraph(c, tick, progress);
                if (tick + 1 >= len) {
                    s.phase = AnimState.ACTIVE;
                    s.phaseTick = 0;
                    c.aimLocked = true;
                    setAnim(level, mob, a, AnimState.ACTIVE, a.active);
                    a.begin(c);
                }
            }
            case AnimState.ACTIVE -> {
                a.activeTick(c, tick);
                if (c.finished || tick + 1 >= a.active) {
                    a.end(c);
                    s.phase = AnimState.RECOVER;
                    s.phaseTick = 0;
                    setAnim(level, mob, a, AnimState.RECOVER, a.recovery);
                }
            }
            case AnimState.RECOVER -> {
                freeze(mob);
                if (tick + 1 >= a.recovery) finish(level, mob, s, a);
            }
            default -> finish(level, mob, s, a);
        }
    }

    private static void finish(ServerLevel level, Mob mob, CombatState s, Ability a) {
        s.cooldowns.put(a.id, a.cooldown);
        Tier tier = TierManager.tier(mob);
        s.globalCooldown = (int) (tier.globalCooldown() * (s.enraged ? 0.7f : 1f)) + mob.getRandom().nextInt(15);
        s.current = null;
        s.ctx = null;
        s.phase = AnimState.IDLE_PHASE;
        mob.setData(ModAttachments.ANIM, AnimState.IDLE);
        mob.syncData(ModAttachments.ANIM);
    }

    public static void setAnim(ServerLevel level, LivingEntity mob, Ability a, int phase, int length) {
        mob.setData(ModAttachments.ANIM, new AnimState(a.pose.ordinal(), phase, level.getGameTime(), Math.max(1, length)));
        mob.syncData(ModAttachments.ANIM);
    }

    public static void setAnim(ServerLevel level, LivingEntity mob, AnimPose pose, int phase, int length) {
        mob.setData(ModAttachments.ANIM, new AnimState(pose.ordinal(), phase, level.getGameTime(), Math.max(1, length)));
        mob.syncData(ModAttachments.ANIM);
    }

    private static void freeze(Mob mob) {
        mob.getNavigation().stop();
        Vec3 v = mob.getDeltaMovement();
        mob.setDeltaMovement(v.x * 0.3, v.y, v.z * 0.3);
        mob.setZza(0);
        mob.setXxa(0);
    }

    private static boolean valid(Mob mob, @Nullable LivingEntity t, Tier tier) {
        if (t == null || !t.isAlive() || t.isRemoved() || t.level() != mob.level() || !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(t)) return false;
        double r = tier == Tier.DEITY ? 96 : tier == Tier.MONSTROUS ? 64 : 24;
        return t.distanceToSqr(mob) < r * r;
    }

    /** Points a tiered creature at a target, for both goal-based and brain-based AI. */
    public static void setTarget(Mob mob, @Nullable LivingEntity t) {
        CombatState s = mob.getData(ModAttachments.COMBAT);
        s.target = t;
        if (t == null) return;
        if (mob.getTargetUnchecked() != t) mob.setTarget(t);
        if (mob.getBrain().checkMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET,
                net.minecraft.world.entity.ai.memory.MemoryStatus.REGISTERED)) {
            mob.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET, t);
        }
    }

    private static @Nullable LivingEntity acquireTarget(ServerLevel level, Mob mob, Tier tier, CombatState s) {
        if (valid(mob, s.target, tier)) {
            if (mob.tickCount % 20 == 0) setTarget(mob, s.target);
            return s.target;
        }
        s.target = null;
        LivingEntity t = mob.getTarget();
        if (!valid(mob, t, tier)) t = mob.getLastHurtByMob();
        if (!valid(mob, t, tier)) t = null;
        if (t == null && mob.tickCount % 10 == 0) {
            // Alphas of peaceful creatures are territorial rather than hunters.
            double range = switch (tier) {
                case ALPHA -> (mob instanceof Enemy) ? 16 : 5;
                case MONSTROUS -> 40;
                case DEITY -> 64;
                default -> 0;
            };
            t = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), range, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        }
        if (t != null) setTarget(mob, t);
        return t;
    }

    private static void approach(ServerLevel level, Mob mob, LivingEntity target, double dist, CombatState s, Tier tier) {
        if (dist < 2 + mob.getBbWidth() * 0.4) return;
        boolean nativeHunter = mob instanceof Enemy && !(mob instanceof Animal);
        if (mob.tickCount % 10 == 0 && (!nativeHunter || tier.isBoss())) {
            boolean ok = mob.getNavigation().moveTo(target, tier.isBoss() ? 1.15 : 1.25);
            if (!ok) s.stuckTicks += 10;
            else s.stuckTicks = 0;
        }
        if (s.stuckTicks > 30 || (tier.isBoss() && !mob.getNavigation().isInProgress())) {
            // Huge monsters frequently have no valid path: steer straight at the hunter instead.
            mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.1);
        }
    }

    /** Monstrous creatures crash through foliage and soft blocks instead of getting stuck on them. */
    private static void breakObstacles(ServerLevel level, Mob mob) {
        if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) return;
        var box = mob.getBoundingBox().inflate(0.4, 0, 0.4);
        int broken = 0;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY + 0.5, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LEAVES) || state.is(BlockTags.REPLACEABLE) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS)
                    || state.is(BlockTags.LOGS) && mob.getBbWidth() > 3) {
                level.destroyBlock(pos, false, mob);
                if (++broken > 12) return;
            }
        }
    }

    /** Aquatic monsters out of water "swim" through the air, eerily. */
    private static void airSwim(Mob mob) {
        mob.setNoGravity(true);
        LivingEntity t = mob.getTarget();
        Vec3 v = mob.getDeltaMovement().scale(0.9);
        if (t != null) {
            Vec3 goal = t.position().add(0, 1.5, 0);
            Vec3 d = goal.subtract(mob.position());
            if (d.length() > mob.getBbWidth() + 2) v = v.add(d.normalize().scale(0.05));
            mob.getLookControl().setLookAt(t);
        } else {
            v = v.add(0, (mob.getRandom().nextFloat() - 0.5f) * 0.02, 0);
        }
        mob.setDeltaMovement(v);
    }

    private static void updateBossBar(ServerLevel level, Mob mob, CombatState s, Tier tier) {
        if (s.bossBar == null) {
            CreatureProfile p = CreatureProfiles.get(mob);
            if (p == null) return;
            s.bossBar = new ServerBossEvent(mob.getUUID(), TierManager.displayName(p, tier), tier.barColor(), BossEvent.BossBarOverlay.NOTCHED_10);
            s.bossBar.setDarkenScreen(tier == Tier.DEITY);
            s.bossBar.setPlayBossMusic(true);
        }
        s.bossBar.setProgress(mob.getHealth() / mob.getMaxHealth());
        if (mob.tickCount % 20 == 0) {
            double r = tier == Tier.DEITY ? 96 : 64;
            for (ServerPlayer p : level.players()) {
                boolean near = p.distanceToSqr(mob) < r * r && mob.isAlive();
                if (near && !s.bossBar.getPlayers().contains(p)) s.bossBar.addPlayer(p);
                else if (!near && s.bossBar.getPlayers().contains(p)) s.bossBar.removePlayer(p);
            }
        }
    }

    public static void removeBossBar(Mob mob) {
        CombatState s = mob.getExistingDataOrNull(ModAttachments.COMBAT);
        if (s != null && s.bossBar != null) {
            s.bossBar.removeAllPlayers();
            s.bossBar = null;
        }
    }

    private CombatEngine() {}
}
