package com.joshuafonseca.rpghealthbar.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.EventHooks;

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

    /** Colors for heart types other mods add, taken from their heart texture. */
    private static final Map<Gui.HeartType, Integer> MODDED = new HashMap<>();

    private StatusColors() {
    }

    /** The health bar's color for the player's current heart type. */
    public static int health(Player player) {
        Gui.HeartType type = heartType(player);
        if (type == Gui.HeartType.NORMAL) {
            return HEALTH;
        }
        if (type == Gui.HeartType.POISIONED) {
            return POISONED;
        }
        if (type == Gui.HeartType.WITHERED) {
            return WITHERED;
        }
        if (type == Gui.HeartType.FROZEN) {
            return FROZEN;
        }
        return MODDED.computeIfAbsent(type, StatusColors::fromHeartTexture);
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

    /**
     * Vanilla's rules from {@code Gui.HeartType.forPlayer} (not accessible), then NeoForge's event,
     * which lets other mods change the heart type just as they would for the vanilla hearts.
     */
    private static Gui.HeartType heartType(Player player) {
        Gui.HeartType type;
        if (player.hasEffect(MobEffects.POISON)) {
            type = Gui.HeartType.POISIONED;
        } else if (player.hasEffect(MobEffects.WITHER)) {
            type = Gui.HeartType.WITHERED;
        } else if (player.isFullyFrozen()) {
            type = Gui.HeartType.FROZEN;
        } else {
            type = Gui.HeartType.NORMAL;
        }
        return EventHooks.firePlayerHeartTypeEvent(player, type);
    }

    /** The average color of a heart type's full-heart sprite, or the normal red if it can't be read. */
    private static int fromHeartTexture(Gui.HeartType type) {
        try {
            NativeImage image = Minecraft.getInstance().getGuiSprites()
                    .getSprite(type.getSprite(false, false, false)).contents().getOriginalImage();
            long r = 0;
            long g = 0;
            long b = 0;
            long count = 0;
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    int abgr = image.getPixelRGBA(x, y);
                    if ((abgr >>> 24) < 128) {
                        continue;
                    }
                    r += abgr & 0xFF;
                    g += (abgr >> 8) & 0xFF;
                    b += (abgr >> 16) & 0xFF;
                    count++;
                }
            }
            if (count == 0) {
                return HEALTH;
            }
            return 0xFF000000 | (int) (r / count) << 16 | (int) (g / count) << 8 | (int) (b / count);
        } catch (RuntimeException e) {
            return HEALTH;
        }
    }
}
