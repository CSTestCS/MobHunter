package com.mobhunter.registry;

import com.mobhunter.MobHunter;
import com.mobhunter.gear.GearKind;
import com.mobhunter.gear.HunterArmorItem;
import com.mobhunter.gear.HunterBowItem;
import com.mobhunter.gear.HunterWeaponItem;
import com.mobhunter.gear.MaterialPart;
import com.mobhunter.gear.MonsterMaterialItem;
import com.mobhunter.hunt.HuntContractItem;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MobHunter.MODID);

    public static final TagKey<Item> MONSTER_MATERIALS = TagKey.create(Registries.ITEM, MobHunter.id("monster_materials"));
    public static final ResourceKey<EquipmentAsset> HUNTER_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, MobHunter.id("hunter"));
    public static final ArmorMaterial HUNTER_ARMOR = new ArmorMaterial(25,
            Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 6, ArmorType.LEGGINGS, 5, ArmorType.BOOTS, 2, ArmorType.BODY, 6),
            12, SoundEvents.ARMOR_EQUIP_LEATHER, 1f, 0f, MONSTER_MATERIALS, HUNTER_ASSET);

    public static final DeferredItem<MonsterMaterialItem> MONSTER_HIDE = ITEMS.registerItem("monster_hide", p -> new MonsterMaterialItem(MaterialPart.HIDE, p));
    public static final DeferredItem<MonsterMaterialItem> MONSTER_CLAW = ITEMS.registerItem("monster_claw", p -> new MonsterMaterialItem(MaterialPart.CLAW, p));
    public static final DeferredItem<MonsterMaterialItem> MONSTER_GEM = ITEMS.registerItem("monster_gem", p -> new MonsterMaterialItem(MaterialPart.GEM, p),
            p -> p.rarity(Rarity.RARE));
    public static final DeferredItem<HuntContractItem> HUNT_CONTRACT = ITEMS.registerItem("hunt_contract", HuntContractItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<HunterWeaponItem> GREAT_SWORD = weapon(GearKind.GREAT_SWORD);
    public static final DeferredItem<HunterWeaponItem> LONG_SWORD = weapon(GearKind.LONG_SWORD);
    public static final DeferredItem<HunterWeaponItem> HAMMER = weapon(GearKind.HAMMER);
    public static final DeferredItem<HunterWeaponItem> DUAL_BLADES = weapon(GearKind.DUAL_BLADES);
    public static final DeferredItem<HunterBowItem> HUNTING_BOW = ITEMS.registerItem("hunting_bow", HunterBowItem::new, p -> p.durability(650).enchantable(1));

    public static final DeferredItem<HunterArmorItem> HUNTER_HELMET = armor(GearKind.HELMET, ArmorType.HELMET);
    public static final DeferredItem<HunterArmorItem> HUNTER_CHESTPLATE = armor(GearKind.CHESTPLATE, ArmorType.CHESTPLATE);
    public static final DeferredItem<HunterArmorItem> HUNTER_LEGGINGS = armor(GearKind.LEGGINGS, ArmorType.LEGGINGS);
    public static final DeferredItem<HunterArmorItem> HUNTER_BOOTS = armor(GearKind.BOOTS, ArmorType.BOOTS);

    public static final DeferredItem<net.minecraft.world.item.BlockItem> HUNTING_BOARD = ITEMS.registerSimpleBlockItem(ModBlocks.HUNTING_BOARD);

    private static DeferredItem<HunterWeaponItem> weapon(GearKind kind) {
        return ITEMS.registerItem(kind.name, p -> new HunterWeaponItem(kind, p), p -> p.sword(ToolMaterial.IRON, kind.damage - 2, kind.speed));
    }

    private static DeferredItem<HunterArmorItem> armor(GearKind kind, ArmorType type) {
        return ITEMS.registerItem("hunter_" + kind.name, p -> new HunterArmorItem(kind, p), p -> p.humanoidArmor(HUNTER_ARMOR, type));
    }

    private ModItems() {}
}
