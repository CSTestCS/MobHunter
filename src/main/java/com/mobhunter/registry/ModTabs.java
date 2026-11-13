package com.mobhunter.registry;

import com.mobhunter.MobHunter;
import com.mobhunter.core.Tier;
import com.mobhunter.gear.GearKind;
import com.mobhunter.gear.GearStats;
import com.mobhunter.gear.MonsterMaterialItem;
import com.mobhunter.gear.MonsterSource;
import com.mobhunter.hunt.HuntContractItem;
import com.mobhunter.profile.CreatureProfile;
import com.mobhunter.profile.CreatureProfiles;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MobHunter.MODID);

    public static final Supplier<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mobhunter"))
            .icon(() -> new ItemStack(ModItems.GREAT_SWORD.get()))
            .displayItems((params, out) -> {
                out.accept(ModItems.HUNTING_BOARD.get());
                CreatureProfile sheep = CreatureProfiles.get(net.minecraft.resources.Identifier.withDefaultNamespace("sheep"));
                CreatureProfile piglin = CreatureProfiles.get(net.minecraft.resources.Identifier.withDefaultNamespace("zombified_piglin"));
                CreatureProfile creeper = CreatureProfiles.get(net.minecraft.resources.Identifier.withDefaultNamespace("creeper"));
                CreatureProfile spider = CreatureProfiles.get(net.minecraft.resources.Identifier.withDefaultNamespace("spider"));
                if (sheep != null) out.accept(HuntContractItem.create(ModItems.HUNT_CONTRACT.get(), sheep, Tier.DEITY));
                if (piglin != null) out.accept(HuntContractItem.create(ModItems.HUNT_CONTRACT.get(), piglin, Tier.DEITY));
                if (creeper != null) out.accept(HuntContractItem.create(ModItems.HUNT_CONTRACT.get(), creeper, Tier.MONSTROUS));
                if (spider != null) out.accept(HuntContractItem.create(ModItems.HUNT_CONTRACT.get(), spider, Tier.MONSTROUS));
                for (CreatureProfile p : new CreatureProfile[]{creeper, spider, sheep}) {
                    if (p == null) continue;
                    for (Tier t : new Tier[]{Tier.ALPHA, Tier.MONSTROUS, Tier.DEITY}) {
                        out.accept(MonsterMaterialItem.create(ModItems.MONSTER_HIDE.get(), p, t, 1));
                        out.accept(MonsterMaterialItem.create(ModItems.MONSTER_CLAW.get(), p, t, 1));
                        out.accept(MonsterMaterialItem.create(ModItems.MONSTER_GEM.get(), p, t, 1));
                    }
                }
                if (creeper != null) {
                    MonsterSource src = new MonsterSource(creeper.id(), Tier.MONSTROUS);
                    out.accept(GearStats.forge(ModItems.GREAT_SWORD.get(), GearKind.GREAT_SWORD, src));
                    out.accept(GearStats.forge(ModItems.LONG_SWORD.get(), GearKind.LONG_SWORD, src));
                    out.accept(GearStats.forge(ModItems.HAMMER.get(), GearKind.HAMMER, src));
                    out.accept(GearStats.forge(ModItems.DUAL_BLADES.get(), GearKind.DUAL_BLADES, src));
                    out.accept(GearStats.forge(ModItems.HUNTING_BOW.get(), GearKind.HUNTING_BOW, src));
                    out.accept(GearStats.forge(ModItems.HUNTER_HELMET.get(), GearKind.HELMET, src));
                    out.accept(GearStats.forge(ModItems.HUNTER_CHESTPLATE.get(), GearKind.CHESTPLATE, src));
                    out.accept(GearStats.forge(ModItems.HUNTER_LEGGINGS.get(), GearKind.LEGGINGS, src));
                    out.accept(GearStats.forge(ModItems.HUNTER_BOOTS.get(), GearKind.BOOTS, src));
                }
            })
            .build());

    private ModTabs() {}
}
