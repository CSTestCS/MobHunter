package com.mobhunter.ability;

import net.minecraft.world.entity.EntityTypes;
import com.mobhunter.core.AnimPose;
import com.mobhunter.core.Element;
import com.mobhunter.core.Tier;
import com.mobhunter.core.TierManager;
import com.mobhunter.entity.ElementOrb;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Reusable ability families. Each named ability in {@link Abilities} is one of these with parameters. */
public final class Families {

    static void sound(Ctx c, SoundEvent s, float vol, float pitch) {
        c.level.playSound(null, c.mob.getX(), c.mob.getY(), c.mob.getZ(), s, SoundSource.HOSTILE, vol + c.size() * 0.3f, pitch);
    }

    static void sound(Ctx c, net.minecraft.core.Holder<SoundEvent> s, float vol, float pitch) {
        sound(c, s.value(), vol, pitch);
    }

    static Element el(Ctx c, Element override) {
        return override != null ? override : c.element;
    }

    static double ground(Ctx c, Vec3 v) {
        BlockPos p = BlockPos.containing(v.x, v.y + 2, v.z);
        for (int i = 0; i < 8; i++) {
            if (!c.level.getBlockState(p.below()).isAir()) return p.getY();
            p = p.below();
        }
        return v.y;
    }

    static Vec3 onGround(Ctx c, Vec3 v) {
        return new Vec3(v.x, ground(c, v), v.z);
    }

    static void groundDust(Ctx c, Vec3 at, int count, double spread) {
        BlockState below = c.level.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
        if (below.isAir()) return;
        c.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), true, false, at.x, at.y + 0.1, at.z, count, spread, 0.1, spread, 0.15);
    }

    // ------------------------------------------------------------------------------------------ melee
    /** Swipe, bite, kick or sweep in an arc in front (or behind) of the creature. */
    public static class Melee extends Ability {
        final float reach, halfArc, knock;
        final int hits;
        final boolean behind;
        final Element override;
        final float launch;

        public Melee(String id, float reach, float halfArc, float knock, int hits, boolean behind, Element override, float launch) {
            super(id);
            this.reach = reach;
            this.halfArc = halfArc;
            this.knock = knock;
            this.hits = hits;
            this.behind = behind;
            this.override = override;
            this.launch = launch;
            this.maxRange = reach + 0.5f;
        }

        float reach(Ctx c) {
            return reach + c.size() * 0.6f;
        }

        Vec3 dir(Ctx c) {
            Vec3 f = c.facing();
            return behind ? f.scale(-1) : f;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            if (behind) {
                Vec3 d = c.flatDirToAim().scale(-1);
                c.aim = c.mob.position().add(d.scale(4));
                c.faceAim();
                c.aim = c.target.position();
            } else c.faceAim();
            if (tick % 3 == 0) {
                if (halfArc >= 179) Markers.circle(c.level, c.pos(), reach(c), Markers.warn(progress));
                else Markers.cone(c.level, c.pos(), dir(c), reach(c), halfArc, Markers.warn(progress));
            }
        }

        @Override
        public void begin(Ctx c) {
            Vec3 f = dir(c);
            if (!behind) c.mob.push(f.x * 0.25 * Math.max(1, c.size() * 0.5), 0, f.z * 0.25 * Math.max(1, c.size() * 0.5));
            sound(c, halfArc > 90 ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.RAVAGER_ATTACK, 1f, 0.6f + c.random.nextFloat() * 0.3f);
            if (hits <= 1) strike(c);
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            if (hits > 1 && tick % Math.max(1, active / hits) == 0) {
                c.hit.clear();
                strike(c);
            }
        }

        void strike(Ctx c) {
            Vec3 f = dir(c);
            float r = reach(c);
            List<LivingEntity> victims = halfArc >= 179 ? HunterDamage.inRadius(c, c.pos(), r, c.height() + 1)
                    : HunterDamage.inCone(c, c.pos(), f, r, halfArc);
            for (LivingEntity v : victims) {
                if (HunterDamage.hit(c, v, damage / Math.max(1, hits * 0.6f), el(c, override), knock) && launch > 0) {
                    v.push(0, launch, 0);
                    v.needsSync = true;
                }
            }
            for (int i = -2; i <= 2; i++) {
                double a = Math.atan2(f.z, f.x) + Math.toRadians(halfArc >= 179 ? i * 72 : i * halfArc / 2.5);
                Vec3 p = c.pos().add(Math.cos(a) * r * 0.7, c.height() * 0.4, Math.sin(a) * r * 0.7);
                c.level.sendParticles(ParticleTypes.SWEEP_ATTACK, true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
    }

    // ------------------------------------------------------------------------------------------ dash
    /** A straight-line charge. Multiple dashes re-aim between each one. */
    public static class Dash extends Ability {
        final float speed, knock;
        final int dashes;
        final boolean threeD;
        final boolean roarAfter;

        public Dash(String id, float speed, int dashes, float knock, boolean threeD, boolean roarAfter) {
            super(id);
            this.speed = speed;
            this.dashes = dashes;
            this.knock = knock;
            this.threeD = threeD;
            this.roarAfter = roarAfter;
            this.minRange = 3;
            this.maxRange = 22;
            this.pose = AnimPose.CHARGE;
        }

        float spd(Ctx c) {
            return speed * (0.8f + Math.min(2.2f, c.size() * 0.25f));
        }

        int perDash() {
            return Math.max(8, active / dashes);
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            c.dir = threeD ? c.aim.subtract(c.center()).normalize() : c.flatDirToAim();
            if (tick % 3 == 0) {
                Vec3 end = c.pos().add(c.dir.scale(spd(c) * perDash()));
                Markers.line(c.level, c.pos(), new Vec3(end.x, c.pos().y, end.z), c.size() * 0.5 + 0.5, Markers.warn(progress));
            }
            if (tick % 4 == 0) groundDust(c, c.pos().subtract(c.dir.scale(c.size() * 0.5)), 6, c.size() * 0.3);
        }

        @Override
        public void begin(Ctx c) {
            sound(c, SoundEvents.RAVAGER_ROAR, 0.8f, 1.3f);
            if (threeD) {
                c.savedNoGravity = c.mob.isNoGravity();
                c.mob.setNoGravity(true);
            }
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            int seg = tick / perDash();
            if (seg >= dashes) {
                c.finished = true;
                return;
            }
            if (tick % perDash() == 0 && seg > 0) {
                c.hit.clear();
                c.aim = c.target.position();
                c.faceAim();
                c.dir = threeD ? c.aim.subtract(c.center()).normalize() : c.flatDirToAim();
                sound(c, SoundEvents.RAVAGER_STEP, 1f, 0.7f);
            }
            int inSeg = tick % perDash();
            if (inSeg < 3 && seg > 0) {
                c.mob.setDeltaMovement(c.mob.getDeltaMovement().multiply(0.2, 1, 0.2));
                return;
            }
            Vec3 v = c.dir.scale(spd(c));
            c.mob.setDeltaMovement(threeD ? v : new Vec3(v.x, c.mob.getDeltaMovement().y, v.z));
            c.mob.needsSync = true;
            for (LivingEntity e : c.level.getEntitiesOfClass(LivingEntity.class, c.mob.getBoundingBox().inflate(0.6), e -> HunterDamage.isFoe(c.mob, e))) {
                HunterDamage.hit(c, e, damage, c.element, knock);
            }
            if (tick % 2 == 0) groundDust(c, c.pos(), 4, c.size() * 0.4);
            if (c.mob.horizontalCollision && !threeD) {
                // Slammed into a wall: dazed.
                sound(c, SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 0.6f);
                c.level.sendParticles(ParticleTypes.EXPLOSION, c.mob.getX(), c.mob.getY() + 1, c.mob.getZ(), 1, 0, 0, 0, 0);
                c.mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 4));
                c.finished = true;
            }
        }

        @Override
        public void end(Ctx c) {
            c.mob.setDeltaMovement(c.mob.getDeltaMovement().multiply(0.1, threeD ? 0.1 : 1, 0.1));
            if (threeD) c.mob.setNoGravity(c.savedNoGravity);
            if (roarAfter) Roar.roarAt(c, 8 + c.size(), false);
        }
    }

    // ------------------------------------------------------------------------------------------ leap
    /** Leaps at the (locked) target position and slams down. */
    public static class Leap extends Ability {
        final float radius, knock, heightBoost;

        public Leap(String id, float radius, float knock, float heightBoost) {
            super(id);
            this.radius = radius;
            this.knock = knock;
            this.heightBoost = heightBoost;
            this.minRange = 2;
            this.maxRange = 16;
            this.pose = AnimPose.CROUCH;
            this.active = 50;
        }

        float rad(Ctx c) {
            return radius + c.size() * 0.6f;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            if (tick % 3 == 0) Markers.fillingCircle(c.level, onGround(c, c.aim), rad(c), progress);
            if (tick % 5 == 0) groundDust(c, c.pos(), 6, c.size() * 0.4);
        }

        @Override
        public void begin(Ctx c) {
            Vec3 d = c.aim.subtract(c.pos());
            double dist = Math.sqrt(d.x * d.x + d.z * d.z);
            double t = Mth.clamp(dist / 0.85, 12, 28);
            double vy = 0.04 * t * heightBoost + Mth.clamp(d.y / t, -0.5, 1.0);
            c.mob.setDeltaMovement(d.x / t, vy, d.z / t);
            c.mob.needsSync = true;
            sound(c, SoundEvents.RAVAGER_STEP, 1f, 0.8f);
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            if (tick > 3 && (c.mob.onGround() || c.mob.isInWater())) {
                land(c);
                c.finished = true;
            }
        }

        void land(Ctx c) {
            float r = rad(c);
            for (LivingEntity v : HunterDamage.inRadius(c, c.pos(), r, c.height() + 2)) HunterDamage.hit(c, v, damage, c.element, knock);
            sound(c, SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 0.7f);
            c.level.sendParticles(ParticleTypes.EXPLOSION, true, false, c.mob.getX(), c.mob.getY() + 0.5, c.mob.getZ(), 3, r * 0.3, 0.2, r * 0.3, 0);
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                groundDust(c, c.pos().add(Math.cos(a) * r * 0.8, 0, Math.sin(a) * r * 0.8), 5, 0.3);
            }
        }
    }

    // ------------------------------------------------------------------------------------------ shockwave
    /** Rears up and slams the ground, sending rings outward. Jump over them! */
    public static class Shockwave extends Ability {
        final float maxRadius;
        final Element override;

        public Shockwave(String id, float maxRadius, Element override) {
            super(id);
            this.maxRadius = maxRadius;
            this.override = override;
            this.pose = AnimPose.REAR_UP;
            this.maxRange = maxRadius * 0.8f;
            this.active = 24;
        }

        float rad(Ctx c) {
            return maxRadius + c.size() * 1.2f;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            if (tick % 4 == 0) {
                Markers.circle(c.level, c.pos(), rad(c) * 0.33, Markers.warn(progress));
                Markers.circle(c.level, c.pos(), rad(c) * 0.66, Markers.warn(progress));
                Markers.circle(c.level, c.pos(), rad(c), Markers.warn(progress));
            }
        }

        @Override
        public void begin(Ctx c) {
            sound(c, SoundEvents.WARDEN_ATTACK_IMPACT, 1.5f, 0.6f);
            sound(c, SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 0.5f);
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            float r = rad(c) * (tick + 1) / active;
            Element e = el(c, override);
            int n = (int) Math.max(12, r * 5);
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                Vec3 p = c.pos().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                if (i % 2 == 0) groundDust(c, p, 2, 0.2);
                else c.level.sendParticles(e == Element.RAW || e == Element.EARTH ? ParticleTypes.POOF : e.particle(), true, false, p.x, p.y + 0.2, p.z, 1, 0, 0.05, 0, 0.02);
            }
            for (LivingEntity v : HunterDamage.inRadius(c, c.pos(), r + 0.5, 2)) {
                double d = HunterDamage.horizontalDist(v.position(), c.pos());
                boolean grounded = v.onGround() || v.getY() - c.mob.getY() < 0.6;
                if (d > r - 1.6 && grounded) HunterDamage.hit(c, v, damage, e, 0.8);
            }
        }
    }

    // ------------------------------------------------------------------------------------------ breath
    /** A cone of elemental breath (or spray, or screech). */
    public static class Breath extends Ability {
        final float length, halfAngle;
        final Element override;
        final float push;

        public Breath(String id, float length, float halfAngle, Element override, float push) {
            super(id);
            this.length = length;
            this.halfAngle = halfAngle;
            this.override = override;
            this.push = push;
            this.pose = AnimPose.INFLATE;
            this.maxRange = length * 0.9f;
            this.active = 36;
        }

        float len(Ctx c) {
            return length + c.size();
        }

        Vec3 mouth(Ctx c) {
            return c.pos().add(c.facing().scale(c.size() * 0.55)).add(0, c.mob.getEyeHeight() * 0.85, 0);
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            c.dir = c.flatDirToAim();
            if (tick % 3 == 0) Markers.cone(c.level, c.pos(), c.dir, len(c), halfAngle, Markers.warn(progress));
            Element e = el(c, override);
            Vec3 m = mouth(c);
            c.level.sendParticles(e.dust(1.5f), true, false, m.x, m.y, m.z, 3, 0.3, 0.3, 0.3, 0);
        }

        @Override
        public void begin(Ctx c) {
            Element e = el(c, override);
            sound(c, switch (e) {
                case FIRE -> SoundEvents.BLAZE_SHOOT;
                case ICE -> SoundEvents.POWDER_SNOW_STEP;
                case SONIC -> SoundEvents.WARDEN_SONIC_CHARGE;
                case WIND -> SoundEvents.BREEZE_WIND_CHARGE_BURST.value();
                case WATER -> SoundEvents.GENERIC_SPLASH;
                default -> SoundEvents.LLAMA_SPIT;
            }, 1.2f, 0.7f);
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            Element e = el(c, override);
            Vec3 m = mouth(c);
            float l = len(c);
            for (int i = 0; i < 6; i++) {
                double a = Math.atan2(c.dir.z, c.dir.x) + Math.toRadians((c.random.nextDouble() * 2 - 1) * halfAngle);
                double sp = 0.4 + l * 0.05;
                c.level.sendParticles(e == Element.WIND || e == Element.SONIC ? ParticleTypes.CLOUD : e.particle(), true, false, m.x, m.y, m.z, 0,
                        Math.cos(a), -0.05, Math.sin(a), sp);
                c.level.sendParticles(e.dust(2f), true, false, m.x + Math.cos(a) * l * c.random.nextDouble(), m.y - c.random.nextDouble() * m.subtract(c.pos()).y,
                        m.z + Math.sin(a) * l * c.random.nextDouble(), 1, 0.1, 0.1, 0.1, 0);
            }
            if (tick % 6 == 0) {
                for (LivingEntity v : HunterDamage.inCone(c, c.pos(), c.dir, l, halfAngle)) {
                    HunterDamage.hitAgain(c, v, damage * 0.3f, e, 0);
                    if (push > 0) {
                        v.push(c.dir.x * push, 0.15, c.dir.z * push);
                        v.needsSync = true;
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------ volley
    /** Fires elemental projectiles. {@code arc} lobs them onto the locked target position. */
    public static class Volley extends Ability {
        final int count;
        final float spread, speed, splash;
        final boolean arc;
        final Element override;

        public Volley(String id, int count, float spread, float speed, float splash, boolean arc, Element override) {
            super(id);
            this.count = count;
            this.spread = spread;
            this.speed = speed;
            this.splash = splash;
            this.arc = arc;
            this.override = override;
            this.minRange = 4;
            this.maxRange = 28;
            this.pose = AnimPose.LEAN_BACK;
            this.active = Math.max(6, count * 3);
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            Element e = el(c, override);
            Vec3 m = c.center().add(c.facing().scale(c.size() * 0.5));
            c.level.sendParticles(e.dust(1.4f), true, false, m.x, m.y, m.z, 2, 0.4, 0.4, 0.4, 0);
            if (arc && tick % 3 == 0) Markers.fillingCircle(c.level, onGround(c, c.aim), 1.5 + splash, progress);
            else if (tick % 4 == 0) Markers.line(c.level, c.pos(), onGround(c, c.aim), 0.4, Markers.warn(progress));
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            int every = Math.max(1, active / count);
            if (tick % every != 0 || c.counter >= count) return;
            c.counter++;
            Element e = el(c, override);
            Vec3 from = c.center().add(c.facing().scale(c.size() * 0.55 + 0.3)).add(0, c.height() * 0.2, 0);
            Vec3 to = c.aimLocked ? c.aim : c.target.position();
            to = to.add(0, c.target.getBbHeight() * 0.5, 0);
            ElementOrb orb = ElementOrb.create(c.level, c.mob, from, e, c.dmg(damage), splash, c.potency(), 0.3f + Math.min(1.5f, c.size() * 0.15f));
            Vec3 d = to.subtract(from);
            double yawJitter = Math.toRadians((c.random.nextDouble() * 2 - 1) * spread);
            double cos = Math.cos(yawJitter), sin = Math.sin(yawJitter);
            d = new Vec3(d.x * cos - d.z * sin, d.y, d.x * sin + d.z * cos);
            if (arc) {
                double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
                double t = Math.max(10, horiz / speed);
                orb.gravity(0.05);
                orb.setDeltaMovement(d.x / t, d.y / t + 0.025 * t, d.z / t);
            } else {
                orb.setDeltaMovement(d.normalize().scale(speed));
            }
            c.level.addFreshEntity(orb);
            sound(c, e == Element.FIRE ? SoundEvents.BLAZE_SHOOT : SoundEvents.SNOWBALL_THROW, 0.7f, 0.5f + c.random.nextFloat() * 0.4f);
        }
    }

    // ------------------------------------------------------------------------------------------ spikes
    /** Jaws or spikes erupt from the ground in a line (or rings). */
    public static class Spikes extends Ability {
        final int count;
        final boolean radial;

        public Spikes(String id, int count, boolean radial) {
            super(id);
            this.count = count;
            this.radial = radial;
            this.minRange = radial ? 0 : 3;
            this.maxRange = radial ? 8 : 18;
            this.pose = AnimPose.REAR_UP;
            this.active = 20;
        }

        void plan(Ctx c) {
            c.marks.clear();
            if (radial) {
                for (int ring = 1; ring <= 2; ring++) {
                    int n = 6 * ring;
                    for (int i = 0; i < n; i++) {
                        double a = i * Math.PI * 2 / n;
                        c.marks.add(onGround(c, c.pos().add(Math.cos(a) * (c.size() * 0.5 + ring * 2), 0, Math.sin(a) * (c.size() * 0.5 + ring * 2))));
                    }
                }
            } else {
                Vec3 d = c.flatDirToAim();
                for (int i = 1; i <= count; i++) c.marks.add(onGround(c, c.pos().add(d.scale(c.size() * 0.5 + i * 1.3))));
            }
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            if (!c.aimLocked || c.marks.isEmpty()) plan(c);
            if (tick % 3 == 0) for (Vec3 m : c.marks) Markers.circle(c.level, m, 0.6, Markers.warn(progress));
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            int per = Math.max(1, c.marks.size() / Math.max(1, active - 2));
            for (int k = 0; k < per; k++) {
                int i = c.counter++;
                if (i >= c.marks.size()) return;
                Vec3 m = c.marks.get(i);
                EvokerFangs fangs = new EvokerFangs(c.level, m.x, m.y, m.z, c.mob.getYRot() * Mth.DEG_TO_RAD, 0, c.mob);
                c.level.addFreshEntity(fangs);
                for (LivingEntity v : HunterDamage.inRadius(c, m, 1.0, 2)) HunterDamage.hit(c, v, damage, Element.EARTH, 0.2);
            }
        }
    }

    // ------------------------------------------------------------------------------------------ nova
    /** Swells up and detonates. {@code chain} makes a marching line of blasts toward the target. */
    public static class Nova extends Ability {
        final float radius;
        final Element override;
        final int chain;
        final boolean lightning;

        public Nova(String id, float radius, Element override, int chain, boolean lightning) {
            super(id);
            this.radius = radius;
            this.override = override;
            this.chain = chain;
            this.lightning = lightning;
            this.pose = AnimPose.INFLATE;
            this.maxRange = chain > 0 ? 16 : radius;
            this.active = chain > 0 ? chain * 4 + 2 : 4;
        }

        float rad(Ctx c) {
            return radius + c.size() * 0.7f;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            if (tick % 6 == 0) sound(c, SoundEvents.CREEPER_PRIMED, 0.6f, 0.5f + progress);
            if (chain > 0) {
                if (!c.aimLocked) {
                    c.marks.clear();
                    Vec3 d = c.aim.subtract(c.pos());
                    for (int i = 1; i <= chain; i++) c.marks.add(onGround(c, c.pos().add(d.scale(i / (double) chain))));
                }
                if (tick % 3 == 0) for (Vec3 m : c.marks) Markers.fillingCircle(c.level, m, radius * 0.7, progress);
            } else if (tick % 3 == 0) Markers.fillingCircle(c.level, c.pos(), rad(c), progress);
            Element e = el(c, override);
            c.level.sendParticles(e.dust(2f), true, false, c.mob.getX(), c.mob.getY() + c.height() * 0.5, c.mob.getZ(), 4, c.size() * 0.4, c.height() * 0.4, c.size() * 0.4, 0);
        }

        void blast(Ctx c, Vec3 at, float r) {
            Element e = el(c, override);
            for (LivingEntity v : HunterDamage.inRadius(c, at, r, 4)) HunterDamage.hit(c, v, damage, e, 1.2);
            c.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, true, false, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
            c.level.sendParticles(e.particle(), true, false, at.x, at.y + 0.5, at.z, 30, r * 0.4, 0.5, r * 0.4, 0.1);
            c.level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5f, 0.8f);
            if (lightning) strikeVisual(c.level, at);
        }

        @Override
        public void begin(Ctx c) {
            if (chain == 0) blast(c, c.pos(), rad(c));
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            if (chain > 0 && tick % 4 == 0) {
                int i = c.counter++;
                if (i < c.marks.size()) {
                    c.hit.clear();
                    blast(c, c.marks.get(i), radius);
                }
            }
        }
    }

    public static void strikeVisual(ServerLevel level, Vec3 at) {
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.setPos(at);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    // ------------------------------------------------------------------------------------------ blink
    /** Vanishes and reappears behind the target, striking immediately. */
    public static class Blink extends Ability {
        public Blink(String id) {
            super(id);
            this.pose = AnimPose.SHAKE;
            this.minRange = 3;
            this.maxRange = 24;
            this.windup = 16;
            this.active = 6;
        }

        Vec3 behind(Ctx c) {
            Vec3 look = c.target.getLookAngle();
            look = new Vec3(look.x, 0, look.z);
            if (look.lengthSqr() < 1e-4) look = new Vec3(0, 0, 1);
            return c.target.position().subtract(look.normalize().scale(2 + c.size() * 0.6));
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.level.sendParticles(ParticleTypes.PORTAL, true, false, c.mob.getX(), c.mob.getY() + c.height() * 0.5, c.mob.getZ(), 8, c.size() * 0.4, c.height() * 0.4, c.size() * 0.4, 0.5);
            Vec3 b = behind(c);
            if (tick % 3 == 0) Markers.circle(c.level, onGround(c, b), 1 + c.size() * 0.4, ParticleTypes.REVERSE_PORTAL);
            if (tick == 0) sound(c, SoundEvents.ENDERMAN_STARE, 0.8f, 1.2f);
        }

        @Override
        public void begin(Ctx c) {
            Vec3 b = behind(c);
            c.level.sendParticles(ParticleTypes.REVERSE_PORTAL, true, false, c.mob.getX(), c.mob.getY() + 1, c.mob.getZ(), 40, c.size() * 0.5, c.height() * 0.5, c.size() * 0.5, 0.1);
            c.mob.teleportTo(b.x, ground(c, b), b.z);
            c.aim = c.target.position();
            c.faceAim();
            sound(c, SoundEvents.ENDERMAN_TELEPORT, 1.2f, 0.7f);
            for (LivingEntity v : HunterDamage.inCone(c, c.pos(), c.facing(), 2.5 + c.size(), 70)) HunterDamage.hit(c, v, damage, c.element, 0.8);
        }
    }

    // ------------------------------------------------------------------------------------------ summon
    public static class Summon extends Ability {
        final Function<Ctx, EntityType<?>> type;
        final int count;
        final boolean fromGround;
        final Tier minionTier;

        public Summon(String id, Function<Ctx, EntityType<?>> type, int count, boolean fromGround, Tier minionTier) {
            super(id);
            this.type = type;
            this.count = count;
            this.fromGround = fromGround;
            this.minionTier = minionTier;
            this.pose = AnimPose.ROAR;
            this.maxRange = 30;
            this.cooldown = 400;
            this.windup = 30;
            this.weight = 5;
        }

        @Override
        public boolean canUse(Ctx c, double dist) {
            if (!super.canUse(c, dist)) return false;
            int existing = c.level.getEntitiesOfClass(Mob.class, c.mob.getBoundingBox().inflate(32), m -> m.entityTags().contains(HunterDamage.MINION_TAG)).size();
            return existing < 6;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            if (tick == 0) sound(c, SoundEvents.EVOKER_PREPARE_SUMMON, 1.2f, 0.6f);
            Markers.converge(c.level, c.pos(), 3 + c.size(), tick, ParticleTypes.SOUL);
        }

        @Override
        public void begin(Ctx c) {
            EntityType<?> t = type.apply(c);
            int n = count + c.tier.rank - 1;
            for (int i = 0; i < n; i++) {
                double a = c.random.nextDouble() * Math.PI * 2;
                Vec3 base = fromGround ? c.target.position() : c.pos();
                double r = fromGround ? 3 + c.random.nextDouble() * 3 : c.size() + 1.5;
                Vec3 at = onGround(c, base.add(Math.cos(a) * r, 0, Math.sin(a) * r));
                if (!(t.create(c.level, EntitySpawnReason.MOB_SUMMONED) instanceof Mob m)) continue;
                m.snapTo(at.x, at.y, at.z, c.random.nextFloat() * 360f, 0f);
                m.addTag(HunterDamage.MINION_TAG);
                if (c.target != null) com.mobhunter.core.CombatEngine.setTarget(m, c.target);
                c.level.addFreshEntity(m);
                if (minionTier != Tier.NONE) TierManager.applyTier(m, minionTier, false);
                c.level.sendParticles(fromGround ? ParticleTypes.SOUL : ParticleTypes.POOF, true, false, at.x, at.y + 0.5, at.z, 15, 0.4, 0.5, 0.4, 0.05);
                if (fromGround) groundDust(c, at, 20, 0.6);
            }
        }
    }

    // ------------------------------------------------------------------------------------------ lightning
    public static class Lightning extends Ability {
        final int strikes;

        public Lightning(String id, int strikes) {
            super(id);
            this.strikes = strikes;
            this.pose = AnimPose.REAR_UP;
            this.maxRange = 26;
            this.windup = 32;
            this.active = 12;
            this.lockAt = 0.55f;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            if (!c.aimLocked) {
                c.marks.clear();
                c.marks.add(onGround(c, c.aim));
                int n = strikes + c.tier.rank;
                for (int i = 1; i < n; i++) {
                    double a = i * Math.PI * 2 / (n - 1) + c.mob.tickCount * 0.01;
                    c.marks.add(onGround(c, c.aim.add(Math.cos(a) * 4, 0, Math.sin(a) * 4)));
                }
            }
            if (tick % 3 == 0) for (Vec3 m : c.marks) {
                Markers.fillingCircle(c.level, m, 2.2, progress);
                c.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, false, m.x, m.y + 1 + c.random.nextDouble() * 4, m.z, 2, 0.2, 1, 0.2, 0);
            }
            if (tick == 0) sound(c, SoundEvents.TRIDENT_THUNDER.value(), 0.6f, 1.6f);
        }

        @Override
        public void begin(Ctx c) {
            for (Vec3 m : c.marks) {
                strikeVisual(c.level, m);
                for (LivingEntity v : HunterDamage.inRadius(c, m, 2.4, 4)) HunterDamage.hit(c, v, damage, Element.THUNDER, 0.5);
            }
        }
    }

    // ------------------------------------------------------------------------------------------ burrow
    /** Digs underground, tunnels beneath the target and erupts. */
    public static class Burrow extends Ability {
        public Burrow(String id) {
            super(id);
            this.pose = AnimPose.BURROW;
            this.windup = 26;
            this.active = 34;
            this.recovery = 22;
            this.minRange = 4;
            this.maxRange = 24;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            groundDust(c, c.pos(), 10, c.size() * 0.5);
            if (tick % 5 == 0) sound(c, SoundEvents.WARDEN_DIG, 0.8f, 1.2f);
        }

        @Override
        public void begin(Ctx c) {
            c.mob.setInvisible(true);
            c.mob.setPermanentlyInvulnerable(true);
            c.extra = c.pos();
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            Vec3 start = (Vec3) c.extra;
            if (tick < active - 12) c.aim = c.target.position();
            float t = Math.min(1f, (tick + 1) / (float) (active - 12));
            Vec3 under = start.add(c.aim.subtract(start).scale(t));
            groundDust(c, onGround(c, under), 8, 0.5 + c.size() * 0.2);
            if (tick % 4 == 0) sound(c, SoundEvents.ROOTED_DIRT_BREAK, 1f, 0.5f);
            if (tick >= active - 12) {
                Markers.fillingCircle(c.level, onGround(c, c.aim), 1.5 + c.size() * 0.7, (tick - (active - 12)) / 12f);
            }
            if (tick == active - 1) {
                Vec3 at = onGround(c, c.aim);
                c.mob.teleportTo(at.x, at.y, at.z);
                c.mob.setInvisible(false);
                c.mob.setPermanentlyInvulnerable(false);
                c.mob.setDeltaMovement(0, 0.5, 0);
                for (LivingEntity v : HunterDamage.inRadius(c, at, 1.5 + c.size() * 0.7, 4)) {
                    if (HunterDamage.hit(c, v, damage, c.element, 0.5)) {
                        v.push(0, 1.0, 0);
                        v.needsSync = true;
                    }
                }
                groundDust(c, at, 60, c.size());
                sound(c, SoundEvents.WARDEN_EMERGE, 1.5f, 1f);
            }
        }

        @Override
        public void end(Ctx c) {
            c.mob.setInvisible(false);
            c.mob.setPermanentlyInvulnerable(false);
        }
    }

    // ------------------------------------------------------------------------------------------ dive
    /** Rises into the sky, then dives onto the marked spot. */
    public static class Dive extends Ability {
        final Element override;

        public Dive(String id, Element override) {
            super(id);
            this.override = override;
            this.pose = AnimPose.HOVER;
            this.windup = 30;
            this.active = 30;
            this.minRange = 3;
            this.maxRange = 26;
            this.lockAt = 0.8f;
        }

        float rad(Ctx c) {
            return 2 + c.size() * 0.6f;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            if (tick == 0) {
                c.savedNoGravity = c.mob.isNoGravity();
                c.mob.setNoGravity(true);
                sound(c, SoundEvents.PHANTOM_SWOOP, 1.4f, 0.6f);
            }
            double targetY = c.aim.y + 7 + c.height();
            double vy = c.mob.getY() < targetY ? 0.25 : 0;
            c.mob.setDeltaMovement(c.mob.getDeltaMovement().multiply(0.6, 0, 0.6).add(0, vy, 0));
            if (tick % 3 == 0) Markers.fillingCircle(c.level, onGround(c, c.aim), rad(c), progress);
        }

        @Override
        public void begin(Ctx c) {
            c.dir = onGround(c, c.aim).subtract(c.center()).normalize();
            sound(c, SoundEvents.PHANTOM_BITE, 1.5f, 0.5f);
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            c.mob.setDeltaMovement(c.dir.scale(1.1 + c.size() * 0.1));
            c.mob.needsSync = true;
            c.level.sendParticles(ParticleTypes.CLOUD, true, false, c.mob.getX(), c.mob.getY() + c.height() * 0.5, c.mob.getZ(), 2, 0.2, 0.2, 0.2, 0);
            boolean arrived = c.center().distanceTo(onGround(c, c.aim)) < 1.5 + c.size() * 0.5;
            if (arrived || c.mob.onGround() || c.mob.horizontalCollision || c.mob.verticalCollision) {
                Element e = el(c, override);
                for (LivingEntity v : HunterDamage.inRadius(c, c.pos(), rad(c), 3 + c.height())) HunterDamage.hit(c, v, damage, e, 1.0);
                c.level.sendParticles(ParticleTypes.EXPLOSION, true, false, c.mob.getX(), c.mob.getY(), c.mob.getZ(), 2, 0.5, 0.1, 0.5, 0);
                groundDust(c, c.pos(), 30, rad(c) * 0.5);
                sound(c, SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 1.0f);
                c.finished = true;
            }
        }

        @Override
        public void end(Ctx c) {
            c.mob.setNoGravity(c.savedNoGravity);
            c.mob.setDeltaMovement(0, 0.2, 0);
        }
    }

    // ------------------------------------------------------------------------------------------ roar
    public static class Roar extends Ability {
        public Roar(String id) {
            super(id);
            this.pose = AnimPose.ROAR;
            this.windup = 14;
            this.active = 26;
            this.recovery = 10;
            this.maxRange = 14;
            this.cooldown = 500;
            this.weight = 4;
        }

        static void roarAt(Ctx c, float radius, boolean stun) {
            sound(c, c.tier == Tier.DEITY ? SoundEvents.WARDEN_ROAR : SoundEvents.RAVAGER_ROAR, 2.5f, c.tier == Tier.DEITY ? 0.6f : 0.75f);
            c.level.sendParticles(ParticleTypes.SONIC_BOOM, true, false, c.mob.getX(), c.mob.getY() + c.height() * 0.7, c.mob.getZ(), 1, 0, 0, 0, 0);
            for (LivingEntity v : HunterDamage.inRadius(c, c.pos(), radius, c.height() + 4)) {
                Vec3 d = v.position().subtract(c.pos()).normalize();
                v.push(d.x * 0.9, 0.3, d.z * 0.9);
                v.needsSync = true;
                if (stun) {
                    v.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 3));
                    v.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 40, 2));
                }
            }
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            if (tick % 4 == 0) Markers.circle(c.level, c.pos(), 6 + c.size() * 1.5, Markers.warn(progress));
        }

        @Override
        public void begin(Ctx c) {
            roarAt(c, 6 + c.size() * 1.5f, true);
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            if (tick % 4 == 0) {
                float r = (tick + 2) * 0.6f + c.size();
                Markers.circle(c.level, c.pos().add(0, c.height() * 0.5, 0), r, ParticleTypes.CLOUD);
            }
        }
    }

    // ------------------------------------------------------------------------------------------ cloud
    /** Envelops the area in a lingering cloud (sand, spores, ink, mud). */
    public static class Cloud extends Ability {
        final float radius;
        final Element override;
        final boolean blind;

        public Cloud(String id, float radius, Element override, boolean blind) {
            super(id);
            this.radius = radius;
            this.override = override;
            this.blind = blind;
            this.pose = AnimPose.SHAKE;
            this.maxRange = radius;
            this.active = 60;
            this.windup = 22;
        }

        float rad(Ctx c) {
            return radius + c.size();
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            Element e = el(c, override);
            Markers.converge(c.level, c.pos(), rad(c), tick, e.dust(2f));
            if (tick % 4 == 0) Markers.circle(c.level, c.pos(), rad(c), Markers.warn(progress));
        }

        @Override
        public void begin(Ctx c) {
            sound(c, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.2f, 0.5f);
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            Element e = el(c, override);
            float r = rad(c);
            for (int i = 0; i < 8; i++) {
                double a = c.random.nextDouble() * Math.PI * 2, d = c.random.nextDouble() * r;
                c.level.sendParticles(blind ? ParticleTypes.SQUID_INK : ParticleTypes.CAMPFIRE_COSY_SMOKE, true, false, c.mob.getX() + Math.cos(a) * d,
                        c.mob.getY() + c.random.nextDouble() * 3, c.mob.getZ() + Math.sin(a) * d, 0, 0, 0.02, 0, 1);
                c.level.sendParticles(e.dust(3f), true, false, c.mob.getX() + Math.cos(a) * d, c.mob.getY() + c.random.nextDouble() * 3,
                        c.mob.getZ() + Math.sin(a) * d, 1, 0.3, 0.3, 0.3, 0);
            }
            if (tick % 10 == 0) {
                for (LivingEntity v : HunterDamage.inRadius(c, c.pos(), r, 5)) {
                    HunterDamage.hitAgain(c, v, damage * 0.25f, e, 0);
                    if (blind) v.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------ pull
    /** Tongue / tentacle / hook that drags the target in. */
    public static class Pull extends Ability {
        public Pull(String id) {
            super(id);
            this.pose = AnimPose.LEAN_BACK;
            this.minRange = 3;
            this.maxRange = 14;
            this.windup = 16;
            this.active = 6;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            if (tick % 3 == 0) Markers.line(c.level, c.pos(), onGround(c, c.aim), 0.5, Markers.warn(progress));
        }

        @Override
        public void begin(Ctx c) {
            Vec3 mouth = c.center().add(c.facing().scale(c.size() * 0.5));
            for (LivingEntity v : HunterDamage.alongLine(c, mouth, c.aim.add(0, 1, 0).add(c.flatDirToAim().scale(1.5)), 1.2)) {
                if (HunterDamage.hit(c, v, damage, c.element, 0)) {
                    Vec3 d = mouth.subtract(v.position()).normalize();
                    v.setDeltaMovement(d.scale(1.4).add(0, 0.3, 0));
                    v.needsSync = true;
                }
            }
            for (double t = 0; t < 1; t += 0.05) {
                Vec3 p = mouth.add(c.aim.add(0, 1, 0).subtract(mouth).scale(t));
                c.level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(0xE06080, 1.2f), true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            sound(c, SoundEvents.FROG_TONGUE, 1.5f, 0.5f);
        }
    }

    // ------------------------------------------------------------------------------------------ gaze
    /** Locks on with a tracking beam, then fires a devastating ray. */
    public static class Gaze extends Ability {
        final Element override;

        public Gaze(String id, Element override) {
            super(id);
            this.override = override;
            this.pose = AnimPose.LEAN_BACK;
            this.windup = 44;
            this.active = 4;
            this.recovery = 26;
            this.minRange = 3;
            this.maxRange = 30;
            this.lockAt = 0.85f;
        }

        Vec3 eye(Ctx c) {
            return c.mob.getEyePosition().add(c.facing().scale(c.size() * 0.3));
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            Vec3 from = eye(c), to = c.aim.add(0, c.target.getBbHeight() * 0.5, 0);
            Element e = el(c, override);
            if (tick % 2 == 0) for (double t = 0; t < 1; t += 0.04) {
                Vec3 p = from.add(to.subtract(from).scale(t));
                c.level.sendParticles(progress > 0.8f ? Markers.warn(1f) : e.dust(0.8f + progress), true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            if (tick % 10 == 0) sound(c, SoundEvents.GUARDIAN_ATTACK, 1f, 0.5f + progress);
        }

        @Override
        public void begin(Ctx c) {
            Vec3 from = eye(c);
            Vec3 dir = c.aim.add(0, c.target.getBbHeight() * 0.5, 0).subtract(from).normalize();
            Vec3 to = from.add(dir.scale(maxRange + c.size() * 2));
            for (double t = 0; t < 1; t += 0.03) {
                Vec3 p = from.add(to.subtract(from).scale(t));
                c.level.sendParticles(ParticleTypes.SONIC_BOOM, true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            for (LivingEntity v : HunterDamage.alongLine(c, from, to, 1.3)) HunterDamage.hit(c, v, damage, el(c, override), 1.4);
            sound(c, SoundEvents.WARDEN_SONIC_BOOM, 2.5f, 0.8f);
        }
    }

    // ------------------------------------------------------------------------------------------ void pull
    /** Drags everything toward itself, then implodes. */
    public static class VoidPull extends Ability {
        public VoidPull(String id) {
            super(id);
            this.pose = AnimPose.INFLATE;
            this.windup = 46;
            this.active = 4;
            this.recovery = 30;
            this.maxRange = 22;
            this.cooldown = 360;
        }

        float rad(Ctx c) {
            return 4 + c.size() * 0.8f;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            Markers.converge(c.level, c.pos(), 14 + c.size(), tick, ParticleTypes.REVERSE_PORTAL);
            if (tick % 3 == 0) Markers.fillingCircle(c.level, c.pos(), rad(c), progress);
            if (tick % 10 == 0) sound(c, SoundEvents.PORTAL_AMBIENT, 2f, 0.5f);
            for (LivingEntity v : HunterDamage.inRadius(c, c.pos(), 16 + c.size(), 8)) {
                Vec3 d = c.pos().subtract(v.position()).normalize().scale(0.06 + 0.04 * progress);
                v.push(d.x, 0, d.z);
                v.needsSync = true;
            }
        }

        @Override
        public void begin(Ctx c) {
            for (LivingEntity v : HunterDamage.inRadius(c, c.pos(), rad(c), c.height() + 3)) HunterDamage.hit(c, v, damage, Element.VOID, 1.8);
            c.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, true, false, c.mob.getX(), c.mob.getY() + 1, c.mob.getZ(), 2, 1, 0.5, 1, 0);
            c.level.sendParticles(ParticleTypes.REVERSE_PORTAL, true, false, c.mob.getX(), c.mob.getY() + 1, c.mob.getZ(), 120, rad(c) * 0.5, 1, rad(c) * 0.5, 0.4);
            sound(c, SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.4f);
        }
    }

    // ------------------------------------------------------------------------------------------ meteor rain
    /** Calls down a rain of elemental meteors on marked spots. */
    public static class MeteorRain extends Ability {
        final int meteors;
        final Element override;

        public MeteorRain(String id, int meteors, Element override) {
            super(id);
            this.meteors = meteors;
            this.override = override;
            this.pose = AnimPose.REAR_UP;
            this.windup = 36;
            this.active = 50;
            this.recovery = 20;
            this.maxRange = 32;
            this.cooldown = 300;
            this.lockAt = 0.4f;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            if (!c.aimLocked) {
                c.marks.clear();
                c.marks.add(onGround(c, c.aim));
                for (int i = 1; i < meteors; i++) {
                    double a = c.random.nextDouble() * Math.PI * 2, r = 2 + c.random.nextDouble() * 9;
                    c.marks.add(onGround(c, c.aim.add(Math.cos(a) * r, 0, Math.sin(a) * r)));
                }
            }
            if (tick % 4 == 0) for (Vec3 m : c.marks) Markers.fillingCircle(c.level, m, 2.0, progress);
            if (tick == 0) sound(c, SoundEvents.WITHER_SPAWN, 0.8f, 1.4f);
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            int every = Math.max(1, active / Math.max(1, c.marks.size()));
            if (tick % every != 0) return;
            int i = c.counter++;
            if (i >= c.marks.size()) return;
            Vec3 m = c.marks.get(i);
            Element e = el(c, override);
            ElementOrb orb = ElementOrb.create(c.level, c.mob, m.add(c.random.nextDouble() * 4 - 2, 26, c.random.nextDouble() * 4 - 2), e, c.dmg(damage), 2.2f, c.potency(), 1.4f);
            orb.setDeltaMovement(m.subtract(orb.position()).normalize().scale(1.3));
            c.level.addFreshEntity(orb);
        }
    }

    // ------------------------------------------------------------------------------------------ gale
    /** Inhales, then blasts a gust that throws foes away. */
    public static class Gale extends Ability {
        public Gale(String id) {
            super(id);
            this.pose = AnimPose.INFLATE;
            this.windup = 24;
            this.active = 12;
            this.maxRange = 14;
        }

        @Override
        public void telegraph(Ctx c, int tick, float progress) {
            c.faceAim();
            c.dir = c.flatDirToAim();
            if (tick % 3 == 0) Markers.cone(c.level, c.pos(), c.dir, 10 + c.size(), 35, Markers.warn(progress));
            Vec3 m = c.center().add(c.dir.scale(c.size() * 0.6 + 3));
            c.level.sendParticles(ParticleTypes.SMALL_GUST, true, false, m.x, m.y, m.z, 2, 1.5, 1.5, 1.5, 0);
        }

        @Override
        public void begin(Ctx c) {
            sound(c, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 2f, 0.6f);
            for (LivingEntity v : HunterDamage.inCone(c, c.pos(), c.dir, 10 + c.size(), 35)) {
                HunterDamage.hit(c, v, damage, Element.WIND, 0);
                v.setDeltaMovement(c.dir.x * 2.2, 0.6, c.dir.z * 2.2);
                v.needsSync = true;
            }
        }

        @Override
        public void activeTick(Ctx c, int tick) {
            Vec3 m = c.center().add(c.dir.scale(c.size() * 0.6 + tick));
            c.level.sendParticles(ParticleTypes.GUST, true, false, m.x, m.y, m.z, 1, 0.5, 0.5, 0.5, 0);
        }
    }

    private Families() {}
}
