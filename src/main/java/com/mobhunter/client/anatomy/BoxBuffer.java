package com.mobhunter.client.anatomy;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Collects generated boxes (in model pixel units, already transformed into the creature's posed space)
 * and writes them as textured quads.
 */
public final class BoxBuffer {
    /** Atlas tiles of anatomy.png (64x16, four 16x16 tiles). */
    public static final int BANDED = 0, PLATE = 1, CRYSTAL = 2, ORGANIC = 3;

    private record Box(Matrix4f m, Matrix3f n, float x0, float y0, float z0, float x1, float y1, float z1, int color, int tile) {}

    private final List<Box> boxes = new ArrayList<>();

    public void clear() {
        boxes.clear();
    }

    public boolean isEmpty() {
        return boxes.isEmpty();
    }

    /** Adds an axis-aligned box (pixel units) in the space of {@code m}. */
    public void box(Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, int color, int tile) {
        boxes.add(new Box(new Matrix4f(m), new Matrix3f(m).invert().transpose(), x0 / 16f, y0 / 16f, z0 / 16f, x1 / 16f, y1 / 16f, z1 / 16f, color, tile));
    }

    /** Adds a box centred on the origin of {@code m} (pixel units). */
    public void centered(Matrix4f m, float sx, float sy, float sz, int color, int tile) {
        box(m, -sx / 2, -sy / 2, -sz / 2, sx / 2, sy / 2, sz / 2, color, tile);
    }

    public void write(VertexConsumer buf, int light, int overlay) {
        Vector3f p = new Vector3f();
        Vector3f nv = new Vector3f();
        for (Box b : boxes) {
            float u0 = b.tile * 0.25f, u1 = u0 + 0.25f;
            float[][] c = {
                    {b.x0, b.y0, b.z0}, {b.x1, b.y0, b.z0}, {b.x1, b.y1, b.z0}, {b.x0, b.y1, b.z0},
                    {b.x0, b.y0, b.z1}, {b.x1, b.y0, b.z1}, {b.x1, b.y1, b.z1}, {b.x0, b.y1, b.z1}};
            int[][] faces = {{0, 1, 2, 3}, {5, 4, 7, 6}, {4, 0, 3, 7}, {1, 5, 6, 2}, {4, 5, 1, 0}, {3, 2, 6, 7}};
            float[][] normals = {{0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}};
            float[][] uv = {{u0, 0}, {u1, 0}, {u1, 1}, {u0, 1}};
            for (int f = 0; f < 6; f++) {
                b.n.transform(nv.set(normals[f][0], normals[f][1], normals[f][2])).normalize();
                for (int v = 0; v < 4; v++) {
                    float[] q = c[faces[f][v]];
                    b.m.transformPosition(p.set(q[0], q[1], q[2]));
                    buf.addVertex(p.x, p.y, p.z, b.color, uv[v][0], uv[v][1], overlay, light, nv.x, nv.y, nv.z);
                }
            }
        }
    }
}
