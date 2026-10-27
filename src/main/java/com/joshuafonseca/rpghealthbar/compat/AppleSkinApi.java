package com.joshuafonseca.rpghealthbar.compat;

import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import squeek.appleskin.ModConfig;
import squeek.appleskin.api.food.FoodValues;
import squeek.appleskin.helpers.FoodHelper;

/** References AppleSkin's classes directly; only loaded through {@link AppleSkinCompat}. */
final class AppleSkinApi {
    private AppleSkinApi() {
    }

    /** AppleSkin's "show saturation overlay" setting, which also controls the outline on our bar. */
    static boolean showSaturation() {
        return ModConfig.SHOW_SATURATION_OVERLAY.get();
    }

    /** AppleSkin's "show food values overlay" setting: whether held food's value is previewed. */
    static boolean showFoodValues() {
        return ModConfig.SHOW_FOOD_VALUES_OVERLAY.get();
    }

    /** How opaque AppleSkin's previews get at the brightest point of their flash. */
    static float maxFlashAlpha() {
        return ModConfig.MAX_HUD_OVERLAY_FLASH_ALPHA.get().floatValue();
    }

    /**
     * The held food's values as AppleSkin works them out (other mods can change them through its
     * API), or null if no food is held that the player can eat. Like AppleSkin, the main hand comes
     * first, then the off hand if its config allows.
     */
    @Nullable
    static AppleSkinCompat.FoodValue heldFood(Player player) {
        FoodValues food = edible(player, player.getMainHandItem());
        if (food == null && ModConfig.SHOW_FOOD_VALUES_OVERLAY_WHEN_OFFHAND.get()) {
            food = edible(player, player.getOffhandItem());
        }
        return food == null ? null : new AppleSkinCompat.FoodValue(food.hunger, food.getSaturationIncrement());
    }

    @Nullable
    private static FoodValues edible(Player player, ItemStack stack) {
        if (stack.isEmpty() || !FoodHelper.isFood(stack, player) || !FoodHelper.canConsume(stack, player)) {
            return null;
        }
        return FoodHelper.getModifiedFoodValues(stack, player);
    }
}
