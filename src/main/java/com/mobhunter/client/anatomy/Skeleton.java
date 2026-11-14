package com.mobhunter.client.anatomy;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/** The posed cubes of a creature's model, sorted into anatomical regions. */
public final class Skeleton {
    public enum Region { HEAD, BODY, LEG, ARM, TAIL, WING, OTHER }

    /** One cube of the model: its posed transform and extents in pixels (Y down, front is -Z). */
    public record Bone(Region region, String name, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1) {
        public float w() { return x1 - x0; }
        public float h() { return y1 - y0; }
        public float d() { return z1 - z0; }
        public float cx() { return (x0 + x1) / 2; }
        public float cy() { return (y0 + y1) / 2; }
        public float cz() { return (z0 + z1) / 2; }
        public float volume() { return Math.max(0.01f, w()) * Math.max(0.01f, h()) * Math.max(0.01f, d()); }

        public Matrix4f at(float px, float py, float pz) {
            return new Matrix4f(m).translate(px / 16f, py / 16f, pz / 16f);
        }
    }

    public final List<Bone> bones = new ArrayList<>();
    public @Nullable Bone head;
    public @Nullable Bone body;

    static Region classify(String path) {
        String n = path.substring(path.lastIndexOf('/') + 1).toLowerCase();
        if (n.contains("head") && !n.contains("wear")) return Region.HEAD;
        if (n.contains("body") || n.contains("torso") || n.contains("chest") || n.equals("cube") || n.contains("shell") || n.equals("base")
                || n.equals("lid") || n.contains("main")) return Region.BODY;
        if (n.contains("leg") || n.contains("foot")) return Region.LEG;
        if (n.contains("arm") && !n.contains("armor")) return Region.ARM;
        if (n.contains("tail")) return Region.TAIL;
        if (n.contains("wing")) return Region.WING;
        return Region.OTHER;
    }

    private static final Vector3f[] AXES = {new Vector3f(1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, -1, 0),
            new Vector3f(0, 0, 1), new Vector3f(0, 0, -1)};

    /**
     * Re-expresses a cube in a canonical frame (Y down, front -Z, as seen from the creature) no matter how the
     * model author rotated it, so "the top of the body" really is its top (vanilla quadruped bodies are stored
     * rotated by 90 degrees).
     */
    static @Nullable Bone orient(Region region, String name, Matrix4f m, Matrix4f rootInv, float x0, float y0, float z0, float x1, float y1, float z1) {
        Matrix3f rel = new Matrix3f(new Matrix4f(rootInv).mul(m));
        // Parts collapsed to zero scale (hidden tongues, saddles...) have no orientation: skip them.
        float det = rel.determinant();
        if (!Float.isFinite(det) || Math.abs(det) < 1.0E-6f) return null;
        Vector3f up = null, front = null;
        float bestUp = -2, bestFront = -2;
        for (Vector3f a : AXES) {
            Vector3f v = rel.transform(new Vector3f(a)).normalize();
            if (-v.y > bestUp) {
                bestUp = -v.y;
                up = a;
            }
        }
        for (Vector3f a : AXES) {
            if (Math.abs(a.dot(up)) > 0.5f) continue;
            Vector3f v = rel.transform(new Vector3f(a)).normalize();
            if (-v.z > bestFront) {
                bestFront = -v.z;
                front = a;
            }
        }
        if (up == null || front == null) return null;
        Vector3f cy = new Vector3f(up).negate(), cz = new Vector3f(front).negate();
        Vector3f cx = new Vector3f(cy).cross(cz);
        Matrix3f basis = new Matrix3f().setColumn(0, cx).setColumn(1, cy).setColumn(2, cz);
        float[] half = {(x1 - x0) / 2, (y1 - y0) / 2, (z1 - z0) / 2};
        float hx = Math.abs(cx.x) * half[0] + Math.abs(cx.y) * half[1] + Math.abs(cx.z) * half[2];
        float hy = Math.abs(cy.x) * half[0] + Math.abs(cy.y) * half[1] + Math.abs(cy.z) * half[2];
        float hz = Math.abs(cz.x) * half[0] + Math.abs(cz.y) * half[1] + Math.abs(cz.z) * half[2];
        Matrix4f frame = new Matrix4f(m).translate((x0 + x1) / 32f, (y0 + y1) / 32f, (z0 + z1) / 32f).mul(new Matrix4f(basis));
        return new Bone(region, name, frame, -hx, -hy, -hz, hx, hy, hz);
    }

    public static Skeleton scan(ModelPart root, PoseStack ps) {
        Skeleton s = new Skeleton();
        Matrix4f rootInv = new Matrix4f(ps.last().pose()).invert();
        root.visit(ps, (pose, path, index, cube) -> {
            if (cube.maxX - cube.minX <= 0.01f && cube.maxY - cube.minY <= 0.01f) return;
            Bone b = orient(classify(path), path, new Matrix4f(pose.pose()), rootInv, cube.minX, cube.minY, cube.minZ, cube.maxX, cube.maxY, cube.maxZ);
            if (b != null) s.bones.add(b);
        });
        Bone biggest = null;
        for (Bone b : s.bones) {
            if (biggest == null || b.volume() > biggest.volume()) biggest = b;
            if (b.region() == Region.HEAD && (s.head == null || b.volume() > s.head.volume())) s.head = b;
            if (b.region() == Region.BODY && (s.body == null || b.volume() > s.body.volume())) s.body = b;
        }
        if (s.body == null) s.body = biggest;
        if (s.head == null) s.head = s.body;
        return s;
    }

    public List<Bone> all(Region r) {
        List<Bone> list = new ArrayList<>();
        for (Bone b : bones) if (b.region() == r) list.add(b);
        return list;
    }
}
