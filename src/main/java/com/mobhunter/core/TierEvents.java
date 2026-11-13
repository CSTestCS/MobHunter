package com.mobhunter.core;

import com.mobhunter.MobHunter;
import com.mobhunter.ability.HunterDamage;
import com.mobhunter.gear.MaterialDrops;
import com.mobhunter.hunt.HuntManager;
import com.mobhunter.profile.CreatureProfile;
import com.mobhunter.profile.CreatureProfiles;
import com.mobhunter.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = MobHunter.MODID)
public final class TierEvents {
    private static final String PENDING_ALPHA = "mobhunter_pending_alpha";
    /** Chance that a natural group spawn includes an Alpha. */
    public static final float ALPHA_CHANCE = 0.06f;

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        EntitySpawnReason reason = event.getSpawnType();
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION && reason != EntitySpawnReason.STRUCTURE
                && reason != EntitySpawnReason.PATROL) return;
        if (mob.entityTags().contains(HunterDamage.MINION_TAG) || mob.isBaby()) return;
        if (mob instanceof EnderDragon || mob instanceof WitherBoss) return;
        if (CreatureProfiles.get(mob) == null) return;
        if (mob.getRandom().nextFloat() >= ALPHA_CHANCE) return;
        // At most one Alpha per pack: skip when one of its kind already leads a group nearby.
        boolean hasAlpha = !event.getLevel().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(40),
                m -> m.getType() == mob.getType() && TierManager.tier(m) == Tier.ALPHA).isEmpty();
        if (hasAlpha) return;
        mob.addTag(PENDING_ALPHA);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) return;
        if (mob.entityTags().contains(PENDING_ALPHA)) {
            mob.removeTag(PENDING_ALPHA);
            TierManager.applyTier(mob, Tier.ALPHA, true);
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Mob mob && !event.getLevel().isClientSide()) CombatEngine.removeBossBar(mob);
    }

    @SubscribeEvent
    public static void onTickPre(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.monster.Creeper creeper && !creeper.level().isClientSide()
                && TierManager.tier(creeper) != Tier.NONE) {
            creeper.setSwellDir(-1);
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (!(e instanceof Mob mob) || !(e.level() instanceof ServerLevel level)) return;
        Tier tier = mob.getExistingDataOrNull(ModAttachments.TIER);
        if (tier == null || tier == Tier.NONE) return;
        CombatEngine.tick(level, mob, tier);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        // Tiered creatures never use plain run-and-melee attacks: all their damage comes from telegraphed moves.
        if (attacker instanceof Mob mob && event.getSource().getDirectEntity() == attacker && !HunterDamage.ABILITY_DAMAGE.get()
                && TierManager.tier(mob) != Tier.NONE && !(mob instanceof com.mobhunter.deity.DeityBoss)) {
            event.setCanceled(true);
            return;
        }
        LivingEntity victim = event.getEntity();
        // Tiered creatures are tougher against their own element.
        Tier vt = TierManager.tier(victim);
        if (vt != Tier.NONE) {
            CreatureProfile p = CreatureProfiles.get(victim.getType());
            if (p != null && p.element().matches(event.getSource())) event.setAmount(event.getAmount() * 0.25f);
            CombatState s = victim.getExistingDataOrNull(ModAttachments.COMBAT);
            if (s != null && s.entrance != null) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        Tier tier = TierManager.tier(e);
        if (tier == Tier.NONE || !(e.level() instanceof ServerLevel level)) return;
        CreatureProfile profile = CreatureProfiles.get(e.getType());
        if (profile == null) return;
        MaterialDrops.addDrops(level, e, profile, tier, event.getDrops(), event.getSource());
    }

    @SubscribeEvent
    public static void onXp(LivingExperienceDropEvent event) {
        Tier tier = TierManager.tier(event.getEntity());
        if (tier == Tier.NONE) return;
        int bonus = switch (tier) {
            case ALPHA -> 20;
            case MONSTROUS -> 250;
            case DEITY -> 1200;
            default -> 0;
        };
        event.setDroppedExperience(event.getDroppedExperience() + bonus);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level) || !(e instanceof Mob mob)) return;
        Tier tier = TierManager.tier(mob);
        if (!tier.isBoss()) return;
        CombatEngine.removeBossBar(mob);
        Component name = mob.getDisplayName();
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(mob) < 128 * 128) {
                p.sendSystemMessage(Component.translatable("mobhunter.hunt.slain", name).withStyle(tier.color));
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
            }
        }
        HuntManager.onBossSlain(level, mob, tier);
    }

    private TierEvents() {}
}
