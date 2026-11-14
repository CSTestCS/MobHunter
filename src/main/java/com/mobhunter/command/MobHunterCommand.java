package com.mobhunter.command;

import com.mobhunter.MobHunter;
import com.mobhunter.core.Tier;
import com.mobhunter.core.TierManager;
import com.mobhunter.hunt.ContractData;
import com.mobhunter.hunt.HuntContractItem;
import com.mobhunter.hunt.HuntManager;
import com.mobhunter.profile.CreatureProfile;
import com.mobhunter.profile.CreatureProfiles;
import com.mobhunter.registry.ModItems;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /mobhunter summon &lt;creature&gt; &lt;tier&gt;  - spawns a tiered creature (with its entrance)
 * /mobhunter contract &lt;creature&gt; &lt;tier&gt; - gives a hunt contract
 */
@EventBusSubscriber(modid = MobHunter.MODID)
public final class MobHunterCommand {

    private static CompletableFuture<Suggestions> creatures(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggest(CreatureProfiles.all().stream().map(p -> p.id().getPath()), b);
    }

    private static CompletableFuture<Suggestions> tiers(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggest(new String[]{"alpha", "monstrous", "deity"}, b);
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mobhunter")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("summon")
                        .then(Commands.argument("creature", StringArgumentType.word()).suggests(MobHunterCommand::creatures)
                                .then(Commands.argument("tier", StringArgumentType.word()).suggests(MobHunterCommand::tiers)
                                        .executes(ctx -> summon(ctx, StringArgumentType.getString(ctx, "creature"), StringArgumentType.getString(ctx, "tier"))))))
                .then(Commands.literal("contract")
                        .then(Commands.argument("creature", StringArgumentType.word()).suggests(MobHunterCommand::creatures)
                                .then(Commands.argument("tier", StringArgumentType.word()).suggests(MobHunterCommand::tiers)
                                        .executes(ctx -> contract(ctx, StringArgumentType.getString(ctx, "creature"), StringArgumentType.getString(ctx, "tier"))))))
                .then(Commands.literal("selftest").then(Commands.argument("tier", StringArgumentType.word()).suggests(MobHunterCommand::tiers)
                        .executes(ctx -> selftest(ctx, Tier.byName(StringArgumentType.getString(ctx, "tier")), null))
                        .then(Commands.argument("creature", StringArgumentType.word()).suggests(MobHunterCommand::creatures)
                                .executes(ctx -> selftest(ctx, Tier.byName(StringArgumentType.getString(ctx, "tier")), StringArgumentType.getString(ctx, "creature"))))))
                .then(Commands.literal("gearstand")
                        .then(Commands.argument("creature", StringArgumentType.word()).suggests(MobHunterCommand::creatures)
                                .then(Commands.argument("tier", StringArgumentType.word()).suggests(MobHunterCommand::tiers)
                                        .executes(ctx -> gearStand(ctx, StringArgumentType.getString(ctx, "creature"), StringArgumentType.getString(ctx, "tier"))))))
                .then(Commands.literal("huntstep").executes(MobHunterCommand::huntStep))
                .then(Commands.literal("abandon").executes(ctx -> {
                    HuntManager.abandon(ctx.getSource().getPlayerOrException());
                    return 1;
                })));
    }

    private static CreatureProfile profile(String name) {
        Identifier id = name.contains(":") ? Identifier.parse(name) : Identifier.withDefaultNamespace(name);
        return CreatureProfiles.get(id);
    }

    private static int summon(CommandContext<CommandSourceStack> ctx, String creature, String tierName) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CreatureProfile p = profile(creature);
        Tier tier = Tier.byName(tierName);
        if (p == null || tier == Tier.NONE) {
            ctx.getSource().sendFailure(Component.literal("Unknown creature or tier"));
            return 0;
        }
        var look = player.getLookAngle();
        var at = HuntManager.surface(player.level(), net.minecraft.core.BlockPos.containing(player.position().add(look.x * 12, 0, look.z * 12)));
        if (tier == Tier.ALPHA) {
            var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(p.id());
            if (type.create(player.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND) instanceof Mob mob) {
                mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
                player.level().addFreshEntity(mob);
                TierManager.applyTier(mob, Tier.ALPHA, true);
            }
        } else {
            HuntManager.summon(player.level(), p, tier, at, player);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Summoned ").append(TierManager.displayName(p, tier)), true);
        return 1;
    }

    private static int contract(CommandContext<CommandSourceStack> ctx, String creature, String tierName) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CreatureProfile p = profile(creature);
        Tier tier = Tier.byName(tierName);
        if (p == null || !tier.isBoss()) {
            ctx.getSource().sendFailure(Component.literal("Contracts exist for monstrous and deity tiers"));
            return 0;
        }
        player.getInventory().add(HuntContractItem.create(ModItems.HUNT_CONTRACT.get(), p, tier));
        return 1;
    }

    /** Spawns every creature at a tier next to a sturdy dummy target to exercise every moveset. */
    private static int selftest(CommandContext<CommandSourceStack> ctx, Tier tier, String only) {
        var level = ctx.getSource().getLevel();
        var origin = net.minecraft.core.BlockPos.containing(ctx.getSource().getPosition());
        int i = 0;
        for (CreatureProfile p : CreatureProfiles.all()) {
            if (only != null && !p.id().getPath().equals(only)) continue;
            if (only == null && tier == Tier.DEITY && i >= 6) break;
            int spacing = tier == Tier.ALPHA ? 14 : tier == Tier.MONSTROUS ? 22 : 48;
            var at = HuntManager.surface(level, origin.offset((i % 12) * spacing, 0, (i / 12) * spacing));
            i++;
            Mob mob;
            if (tier == Tier.ALPHA) {
                var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(p.id());
                if (!(type.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND) instanceof Mob m)) continue;
                m.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
                level.addFreshEntity(m);
                TierManager.applyTier(m, Tier.ALPHA, true);
                mob = m;
            } else {
                mob = HuntManager.summon(level, p, tier, at, null);
            }
            if (mob == null) continue;
            var dummy = net.minecraft.world.entity.EntityTypes.IRON_GOLEM.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (dummy == null) continue;
            dummy.snapTo(at.getX() + 6.5, at.getY(), at.getZ() + 0.5, 0, 0);
            dummy.setNoAi(true);
            dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
            dummy.setHealth(1000);
            level.addFreshEntity(dummy);
            dummy.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.RESISTANCE, 100000, 3));
            dummy.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 100000, 4));
            com.mobhunter.core.CombatEngine.setTarget(mob, dummy);
        }
        int n = i;
        ctx.getSource().sendSuccess(() -> Component.literal("Selftest spawned " + n + " creatures"), true);
        return n;
    }

    /** Debug helper: walks the executing player through their hunt (teleport to clue, then investigate it). */
    private static int huntStep(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        var hunt = HuntManager.hunt(player);
        if (!hunt.active()) {
            // Read the first contract in the inventory, as if the player had used it.
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                var stack = player.getInventory().getItem(i);
                var data = stack.get(com.mobhunter.registry.ModComponents.CONTRACT.get());
                if (data != null && HuntManager.start(player, data)) {
                    stack.shrink(1);
                    return 1;
                }
            }
            ctx.getSource().sendFailure(Component.literal("No active hunt"));
            return 0;
        }
        var clue = hunt.cluePos();
        if (!hunt.materialized()) {
            var ground = HuntManager.surface(player.level(), clue);
            player.teleportTo(ground.getX() + 0.5, ground.getY() + 6, ground.getZ() - 9.5);
            ctx.getSource().sendSuccess(() -> Component.literal("Moved to clue " + hunt.stage()), false);
            return 1;
        }
        boolean ok = hunt.tier() == Tier.DEITY && hunt.stage() >= 3 && hunt.shrinePos().isPresent()
                ? HuntManager.disturbShrine(player, hunt.shrinePos().get())
                : HuntManager.investigate(player, clue);
        ctx.getSource().sendSuccess(() -> Component.literal("Investigated: " + ok), false);
        return ok ? 1 : 0;
    }

    /** Debug helper: a mannequin wearing a creature's full Plate set, with its Blade, Spear and materials floating beside it. */
    private static int gearStand(CommandContext<CommandSourceStack> ctx, String creature, String tierName) {
        var level = ctx.getSource().getLevel();
        CreatureProfile p = profile(creature);
        Tier tier = Tier.byName(tierName);
        if (p == null || tier == Tier.NONE) return 0;
        var src = new com.mobhunter.gear.MonsterSource(p.id(), tier);
        var pos = ctx.getSource().getPosition();
        var stand = net.minecraft.world.entity.EntityTypes.MANNEQUIN.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (stand == null) return 0;
        stand.snapTo(pos.x, pos.y, pos.z, 0, 0);
        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, com.mobhunter.gear.GearStats.forge(ModItems.HUNTER_HELMET.get(), com.mobhunter.gear.GearKind.HELMET, src));
        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, com.mobhunter.gear.GearStats.forge(ModItems.HUNTER_CHESTPLATE.get(), com.mobhunter.gear.GearKind.CHESTPLATE, src));
        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, com.mobhunter.gear.GearStats.forge(ModItems.HUNTER_LEGGINGS.get(), com.mobhunter.gear.GearKind.LEGGINGS, src));
        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, com.mobhunter.gear.GearStats.forge(ModItems.HUNTER_BOOTS.get(), com.mobhunter.gear.GearKind.BOOTS, src));
        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, com.mobhunter.gear.GearStats.forge(ModItems.HUNTER_BLADE.get(), com.mobhunter.gear.GearKind.BLADE, src));
        stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, com.mobhunter.gear.GearStats.forge(ModItems.HUNTER_SPEAR.get(), com.mobhunter.gear.GearKind.SPEAR, src));
        level.addFreshEntity(stand);
        net.minecraft.world.item.ItemStack[] shown = {
                com.mobhunter.gear.GearStats.forge(ModItems.HUNTER_BLADE.get(), com.mobhunter.gear.GearKind.BLADE, src),
                com.mobhunter.gear.GearStats.forge(ModItems.HUNTER_SPEAR.get(), com.mobhunter.gear.GearKind.SPEAR, src),
                com.mobhunter.gear.GearStats.forge(ModItems.HUNTER_HELMET.get(), com.mobhunter.gear.GearKind.HELMET, src),
                com.mobhunter.gear.MonsterMaterialItem.create(ModItems.MONSTER_HIDE.get(), p, tier, 1),
                com.mobhunter.gear.MonsterMaterialItem.create(ModItems.MONSTER_CLAW.get(), p, tier, 1),
                com.mobhunter.gear.MonsterMaterialItem.create(ModItems.MONSTER_GEM.get(), p, tier, 1)};
        for (int i = 0; i < shown.length; i++) {
            var item = new net.minecraft.world.entity.item.ItemEntity(level, pos.x - 1.5 + i * 0.6, pos.y + 2.4, pos.z - 0.8, shown[i]);
            item.setNeverPickUp();
            item.setUnlimitedLifetime();
            item.setNoGravity(true);
            item.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            level.addFreshEntity(item);
        }
        return 1;
    }

    private MobHunterCommand() {}
}
