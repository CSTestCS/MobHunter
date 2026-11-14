package com.mobhunter.client.anatomy;

import com.mobhunter.core.Tier;
import com.mobhunter.profile.Bestiary;
import com.mobhunter.profile.Bestiary.Feature;
import com.mobhunter.client.anatomy.Skeleton.Bone;
import com.mobhunter.client.anatomy.Skeleton.Region;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Grows the custom anatomy of an Alpha, Monstrous or Deity creature onto its skeleton.
 * <ul>
 *     <li>Alpha: thicker reinforced material and a modest version of its defining feature.</li>
 *     <li>Monstrous: every structure exaggerated, and a clearly visible glowing core.</li>
 *     <li>Deity: an enormous defining feature and a blazing heart.</li>
 * </ul>
 */
public final class Anatomy {
    static final int IVORY = 0xFFE8DCC0, GOLD = 0xFFF2C230, RED_CAP = 0xFFB0302A, LEAF = 0xFF4C9A2A;

    static int argb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    static int shade(int argb, float k) {
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * k)), g = Math.min(255, (int) (((argb >> 8) & 0xFF) * k)), b = Math.min(255, (int) ((argb & 0xFF) * k));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static int mix(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF), g = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF), bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    static float hash(int i, int salt) {
        int h = i * 374761393 + salt * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }

    /** Builds the anatomy for one frame. */
    public static void build(Skeleton sk, Bestiary.Entry e, int creatureColor, Tier tier, float age, BoxBuffer solid, BoxBuffer glow) {
        if (sk.body == null) return;
        Ctx c = new Ctx(sk, e, creatureColor, tier, age, solid, glow);
        boolean first = true;
        int n = 0;
        for (Feature f : e.features()) {
            if (tier == Tier.ALPHA && n >= 1) break;
            float s = switch (tier) {
                case ALPHA -> 0.7f;
                case MONSTROUS -> first ? 2.0f : 1.5f;
                case DEITY -> first ? 3.4f : 1.8f;
                default -> 0;
            };
            c.feature(f, s, n);
            first = false;
            n++;
        }
        // Alphas are reinforced: thicker plating of their defining material.
        if (tier == Tier.ALPHA && !e.features().get(0).equals(Feature.PLATES)) c.feature(Feature.PLATES, 0.5f, 7);
        if (tier == Tier.MONSTROUS || tier == Tier.DEITY) c.core(tier == Tier.DEITY ? 2.2f : 1.4f);
    }

    static final class Ctx {
        final Skeleton sk;
        final Bone head, body;
        final Bestiary.Entry e;
        final int primary, dark, element, salt;
        final Tier tier;
        final float t;
        final BoxBuffer solid, glow;

        Ctx(Skeleton sk, Bestiary.Entry e, int color, Tier tier, float age, BoxBuffer solid, BoxBuffer glow) {
            this.sk = sk;
            this.head = sk.head;
            this.body = sk.body;
            this.e = e;
            this.primary = tier == Tier.ALPHA ? shade(argb(color), 1.15f) : argb(color);
            this.dark = shade(argb(color), 0.55f);
            this.element = argb(e.elementColor());
            this.salt = e.id().hashCode();
            this.tier = tier;
            this.t = age;
            this.solid = solid;
            this.glow = glow;
        }

        float rnd(int i) {
            return hash(i, salt);
        }

        /** A curving chain of tapering segments: horns, tusks, tails, tendrils, stingers. */
        void chain(Matrix4f base, int segs, float size, float len, float rx, float ry, float rz, int color, int tile, BoxBuffer buf, float taper) {
            Matrix4f m = new Matrix4f(base);
            float sz = size;
            for (int k = 0; k < segs; k++) {
                buf.box(m, -sz / 2, -len, -sz / 2, sz / 2, 0, sz / 2, color, tile);
                m.translate(0, -len / 16f * 0.92f, 0).rotateZYX(rz, ry, rx);
                sz = Math.max(0.4f, sz * taper);
            }
        }

        void feature(Feature f, float s, int index) {
            switch (f) {
                case HORNS -> horns(s);
                case TUSKS -> tusks(s);
                case SPIKES -> spikes(s);
                case PLATES -> plates(s);
                case CRYSTALS -> crystals(s);
                case BONES -> bones(s);
                case FEATHERS -> feathers(s);
                case FINS -> fins(s);
                case FUNGUS -> fungus(s);
                case FOLIAGE -> foliage(s);
                case FLAMES -> flames(s);
                case CLAWS -> claws(s);
                case MANE -> mane(s);
                case SHELL -> shell(s);
                case CROWN -> crown(s);
                case STINGER -> stinger(s);
                case WINGS -> wings(s);
                case TENDRILS -> tendrils(s);
                case HALO -> halo(s);
            }
        }

        void horns(float s) {
            Bone h = head;
            for (int side = -1; side <= 1; side += 2) {
                Matrix4f m = h.at(h.cx() + side * h.w() * 0.38f, h.y0() + 0.5f, h.cz() + h.d() * 0.1f).rotateZYX(side * -0.55f, 0, 0.25f);
                chain(m, 4, 2.2f * s, 2.6f * s, 0.55f, 0, side * -0.35f, mix(IVORY, primary, 0.15f), BoxBuffer.BANDED, solid, 0.78f);
            }
        }

        void tusks(float s) {
            Bone h = head;
            for (int side = -1; side <= 1; side += 2) {
                Matrix4f m = h.at(h.cx() + side * h.w() * 0.32f, h.y1() - 0.5f, h.z0() + 1).rotateZYX(side * -0.2f, 0, -1.9f);
                chain(m, 3, 1.6f * s, 2.4f * s, -0.55f, 0, 0, IVORY, BoxBuffer.BANDED, solid, 0.75f);
            }
        }

        void spikes(float s) {
            Bone b = body;
            boolean alongZ = b.d() >= b.w();
            int n = 3 + 2 * tier.rank;
            for (int i = 0; i < n; i++) {
                float u = (i + 0.5f) / n;
                float x = alongZ ? b.cx() : Mth.lerp(u, b.x0() + 1, b.x1() - 1);
                float z = alongZ ? Mth.lerp(u, b.z0() + 1, b.z1() - 1) : b.cz();
                float hgt = (2.5f + 2.5f * Mth.sin(u * Mth.PI)) * s;
                Matrix4f m = b.at(x, b.y0(), z).rotateZYX(0, 0, 0.25f);
                chain(m, 3, 2.2f * s, hgt / 2, 0.12f, 0, 0, dark, BoxBuffer.BANDED, solid, 0.6f);
            }
            for (Bone tail : sk.all(Region.TAIL)) {
                Matrix4f m = tail.at(tail.cx(), tail.y0(), tail.cz());
                chain(m, 2, 1.4f * s, 1.6f * s, 0.2f, 0, 0, dark, BoxBuffer.BANDED, solid, 0.6f);
            }
        }

        void plates(float s) {
            float th = 1.2f * s + (tier == Tier.ALPHA ? 0.4f : 0);
            int col = tier == Tier.ALPHA ? shade(primary, 0.9f) : shade(primary, 0.7f);
            Bone b = body;
            int segs = 3;
            for (int i = 0; i < segs; i++) {
                float z0 = Mth.lerp(i / (float) segs, b.z0(), b.z1()), z1 = Mth.lerp((i + 1.1f) / segs, b.z0(), b.z1());
                solid.box(b.m(), b.x0() - 0.6f * s, b.y0() - th, z0, b.x1() + 0.6f * s, b.y0() + 0.2f, Math.min(b.z1(), z1), shade(col, 1 - i * 0.06f), BoxBuffer.PLATE);
            }
            solid.box(b.m(), b.x0() - th, b.y0(), b.z0() + 1, b.x0(), b.cy(), b.z1() - 1, col, BoxBuffer.PLATE);
            solid.box(b.m(), b.x1(), b.y0(), b.z0() + 1, b.x1() + th, b.cy(), b.z1() - 1, col, BoxBuffer.PLATE);
            if (head != body) {
                Bone h = head;
                solid.box(h.m(), h.x0() - 0.3f, h.y0() - th, h.z0() - 0.3f, h.x1() + 0.3f, h.y0() + 0.2f, h.cz(), col, BoxBuffer.PLATE);
            }
        }

        void crystals(float s) {
            Bone b = body;
            int n = 3 + 2 * tier.rank;
            for (int i = 0; i < n; i++) {
                float x = Mth.lerp(rnd(i), b.x0() + 1, b.x1() - 1), z = Mth.lerp(rnd(i + 40), b.z0() + 1, b.z1() - 1);
                for (int k = 0; k < 3; k++) {
                    Matrix4f m = b.at(x, b.y0(), z).rotateZYX((rnd(i * 3 + k) - 0.5f) * 1.1f, rnd(i * 7 + k) * 3f, (rnd(i * 5 + k + 9) - 0.5f) * 1.1f);
                    float len = (3 + 3 * rnd(i * 11 + k)) * s;
                    solid.box(m, -0.9f * s, -len, -0.9f * s, 0.9f * s, 0.5f, 0.9f * s, element, BoxBuffer.CRYSTAL);
                    if (tier == Tier.DEITY) glow.box(m, -0.5f * s, -len * 0.9f, -0.5f * s, 0.5f * s, 0, 0.5f * s, element, 0);
                }
            }
        }

        void bones(float s) {
            Bone b = body;
            int ribs = 3 + tier.rank;
            for (int i = 0; i < ribs; i++) {
                float z = Mth.lerp((i + 0.5f) / ribs, b.z0() + 1, b.z1() - 1);
                for (int side = -1; side <= 1; side += 2) {
                    Matrix4f m = b.at(side < 0 ? b.x0() : b.x1(), b.y0() + b.h() * 0.25f, z).rotateZYX(side * 1.9f, 0, 0);
                    chain(m, 3, 1.1f * s, 2.2f * s, 0, 0, side * 0.45f, IVORY, BoxBuffer.BANDED, solid, 0.85f);
                }
            }
            for (Bone leg : sk.all(Region.LEG)) {
                Matrix4f m = leg.at(leg.cx(), leg.cy(), leg.z1()).rotateZYX(0, 0, 1.3f);
                chain(m, 2, 1f * s, 1.8f * s, 0.3f, 0, 0, IVORY, BoxBuffer.BANDED, solid, 0.6f);
            }
        }

        void feathers(float s) {
            Bone h = head;
            for (int i = -2; i <= 2; i++) {
                Matrix4f m = h.at(h.cx() + i * 0.6f, h.y0(), h.cz()).rotateZYX(i * 0.18f, 0, 0.35f);
                solid.box(m, -0.4f, -(4 + 2 * (2 - Math.abs(i))) * s, -1.2f * s, 0.4f, 0.3f, 1.2f * s, i % 2 == 0 ? primary : element, BoxBuffer.ORGANIC);
            }
            Bone b = body;
            for (int i = 0; i < 4; i++) {
                for (int side = -1; side <= 1; side += 2) {
                    Matrix4f m = b.at(b.cx() + side * b.w() * 0.3f, b.y0(), Mth.lerp(i / 4f, b.cz(), b.z1())).rotateZYX(side * -0.5f, 0, 0.9f);
                    solid.box(m, -0.4f, -4.5f * s, -1.2f * s, 0.4f, 0, 1.2f * s, shade(primary, 0.85f), BoxBuffer.ORGANIC);
                }
            }
        }

        void fins(float s) {
            Bone b = body;
            boolean alongZ = b.d() >= b.w();
            int n = 4;
            for (int i = 0; i < n; i++) {
                float u = (i + 0.5f) / n;
                float hgt = (5 - i) * 0.9f * s;
                float z = alongZ ? Mth.lerp(u, b.z0() + 1, b.z1() - 1) : b.cz();
                solid.box(b.m(), b.cx() - 0.35f, b.y0() - hgt, z - 1.2f * s, b.cx() + 0.35f, b.y0(), z + 1.2f * s, mix(primary, element, 0.35f), BoxBuffer.ORGANIC);
            }
            for (int side = -1; side <= 1; side += 2) {
                float wave = Mth.sin(t * 0.25f + side) * 0.3f;
                Matrix4f m = b.at(side < 0 ? b.x0() : b.x1(), b.cy(), b.z0() + b.d() * 0.3f).rotateZYX(side * (1.2f + wave), 0, 0.4f);
                solid.box(m, -0.3f, -4 * s, -1.5f * s, 0.3f, 0, 1.5f * s, mix(primary, element, 0.5f), BoxBuffer.ORGANIC);
            }
        }

        void fungus(float s) {
            Bone b = body;
            int n = 2 + tier.rank;
            for (int i = 0; i < n; i++) {
                float x = Mth.lerp(rnd(i + 3), b.x0() + 1, b.x1() - 1), z = Mth.lerp(rnd(i + 13), b.z0() + 1, b.z1() - 1);
                float stem = (2 + 2 * rnd(i + 23)) * s, cap = (3 + 2 * rnd(i + 33)) * s;
                Matrix4f m = b.at(x, b.y0(), z);
                solid.box(m, -0.8f * s, -stem, -0.8f * s, 0.8f * s, 0, 0.8f * s, IVORY, BoxBuffer.ORGANIC);
                solid.box(m, -cap / 2, -stem - 1.4f * s, -cap / 2, cap / 2, -stem, cap / 2, i % 2 == 0 ? RED_CAP : shade(primary, 0.8f), BoxBuffer.ORGANIC);
            }
        }

        void foliage(float s) {
            Bone b = body;
            int n = 4 + tier.rank * 2;
            for (int i = 0; i < n; i++) {
                float x = Mth.lerp(rnd(i + 5), b.x0(), b.x1()), z = Mth.lerp(rnd(i + 15), b.z0(), b.z1());
                float sz = (2 + 1.5f * rnd(i + 25)) * s;
                solid.box(b.m(), x - sz / 2, b.y0() - sz * 0.8f, z - sz / 2, x + sz / 2, b.y0() + 0.3f, z + sz / 2, mix(LEAF, element, 0.3f * rnd(i)), BoxBuffer.ORGANIC);
            }
            for (int i = 0; i < 2 + tier.rank; i++) {
                for (int side = -1; side <= 1; side += 2) {
                    float z = Mth.lerp(rnd(i + 55), b.z0() + 1, b.z1() - 1);
                    float sway = Mth.sin(t * 0.08f + i) * 0.1f;
                    Matrix4f m = b.at(side < 0 ? b.x0() - 0.3f : b.x1() + 0.3f, b.y0() + 0.5f, z).rotateZYX(Mth.PI + sway, 0, 0);
                    chain(m, 3, 0.8f * s, 1.8f * s, 0.05f, 0, 0, shade(LEAF, 0.8f), BoxBuffer.ORGANIC, solid, 0.9f);
                }
            }
        }

        void flames(float s) {
            Bone b = body;
            int n = 3 + 2 * tier.rank;
            for (int i = 0; i < n; i++) {
                float x = Mth.lerp(rnd(i + 7), b.x0() + 1, b.x1() - 1), z = Mth.lerp(rnd(i + 17), b.z0() + 1, b.z1() - 1);
                float flick = 0.7f + 0.3f * Mth.sin(t * 0.6f + i * 1.7f);
                solid.box(b.m(), x - 1 * s, b.y0() - 1 * s, z - 1 * s, x + 1 * s, b.y0() + 0.2f, z + 1 * s, dark, BoxBuffer.BANDED);
                glow.box(b.m(), x - 0.8f * s, b.y0() - (3 + 3 * rnd(i)) * s * flick, z - 0.8f * s, x + 0.8f * s, b.y0() - 0.8f * s, z + 0.8f * s, element, 0);
            }
        }

        void claws(float s) {
            java.util.List<Bone> limbs = sk.all(Region.LEG);
            limbs.addAll(sk.all(Region.ARM));
            for (Bone l : limbs) {
                for (int k = -1; k <= 1; k++) {
                    Matrix4f m = l.at(l.cx() + k * l.w() * 0.3f, l.y1() - 0.4f, l.z0()).rotateZYX(0, 0, -1.25f);
                    chain(m, 2, 0.8f * s, 1.6f * s, -0.5f, 0, 0, shade(IVORY, 0.8f), BoxBuffer.BANDED, solid, 0.6f);
                }
            }
        }

        void mane(float s) {
            Bone h = head;
            int n = 10;
            for (int i = 0; i < n; i++) {
                float a = Mth.PI * i / (n - 1);
                float x = h.cx() - Mth.cos(a) * (h.w() * 0.55f + s);
                float y = h.y0() + Mth.sin(a) * 0 + (1 - Mth.sin(a)) * h.h() * 0.6f;
                float sz = (2 + 1.5f * Mth.sin(a)) * s;
                solid.box(h.m(), x - sz / 2, y - sz, h.z1() - 0.5f, x + sz / 2, y + sz * 0.5f, h.z1() + sz * 1.2f, i % 2 == 0 ? shade(primary, 0.7f) : shade(primary, 0.55f), BoxBuffer.ORGANIC);
            }
            if (body != head) {
                Bone b = body;
                for (int i = 0; i < 5; i++) {
                    float z = Mth.lerp(i / 5f, b.z0(), b.cz());
                    solid.box(b.m(), b.cx() - 1.2f * s, b.y0() - 2.5f * s, z, b.cx() + 1.2f * s, b.y0(), z + 2 * s, shade(primary, 0.6f), BoxBuffer.ORGANIC);
                }
            }
        }

        void shell(float s) {
            Bone b = body;
            float th = 1.8f * s;
            for (int k = 0; k < 3; k++) {
                float shrink = k * (1.6f + 0.6f * s);
                solid.box(b.m(), b.x0() - 1.5f * s + shrink, b.y0() - th * (k + 1), b.z0() - 1 * s + shrink * 0.8f, b.x1() + 1.5f * s - shrink,
                        b.y0() - th * k + 0.2f, b.z1() + 1 * s - shrink * 0.8f, shade(k % 2 == 0 ? dark : primary, 0.9f + k * 0.08f), BoxBuffer.PLATE);
            }
        }

        void crown(float s) {
            Bone h = head;
            int n = 8;
            for (int i = 0; i < n; i++) {
                float a = Mth.TWO_PI * i / n;
                float x = h.cx() + Mth.cos(a) * h.w() * 0.42f, z = h.cz() + Mth.sin(a) * h.d() * 0.42f;
                float hgt = (i % 2 == 0 ? 3.5f : 2f) * s;
                solid.box(h.m(), x - 0.7f * s, h.y0() - hgt, z - 0.7f * s, x + 0.7f * s, h.y0(), z + 0.7f * s, GOLD, BoxBuffer.PLATE);
            }
            solid.box(h.m(), h.x0() - 0.3f, h.y0() - 1.2f * s, h.z0() - 0.3f, h.x1() + 0.3f, h.y0() + 0.2f, h.z1() + 0.3f, shade(GOLD, 0.85f), BoxBuffer.PLATE);
            if (tier == Tier.DEITY) glow.centered(h.at(h.cx(), h.y0() - 2.5f * s, h.z0() + 0.5f), 1.4f * s, 1.4f * s, 1.4f * s, element, 0);
        }

        void stinger(float s) {
            Bone b = body;
            Matrix4f m = b.at(b.cx(), b.cy(), b.z1()).rotateZYX(0, 0, 1.75f);
            chain(m, 4, 2.2f * s, 2.2f * s, 0.12f, 0, 0, dark, BoxBuffer.BANDED, solid, 0.65f);
        }

        void wings(float s) {
            Bone b = body;
            float flap = Mth.sin(t * 0.35f) * 0.45f;
            for (int side = -1; side <= 1; side += 2) {
                Matrix4f m = b.at(side < 0 ? b.x0() : b.x1(), b.y0() + 0.5f, b.cz()).rotateZYX(side * (0.9f + flap), 0, 0);
                float span = Math.max(6, b.w() * 1.2f) * s, chord = Math.max(4, b.d() * 0.8f) * s;
                solid.box(m, side < 0 ? -0.4f : -0.4f, -span, -chord / 2, 0.4f, 0, chord / 2, mix(primary, element, 0.3f), BoxBuffer.ORGANIC);
                solid.box(m, -0.8f, -span, -chord / 2 - 0.5f, 0.8f, 0, -chord / 2 + 0.6f, dark, BoxBuffer.BANDED);
            }
        }

        void tendrils(float s) {
            Bone b = body;
            int n = 4 + tier.rank * 2;
            for (int i = 0; i < n; i++) {
                float x = Mth.lerp(rnd(i + 70), b.x0() + 1, b.x1() - 1), z = Mth.lerp(rnd(i + 80), b.z0() + 1, b.z1() - 1);
                float sway = Mth.sin(t * 0.1f + i * 1.3f) * 0.25f;
                Matrix4f m = b.at(x, b.y1(), z).rotateZYX(Mth.PI + sway, 0, sway * 0.5f);
                chain(m, 5, 1.2f * s, 1.8f * s, sway * 0.4f, 0, 0.1f, i % 2 == 0 ? dark : mix(dark, element, 0.5f), BoxBuffer.ORGANIC, solid, 0.85f);
            }
        }

        void halo(float s) {
            Bone h = head;
            int n = 12;
            float r = h.w() * 0.55f + 2 * s;
            float rot = t * 0.05f;
            for (int i = 0; i < n; i++) {
                float a = Mth.TWO_PI * i / n + rot;
                Matrix4f m = h.at(h.cx() + Mth.cos(a) * r, h.y0() - 3 * s - 2, h.cz() + Mth.sin(a) * r);
                glow.centered(m, 1.2f * s, 0.6f * s, 1.2f * s, element, 0);
            }
        }

        /** The glowing core / heart, embedded in the chest and caged by ribs of its material. */
        void core(float scale) {
            Bone b = body;
            float pulse = 1f + 0.12f * Mth.sin(t * 0.25f);
            float sz = Math.min(Math.min(b.w(), b.h()) * 0.5f, 5f) * scale * pulse;
            Matrix4f m = b.at(b.cx(), b.cy(), b.z0() - 0.2f);
            glow.centered(m, sz, sz, sz * 0.6f, element, 0);
            glow.centered(m, sz * 1.5f, sz * 1.5f, sz * 0.3f, shade(element, 0.6f), 0);
            for (int i = -1; i <= 1; i += 2) {
                solid.box(b.m(), b.cx() + i * sz * 0.8f - 0.4f, b.cy() - sz, b.z0() - 0.9f, b.cx() + i * sz * 0.8f + 0.4f, b.cy() + sz, b.z0(), dark, BoxBuffer.BANDED);
            }
            solid.box(b.m(), b.cx() - sz, b.cy() - sz * 0.9f, b.z0() - 0.9f, b.cx() + sz, b.cy() - sz * 0.9f + 0.8f, b.z0(), dark, BoxBuffer.BANDED);
        }
    }

    private Anatomy() {}
}
