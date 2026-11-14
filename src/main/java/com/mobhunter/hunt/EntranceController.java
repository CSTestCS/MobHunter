package com.mobhunter.hunt;

import com.mobhunter.ability.Families;
import com.mobhunter.ability.HunterDamage;
import com.mobhunter.core.AnimPose;
import com.mobhunter.core.AnimState;
import com.mobhunter.core.CombatEngine;
import com.mobhunter.deity.DeityBoss;
import com.mobhunter.profile.Entrance;
import com.mobhunter.registry.ModBlocks;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Choreographs the dramatic arrival of a hunted creature. While it runs the creature has no AI and
 * cannot be harmed; when it finishes the creature roars and the fight begins.
 */
public final class EntranceController {
    public enum Style { MONSTROUS, DEITY, RAM_GOD, GIANT_PIGLIN }

    private final Style style;
    private final Entrance entrance;
    private final BlockPos at;
    private final @Nullable UUID hunter;
    private int tick;
    private final int duration;
    private Vec3 start = Vec3.ZERO;
    private Vec3 end = Vec3.ZERO;
    private boolean wasNoGravity;

    public EntranceController(Style style, Entrance entrance, BlockPos at, @Nullable UUID hunter) {
        this.style = style;
        this.entrance = entrance;
        this.at = at;
        this.hunter = hunter;
        this.duration = switch (style) {
            case MONSTROUS -> 110;
            case DEITY -> 220;
            case RAM_GOD -> 240;
            case GIANT_PIGLIN -> 260;
        };
    }

    public void begin(ServerLevel level, Mob mob) {
        end = mob.position();
        wasNoGravity = mob.isNoGravity();
        mob.setNoAi(true);
        mob.setNoGravity(true);
        float h = mob.getBbHeight();
        start = switch (style) {
            case MONSTROUS -> switch (entrance) {
                case BURROW, GEYSER, INFERNO -> end.subtract(0, h + 1, 0);
                case SKY_DIVE -> end.add(0, 48, 0);
                case CRASH_LANDING -> end.add(mob.getLookAngle().scale(-34)).add(0, 18, 0);
                default -> end;
            };
            case DEITY -> mob instanceof com.mobhunter.deity.EldritchDeity d && d.form().movement() == com.mobhunter.deity.form.DeityForm.Movement.ANCHORED
                    ? end.subtract(0, h + 2, 0) : end.add(0, 40, 0);
            case RAM_GOD -> end.add(0, 14, 0);
            case GIANT_PIGLIN -> end.subtract(0, h + 2, 0);
        };
        if (style == Style.RAM_GOD) end = end.add(0, 14, 0);
        mob.setPos(start);
        boolean hidden = style != Style.GIANT_PIGLIN && !(style == Style.MONSTROUS && (entrance == Entrance.BURROW || entrance == Entrance.GEYSER || entrance == Entrance.INFERNO));
        mob.setInvisible(hidden);
        if (style == Style.DEITY || style == Style.RAM_GOD) {
            level.getServer().setWeatherParameters(0, 6000, true, true);
        }
    }

    private void sound(ServerLevel level, SoundEvent s, float vol, float pitch) {
        level.playSound(null, at, s, SoundSource.HOSTILE, vol, pitch);
    }

    private void ring(ServerLevel level, Vec3 c, double r, ParticleOptions p, int n) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n + tick * 0.05;
            level.sendParticles(p, true, false, c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0.02);
        }
    }

    private void groundDust(ServerLevel level, Vec3 c, int count, double spread) {
        BlockState below = level.getBlockState(BlockPos.containing(c.x, c.y - 0.5, c.z));
        if (below.isAir()) below = Blocks.DIRT.defaultBlockState();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), true, false, c.x, c.y + 0.2, c.z, count, spread, 0.3, spread, 0.3);
    }

    private void pulseDarkness(ServerLevel level, int radius) {
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().distSqr(at) < radius * radius) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
        }
    }

    /** @return true when the entrance is over. */
    public boolean tick(ServerLevel level, Mob mob) {
        int t = tick++;
        float p = t / (float) duration;
        Vec3 c = Vec3.atBottomCenterOf(at);
        switch (style) {
            case MONSTROUS -> monstrous(level, mob, t, p, c);
            case DEITY -> deity(level, mob, t, p, c);
            case RAM_GOD -> ramGod(level, mob, t, p, c);
            case GIANT_PIGLIN -> giant(level, mob, t, p, c);
        }
        if (tick >= duration) {
            finish(level, mob);
            return true;
        }
        return false;
    }

    private void lerp(Mob mob, float f) {
        f = Math.max(0, Math.min(1, f));
        mob.setPos(start.lerp(end, f));
        mob.setDeltaMovement(Vec3.ZERO);
    }

    private void monstrous(ServerLevel level, Mob mob, int t, float p, Vec3 c) {
        double r = Math.max(3, mob.getBbWidth());
        switch (entrance) {
            case BURROW -> {
                if (t < 50) {
                    groundDust(level, c, 12, r * 0.4);
                    if (t % 8 == 0) sound(level, SoundEvents.WARDEN_DIG, 2f, 0.7f);
                    if (t % 10 == 0) ring(level, c, r * (t / 50f), new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), 20);
                } else {
                    if (t == 50) CombatEngine.setAnim(level, mob, AnimPose.EMERGE, AnimState.ACTIVE, duration - 50);
                    lerp(mob, (t - 50) / (float) (duration - 60));
                    groundDust(level, c, 20, r * 0.5);
                    if (t % 6 == 0) sound(level, SoundEvents.WARDEN_EMERGE, 2f, 0.6f);
                }
            }
            case SKY_DIVE -> {
                if (t < 60) {
                    ring(level, c, r * (1.2 - t / 60.0), new net.minecraft.core.particles.DustParticleOptions(0x202020, 2f), 24);
                    if (t % 15 == 0) sound(level, SoundEvents.PHANTOM_SWOOP, 3f, 0.5f);
                } else {
                    if (t == 60) mob.setInvisible(false);
                    lerp(mob, (t - 60) / 20f);
                    if (t == 80) impact(level, mob, c, r * 2);
                }
            }
            case CRASH_LANDING -> {
                if (t < 40) {
                    if (t % 20 == 0) sound(level, SoundEvents.RAVAGER_ROAR, 4f, 0.5f);
                    ring(level, c, r * 1.5, new net.minecraft.core.particles.DustParticleOptions(0xFF3020, 1.6f), 24);
                } else if (t < 70) {
                    if (t == 40) mob.setInvisible(false);
                    float f = (t - 40) / 30f;
                    Vec3 pos = start.lerp(end, f).add(0, Math.sin(f * Math.PI) * 14, 0);
                    mob.setPos(pos);
                } else if (t == 70) impact(level, mob, c, r * 2.2);
            }
            case GEYSER -> {
                if (t < 50) {
                    level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, true, false, c.x, c.y, c.z, 20, r * 0.4, 0.2, r * 0.4, 0.2);
                    level.sendParticles(ParticleTypes.SPLASH, true, false, c.x, c.y + 0.5, c.z, 20, r * 0.5, 0.2, r * 0.5, 0.3);
                    if (t % 10 == 0) sound(level, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 2f, 0.6f);
                } else {
                    if (t == 50) {
                        sound(level, SoundEvents.GENERIC_SPLASH, 3f, 0.5f);
                        end = end.add(0, 4, 0);
                    }
                    for (int i = 0; i < 6; i++)
                        level.sendParticles(ParticleTypes.FALLING_WATER, true, false, c.x, c.y + i * 2, c.z, 10, r * 0.4, 1, r * 0.4, 0);
                    lerp(mob, (t - 50) / 25f);
                    if (t == 90) mob.setNoGravity(false);
                }
            }
            case RIFT -> {
                for (int i = 0; i < 30; i++) {
                    double a = i * Math.PI / 15;
                    double rr = r * Math.min(1, t / 40.0);
                    Vec3 right = new Vec3(Math.cos(Math.toRadians(mob.getYRot())), 0, Math.sin(Math.toRadians(mob.getYRot())));
                    Vec3 pt = c.add(right.scale(Math.cos(a) * rr)).add(0, mob.getBbHeight() * 0.5 + Math.sin(a) * rr * 1.4, 0);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, true, false, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0.02);
                }
                if (t % 20 == 0) sound(level, SoundEvents.ENDERMAN_STARE, 2f, 0.5f);
                if (t == 50) appear(level, mob, 50);
            }
            case THUNDERSTRIKE -> {
                if (t == 0) level.getServer().setWeatherParameters(0, 1200, true, true);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, false, c.x, c.y + 3, c.z, 10, r, 3, r, 0.1);
                if (t % 15 == 0 && t < 60) Families.strikeVisual(level, c.add(level.getRandom().nextGaussian() * r * 2, 0, level.getRandom().nextGaussian() * r * 2));
                if (t == 60) {
                    Families.strikeVisual(level, c);
                    Families.strikeVisual(level, c.add(1, 0, 1));
                    appear(level, mob, 20);
                }
            }
            case INFERNO -> {
                level.sendParticles(ParticleTypes.FLAME, true, false, c.x, c.y + 1, c.z, 16, r * 0.5, 1.5, r * 0.5, 0.05);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, true, false, c.x, c.y + 3, c.z, 8, r * 0.5, 2, r * 0.5, 0.05);
                if (t % 10 == 0) sound(level, SoundEvents.BLAZE_BURN, 2f, 0.5f);
                if (t >= 50) {
                    if (t == 50) {
                        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, true, false, c.x, c.y + 1, c.z, 2, r * 0.3, 0.5, r * 0.3, 0);
                        sound(level, SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.6f);
                        CombatEngine.setAnim(level, mob, AnimPose.EMERGE, AnimState.ACTIVE, 40);
                    }
                    lerp(mob, (t - 50) / 40f);
                }
            }
            case ASSEMBLE -> {
                for (int i = 0; i < 8; i++) {
                    double a = level.getRandom().nextDouble() * Math.PI * 2, rr = r * 2 * (1 - p) + 1;
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()), true, false,
                            c.x + Math.cos(a) * rr, c.y + level.getRandom().nextDouble() * 3, c.z + Math.sin(a) * rr, 1, 0, 0, 0, 0);
                }
                if (t % 10 == 0) sound(level, SoundEvents.SKELETON_STEP, 3f, 0.4f);
                if (t == 40) appear(level, mob, 60);
            }
            case FOG -> {
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, false, c.x, c.y + 1, c.z, 8, r * 1.5, 1, r * 1.5, 0.01);
                if (t % 25 == 0) sound(level, SoundEvents.AMBIENT_CAVE.value(), 3f, 0.6f);
                if (t == 50) appear(level, mob, 50);
            }
        }
    }

    private void appear(ServerLevel level, Mob mob, int formTicks) {
        mob.setPos(end);
        mob.setInvisible(false);
        CombatEngine.setAnim(level, mob, AnimPose.FORM, AnimState.ACTIVE, formTicks);
    }

    private void impact(ServerLevel level, Mob mob, Vec3 c, double r) {
        mob.setPos(end);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, true, false, c.x, c.y + 0.5, c.z, 3, r * 0.3, 0.2, r * 0.3, 0);
        groundDust(level, c, 120, r * 0.6);
        sound(level, SoundEvents.GENERIC_EXPLODE.value(), 4f, 0.5f);
        shove(level, mob, r * 1.5, 1.2);
    }

    private void shove(ServerLevel level, Mob mob, double radius, double strength) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(radius), e -> HunterDamage.isFoe(mob, e))) {
            Vec3 d = e.position().subtract(mob.position());
            d = new Vec3(d.x, 0, d.z).normalize();
            e.push(d.x * strength, 0.5, d.z * strength);
            e.needsSync = true;
        }
    }

    // ---------------------------------------------------------------------------- deity entrances

    private void deity(ServerLevel level, Mob mob, int t, float p, Vec3 c) {
        if (t % 40 == 0 && t < 100) pulseDarkness(level, 80);
        if (t < 70) {
            ring(level, c, 9, ParticleTypes.SOUL_FIRE_FLAME, 36);
            if (t % 20 == 0) sound(level, SoundEvents.WARDEN_HEARTBEAT, 4f, 0.5f);
        } else if (t < 140) {
            if (t % 8 == 0) {
                double a = (t / 8) * 0.9;
                Families.strikeVisual(level, c.add(Math.cos(a) * 9, 0, Math.sin(a) * 9));
            }
            for (int i = 0; i < 40; i++) level.sendParticles(ParticleTypes.END_ROD, true, false, c.x, c.y + i * 1.2, c.z, 1, 0.1, 0.1, 0.1, 0);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, true, false, c.x, c.y + 40, c.z, 40, 6, 2, 6, 0.3);
            if (t == 70) sound(level, SoundEvents.END_PORTAL_SPAWN, 5f, 0.5f);
        } else if (t < 210) {
            if (t == 140) {
                mob.setInvisible(false);
                CombatEngine.setAnim(level, mob, AnimPose.FORM, AnimState.ACTIVE, 70);
                sound(level, SoundEvents.WITHER_SPAWN, 5f, 0.5f);
            }
            lerp(mob, (t - 140) / 70f);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, true, false, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 30,
                    mob.getBbWidth() * 0.6, mob.getBbHeight() * 0.5, mob.getBbWidth() * 0.6, 0.2);
            if (t % 10 == 0) ring(level, c, (t - 140) * 0.4, ParticleTypes.SONIC_BOOM, 16);
        } else if (t == 210) {
            sound(level, SoundEvents.ENDER_DRAGON_GROWL, 6f, 0.5f);
            sound(level, SoundEvents.WARDEN_ROAR, 6f, 0.5f);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, true, false, c.x, c.y + 1, c.z, 6, 6, 1, 6, 0);
            shove(level, mob, 18, 2.2);
        }
    }

    private void ramGod(ServerLevel level, Mob mob, int t, float p, Vec3 c) {
        Vec3 sky = end;
        if (t % 45 == 0 && t < 90) pulseDarkness(level, 60);
        if (t < 120) {
            // Storm clouds spiral in from the horizon.
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6 + t * 0.06;
                double r = 30 * (1 - t / 120.0) + 4;
                level.sendParticles(ParticleTypes.CLOUD, true, false, sky.x + Math.cos(a) * r, sky.y + Math.sin(t * 0.1 + i) * 2, sky.z + Math.sin(a) * r, 3, 1.5, 0.6, 1.5, 0.01);
            }
            if (t % 20 == 0) sound(level, SoundEvents.LIGHTNING_BOLT_THUNDER, 4f, 0.5f);
            if (t % 25 == 10) Families.strikeVisual(level, c.add(level.getRandom().nextGaussian() * 14, 0, level.getRandom().nextGaussian() * 14));
        } else if (t < 200) {
            if (t == 120) {
                mob.setInvisible(false);
                mob.setPos(end);
                CombatEngine.setAnim(level, mob, AnimPose.FORM, AnimState.ACTIVE, 80);
                sound(level, SoundEvents.GOAT_SCREAMING_LONG_JUMP, 5f, 0.4f);
            }
            level.sendParticles(ParticleTypes.CLOUD, true, false, sky.x, sky.y, sky.z, 30, 5, 2, 5, 0.05);
            if (t % 10 == 0) Families.strikeVisual(level, c.add(Math.cos(t * 0.3) * 9, 0, Math.sin(t * 0.3) * 9));
            if (t == 199 && mob instanceof DeityBoss boss) boss.onEntranceFormed(level);
        } else if (t == 220) {
            sound(level, SoundEvents.GOAT_SCREAMING_RAM_IMPACT, 6f, 0.4f);
            sound(level, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 6f, 0.5f);
            level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, true, false, c.x, c.y + 1, c.z, 4, 4, 1, 4, 0);
            shove(level, mob, 24, 2.8);
        }
    }

    private void giant(ServerLevel level, Mob mob, int t, float p, Vec3 c) {
        if (t == 0) lightPortal(level);
        if (t % 30 == 0 && t < 60) pulseDarkness(level, 60);
        level.sendParticles(ParticleTypes.PORTAL, true, false, c.x, c.y + 0.5, c.z, 30, 3, 0.2, 3, 0.8);
        if (t < 80) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.NETHERRACK.defaultBlockState()), true, false, c.x, c.y + 0.3, c.z, 20, 7, 0.1, 7, 0.4);
            level.sendParticles(ParticleTypes.LAVA, true, false, c.x, c.y + 0.5, c.z, 3, 4, 0.1, 4, 0);
            if (t % 12 == 0) sound(level, SoundEvents.ZOMBIFIED_PIGLIN_ANGRY, 4f, 0.3f);
            if (t % 20 == 0) sound(level, SoundEvents.RESPAWN_ANCHOR_CHARGE, 4f, 0.4f);
        } else if (t < 230) {
            if (t == 80) {
                CombatEngine.setAnim(level, mob, AnimPose.EMERGE, AnimState.ACTIVE, 150);
                sound(level, SoundEvents.PORTAL_TRIGGER, 5f, 0.4f);
            }
            lerp(mob, (t - 80) / 150f);
            level.sendParticles(ParticleTypes.FLAME, true, false, c.x, c.y + 0.5, c.z, 10, 3.5, 0.1, 3.5, 0.1);
            if (t % 15 == 0) sound(level, SoundEvents.ZOMBIFIED_PIGLIN_AMBIENT, 5f, 0.25f);
        } else if (t == 240) {
            sound(level, SoundEvents.ZOMBIFIED_PIGLIN_ANGRY, 6f, 0.2f);
            sound(level, SoundEvents.WARDEN_ROAR, 5f, 0.4f);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, true, false, c.x, c.y + 1, c.z, 6, 5, 1, 5, 0);
            shove(level, mob, 18, 2.0);
            if (mob instanceof DeityBoss boss) boss.onEntranceFormed(level);
        }
    }

    private void lightPortal(ServerLevel level) {
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-6, -1, -6), at.offset(6, 1, 6))) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.SUNKEN_PORTAL.get())) level.setBlock(p, s.setValue(ShrineAltarBlock.LIT, true), 3);
        }
    }

    private void finish(ServerLevel level, Mob mob) {
        mob.setPos(end);
        mob.setInvisible(false);
        mob.setNoAi(false);
        mob.setNoGravity(wasNoGravity || style == Style.RAM_GOD || style == Style.GIANT_PIGLIN);
        CombatEngine.setAnim(level, mob, AnimPose.ROAR, AnimState.ACTIVE, 30);
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 5f, style == Style.MONSTROUS ? 0.6f : 0.4f);
        if (style == Style.MONSTROUS) shove(level, mob, 10, 1.0);
        if (hunter != null && level.getPlayerByUUID(hunter) instanceof ServerPlayer p && !p.isCreative() && !p.isSpectator()) CombatEngine.setTarget(mob, p);
    }
}
