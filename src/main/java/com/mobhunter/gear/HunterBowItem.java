package com.mobhunter.gear;

import com.mobhunter.registry.ModComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A bow strung with monster sinew; its arrows carry the creature's element. */
public class HunterBowItem extends BowItem {
    public HunterBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        MonsterSource src = stack.get(ModComponents.MONSTER_SOURCE.get());
        return src == null ? super.getName(stack) : GearStats.gearName(GearKind.HUNTING_BOW, src);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        MonsterSource src = stack.get(ModComponents.MONSTER_SOURCE.get());
        if (src != null) {
            tooltip.accept(Component.translatable("mobhunter.tooltip.element", Component.translatable(src.element().translationKey()).withStyle(src.element().chat)).withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("mobhunter.tooltip.weapon_element." + src.element().getSerializedName()).withStyle(ChatFormatting.DARK_GREEN));
            tooltip.accept(Component.translatable("mobhunter.tooltip.bow_power", String.format("%.0f%%", GearStats.weaponMult(src.tier()) * 100)).withStyle(ChatFormatting.BLUE));
        }
        tooltip.accept(Component.translatable("mobhunter.tooltip.hunters_edge").withStyle(ChatFormatting.DARK_GRAY));
    }
}
