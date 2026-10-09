package com.gojo.mod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Cursed energy and Infinity state, stored in the player's Forge persistent NBT (server side).
 */
public final class CursedEnergy {
    public static final int MAX = 1000;

    private static final String ENERGY_KEY = "gojo_cursed_energy";
    private static final String INFINITY_KEY = "gojo_infinity";

    private CursedEnergy() {
    }

    public static int get(Player player) {
        CompoundTag tag = player.getPersistentData();
        return tag.contains(ENERGY_KEY) ? tag.getInt(ENERGY_KEY) : MAX;
    }

    public static void set(Player player, int value) {
        player.getPersistentData().putInt(ENERGY_KEY, Mth.clamp(value, 0, MAX));
    }

    /** Creative players cast for free. */
    public static boolean consume(Player player, int cost) {
        if (cost <= 0 || player.getAbilities().instabuild) {
            return true;
        }
        int current = get(player);
        if (current < cost) {
            return false;
        }
        set(player, current - cost);
        return true;
    }

    public static void refund(Player player, int cost) {
        if (cost > 0 && !player.getAbilities().instabuild) {
            set(player, get(player) + cost);
        }
    }

    public static void regen(Player player) {
        int current = get(player);
        if (current < MAX) {
            set(player, current + (hasSixEyes(player) ? 3 : 1));
        }
    }

    public static boolean hasSixEyes(Player player) {
        if (player.getOffhandItem().is(ModItems.SIX_EYES.get())) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.SIX_EYES.get())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInfinity(Player player) {
        return player.getPersistentData().getBoolean(INFINITY_KEY);
    }

    public static void setInfinity(Player player, boolean active) {
        player.getPersistentData().putBoolean(INFINITY_KEY, active);
    }
}
