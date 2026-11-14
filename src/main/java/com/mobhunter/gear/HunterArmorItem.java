package com.mobhunter.gear;

import com.mobhunter.registry.ModComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Armor stitched from monster hide. Resists the creature's element; a full set unlocks its skill. */
public class HunterArmorItem extends Item {
    public final GearKind kind;

    public HunterArmorItem(GearKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public Component getName(ItemStack stack) {
        MonsterSource src = stack.get(ModComponents.MONSTER_SOURCE.get());
        return src == null ? super.getName(stack) : GearStats.gearName(kind, src);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        MonsterSource src = stack.get(ModComponents.MONSTER_SOURCE.get());
        if (src == null) {
            tooltip.accept(Component.translatable("item.mobhunter.material.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        Component el = GearEffects.elementName(src);
        tooltip.accept(Component.translatable("mobhunter.tooltip.resist", el).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("mobhunter.tooltip.set_bonus").withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.literal("  ").append(Component.translatable("mobhunter.set." + src.element().getSerializedName())).withStyle(ChatFormatting.YELLOW));
        tooltip.accept(Component.translatable("mobhunter.tooltip.hunters_hide").withStyle(ChatFormatting.DARK_GRAY));
    }
}
