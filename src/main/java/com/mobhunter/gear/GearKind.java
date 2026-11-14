package com.mobhunter.gear;

import net.minecraft.world.entity.EquipmentSlot;

/** The equipment of the bestiary: a Blade, a Spear and a set of Plate armor per creature and tier. */
public enum GearKind {
    BLADE("blade", 6f, -2.4f, null),
    SPEAR("spear", 4f, -2.8f, null),
    HELMET("helmet", 0, 0, EquipmentSlot.HEAD),
    CHESTPLATE("chestplate", 0, 0, EquipmentSlot.CHEST),
    LEGGINGS("leggings", 0, 0, EquipmentSlot.LEGS),
    BOOTS("boots", 0, 0, EquipmentSlot.FEET);

    public final String name;
    public final float damage;
    public final float speed;
    public final EquipmentSlot slot;

    GearKind(String name, float damage, float speed, EquipmentSlot slot) {
        this.name = name;
        this.damage = damage;
        this.speed = speed;
        this.slot = slot;
    }

    public boolean isArmor() {
        return slot != null;
    }

    public int armor(int rank) {
        int r = Math.max(0, Math.min(2, rank - 1));
        return switch (this) {
            case HELMET -> new int[]{2, 3, 4}[r];
            case CHESTPLATE -> new int[]{6, 8, 9}[r];
            case LEGGINGS -> new int[]{5, 6, 7}[r];
            case BOOTS -> new int[]{2, 3, 4}[r];
            default -> 0;
        };
    }
}
