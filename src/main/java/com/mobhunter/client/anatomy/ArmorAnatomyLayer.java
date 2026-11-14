package com.mobhunter.client.anatomy;

import com.mobhunter.gear.HunterArmorItem;
import com.mobhunter.gear.MonsterSource;
import com.mobhunter.profile.Bestiary;
import com.mobhunter.profile.CreatureProfile;
import com.mobhunter.registry.ModComponents;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Grows the anatomy of a creature's plate armor (horns on the helm, spines on the mail...) on players. */
public class ArmorAnatomyLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public ArmorAnatomyLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    private static @Nullable MonsterSource src(ItemStack s) {
        return s.getItem() instanceof HunterArmorItem ? s.get(ModComponents.MONSTER_SOURCE.get()) : null;
    }

    @Override
    public void submit(PoseStack ps, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) return;
        MonsterSource head = src(state.headEquipment), chest = src(state.chestEquipment), feet = src(state.feetEquipment);
        if (head == null && chest == null && feet == null) return;
        Skeleton sk = Skeleton.scan(getParentModel().root(), ps);
        BoxBuffer solid = new BoxBuffer(), glow = new BoxBuffer();
        for (MonsterSource s : new MonsterSource[]{head, chest, feet}) {
            if (s == null) continue;
            CreatureProfile p = s.profile();
            Bestiary.Entry e = p == null ? null : p.entry();
            if (e == null) continue;
            Anatomy.buildArmor(sk, e, p.color(), s.tier(), state.ageInTicks, s == head, s == chest, s == feet, solid, glow);
        }
        if (!solid.isEmpty()) collector.submitCustomGeometry(ps, RenderTypes.entityCutout(AnatomyLayer.ATLAS), (pose, buf) -> solid.write(buf, light, OverlayTexture.NO_OVERLAY));
        if (!glow.isEmpty()) collector.submitCustomGeometry(ps, RenderTypes.eyes(AnatomyLayer.GLOW), (pose, buf) -> glow.write(buf, 0xF000F0, OverlayTexture.NO_OVERLAY));
    }
}
