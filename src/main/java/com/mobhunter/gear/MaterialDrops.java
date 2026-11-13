package com.mobhunter.gear;

import com.mobhunter.core.Tier;
import com.mobhunter.profile.CreatureProfile;
import com.mobhunter.registry.ModItems;
import java.util.Collection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Carves materials out of slain tiered creatures. */
public final class MaterialDrops {

    public static void addDrops(ServerLevel level, LivingEntity e, CreatureProfile profile, Tier tier, Collection<ItemEntity> drops, DamageSource source) {
        RandomSource r = e.getRandom();
        int hide, claw, gem;
        switch (tier) {
            case ALPHA -> {
                hide = 1 + r.nextInt(2);
                claw = r.nextFloat() < 0.6f ? 1 : 0;
                gem = r.nextFloat() < 0.22f ? 1 : 0;
            }
            case MONSTROUS -> {
                hide = 6 + r.nextInt(4);
                claw = 3 + r.nextInt(3);
                gem = 1 + r.nextInt(2);
            }
            case DEITY -> {
                hide = 12 + r.nextInt(5);
                claw = 6 + r.nextInt(3);
                gem = 3 + r.nextInt(2);
            }
            default -> {
                return;
            }
        }
        add(level, e, drops, ModItems.MONSTER_HIDE.get(), profile, tier, hide);
        add(level, e, drops, ModItems.MONSTER_CLAW.get(), profile, tier, claw);
        add(level, e, drops, ModItems.MONSTER_GEM.get(), profile, tier, gem);
        if (tier.isBoss()) {
            drops.add(new ItemEntity(level, e.getX(), e.getY() + 1, e.getZ(), new ItemStack(Items.EMERALD, tier == Tier.DEITY ? 32 : 10)));
            if (tier == Tier.DEITY) drops.add(new ItemEntity(level, e.getX(), e.getY() + 1, e.getZ(), new ItemStack(Items.NETHERITE_SCRAP, 2)));
        }
    }

    private static void add(ServerLevel level, LivingEntity e, Collection<ItemEntity> drops, Item item, CreatureProfile profile, Tier tier, int count) {
        while (count > 0) {
            int n = Math.min(count, 16);
            count -= n;
            ItemEntity ie = new ItemEntity(level, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), MonsterMaterialItem.create(item, profile, tier, n));
            ie.setDeltaMovement((e.getRandom().nextDouble() - 0.5) * 0.4, 0.35, (e.getRandom().nextDouble() - 0.5) * 0.4);
            drops.add(ie);
        }
    }

    private MaterialDrops() {}
}
