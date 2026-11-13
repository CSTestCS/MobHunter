package com.mobhunter.hunt;

import com.mobhunter.core.Tier;
import com.mobhunter.profile.CreatureProfile;
import com.mobhunter.registry.ModComponents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Loot / trade function that writes a random quarry onto a hunt contract. */
public class AssignContractFunction extends LootItemConditionalFunction {
    public static final MapCodec<AssignContractFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i)
            .and(Tier.CODEC.fieldOf("tier").forGetter(f -> f.tier))
            .apply(i, AssignContractFunction::new));

    private final Tier tier;

    private AssignContractFunction(Optional<Holder<LootItemCondition>> conditions, Tier tier) {
        super(conditions);
        this.tier = tier;
    }

    @Override
    public MapCodec<? extends LootItemConditionalFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        CreatureProfile p = HuntContractItem.randomTarget(context.getRandom(), tier);
        stack.set(ModComponents.CONTRACT.get(), new ContractData(p.id(), tier));
        if (tier == Tier.DEITY) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }
}
