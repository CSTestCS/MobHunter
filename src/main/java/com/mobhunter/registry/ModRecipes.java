package com.mobhunter.registry;

import com.mobhunter.MobHunter;
import com.mobhunter.gear.MonsterForgeRecipe;
import com.mobhunter.hunt.AssignContractFunction;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MobHunter.MODID);
    public static final DeferredRegister<MapCodec<? extends LootItemFunction>> LOOT_FUNCTIONS = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, MobHunter.MODID);

    public static final Supplier<RecipeSerializer<MonsterForgeRecipe>> MONSTER_FORGE = SERIALIZERS.register("monster_forge",
            () -> new RecipeSerializer<>(MonsterForgeRecipe.MAP_CODEC, MonsterForgeRecipe.STREAM_CODEC));
    public static final Supplier<MapCodec<AssignContractFunction>> ASSIGN_CONTRACT = LOOT_FUNCTIONS.register("assign_contract", () -> AssignContractFunction.MAP_CODEC);

    private ModRecipes() {}
}
