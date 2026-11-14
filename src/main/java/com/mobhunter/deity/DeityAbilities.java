package com.mobhunter.deity;

import net.minecraft.world.entity.EntityTypes;
import static com.mobhunter.core.AnimPose.*;

import com.mobhunter.ability.Abilities;
import com.mobhunter.ability.Ability;
import com.mobhunter.ability.Ctx;
import com.mobhunter.ability.Families;
import com.mobhunter.ability.HunterDamage;
import com.mobhunter.ability.Markers;
import com.mobhunter.core.Element;
import com.mobhunter.core.Tier;
import com.mobhunter.entity.ElementOrb;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** The bespoke movesets of the Ram God and the Giant Zombie Piglin. */
public final class DeityAbilities {

    static Vec3 ground(Ctx c, Vec3 v) {
        BlockPos p = BlockPos.containing(v.x, v.y + 3, v.z);
        for (int i = 0; i < 24; i++) {
            if (!c.level.getBlockState(p.below()).isAir()) return new Vec3(v.x, p.getY(), v.z);
            p = p.below();
        }
        return v;
    }

    static void play(Ctx c, net.minecraft.sounds.SoundEvent s, float vol, float pitch) {
        c.level.playSound(null, c.mob.getX(), c.mob.getY(), c.mob.getZ(), s, SoundSource.HOSTILE, vol, pitch);
    }

    // ===================================================================================== Ram God

    /** Storm clouds slide over marked spots, crackle, and call down lightning. */
    static class RamLightning extends Ability {
        final int strikesPerCloud;

        RamLightning(String id, int strikesPerCloud) {
            super(id);
            this.strikesPerCloud = strikesPerCloud;
            pose = RAM_CALL_STORM;
            windup = 36;
            active = 10;
            recovery = 24;
            cooldown = 120;
            maxRange = 40;
            lockAt = 0.6f;
            damage = 9;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            List<StormCloud> clouds = c.mob instanceof RamGod g ? g.clouds() : List.of();
            if (!c.aimLocked) {
                c.marks.clear();
                c.marks.add(ground(c, c.aim));
                int n = Math.max(2, clouds.size() * strikesPerCloud);
                for (int i = 1; i < n; i++) {
                    double a = i * Math.PI * 2 / (n - 1) + c.mob.tickCount * 0.02;
                    double r = 3 + (i % 3) * 2.5;
                    c.marks.add(ground(c, c.aim.add(Math.cos(a) * r, 0, Math.sin(a) * r)));
                }
            }
            // The Lightning Cloud takes position; the horns gather charge.
            StormCloud bolt = c.mob instanceof RamGod g ? g.cloud(StormCloud.Kind.LIGHTNING) : null;
            if (bolt != null) {
                bolt.setStation(c.marks.get(0).add(0, 9, 0));
                bolt.setCharge(progress);
            }
            if (c.mob instanceof RamGod g) for (Vec3 tip : g.hornTips()) {
                c.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, false, tip.x, tip.y, tip.z, 2 + (int) (progress * 8), 0.6, 0.6, 0.6, 0.05);
                if (progress > 0.6f && tick % 4 == 0) {
                    for (Vec3 m : c.marks) {
                        Vec3 mid = tip.lerp(m.add(0, 10, 0), c.random.nextDouble());
                        c.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, false, mid.x, mid.y, mid.z, 1, 0.2, 0.2, 0.2, 0);
                    }
                }
            }
            if (tick % 3 == 0) for (Vec3 m : c.marks) {
                Markers.fillingCircle(c.level, m, 2.4, progress);
                c.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, false, m.x, m.y + 0.5 + c.random.nextDouble() * 6, m.z, 2, 0.3, 1.5, 0.3, 0);
            }
            if (tick % 12 == 0) play(c, SoundEvents.TRIDENT_THUNDER.value(), 1.5f, 1.6f);
        }

        @Override
        public void begin(Ctx c) {
            for (Vec3 m : c.marks) {
                Families.strikeVisual(c.level, m);
                for (LivingEntity v : HunterDamage.inRadius(c, m, 2.6, 5)) HunterDamage.hit(c, v, damage, Element.THUNDER, 0.6);
            }
        }

        @Override
        public void end(Ctx c) {
            if (c.mob instanceof RamGod g) for (StormCloud cloud : g.clouds()) cloud.setStation(null);
        }
    }

    static @org.jspecify.annotations.Nullable StormCloud weapon(Ctx c, StormCloud.Kind kind) {
        return c.mob instanceof RamGod g ? g.cloud(kind) : null;
    }

    /** The Wind Cloud carves three cutting gusts across the ground. */
    static class RamGust extends Ability {
        RamGust() {
            super("ram_gust");
            pose = RAM_INHALE;
            windup = 30;
            active = 14;
            recovery = 22;
            cooldown = 140;
            maxRange = 40;
            lockAt = 0.65f;
            damage = 8;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            StormCloud wind = weapon(c, StormCloud.Kind.WIND);
            if (wind != null) {
                wind.setStation(c.aim.add(-8, 8, 0));
                wind.setCharge(progress);
            }
            if (!c.aimLocked) {
                c.marks.clear();
                for (int i = -1; i <= 1; i++) c.marks.add(ground(c, c.aim.add(0, 0, i * 4)));
            }
            if (tick % 3 == 0) for (Vec3 m : c.marks) Markers.line(c.level, m.add(-12, 0, 0), m.add(12, 0, 0), 0.7, Markers.warn(progress));
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            float u = tick / (float) active;
            for (Vec3 m : c.marks) {
                Vec3 p = m.add(-12 + 24 * u, 0.6, 0);
                c.level.sendParticles(ParticleTypes.GUST, true, false, p.x, p.y, p.z, 1, 0.2, 0.2, 0.2, 0);
                for (LivingEntity v : HunterDamage.inRadius(c, p, 1.6, 3)) {
                    if (HunterDamage.hit(c, v, damage, Element.WIND, 0)) {
                        v.setDeltaMovement(1.6, 0.5, 0);
                        v.needsSync = true;
                    }
                }
            }
            if (tick == 0) play(c, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 3f, 0.7f);
        }

        @Override
        public void end(Ctx c) {
            StormCloud wind = weapon(c, StormCloud.Kind.WIND);
            if (wind != null) wind.setStation(null);
        }
    }

    /** The Thunder Cloud drops over the hunters and releases rolling sonic shockwaves. */
    static class RamThunder extends Ability {
        RamThunder() {
            super("ram_thunder");
            pose = RAM_CALL_STORM;
            windup = 34;
            active = 30;
            recovery = 24;
            cooldown = 180;
            maxRange = 40;
            lockAt = 0.6f;
            damage = 9;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            StormCloud thunder = weapon(c, StormCloud.Kind.THUNDER);
            if (thunder != null) {
                thunder.setStation(c.aim.add(0, 7, 0));
                thunder.setCharge(progress);
            }
            Vec3 g = ground(c, c.aim);
            if (tick % 3 == 0) for (int r = 3; r <= 12; r += 3) Markers.circle(c.level, g, r, Markers.warn(progress));
            if (tick % 8 == 0) play(c, SoundEvents.WARDEN_SONIC_CHARGE, 2f, 0.6f);
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            Vec3 g = ground(c, c.aim);
            float r = 1 + tick * 0.45f;
            Markers.circle(c.level, g.add(0, 0.6, 0), r, ParticleTypes.SONIC_BOOM);
            if (tick % 10 == 0) play(c, SoundEvents.WARDEN_SONIC_BOOM, 3f, 0.6f);
            for (LivingEntity v : HunterDamage.inRadius(c, g, r + 0.6, 4)) {
                if (HunterDamage.horizontalDist(v.position(), g) > r - 1.5 && v.onGround()) HunterDamage.hit(c, v, damage, Element.SONIC, 1.6);
            }
        }

        @Override
        public void end(Ctx c) {
            StormCloud thunder = weapon(c, StormCloud.Kind.THUNDER);
            if (thunder != null) thunder.setStation(null);
        }
    }

    /** The Tornado Cloud lowers its rotating column and drags it after the hunters. */
    static class RamTornado extends Ability {
        RamTornado() {
            super("ram_tornado");
            pose = RAM_CALL_STORM;
            windup = 30;
            active = 110;
            recovery = 24;
            cooldown = 300;
            maxRange = 40;
            lockAt = 0.5f;
            damage = 4;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            c.extra = ground(c, c.aim);
            StormCloud tornado = weapon(c, StormCloud.Kind.TORNADO);
            if (tornado != null) {
                tornado.setStation(((Vec3) c.extra).add(0, 10, 0));
                tornado.setCharge(progress);
            }
            if (tick % 3 == 0) Markers.fillingCircle(c.level, (Vec3) c.extra, 3.5, progress);
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            Vec3 at = (Vec3) c.extra;
            if (c.target != null) {
                Vec3 d = c.target.position().subtract(at);
                d = new Vec3(d.x, 0, d.z);
                if (d.length() > 0.5) at = at.add(d.normalize().scale(0.18));
                at = ground(c, at);
                c.extra = at;
            }
            StormCloud tornado = weapon(c, StormCloud.Kind.TORNADO);
            if (tornado != null) tornado.setStation(at.add(0, 10, 0));
            for (int i = 0; i < 6; i++) {
                double a = tick * 0.5 + i * Math.PI / 3, h = i * 1.6, r = 0.8 + h * 0.25;
                c.level.sendParticles(ParticleTypes.CLOUD, true, false, at.x + Math.cos(a) * r, at.y + h, at.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0.02);
            }
            if (tick % 20 == 0) play(c, SoundEvents.BREEZE_WHIRL, 3f, 0.5f);
            for (LivingEntity v : HunterDamage.inRadius(c, at, 3.5, 10)) {
                Vec3 to = at.subtract(v.position());
                Vec3 swirl = new Vec3(-to.z, 0, to.x).normalize().scale(0.35);
                v.setDeltaMovement(swirl.add(to.normalize().scale(0.12)).add(0, 0.32, 0));
                v.needsSync = true;
                v.fallDistance = 0;
                if (tick % 10 == 0) HunterDamage.hitAgain(c, v, damage, Element.WIND, 0);
            }
        }

        @Override
        public void end(Ctx c) {
            StormCloud tornado = weapon(c, StormCloud.Kind.TORNADO);
            if (tornado != null) tornado.setStation(null);
        }
    }

    /** The clouds swell white, then pelt an area with hailstones. */
    static class RamHail extends Ability {
        RamHail() {
            super("ram_hail");
            pose = RAM_CALL_STORM;
            windup = 32;
            active = 70;
            recovery = 24;
            cooldown = 220;
            maxRange = 40;
            lockAt = 0.5f;
            damage = 4;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            Vec3 g = ground(c, c.aim);
            if (tick % 3 == 0) {
                Markers.circle(c.level, g, 10, Markers.warn(progress));
                Markers.circle(c.level, g, 10 * progress, Markers.warn(progress));
            }
            c.level.sendParticles(ParticleTypes.SNOWFLAKE, true, false, g.x, g.y + 14, g.z, 20, 8, 1, 8, 0);
            StormCloud hail = weapon(c, StormCloud.Kind.HAIL);
            if (hail != null) {
                hail.setStation(g.add(0, 15, 0));
                hail.setCharge(progress);
            }
            if (tick == 0) play(c, SoundEvents.POWDER_SNOW_STEP, 3f, 0.4f);
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            Vec3 g = ground(c, c.aim);
            for (int k = 0; k < 2; k++) {
                double a = c.random.nextDouble() * Math.PI * 2, r = Math.sqrt(c.random.nextDouble()) * 10;
                StormCloud hailCloud = weapon(c, StormCloud.Kind.HAIL);
                Vec3 from = hailCloud != null ? hailCloud.position() : g.add(0, 16, 0);
                Vec3 at = new Vec3(g.x + Math.cos(a) * r, from.y - 1, g.z + Math.sin(a) * r);
                ElementOrb hail = ElementOrb.create(c.level, c.mob, at, Element.ICE, c.dmg(damage), 1.2f, c.potency(), 0.6f);
                hail.setDeltaMovement(c.random.nextGaussian() * 0.05, -1.4, c.random.nextGaussian() * 0.05);
                c.level.addFreshEntity(hail);
            }
            if (tick % 10 == 0) play(c, SoundEvents.GLASS_BREAK, 1.2f, 1.6f);
        }

        @Override
        public void end(Ctx c) {
            StormCloud hail = weapon(c, StormCloud.Kind.HAIL);
            if (hail != null) hail.setStation(null);
        }
    }

    // ============================================================================== Giant Piglin

    static Vec3 fwd(Ctx c) {
        return c.facing();
    }

    /** Raises the block-sword overhead and brings it down in a line. */
    static class GiantSmash extends Ability {
        GiantSmash() {
            super("giant_smash");
            pose = GIANT_OVERHEAD;
            windup = 36;
            active = 26;
            recovery = 34;
            cooldown = 100;
            maxRange = 22;
            damage = 12;
            lockAt = 0.75f;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            Vec3 from = c.pos().add(fwd(c).scale(3));
            Vec3 to = c.pos().add(fwd(c).scale(19));
            if (tick % 3 == 0) Markers.line(c.level, ground(c, from), ground(c, to), 1.8, Markers.warn(progress));
            if (tick == 0) play(c, SoundEvents.ZOMBIFIED_PIGLIN_ANGRY, 4f, 0.3f);
        }

        @Override
        public void begin(Ctx c) {
            Vec3 from = c.pos().add(fwd(c).scale(3));
            Vec3 to = c.pos().add(fwd(c).scale(19));
            for (LivingEntity v : HunterDamage.alongLine(c, from.add(0, 1, 0), to.add(0, 1, 0), 2.2)) HunterDamage.hit(c, v, damage, Element.FIRE, 1.4);
            for (double t = 0; t <= 1; t += 0.08) {
                Vec3 p = ground(c, from.lerp(to, t));
                c.level.sendParticles(ParticleTypes.EXPLOSION, true, false, p.x, p.y + 0.3, p.z, 1, 0.5, 0, 0.5, 0);
                c.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.NETHERRACK.defaultBlockState()), true, false, p.x, p.y + 0.3, p.z, 20, 1, 0.2, 1, 0.4);
            }
            play(c, SoundEvents.MACE_SMASH_GROUND_HEAVY, 5f, 0.4f);
            play(c, SoundEvents.GENERIC_EXPLODE.value(), 4f, 0.5f);
            c.extra = ground(c, c.pos().add(fwd(c).scale(14)));
        }

        /** The sword strikes the ground and sends out a burning Nether shockwave. */
        @Override
        public void activeTick(Ctx c, int tick) {
            if (!(c.extra instanceof Vec3 at) || tick == 0) return;
            float r = tick * 0.75f;
            int n = (int) Math.max(12, r * 6);
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                c.level.sendParticles(i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME, true, false,
                        at.x + Math.cos(a) * r, at.y + 0.3, at.z + Math.sin(a) * r, 1, 0, 0.1, 0, 0.02);
            }
            if (tick % 5 == 0) play(c, SoundEvents.BLAZE_SHOOT, 3f, 0.4f);
            for (LivingEntity v : HunterDamage.inRadius(c, at, r + 0.6, 3)) {
                if (HunterDamage.horizontalDist(v.position(), at) > r - 1.4 && (v.onGround() || v.getY() - at.y < 0.6) && HunterDamage.hitAgain(c, v, damage * 0.5f, Element.FIRE, 0.8)) {
                    v.igniteForSeconds(4);
                }
            }
        }
    }

    /** Draws the sword back to its side and sweeps it across everything in front. */
    static class GiantSlice extends Ability {
        GiantSlice() {
            super("giant_slice");
            pose = GIANT_SIDE_SWING;
            windup = 30;
            active = 10;
            recovery = 30;
            cooldown = 90;
            maxRange = 16;
            damage = 10;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            if (tick % 3 == 0) {
                Markers.cone(c.level, c.pos(), fwd(c), 16, 85, Markers.warn(progress));
                Markers.circle(c.level, c.pos(), 4.5, ParticleTypes.HAPPY_VILLAGER);
            }
            if (tick == 0) play(c, SoundEvents.PLAYER_ATTACK_SWEEP, 4f, 0.3f);
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            double base = Math.atan2(fwd(c).z, fwd(c).x);
            double a = base + Math.toRadians(85 - 170 * (tick / (double) active));
            for (double r = 5; r < 16; r += 1.5) {
                Vec3 p = c.pos().add(Math.cos(a) * r, 2 + r * 0.05, Math.sin(a) * r);
                c.level.sendParticles(ParticleTypes.SWEEP_ATTACK, true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            if (tick == active / 2) {
                for (LivingEntity v : HunterDamage.inCone(c, c.pos(), fwd(c), 16, 85)) {
                    if (HunterDamage.horizontalDist(v.position(), c.pos()) > 4.5) HunterDamage.hit(c, v, damage, Element.FIRE, 2.2);
                }
                play(c, SoundEvents.PLAYER_ATTACK_STRONG, 5f, 0.3f);
            }
        }
    }

    /** Reaches for the hunter, grabs them, and hurls them far away. */
    static class GiantGrab extends Ability {
        GiantGrab() {
            super("giant_grab");
            pose = GIANT_REACH;
            windup = 26;
            active = 44;
            recovery = 24;
            cooldown = 200;
            maxRange = 14;
            minRange = 2;
            damage = 7;
            lockAt = 0.8f;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            if (tick % 3 == 0) Markers.fillingCircle(c.level, ground(c, c.aim), 3, progress);
        }

        @Override
        public void begin(Ctx c) {
            c.extra = null;
            for (LivingEntity v : HunterDamage.inRadius(c, c.aim, 3, 4)) {
                c.extra = v;
                if (c.mob instanceof GiantZombiePiglin g) g.setHeld(v);
                HunterDamage.hit(c, v, damage, Element.RAW, 0);
                play(c, SoundEvents.ZOMBIFIED_PIGLIN_ANGRY, 4f, 0.4f);
                break;
            }
            if (c.extra == null) {
                Vec3 g = ground(c, c.aim);
                c.level.sendParticles(ParticleTypes.EXPLOSION, true, false, g.x, g.y, g.z, 2, 1, 0, 1, 0);
                play(c, SoundEvents.MACE_SMASH_GROUND, 3f, 0.5f);
                c.finished = true;
            }
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            if (!(c.extra instanceof LivingEntity v) || !(c.mob instanceof GiantZombiePiglin g) || !v.isAlive()) {
                c.finished = true;
                return;
            }
            Vec3 hand = g.handPos();
            if (tick < 30) {
                v.teleportTo(hand.x, hand.y - 1, hand.z);
                v.setDeltaMovement(Vec3.ZERO);
                v.fallDistance = 0;
                if (tick % 10 == 9) HunterDamage.hitAgain(c, v, damage * 0.3f, Element.RAW, 0);
            } else if (tick == 30) {
                Vec3 throwDir = fwd(c).scale(2.6).add(0, 1.1, 0);
                v.setDeltaMovement(throwDir);
                v.needsSync = true;
                if (v instanceof net.minecraft.server.level.ServerPlayer sp) sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
                g.setHeld(null);
                play(c, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 4f, 0.4f);
            }
        }

        @Override
        public void end(Ctx c) {
            if (c.mob instanceof GiantZombiePiglin g) g.setHeld(null);
        }
    }

    /** Tears a chunk of the earth out of the ground and throws it. */
    static class GiantBoulder extends Ability {
        GiantBoulder() {
            super("giant_boulder");
            pose = GIANT_SCOOP;
            windup = 32;
            active = 4;
            recovery = 26;
            cooldown = 110;
            minRange = 10;
            maxRange = 48;
            damage = 11;
            lockAt = 0.7f;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            if (tick % 3 == 0) Markers.fillingCircle(c.level, ground(c, c.aim), 4, progress);
            Vec3 scoop = c.pos().add(fwd(c).scale(5));
            c.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.NETHERRACK.defaultBlockState()), true, false, scoop.x, scoop.y + 0.5, scoop.z, 8, 1.5, 0.3, 1.5, 0.2);
        }

        @Override
        public void begin(Ctx c) {
            Vec3 from = c.mob instanceof GiantZombiePiglin g ? g.handPos().add(0, 3, 0) : c.center();
            Vec3 to = ground(c, c.aim);
            ElementOrb boulder = ElementOrb.create(c.level, c.mob, from, Element.EARTH, c.dmg(damage), 4f, c.potency(), 2.6f);
            Vec3 d = to.subtract(from);
            double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
            double t = Math.max(14, horiz / 1.4);
            boulder.gravity(0.06);
            boulder.setDeltaMovement(d.x / t, d.y / t + 0.03 * t, d.z / t);
            c.level.addFreshEntity(boulder);
            play(c, SoundEvents.RAVAGER_ROAR, 3f, 0.4f);
        }
    }

    public static void init() {
        Abilities.register(new RamLightning("ram_lightning", 1));
        Abilities.register(new RamLightning("ram_surge", 3)).timing(40, 10, 30, 300).damage(10);
        Abilities.register(new RamHail());
        Abilities.register(new RamGust());
        Abilities.register(new RamThunder());
        Abilities.register(new RamTornado());
        Abilities.register(new Families.Gale("ram_gale")).range(0, 30).pose(RAM_INHALE).damage(6).cooldown = 140;
        Abilities.register(new Families.Dash("ram_dive", 0.55f, 1, 2.2f, true, false)).timing(28, 26, 30, 160).range(6, 34).pose(RAM_LOWER_HORNS).damage(12);
        Abilities.register(new GiantSmash());
        Abilities.register(new GiantSlice());
        Abilities.register(new GiantGrab());
        Abilities.register(new GiantBoulder());
        Abilities.register(new Families.Summon("giant_horde", c -> EntityTypes.ZOMBIFIED_PIGLIN, 4, false, Tier.NONE)).pose = GIANT_ROAR;
        Abilities.register(new Families.MeteorRain("giant_eruption", 12, Element.FIRE)).pose(GIANT_ROAR).damage(9);
    }

    private DeityAbilities() {}
}
