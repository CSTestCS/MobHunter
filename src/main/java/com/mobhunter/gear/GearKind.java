package com.mobhunter.gear;

import net.minecraft.world.entity.EquipmentSlot;

public enum GearKind {
    GREAT_SWORD("great_sword", 9f, -3.15f, null),
    LONG_SWORD("long_sword", 6.5f, -2.5f, null),
    HAMMER("hammer", 8f, -3.0f, null),
    DUAL_BLADES("dual_blades", 3.5f, -1.5f, null),
    HUNTING_BOW("hunting_bow", 0f, 0f, null),
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
        int[][] t = switch (this) {
            case HELMET -> new int[][]{{2}, {3}, {4}};
            case CHESTPLATE -> new int[][]{{6}, {8}, {9}};
            case LEGGINGS -> new int[][]{{5}, {6}, {7}};
            case BOOTS -> new int[][]{{2}, {3}, {4}};
            default -> new int[][]{{0}, {0}, {0}};
        };
        return t[Math.max(0, Math.min(2, rank - 1))][0];
    }
}
