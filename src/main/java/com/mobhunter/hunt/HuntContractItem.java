package com.mobhunter.hunt;

import com.mobhunter.core.Tier;
import com.mobhunter.core.TierManager;
import com.mobhunter.profile.CreatureProfile;
import com.mobhunter.profile.CreatureProfiles;
import com.mobhunter.registry.ModComponents;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** Bought from the Hunt Master. Use it to begin tracking a Monstrous creature or a Deity. */
public class HuntContractItem extends Item {
    public HuntContractItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(Item item, CreatureProfile profile, Tier tier) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.CONTRACT.get(), new ContractData(profile.id(), tier));
        if (tier == Tier.DEITY) stack.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    public static CreatureProfile randomTarget(RandomSource random, Tier tier) {
        List<CreatureProfile> list = CreatureProfiles.contractable();
        return list.get(random.nextInt(list.size()));
    }

    @Override
    public Component getName(ItemStack stack) {
        ContractData data = stack.get(ModComponents.CONTRACT.get());
        if (data == null) return super.getName(stack);
        CreatureProfile p = CreatureProfiles.get(data.target());
        Component quarry = p == null ? Component.literal(data.target().toString())
                : data.tier() == Tier.DEITY ? Component.literal(p.epithet()) : TierManager.displayName(p, data.tier());
        return Component.translatable("item.mobhunter.hunt_contract.named", quarry).withStyle(data.tier().color);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown() && HuntManager.hunt(sp).active()) {
            HuntManager.abandon(sp);
            return InteractionResult.SUCCESS;
        }
        ContractData data = stack.get(ModComponents.CONTRACT.get());
        if (data == null) {
            CreatureProfile p = randomTarget(player.getRandom(), Tier.MONSTROUS);
            data = new ContractData(p.id(), Tier.MONSTROUS);
        }
        if (HuntManager.start(sp, data)) {
            stack.consume(1, player);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        ContractData data = stack.get(ModComponents.CONTRACT.get());
        if (data != null && data.tier() == Tier.DEITY) {
            CreatureProfile p = CreatureProfiles.get(data.target());
            if (p != null) tooltip.accept(Component.translatable("mobhunter.tooltip.deity_of", TierManager.creatureName(p)).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            tooltip.accept(Component.translatable("mobhunter.tooltip.contract_deity").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("mobhunter.tooltip.contract").withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("mobhunter.tooltip.contract_abandon").withStyle(ChatFormatting.DARK_GRAY));
    }
}
