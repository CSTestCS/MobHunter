package com.mobhunter.gear;

import com.mobhunter.MobHunter;
import com.mobhunter.core.Tier;
import com.mobhunter.profile.CreatureProfile;
import com.mobhunter.registry.ModComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;

/** Computes the stats a forged piece of gear gets from the creature and tier it was made from. */
public final class GearStats {

    public static float weaponMult(Tier tier) {
        return switch (tier) {
            case MONSTROUS -> 1.5f;
            case DEITY -> 2.1f;
            default -> 1.0f;
        };
    }

    public static int durability(Tier tier, GearKind kind) {
        int base = switch (tier) {
            case MONSTROUS -> 1600;
            case DEITY -> 3600;
            default -> 650;
        };
        return kind.isArmor() ? base / 3 : base;
    }

    public static ItemStack forge(Item item, GearKind kind, MonsterSource src) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.MONSTER_SOURCE.get(), src);
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(src.color()));
        stack.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.DYED_COLOR, true));
        stack.set(DataComponents.MAX_DAMAGE, durability(src.tier(), kind));
        stack.set(DataComponents.DAMAGE, 0);
        if (src.tier() == Tier.DEITY) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder();
        int rank = Math.max(1, src.tier().rank);
        if (kind.isArmor()) {
            Identifier id = MobHunter.id("armor." + kind.name);
            EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(kind.slot);
            b.add(Attributes.ARMOR, new AttributeModifier(id, kind.armor(rank), AttributeModifier.Operation.ADD_VALUE), group);
            b.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, rank == 1 ? 1 : rank == 2 ? 2.5 : 4, AttributeModifier.Operation.ADD_VALUE), group);
            if (rank >= 2) b.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id, rank * 0.04, AttributeModifier.Operation.ADD_VALUE), group);
            if (rank == 3) b.add(Attributes.MAX_HEALTH, new AttributeModifier(id, 2, AttributeModifier.Operation.ADD_VALUE), group);
        } else if (kind != GearKind.HUNTING_BOW) {
            float dmg = kind.damage * weaponMult(src.tier()) + (rank - 1) * 1.5f;
            b.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, dmg, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            b.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, kind.speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            if (kind == GearKind.GREAT_SWORD || kind == GearKind.HAMMER) {
                b.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(MobHunter.id("reach"), 0.75, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            }
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, b.build());
        return stack;
    }

    public static net.minecraft.network.chat.Component gearName(GearKind kind, MonsterSource src) {
        CreatureProfile p = src.profile();
        net.minecraft.network.chat.Component gear = net.minecraft.network.chat.Component.translatable("mobhunter.gear." + kind.name);
        if (p == null) return gear;
        if (src.tier() == Tier.DEITY) {
            return net.minecraft.network.chat.Component.translatable("mobhunter.gear.deity_named", p.epithet(), gear).withStyle(src.tier().color);
        }
        return net.minecraft.network.chat.Component.translatable("mobhunter.gear.named", net.minecraft.network.chat.Component.translatable(src.tier().translationKey()),
                com.mobhunter.core.TierManager.creatureName(p), gear).withStyle(src.tier().color);
    }

    private GearStats() {}
}
