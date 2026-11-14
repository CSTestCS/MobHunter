package com.mobhunter.gear;

import com.mobhunter.core.Tier;
import com.mobhunter.core.TierManager;
import com.mobhunter.profile.CreatureProfile;
import com.mobhunter.registry.ModComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.core.component.DataComponents;

/** Hide / claw / gem carved from a tiered creature. Its creature and tier live in a data component. */
public class MonsterMaterialItem extends Item {
    public final MaterialPart part;

    public MonsterMaterialItem(MaterialPart part, Properties properties) {
        super(properties);
        this.part = part;
    }

    public static ItemStack create(Item item, CreatureProfile profile, Tier tier, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(ModComponents.MONSTER_SOURCE.get(), new MonsterSource(profile.id(), tier));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(java.util.List.of(), java.util.List.of(),
                java.util.List.of(GearStats.modelKey(new MonsterSource(profile.id(), tier))), java.util.List.of()));
        if (tier == Tier.DEITY) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    /**
     * Alpha gear uses modest fragments, Monstrous gear major anatomical components, Deity gear rare heart-cores:
     * "Alpha Horn Fragment", "Bull Horn", "Bull Deity Heart-Core".
     */
    @Override
    public Component getName(ItemStack stack) {
        MonsterSource src = stack.get(ModComponents.MONSTER_SOURCE.get());
        if (src == null || src.profile() == null) return super.getName(stack);
        CreatureProfile p = src.profile();
        com.mobhunter.profile.Bestiary.Entry e = p.entry();
        String mat = e == null ? "Monster" : e.materialTitle();
        String creature = TierManager.creatureName(p).getString();
        String name = switch (src.tier()) {
            case ALPHA -> switch (part) {
                case HIDE -> "Alpha " + creature + " " + mat + " Hide";
                case CLAW -> "Alpha " + mat + " Fragment";
                case GEM -> "Alpha " + creature + " Core Shard";
            };
            case MONSTROUS -> {
                String t = GearStats.tierPrefix(e, src.tier());
                yield switch (part) {
                    case HIDE -> t + " " + mat + " Hide";
                    case CLAW -> t + " " + mat;
                    case GEM -> t + " Core";
                };
            }
            default -> {
                String t = GearStats.tierPrefix(e, src.tier());
                yield switch (part) {
                    case HIDE -> t + " Divine " + mat + " Hide";
                    case CLAW -> t + " " + mat;
                    case GEM -> t + " Heart-Core";
                };
            }
        };
        return Component.literal(name).withStyle(src.tier().color);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        MonsterSource src = stack.get(ModComponents.MONSTER_SOURCE.get());
        if (src == null) {
            tooltip.accept(Component.translatable("item.mobhunter.material.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.accept(Component.translatable("mobhunter.tooltip.element", GearEffects.elementName(src)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("mobhunter.tooltip.forge_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
