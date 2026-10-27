package com.joshuafonseca.rpghealthbar.compat;

import com.joshuafonseca.rpghealthbar.RpgHealthBar;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

/**
 * Safe to call whether or not AppleSkin is installed. Only touches {@link AppleSkinApi} (which
 * references AppleSkin's classes) when the mod is loaded.
 */
public final class AppleSkinCompat {
    public static final String MODID = "appleskin";
    /** Cleared if using AppleSkin ever fails, e.g. a future version renamed something we use. */
    private static boolean available = ModList.get().isLoaded(MODID);

    /** The held food's preview, worked out once per GUI tick like AppleSkin does (its query fires an event). */
    @Nullable
    private static HeldFood cachedHeldFood;
    private static int cachedTick = -1;

    private AppleSkinCompat() {
    }

    /**
     * What eating the held food would bring hunger and saturation up to, previewed on the hunger
     * bar like AppleSkin previews it on the icons.
     *
     * @param saturation 0 if AppleSkin's saturation overlay is turned off
     * @param flashAlpha opacity at the brightest point of the preview's flash
     */
    public record HeldFood(float food, float saturation, float flashAlpha) {
    }

    /** What the held food restores: hunger points, and saturation (AppleSkin's saturation increment). */
    record FoodValue(int hunger, float saturation) {
    }

    /**
     * The player's saturation, to outline on the hunger bar, or 0 if AppleSkin isn't installed or
     * its saturation overlay is turned off in its config. Vanilla only sends saturation to the client
     * now and then; AppleSkin keeps it up to date, so it's only shown with AppleSkin.
     */
    public static float saturation(Player player) {
        if (!available) {
            return 0;
        }
        try {
            if (!AppleSkinApi.showSaturation()) {
                return 0;
            }
        } catch (LinkageError | RuntimeException e) {
            disable(e);
            return 0;
        }
        return player.getFoodData().getSaturationLevel();
    }

    /**
     * The preview for the food the player holds, or null if AppleSkin isn't installed, its food
     * values overlay is turned off, or no food is held that the player can eat.
     */
    @Nullable
    public static HeldFood heldFood(Player player) {
        if (!available) {
            return null;
        }
        int tick = Minecraft.getInstance().gui.getGuiTicks();
        if (tick != cachedTick) {
            cachedTick = tick;
            try {
                cachedHeldFood = queryHeldFood(player);
            } catch (LinkageError | RuntimeException e) {
                disable(e);
                cachedHeldFood = null;
            }
        }
        return cachedHeldFood;
    }

    @Nullable
    private static HeldFood queryHeldFood(Player player) {
        if (!AppleSkinApi.showFoodValues()) {
            return null;
        }
        FoodValue food = AppleSkinApi.heldFood(player);
        if (food == null) {
            return null;
        }
        // Vanilla's FoodData.eat: hunger is capped at 20, and saturation at the new hunger.
        float foodLevel = player.getFoodData().getFoodLevel();
        float newFood = Math.min(foodLevel + food.hunger(), 20);
        float newSaturation = AppleSkinApi.showSaturation()
                ? Math.min(player.getFoodData().getSaturationLevel() + food.saturation(), newFood)
                : 0;
        return new HeldFood(newFood, newSaturation, AppleSkinApi.maxFlashAlpha());
    }

    private static void disable(Throwable e) {
        available = false;
        RpgHealthBar.LOGGER.warn("Couldn't use AppleSkin; this version may not be supported. Saturation and food previews on the hunger bar are turned off.", e);
    }
}
