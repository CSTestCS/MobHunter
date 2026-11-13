package com.mobhunter.registry;

import com.mobhunter.MobHunter;
import com.mobhunter.gear.MonsterSource;
import com.mobhunter.hunt.ContractData;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MobHunter.MODID);

    public static final Supplier<DataComponentType<MonsterSource>> MONSTER_SOURCE = COMPONENTS.registerComponentType("monster_source",
            b -> b.persistent(MonsterSource.CODEC).networkSynchronized(MonsterSource.STREAM_CODEC));
    public static final Supplier<DataComponentType<ContractData>> CONTRACT = COMPONENTS.registerComponentType("contract",
            b -> b.persistent(ContractData.CODEC).networkSynchronized(ContractData.STREAM_CODEC));

    private ModComponents() {}
}
