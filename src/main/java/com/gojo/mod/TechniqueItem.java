package com.gojo.mod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * An item that casts one of Gojo's techniques on right-click, paid for with cursed energy.
 */
public class TechniqueItem extends Item {
    @FunctionalInterface
    public interface Technique {
        /** Runs server-side only. Return false if the cast fizzled, so the energy is refunded. */
        boolean cast(ServerLevel level, ServerPlayer player);
    }

    private final int cost;
    private final int cooldownTicks;
    private final String description;
    private final Technique technique;

    public TechniqueItem(Properties properties, int cost, int cooldownTicks, String description, Technique technique) {
        super(properties);
        this.cost = cost;
        this.cooldownTicks = cooldownTicks;
        this.description = description;
        this.technique = technique;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (!CursedEnergy.consume(serverPlayer, cost)) {
            serverPlayer.displayClientMessage(Component.literal("Not enough cursed energy (" + cost + " needed)")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        if (!technique.cast(serverLevel, serverPlayer)) {
            CursedEnergy.refund(serverPlayer, cost);
            return InteractionResultHolder.fail(stack);
        }

        if (cooldownTicks > 0) {
            serverPlayer.getCooldowns().addCooldown(this, cooldownTicks);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(description).withStyle(ChatFormatting.GRAY));
        if (cost > 0) {
            tooltip.add(Component.literal("Cost: " + cost + " cursed energy").withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
