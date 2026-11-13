package com.mobhunter.ability;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Ground telegraphs drawn with particles so players can read where an attack will land. */
public final class Markers {
    /** Warning colour of a telegraph: yellow early, red when about to fire. */
    public static DustParticleOptions warn(float progress) {
        int r = 255;
        int g = (int) (220 * (1f - progress));
        return new DustParticleOptions((r << 16) | (g << 8) | 30, 1.6f);
    }

    public static void particle(ServerLevel level, ParticleOptions p, double x, double y, double z) {
        level.sendParticles(p, true, false, x, y, z, 1, 0, 0, 0, 0);
    }

    public static void particle(ServerLevel level, ParticleOptions p, Vec3 v, int count, double spread, double speed) {
        level.sendParticles(p, true, false, v.x, v.y, v.z, count, spread, spread, spread, speed);
    }

    public static void circle(ServerLevel level, Vec3 c, double radius, ParticleOptions p) {
        int n = (int) Math.max(12, radius * 7);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            particle(level, p, c.x + Math.cos(a) * radius, c.y + 0.15, c.z + Math.sin(a) * radius);
        }
    }

    /** Filled disc that "fills up" as the wind-up progresses. */
    public static void fillingCircle(ServerLevel level, Vec3 c, double radius, float progress) {
        circle(level, c, radius, warn(progress));
        double inner = radius * progress;
        if (inner > 0.3) circle(level, c, inner, warn(progress));
    }

    public static void line(ServerLevel level, Vec3 from, Vec3 to, double halfWidth, ParticleOptions p) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.01) return;
        Vec3 dir = d.scale(1 / len);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        for (double t = 0; t <= len; t += 0.6) {
            Vec3 m = from.add(dir.scale(t));
            particle(level, p, m.x + side.x * halfWidth, m.y + 0.15, m.z + side.z * halfWidth);
            particle(level, p, m.x - side.x * halfWidth, m.y + 0.15, m.z - side.z * halfWidth);
        }
    }

    /** Fan-shaped marker in front of an origin. */
    public static void cone(ServerLevel level, Vec3 origin, Vec3 dir, double length, double halfAngleDeg, ParticleOptions p) {
        double base = Math.atan2(dir.z, dir.x);
        double half = Math.toRadians(halfAngleDeg);
        for (double r = 1; r <= length; r += 1.2) {
            int n = (int) Math.max(3, r * half * 2);
            for (int i = 0; i <= n; i++) {
                double a = base - half + (2 * half) * i / n;
                particle(level, p, origin.x + Math.cos(a) * r, origin.y + 0.15, origin.z + Math.sin(a) * r);
            }
        }
    }

    /** Spiral of particles converging on a point (used for "gathering power"). */
    public static void converge(ServerLevel level, Vec3 c, double radius, int tick, ParticleOptions p) {
        for (int i = 0; i < 6; i++) {
            double a = tick * 0.35 + i * Math.PI / 3;
            double r = radius * (1 - (tick % 20) / 20.0);
            level.sendParticles(p, true, false, c.x + Math.cos(a) * r, c.y + 0.5 + (i % 3), c.z + Math.sin(a) * r, 1,
                    0, 0, 0, 0);
        }
    }

    private Markers() {}
}
