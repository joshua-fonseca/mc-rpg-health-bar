package com.joshuafonseca.rpghealthbar.client;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Bar colors that change with status effects, the same way the vanilla hearts and hunger icons do:
 * poison, wither and freezing recolor the health bar, and the Hunger effect turns the hunger bar green.
 */
public final class StatusColors {
    public static final int HEALTH = 0xFFC82828;
    private static final int POISONED = 0xFF8C9A1E;
    private static final int WITHERED = 0xFF3C3636;
    private static final int FROZEN = 0xFF4CBAD8;
    public static final int HUNGER = 0xFFC86A18;
    private static final int HUNGER_EFFECT = 0xFF6E9A2E;
    /** The section just lost is the bar's color at this brightness. */
    private static final float LOST_BRIGHTNESS = 0.53F;

    private StatusColors() {
    }

    /** The health bar's color, by vanilla's rules for the hearts ({@code Gui.HeartType.forPlayer}). */
    public static int health(Player player) {
        if (player.hasEffect(MobEffects.POISON)) {
            return POISONED;
        }
        if (player.hasEffect(MobEffects.WITHER)) {
            return WITHERED;
        }
        if (player.isFullyFrozen()) {
            return FROZEN;
        }
        return HEALTH;
    }

    public static int hunger(Player player) {
        return player.hasEffect(MobEffects.HUNGER) ? HUNGER_EFFECT : HUNGER;
    }

    /** A darker shade of a bar color, for the section just lost. */
    public static int lost(int color) {
        int r = Math.round(((color >> 16) & 0xFF) * LOST_BRIGHTNESS);
        int g = Math.round(((color >> 8) & 0xFF) * LOST_BRIGHTNESS);
        int b = Math.round((color & 0xFF) * LOST_BRIGHTNESS);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
