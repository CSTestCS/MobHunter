package com.mobhunter.registry;

import com.mobhunter.MobHunter;
import com.mobhunter.deity.GiantZombiePiglin;
import com.mobhunter.deity.RamGod;
import com.mobhunter.deity.StormCloud;
import com.mobhunter.entity.ElementOrb;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MobHunter.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ElementOrb>> ELEMENT_ORB = ENTITIES.registerEntityType("element_orb", ElementOrb::new,
            MobCategory.MISC, b -> b.sized(0.6f, 0.6f).clientTrackingRange(8).updateInterval(2).noSave());
    public static final DeferredHolder<EntityType<?>, EntityType<RamGod>> RAM_GOD = ENTITIES.registerEntityType("ram_god", RamGod::new,
            MobCategory.MONSTER, b -> b.sized(8f, 6f).eyeHeight(3.5f).clientTrackingRange(16).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<StormCloud>> STORM_CLOUD = ENTITIES.registerEntityType("storm_cloud", StormCloud::new,
            MobCategory.MISC, b -> b.sized(3.2f, 1.8f).clientTrackingRange(16).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<GiantZombiePiglin>> GIANT_ZOMBIE_PIGLIN = ENTITIES.registerEntityType("giant_zombie_piglin",
            GiantZombiePiglin::new, MobCategory.MONSTER, b -> b.sized(7f, 12f).eyeHeight(10.5f).clientTrackingRange(16).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<com.mobhunter.deity.EldritchDeity>> ELDRITCH_DEITY = ENTITIES.registerEntityType("eldritch_deity",
            com.mobhunter.deity.EldritchDeity::new, MobCategory.MONSTER, b -> b.sized(6f, 10f).clientTrackingRange(16).fireImmune());

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(RAM_GOD.get(), RamGod.createAttributes().build());
        event.put(STORM_CLOUD.get(), StormCloud.createAttributes().build());
        event.put(GIANT_ZOMBIE_PIGLIN.get(), GiantZombiePiglin.createAttributes().build());
        event.put(ELDRITCH_DEITY.get(), com.mobhunter.deity.EldritchDeity.createAttributes().build());
    }

    private ModEntities() {}
}
