package com.mobhunter.hunt;

import net.minecraft.world.entity.EntityTypes;
import com.mobhunter.MobHunter;
import com.mobhunter.core.CombatState;
import com.mobhunter.core.HuntState;
import com.mobhunter.core.Tier;
import com.mobhunter.core.TierManager;
import com.mobhunter.deity.DeityBoss;
import com.mobhunter.profile.Bestiary;
import com.mobhunter.profile.CreatureProfile;
import com.mobhunter.profile.CreatureProfiles;
import com.mobhunter.registry.ModAttachments;
import com.mobhunter.registry.ModBlocks;
import com.mobhunter.registry.ModEntities;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * Runs hunts: scoutfly guidance, clue generation and investigation, the final arrival of the
 * Monstrous creature, and the shrine of a Deity.
 */
@EventBusSubscriber(modid = MobHunter.MODID)
public final class HuntManager {
    private static final DustParticleOptions SCOUTFLY = new DustParticleOptions(0x7CFF6B, 1.1f);
    private static final String PREY_TAG = "mobhunter_prey";
    private static final int MATERIALIZE_RANGE = 56;

    public static HuntState hunt(ServerPlayer p) {
        return p.getData(ModAttachments.HUNT);
    }

    private static void set(ServerPlayer p, HuntState s) {
        p.setData(ModAttachments.HUNT, s);
    }

    public static boolean start(ServerPlayer player, ContractData data) {
        if (hunt(player).active()) {
            player.sendSystemMessage(Component.translatable("mobhunter.hunt.already").withStyle(ChatFormatting.RED));
            return false;
        }
        CreatureProfile p = CreatureProfiles.get(data.target());
        if (p == null) return false;
        ServerLevel level = player.level();
        BlockPos clue = pickClueXZ(level.getRandom(), player.blockPosition(), 36, 56);
        set(player, new HuntState(true, data.target(), data.tier(), 1, clue, level.dimension(), false, Optional.empty()));
        Component quarry = data.tier() == Tier.DEITY ? Component.literal(p.epithet()) : TierManager.displayName(p, data.tier());
        player.sendSystemMessage(Component.translatable("mobhunter.hunt.start", quarry).withStyle(data.tier().color));
        player.sendSystemMessage(Component.translatable("mobhunter.hunt.scoutflies").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.8f);
        return true;
    }

    public static void abandon(ServerPlayer player) {
        HuntState s = hunt(player);
        if (s.materialized() && player.level().dimension().equals(s.dimension())) clearClues(player.level(), s.cluePos());
        set(player, HuntState.NONE);
        player.sendSystemMessage(Component.translatable("mobhunter.hunt.abandoned").withStyle(ChatFormatting.GRAY));
    }

    private static BlockPos pickClueXZ(RandomSource r, BlockPos from, int min, int max) {
        double a = r.nextDouble() * Math.PI * 2;
        double d = min + r.nextDouble() * (max - min);
        return new BlockPos(from.getX() + (int) (Math.cos(a) * d), from.getY(), from.getZ() + (int) (Math.sin(a) * d));
    }

    /** Finds solid footing near (x, z). Works in the overworld, the end and in roofed dimensions. */
    public static BlockPos surface(ServerLevel level, BlockPos approx) {
        int x = approx.getX(), z = approx.getZ();
        if (!level.dimensionType().hasCeiling()) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos p = new BlockPos(x, y, z);
            while (p.getY() > level.getMinY() + 1 && (level.getBlockState(p.below()).is(net.minecraft.tags.BlockTags.LEAVES) || level.getBlockState(p.below()).isAir())) p = p.below();
            return p;
        }
        for (int dy = 12; dy > -40; dy--) {
            BlockPos p = new BlockPos(x, approx.getY() + dy, z);
            if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && level.getBlockState(p.below()).isSolidRender()) return p;
        }
        return approx;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 5 != 0) return;
        HuntState s = hunt(player);
        if (!s.active() || !player.level().dimension().equals(s.dimension())) return;
        ServerLevel level = player.level();
        BlockPos clue = s.cluePos();
        double dx = clue.getX() + 0.5 - player.getX(), dz = clue.getZ() + 0.5 - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (!s.materialized() && dist < MATERIALIZE_RANGE && level.isLoaded(clue)) {
            materialize(level, player, s);
            s = hunt(player);
            clue = s.cluePos();
        }
        // Scoutflies: a trail of glowing insects drifting toward the next clue.
        Vec3 eye = player.getEyePosition();
        Vec3 target = Vec3.atCenterOf(clue).add(0, s.materialized() ? 0.2 : 0, 0);
        if (!s.materialized()) target = new Vec3(target.x, eye.y - 0.5, target.z);
        Vec3 dir = target.subtract(eye);
        double len = dir.length();
        if (len > 0.1) {
            dir = dir.normalize();
            int n = (int) Math.min(len, 10);
            float phase = (player.tickCount % 40) / 40f;
            for (int i = 0; i < n; i++) {
                double t = 1.2 + i * 0.9 + phase * 0.9;
                if (t > len) break;
                Vec3 p = eye.add(dir.scale(t)).add(Mth.sin((float) (t * 2 + player.tickCount * 0.1)) * 0.15, -0.4 + Mth.cos((float) t) * 0.1, 0);
                level.sendParticles(player, SCOUTFLY, true, false, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
            }
        }
        if (s.materialized() && dist < 8 && player.tickCount % 10 == 0) {
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4 + player.tickCount * 0.05;
                level.sendParticles(player, SCOUTFLY, true, false, clue.getX() + 0.5 + Math.cos(a) * 1.2, clue.getY() + 0.6, clue.getZ() + 0.5 + Math.sin(a) * 1.2, 1, 0, 0, 0, 0);
            }
        }
        if (player.tickCount % 20 == 0) {
            CreatureProfile p = CreatureProfiles.get(s.target());
            Component quarry = p == null ? Component.empty() : s.tier() == Tier.DEITY ? Component.literal(p.epithet()) : TierManager.displayName(p, s.tier());
            String stageKey = s.stage() >= 3 && s.tier() == Tier.DEITY ? "mobhunter.hunt.bar_shrine" : "mobhunter.hunt.bar";
            player.sendOverlayMessage(Component.translatable(stageKey, quarry, s.stage(), (int) dist, compass(dx, dz)).withStyle(ChatFormatting.GREEN));
        }
    }

    private static String compass(double dx, double dz) {
        double ang = Math.toDegrees(Math.atan2(dx, -dz));
        if (ang < 0) ang += 360;
        String[] dirs = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return dirs[(int) Math.round(ang / 45) % 8];
    }

    // ---------------------------------------------------------------------------------- clues

    private static void materialize(ServerLevel level, ServerPlayer player, HuntState s) {
        CreatureProfile profile = CreatureProfiles.get(s.target());
        if (profile == null) return;
        BlockPos pos = surface(level, s.cluePos());
        RandomSource r = level.getRandom();
        if (s.stage() >= 3 && s.tier() == Tier.DEITY) {
            BlockPos altar = ShrineBuilder.build(level, pos, profile);
            set(player, s.materialize(altar).withShrine(altar));
            player.sendSystemMessage(Component.translatable("mobhunter.hunt.shrine_seen").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            return;
        }
        switch (s.stage()) {
            case 1 -> {
                if (r.nextBoolean()) {
                    // A line of tracks leading somewhere.
                    double a = r.nextDouble() * Math.PI * 2;
                    for (int i = -3; i <= 3; i++) {
                        BlockPos t = surface(level, pos.offset((int) Math.round(Math.cos(a) * i * 1.6), 0, (int) Math.round(Math.sin(a) * i * 1.6)));
                        place(level, t, ModBlocks.CLUE_TRACKS.get().defaultBlockState());
                    }
                    place(level, pos, ModBlocks.CLUE_TRACKS.get().defaultBlockState());
                } else {
                    place(level, pos, ModBlocks.CLUE_SHED.get().defaultBlockState());
                    scatter(level, pos, 3, ModBlocks.CLUE_TRACKS.get().defaultBlockState(), 2);
                }
            }
            case 2 -> {
                place(level, pos, ModBlocks.CLUE_REMAINS.get().defaultBlockState());
                scatter(level, pos, 4, ModBlocks.GORE.get().defaultBlockState(), 4);
                scatter(level, pos, 4, Blocks.BONE_BLOCK.defaultBlockState(), 1);
            }
            default -> {
                place(level, pos, ModBlocks.CLUE_OMINOUS.get().defaultBlockState());
                scatter(level, pos, 5, ModBlocks.GORE.get().defaultBlockState(), 7);
                scatter(level, pos, 5, Blocks.BONE_BLOCK.defaultBlockState(), 3);
                scatter(level, pos, 5, Blocks.SKELETON_SKULL.defaultBlockState(), 2);
                spawnPrey(level, pos, profile);
            }
        }
        set(player, s.materialize(pos));
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        BlockState there = level.getBlockState(pos);
        if (there.isAir() || there.canBeReplaced()) level.setBlock(pos, state, Block.UPDATE_ALL);
    }

    private static void scatter(ServerLevel level, BlockPos c, int radius, BlockState state, int count) {
        RandomSource r = level.getRandom();
        for (int i = 0; i < count; i++) {
            BlockPos p = surface(level, c.offset(r.nextInt(radius * 2 + 1) - radius, 0, r.nextInt(radius * 2 + 1) - radius));
            if (!p.equals(c)) place(level, p, state);
        }
    }

    private static void clearClues(ServerLevel level, BlockPos c) {
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-8, -4, -8), c.offset(8, 4, 8))) {
            if (level.getBlockState(p).getBlock() instanceof ClueBlock) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** The prey the monster is stalking: frozen in fear until the hunter disturbs the scene. */
    private static void spawnPrey(ServerLevel level, BlockPos pos, CreatureProfile profile) {
        EntityType<?>[] prey = switch (profile.archetype()) {
            case AQUATIC -> new EntityType<?>[]{EntityTypes.COD, EntityTypes.SALMON};
            case UNDEAD, ILLAGER, SKELETAL -> new EntityType<?>[]{EntityTypes.SHEEP, EntityTypes.PIG, EntityTypes.COW};
            case INFERNAL -> new EntityType<?>[]{EntityTypes.HOGLIN, EntityTypes.PIG};
            default -> new EntityType<?>[]{EntityTypes.SHEEP, EntityTypes.COW, EntityTypes.PIG, EntityTypes.CHICKEN, EntityTypes.RABBIT};
        };
        RandomSource r = level.getRandom();
        for (int i = 0; i < 3 + r.nextInt(3); i++) {
            EntityType<?> t = prey[r.nextInt(prey.length)];
            if (!(t.create(level, EntitySpawnReason.EVENT) instanceof Mob m)) continue;
            BlockPos at = surface(level, pos.offset(r.nextInt(9) - 4, 0, r.nextInt(9) - 4));
            m.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, r.nextFloat() * 360, 0);
            m.setNoAi(true);
            m.addTag(PREY_TAG);
            m.setPersistenceRequired();
            level.addFreshEntity(m);
        }
    }

    private static void releasePrey(ServerLevel level, BlockPos pos) {
        for (Mob m : level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(pos).inflate(12), e -> e.entityTags().contains(PREY_TAG))) {
            m.setNoAi(false);
            m.removeTag(PREY_TAG);
            m.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 1));
        }
    }

    /** Player right-clicked a clue block. */
    public static boolean investigate(ServerPlayer player, BlockPos pos) {
        HuntState s = hunt(player);
        ServerLevel level = player.level();
        if (!s.active() || !s.materialized() || !level.dimension().equals(s.dimension()) || s.cluePos().distSqr(pos) > 64) {
            player.sendOverlayMessage(Component.translatable("mobhunter.hunt.not_yours").withStyle(ChatFormatting.GRAY));
            return false;
        }
        CreatureProfile profile = CreatureProfiles.get(s.target());
        if (profile == null) return false;
        int textStage = s.stage() == 1 ? (level.getBlockState(s.cluePos()).is(ModBlocks.CLUE_SHED.get()) ? 2 : 1) : s.stage() + 1;
        player.sendSystemMessage(Component.literal(profile.archetype().clueText(textStage)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        level.sendParticles(SCOUTFLY, true, false, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.6, 0.4, 0.6, 0.05);
        level.playSound(null, pos, SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 1f, 0.8f);
        clearClues(level, s.cluePos());

        if (s.stage() < 3) {
            int stage = s.stage() + 1;
            boolean deityShrine = stage == 3 && s.tier() == Tier.DEITY;
            BlockPos next = pickClueXZ(level.getRandom(), pos, deityShrine ? 80 : 44, deityShrine ? 110 : 66);
            set(player, s.withClue(stage, next, false));
            player.sendSystemMessage(Component.translatable(deityShrine ? "mobhunter.hunt.next_shrine" : "mobhunter.hunt.next_clue", stage)
                    .withStyle(deityShrine ? ChatFormatting.DARK_PURPLE : ChatFormatting.GREEN));
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.4f);
            return true;
        }
        // Final clue of a Monstrous hunt: the creature arrives.
        releasePrey(level, pos);
        set(player, HuntState.NONE);
        player.sendSystemMessage(Component.translatable("mobhunter.hunt.arrives").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        Vec3 dir = Vec3.directionFromRotation(0, player.getYRot());
        BlockPos arrival = surface(level, BlockPos.containing(Vec3.atCenterOf(pos).add(dir.scale(14))));
        summon(level, profile, s.tier(), arrival, player);
        return true;
    }

    /** Player disturbed a Deity shrine. */
    public static boolean disturbShrine(ServerPlayer player, BlockPos pos) {
        HuntState s = hunt(player);
        ServerLevel level = player.level();
        if (!s.active() || s.tier() != Tier.DEITY || s.shrinePos().isEmpty() || s.shrinePos().get().distSqr(pos) > 18 * 18) {
            player.sendOverlayMessage(Component.translatable("mobhunter.hunt.shrine_dormant").withStyle(ChatFormatting.DARK_PURPLE));
            return false;
        }
        CreatureProfile profile = CreatureProfiles.get(s.target());
        if (profile == null) return false;
        set(player, HuntState.NONE);
        player.sendSystemMessage(Component.translatable("mobhunter.hunt.deity_awakens", profile.epithet()).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        Bestiary.Entry entry = profile.entry();
        if (entry != null) player.sendSystemMessage(Component.translatable("mobhunter.hunt.deity_incarnation", profile.epithet(), TierManager.creatureName(profile),
                Component.literal(entry.elementName())).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        summon(level, profile, Tier.DEITY, s.shrinePos().get(), player);
        return true;
    }

    /** Creates the hunted creature and starts its entrance. Also used by /mobhunter summon. */
    public static @Nullable Mob summon(ServerLevel level, CreatureProfile profile, Tier tier, BlockPos at, @Nullable ServerPlayer hunter) {
        Mob mob = null;
        EntranceController.Style style;
        if (tier == Tier.DEITY && profile.id().getPath().equals("sheep")) {
            mob = ModEntities.RAM_GOD.get().create(level, EntitySpawnReason.EVENT);
            style = EntranceController.Style.RAM_GOD;
        } else if (tier == Tier.DEITY && profile.id().getPath().equals("zombified_piglin")) {
            mob = ModEntities.GIANT_ZOMBIE_PIGLIN.get().create(level, EntitySpawnReason.EVENT);
            style = EntranceController.Style.GIANT_PIGLIN;
        } else {
            EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(profile.id());
            if (type.create(level, EntitySpawnReason.EVENT) instanceof Mob m) mob = m;
            style = tier == Tier.DEITY ? EntranceController.Style.DEITY : EntranceController.Style.MONSTROUS;
        }
        if (mob == null) return null;
        mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, hunter == null ? 0 : hunter.getYRot() + 180, 0);
        if (mob instanceof DeityBoss boss) boss.setHome(at);
        level.addFreshEntity(mob);
        TierManager.applyTier(mob, tier, true);
        CombatState cs = mob.getData(ModAttachments.COMBAT);
        cs.hunter = hunter == null ? null : hunter.getUUID();
        cs.entrance = new EntranceController(style, profile.entrance(), at, hunter == null ? null : hunter.getUUID());
        cs.entrance.begin(level, mob);
        return mob;
    }

    public static void onBossSlain(ServerLevel level, Mob mob, Tier tier) {
        CombatState s = mob.getExistingDataOrNull(ModAttachments.COMBAT);
        if (s == null || s.hunter == null) return;
        if (level.getPlayerByUUID(s.hunter) instanceof ServerPlayer p) {
            p.sendSystemMessage(Component.translatable("mobhunter.hunt.complete").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            p.giveExperienceLevels(tier == Tier.DEITY ? 10 : 3);
        }
        if (mob.getBlockStateOn().getBlock() instanceof ShrineAltarBlock || mob instanceof DeityBoss) {
            level.sendParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + 2, mob.getZ(), 80, 2, 3, 2, 0.1);
        }
    }

    private HuntManager() {}
}
