package com.mobhunter.gear;

import com.mobhunter.MobHunter;
import com.mobhunter.core.Tier;
import com.mobhunter.profile.Bestiary;
import com.mobhunter.profile.CreatureProfile;
import com.mobhunter.registry.ModComponents;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import org.jspecify.annotations.Nullable;

/** Stats, appearance and names of gear forged from a creature of a given tier. */
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

    /** Key that selects the creature- and tier-specific model of an item: "cow_monstrous". */
    public static String modelKey(MonsterSource src) {
        return src.creature().getPath() + "_" + src.tier().getSerializedName();
    }

    public static ItemStack forge(Item item, GearKind kind, MonsterSource src) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.MONSTER_SOURCE.get(), src);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(modelKey(src)), List.of()));
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
            // Every creature's plate armor has its own look.
            ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, MobHunter.id(src.creature().getPath()));
            stack.set(DataComponents.EQUIPPABLE, Equippable.builder(kind.slot).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).setAsset(asset).build());
        } else {
            float dmg = kind.damage * weaponMult(src.tier()) + (rank - 1) * 1.5f;
            b.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, dmg, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            b.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, kind.speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, b.build());
        return stack;
    }

    /** "Alpha", "Bull", "Bull Deity": the prefix of every name in the bestiary. */
    public static String tierPrefix(Bestiary.@Nullable Entry e, Tier tier) {
        if (e == null) return Component.translatable(tier.translationKey()).getString();
        return switch (tier) {
            case MONSTROUS -> e.monstrousTitle();
            case DEITY -> e.deityName();
            default -> "Alpha";
        };
    }

    /** "Alpha Hornblade", "Bull Hornblade", "Bull Deity Horn-Spear", "Bull Deity Hornplate Helm". */
    public static Component gearName(GearKind kind, MonsterSource src) {
        CreatureProfile p = src.profile();
        Bestiary.Entry e = p == null ? null : p.entry();
        if (e == null) return Component.translatable("mobhunter.gear." + kind.name);
        String prefix = tierPrefix(e, src.tier());
        MutableComponent name = switch (kind) {
            case BLADE -> Component.literal(prefix + " " + e.bladeWord());
            case SPEAR -> Component.literal(prefix + " " + e.spearWord());
            default -> Component.literal(prefix + " " + e.plateWord() + " ").append(Component.translatable("mobhunter.gear." + kind.name));
        };
        return name.withStyle(src.tier().color);
    }

    private GearStats() {}
}
