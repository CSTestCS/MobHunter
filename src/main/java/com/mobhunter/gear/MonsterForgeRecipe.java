package com.mobhunter.gear;

import com.mobhunter.registry.ModComponents;
import com.mobhunter.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A shaped recipe whose monster materials must all come from the same creature and tier.
 * The result inherits that creature's element, colour and tier stats.
 */
public class MonsterForgeRecipe extends CustomRecipe {
    public static final MapCodec<MonsterForgeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
            BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("result").forGetter(r -> r.result)
    ).apply(i, MonsterForgeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MonsterForgeRecipe> STREAM_CODEC = StreamCodec.composite(
            ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
            ByteBufCodecs.holderRegistry(net.minecraft.core.registries.Registries.ITEM), r -> r.result,
            MonsterForgeRecipe::new);

    final ShapedRecipePattern pattern;
    final Holder<Item> result;

    public MonsterForgeRecipe(ShapedRecipePattern pattern, Holder<Item> result) {
        this.pattern = pattern;
        this.result = result;
    }

    private static @Nullable MonsterSource commonSource(CraftingInput input) {
        MonsterSource found = null;
        for (ItemStack s : input.items()) {
            if (!(s.getItem() instanceof MonsterMaterialItem)) continue;
            MonsterSource src = s.get(ModComponents.MONSTER_SOURCE.get());
            if (src == null) return null;
            if (found == null) found = src;
            else if (!found.equals(src)) return null;
        }
        return found;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return pattern.matches(input) && commonSource(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        MonsterSource src = commonSource(input);
        Item item = result.value();
        if (src == null) return ItemStack.EMPTY;
        GearKind kind = item instanceof HunterWeaponItem w ? w.kind : item instanceof HunterArmorItem a ? a.kind : GearKind.HUNTING_BOW;
        return GearStats.forge(item, kind, src);
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    @Override
    public RecipeSerializer<MonsterForgeRecipe> getSerializer() {
        return ModRecipes.MONSTER_FORGE.get();
    }
}
