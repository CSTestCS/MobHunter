package com.mobhunter.ability;

import com.mobhunter.core.Element;
import com.mobhunter.gear.GearEffects;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Damage dealing helpers shared by all abilities. */
public final class HunterDamage {
    public static final String MINION_TAG = "mobhunter_minion";
    /** Set while an ability deals damage, so the "no plain melee" rule lets it through. */
    public static final ThreadLocal<Boolean> ABILITY_DAMAGE = ThreadLocal.withInitial(() -> false);

    public static boolean isFoe(LivingEntity mob, Entity e) {
        if (!(e instanceof LivingEntity le) || !le.isAlive() || e == mob) return false;
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(e)) return false;
        if (e.getType() == mob.getType()) return false;
        if (e.entityTags().contains(MINION_TAG)) return false;
        if (mob.isPassengerOfSameVehicle(e) || e.isPassengerOfSameVehicle(mob)) return false;
        return true;
    }

    public static List<LivingEntity> inRadius(Ctx c, Vec3 center, double radius, double height) {
        AABB box = new AABB(center.x - radius, center.y - 1, center.z - radius, center.x + radius, center.y + height, center.z + radius);
        Predicate<LivingEntity> p = e -> isFoe(c.mob, e) && horizontalDist(e.position(), center) <= radius + e.getBbWidth() * 0.5;
        return c.level.getEntitiesOfClass(LivingEntity.class, box, p);
    }

    public static List<LivingEntity> inCone(Ctx c, Vec3 origin, Vec3 dir, double length, double halfAngleDeg) {
        double cos = Math.cos(Math.toRadians(halfAngleDeg));
        AABB box = new AABB(origin, origin).inflate(length, length * 0.6 + c.height(), length);
        return c.level.getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (!isFoe(c.mob, e)) return false;
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(origin);
            Vec3 flat = new Vec3(to.x, 0, to.z);
            double d = flat.length();
            if (d > length + e.getBbWidth() * 0.5) return false;
            if (d < c.size() * 0.5 + 0.5) return true;
            return flat.normalize().dot(new Vec3(dir.x, 0, dir.z).normalize()) >= cos;
        });
    }

    public static List<LivingEntity> alongLine(Ctx c, Vec3 from, Vec3 to, double halfWidth) {
        AABB box = new AABB(from, to).inflate(halfWidth + 1);
        Vec3 seg = to.subtract(from);
        double len2 = seg.lengthSqr();
        return c.level.getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (!isFoe(c.mob, e)) return false;
            Vec3 p = e.position().add(0, e.getBbHeight() * 0.5, 0);
            double t = len2 < 1e-6 ? 0 : Math.max(0, Math.min(1, p.subtract(from).dot(seg) / len2));
            Vec3 closest = from.add(seg.scale(t));
            return p.distanceTo(closest) <= halfWidth + e.getBbWidth() * 0.5;
        });
    }

    public static double horizontalDist(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Deals ability damage with elemental affinity and resistances. Returns true if it landed. */
    public static boolean hit(Ctx c, LivingEntity victim, float baseDamage, Element element, double knockback) {
        if (!c.hit.add(victim.getUUID()) && baseDamage > 0) return false;
        return hitAgain(c, victim, baseDamage, element, knockback);
    }

    /** Like {@link #hit} but ignores the "once per use" rule (multi-hit moves). */
    public static boolean hitAgain(Ctx c, LivingEntity victim, float baseDamage, Element element, double knockback) {
        return apply(c.level, c.mob, victim, c.dmg(baseDamage), element, c.potency(), knockback);
    }

    public static boolean apply(ServerLevel level, LivingEntity attacker, LivingEntity victim, float amount, Element element, int potency, double knockback) {
        float mult = GearEffects.resistance(victim, element);
        boolean landed;
        ABILITY_DAMAGE.set(true);
        try {
            landed = victim.hurtServer(level, level.damageSources().mobAttack(attacker), amount * mult);
        } finally {
            ABILITY_DAMAGE.set(false);
        }
        if (landed) {
            if (mult > 0.3f) element.afflict(victim, potency);
            if (element == Element.BLAST) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, victim.getX(), victim.getY() + 1, victim.getZ(), 1, 0, 0, 0, 0);
            }
            if (knockback > 0) {
                Vec3 d = victim.position().subtract(attacker.position());
                d = new Vec3(d.x, 0, d.z);
                if (d.lengthSqr() < 1e-4) d = new Vec3(0, 0, 1);
                d = d.normalize().scale(knockback);
                double resist = victim.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
                d = d.scale(Math.max(0.2, 1 - resist));
                victim.push(d.x, Math.min(0.9, 0.25 + knockback * 0.25), d.z);
                victim.needsSync = true;
            }
        }
        return landed;
    }

    private HunterDamage() {}
}
