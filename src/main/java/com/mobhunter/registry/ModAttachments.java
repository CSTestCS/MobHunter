package com.mobhunter.registry;

import com.mobhunter.MobHunter;
import com.mobhunter.core.AnimState;
import com.mobhunter.core.CombatState;
import com.mobhunter.core.HuntState;
import com.mobhunter.core.Tier;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MobHunter.MODID);

    /** Tier of a creature. Saved and synced so the client can render tier effects. */
    public static final Supplier<AttachmentType<Tier>> TIER = ATTACHMENTS.register("tier",
            () -> AttachmentType.builder(() -> Tier.NONE)
                    .serialize(Tier.CODEC.fieldOf("tier"), t -> t != Tier.NONE)
                    .sync(Tier.STREAM_CODEC)
                    .build());

    /** Current telegraph animation of a creature (synced, never saved). */
    public static final Supplier<AttachmentType<AnimState>> ANIM = ATTACHMENTS.register("anim",
            () -> AttachmentType.builder(() -> AnimState.IDLE)
                    .sync(AnimState.STREAM_CODEC)
                    .build());

    /** Server-only combat brain of a tiered creature (cooldowns, current ability, entrance). */
    public static final Supplier<AttachmentType<CombatState>> COMBAT = ATTACHMENTS.register("combat",
            () -> AttachmentType.builder(CombatState::new).build());

    /** A player's active hunt contract. */
    public static final Supplier<AttachmentType<HuntState>> HUNT = ATTACHMENTS.register("hunt",
            () -> AttachmentType.builder(() -> HuntState.NONE)
                    .serialize(HuntState.CODEC, HuntState::active)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {}
}
