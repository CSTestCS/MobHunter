package com.mobhunter.client;

import com.mobhunter.MobHunter;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** A dark thunderhead orbiting the Ram God. Rendered at 2.2x scale. */
public class StormCloudModel extends EntityModel<DeityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MobHunter.id("storm_cloud"), "main");
    private final ModelPart[] puffs = new ModelPart[4];

    public StormCloudModel(ModelPart root) {
        super(root);
        for (int i = 0; i < puffs.length; i++) puffs[i] = root.getChild("core").getChild("puff" + i);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition core = root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-6f, -4f, -6f, 12f, 8f, 12f), PartPose.offset(0, 16, 0));
        core.addOrReplaceChild("puff0", CubeListBuilder.create().texOffs(0, 20).addBox(-11f, -3f, -4f, 8f, 6f, 8f), PartPose.ZERO);
        core.addOrReplaceChild("puff1", CubeListBuilder.create().texOffs(32, 20).addBox(3f, -3f, -4f, 8f, 6f, 8f), PartPose.ZERO);
        core.addOrReplaceChild("puff2", CubeListBuilder.create().texOffs(0, 34).addBox(-3f, -7f, -3f, 6f, 4f, 6f), PartPose.ZERO);
        core.addOrReplaceChild("puff3", CubeListBuilder.create().texOffs(24, 34).addBox(-3f, -2f, 4f, 6f, 4f, 6f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(DeityRenderState s) {
        super.setupAnim(s);
        resetPose();
        for (int i = 0; i < puffs.length; i++) {
            puffs[i].y = Mth.sin(s.ageInTicks * 0.08f + i * 1.7f) * 0.8f;
            float sc = 1f + Mth.sin(s.ageInTicks * 0.1f + i) * 0.06f + s.charge * 0.15f;
            puffs[i].xScale = puffs[i].yScale = puffs[i].zScale = sc;
        }
    }
}
