package com.mobhunter.client;

import com.mobhunter.core.AnimPose;
import com.mobhunter.core.AnimState;
import com.mobhunter.core.Tier;
import com.mobhunter.deity.EldritchDeity;
import com.mobhunter.deity.form.DeityForm;
import com.mobhunter.deity.form.DeityForm.Layout;
import com.mobhunter.deity.form.DeityForm.Part;
import com.mobhunter.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Draws a Deity by assembling its {@link DeityForm}: copies of real creatures (rendered through their own
 * vanilla renderers from stand-in entities) and real block models, arranged into wheels, spheres, swarms,
 * towers, spirals, tentacles and spires that move with time.
 */
public class EldritchDeityRenderer extends EntityRenderer<EldritchDeity, EldritchDeityRenderer.State> {
    private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();
    private static final Map<EntityType<?>, Entity> STAND_INS = new HashMap<>();
    private static Level standInLevel;
    private static final java.util.Set<EntityType<?>> FAILED = new java.util.HashSet<>();
    private final BlockModelResolver blocks;

    public static class State extends EntityRenderState {
        public DeityForm form;
        public float bodyRot;
        public TelegraphData telegraph;
        public float deathTime;
        public float hurt;
        public final Map<EntityType<?>, EntityRenderState> creatures = new IdentityHashMap<>();
        public final Map<BlockState, BlockModelRenderState> blocks = new IdentityHashMap<>();
    }

    public EldritchDeityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blocks = context.getBlockModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    private static Entity standIn(EntityType<?> type, Part part, Level level) {
        if (standInLevel != level) {
            STAND_INS.clear();
            standInLevel = level;
        }
        return STAND_INS.computeIfAbsent(type, t -> {
            Entity e = t.create(level, EntitySpawnReason.LOAD);
            if (e != null) e.setId(-1_000_000 - STAND_INS.size());
            if (e != null && part.setup() != null) part.setup().accept(e);
            return e;
        });
    }

    @Override
    public void extractRenderState(EldritchDeity e, State s, float pt) {
        super.extractRenderState(e, s, pt);
        s.form = e.form();
        s.bodyRot = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
        s.deathTime = e.deathTime > 0 ? e.deathTime + pt : 0;
        s.hurt = e.hurtTime > 0 ? (e.hurtTime - pt) / 10f : 0;
        AnimState a = e.getExistingDataOrNull(ModAttachments.ANIM);
        if (a == null) a = AnimState.IDLE;
        s.telegraph = new TelegraphData(Tier.DEITY, a.animPose(), a.phase(), a.progress(e.level().getGameTime() + pt), e.getBbWidth(), e.getBbHeight(), false);
        s.creatures.clear();
        for (Part p : s.form.parts()) {
            if (p.creature() != null && !s.creatures.containsKey(p.creature())) {
                Entity d = standIn(p.creature(), p, e.level());
                if (d == null) {
                    if (FAILED.add(p.creature())) com.mobhunter.MobHunter.LOGGER.warn("Deity stand-in {} could not be created", p.creature());
                    continue;
                }
                d.setPos(e.getX(), e.getY(), e.getZ());
                d.xo = d.getX();
                d.yo = d.getY();
                d.zo = d.getZ();
                d.tickCount = e.tickCount;
                d.setYRot(0);
                d.yRotO = 0;
                if (d instanceof LivingEntity le) {
                    le.yBodyRot = le.yBodyRotO = 0;
                    le.yHeadRot = le.yHeadRotO = 0;
                }
                try {
                    EntityRenderState cs = entityRenderDispatcher.extractEntity(d, pt);
                    cs.shadowPieces.clear();
                    cs.lightCoords = s.lightCoords;
                    s.creatures.put(p.creature(), cs);
                } catch (RuntimeException ex) {
                    // A creature that cannot be drawn as a stand-in is simply left out of the body.
                    if (FAILED.add(p.creature())) com.mobhunter.MobHunter.LOGGER.warn("Deity stand-in {} could not be drawn", p.creature(), ex);
                }
            } else if (p.block() != null && !s.blocks.containsKey(p.block())) {
                BlockModelRenderState bs = new BlockModelRenderState();
                blocks.update(bs, p.block(), CONTEXT);
                s.blocks.put(p.block(), bs);
            }
        }
    }

    @Override
    protected AABB getBoundingBoxForCulling(EldritchDeity e, float pt) {
        return e.getBoundingBox().inflate(16, 12, 16);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        if (s.form == null || s.isInvisible) return;
        ps.pushPose();
        TelegraphPoses.apply(ps, s.telegraph, s.bodyRot, s.ageInTicks);
        if (s.telegraph.pose() == AnimPose.FORM && s.telegraph.phase() == AnimState.ACTIVE) {
            float k = Math.max(0.02f, s.telegraph.progress());
            ps.scale(k, k, k);
        }
        if (s.deathTime > 0) {
            float k = Math.max(0f, 1f - s.deathTime / 20f);
            ps.translate(0, -s.deathTime * 0.2f, 0);
            ps.scale(k, k, k);
        }
        ps.rotateDegrees(Axis.YP, -s.bodyRot);
        for (Part p : s.form.parts()) {
            if (p.creature() != null) {
                EntityRenderState cs = s.creatures.get(p.creature());
                if (cs == null) continue;
                float h = p.creature().getDimensions().height();
                place(p, s.ageInTicks, h, (x, y, z, yaw, pitch, roll, scale) -> {
                    ps.pushPose();
                    ps.translate(x, y, z);
                    ps.rotateDegrees(Axis.YP, yaw);
                    if (pitch != 0) ps.rotateDegrees(Axis.XP, pitch);
                    if (roll != 0) ps.rotateDegrees(Axis.ZP, roll);
                    ps.scale(scale, scale, scale);
                    entityRenderDispatcher.submit(cs, camera, 0, 0, 0, ps, collector);
                    ps.popPose();
                });
            } else if (p.block() != null) {
                BlockModelRenderState bs = s.blocks.get(p.block());
                if (bs == null) continue;
                place(p, s.ageInTicks, 1f, (x, y, z, yaw, pitch, roll, scale) -> {
                    ps.pushPose();
                    ps.translate(x, y, z);
                    ps.rotateDegrees(Axis.YP, yaw);
                    if (pitch != 0) ps.rotateDegrees(Axis.XP, pitch);
                    if (roll != 0) ps.rotateDegrees(Axis.ZP, roll);
                    ps.scale(scale, scale, scale);
                    ps.translate(-0.5f, 0f, -0.5f);
                    bs.submit(ps, collector, s.lightCoords, OverlayTexture.NO_OVERLAY, s.outlineColor);
                    ps.popPose();
                });
            }
        }
        ps.popPose();
        super.submit(s, ps, collector, camera);
    }

    interface Placer {
        void at(float x, float y, float z, float yaw, float pitch, float roll, float scale);
    }

    private static float hash(int i, int salt) {
        int h = i * 374761393 + salt * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }

    /** Computes every placement of a part at time {@code t}. {@code unitHeight} is the height of one copy. */
    static void place(Part p, float t, float unitHeight, Placer out) {
        float s = p.scale(), r = p.radius(), y = p.y();
        int n = Math.max(1, p.count());
        float spin = p.spin() * t * Mth.DEG_TO_RAD;
        float deg = Mth.RAD_TO_DEG;
        switch (p.layout()) {
            case SINGLE -> out.at(0, y + Mth.sin(t * 0.05f) * 0.15f * s, 0, p.spin() * t, 0, 0, s);
            case RING -> {
                for (int i = 0; i < n; i++) {
                    float a = Mth.TWO_PI * i / n + spin;
                    float bob = Mth.sin(t * 0.2f + i) * 0.15f * s;
                    out.at(Mth.cos(a) * r, y + bob, Mth.sin(a) * r, -a * deg, 0, 0, s);
                }
            }
            case WHEEL -> {
                float back = -(r * 0.3f + 2f);
                for (int i = 0; i < n; i++) {
                    float a = Mth.TWO_PI * i / n + spin;
                    out.at(Mth.cos(a) * r, y + Mth.sin(a) * r, back, 0, 0, a * deg - 90, s);
                }
            }
            case SPHERE -> {
                for (int i = 0; i < n; i++) {
                    float yy = 1 - 2 * (i + 0.5f) / n;
                    float rr = Mth.sqrt(Math.max(0, 1 - yy * yy));
                    float phi = i * 2.39996f + spin;
                    out.at(Mth.cos(phi) * rr * r, y + yy * r, Mth.sin(phi) * rr * r, 90 - phi * deg, 0, 0, s);
                }
            }
            case SWARM -> {
                for (int i = 0; i < n; i++) {
                    float rr = r * (0.45f + 0.55f * hash(i, 1));
                    float h = y + (hash(i, 2) - 0.5f) * r * 0.9f + Mth.sin(t * 0.06f + i) * 0.8f;
                    float a = hash(i, 3) * Mth.TWO_PI + spin * (0.6f + hash(i, 4));
                    out.at(Mth.cos(a) * rr, h, Mth.sin(a) * rr, -a * deg, Mth.sin(t * 0.1f + i) * 10, 0, s);
                }
            }
            case STACK -> {
                int count = p.creature() != null ? n : Math.max(n, p.length());
                float step = s * unitHeight * (p.creature() != null ? 0.92f : 1f);
                for (int i = 0; i < count; i++) {
                    out.at(Mth.sin(t * 0.05f + i) * 0.1f * s, y + i * step, 0, p.spin() * t + i * 25, 0, 0, s);
                }
            }
            case PYRAMID -> {
                float step = s * unitHeight * 0.85f;
                for (int k = 0; k < n; k++) {
                    float rk = r * (1f - k / (float) n);
                    int copies = rk < 0.5f ? 1 : Math.max(3, (int) (Mth.TWO_PI * rk / (s * 1.1f)));
                    for (int i = 0; i < copies; i++) {
                        float a = Mth.TWO_PI * i / copies + k * 0.4f;
                        out.at(Mth.cos(a) * rk, y + k * step, Mth.sin(a) * rk, 90 - a * deg, 0, 0, s);
                    }
                }
            }
            case HELIX -> {
                if (p.creature() != null) {
                    for (int i = 0; i < n; i++) {
                        float a = i * 0.55f + spin;
                        float rr = r * (1f - 0.35f * i / n);
                        out.at(Mth.cos(a) * rr, y + i * (s * unitHeight * 0.55f + 0.3f), Mth.sin(a) * rr, -a * deg, -15, 0, s);
                    }
                } else {
                    for (int j = 0; j < n; j++) {
                        for (int k = 0; k < p.length(); k++) {
                            float a = Mth.TWO_PI * j / n + k * 0.45f + spin;
                            float rr = r * (0.35f + 0.65f * k / p.length());
                            out.at(Mth.cos(a) * rr, y + k * s, Mth.sin(a) * rr, -a * deg, 0, 0, s);
                        }
                    }
                }
            }
            case SPIRES, CROWN, CAGE -> {
                int len = Math.max(1, p.length());
                for (int j = 0; j < n; j++) {
                    float a = Mth.TWO_PI * j / n + spin;
                    for (int k = 0; k < len; k++) {
                        float sk = p.layout() == Layout.SPIRES ? s * (1f - 0.5f * k / len) : s;
                        out.at(Mth.cos(a) * r, y + k * s, Mth.sin(a) * r, -a * deg, 0, 0, sk);
                    }
                }
            }
            case TENTACLES -> {
                int len = Math.max(2, p.length());
                for (int j = 0; j < n; j++) {
                    float a = Mth.TWO_PI * j / n + spin * 0.2f;
                    float cx = Mth.cos(a), cz = Mth.sin(a);
                    for (int k = 0; k < len; k++) {
                        float out1 = r + k * s * 0.5f;
                        float wave = Mth.sin(t * 0.08f + k * 0.5f + j) * k * 0.15f * s;
                        float sk = s * (1f - 0.55f * k / len);
                        out.at(cx * out1 - cz * wave, y - k * s * 0.75f, cz * out1 + cx * wave, -a * deg, 0, 0, sk);
                    }
                }
            }
            case LEGS -> {
                int len = Math.max(3, p.length());
                float span = len * s * 0.65f;
                for (int j = 0; j < n; j++) {
                    float a = Mth.TWO_PI * (j + 0.5f) / n + spin;
                    float cx = Mth.cos(a), cz = Mth.sin(a);
                    float step = Mth.sin(t * 0.15f + j * 1.7f) * 0.3f * s;
                    for (int k = 0; k < len; k++) {
                        float u = k / (float) (len - 1);
                        float hor = r + u * span;
                        float vert = y * (1 - u) + Mth.sin(u * Mth.PI) * len * s * 0.3f + step * u;
                        out.at(cx * hor, vert, cz * hor, -a * deg, 0, 0, s);
                    }
                }
            }
        }
    }
}
