package com.joshuafonseca.rpghealthbar.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Draws the pieces of a single bar: frame or track, shaded fill, and label. */
public final class BarRenderer {
    private static final int BORDER_COLOR = 0xC0000000;
    private static final int BACKGROUND_COLOR = 0x80000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_PADDING = 3;
    /** The fill's top row is mixed this much toward white, its bottom row darkened by this much. */
    private static final float HIGHLIGHT = 0.3F;
    private static final float SHADE = 0.3F;

    private BarRenderer() {
    }

    /**
     * 1px dark, semi-transparent border with a darker background inside. The corner pixels are left
     * out, rounding the corners like the XP bar. Fill with {@code inset} 1.
     */
    public static void drawFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 1, y, x + width - 1, y + 1, BORDER_COLOR);
        graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, BORDER_COLOR);
        graphics.fill(x, y + 1, x + 1, y + height - 1, BORDER_COLOR);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, BORDER_COLOR);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, BACKGROUND_COLOR);
    }

    /** Background only, for thin bars without a border, with rounded corners. Fill with {@code inset} 0. */
    public static void drawTrack(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 1, y, x + width - 1, y + 1, BACKGROUND_COLOR);
        graphics.fill(x, y + 1, x + width, y + height - 1, BACKGROUND_COLOR);
        graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, BACKGROUND_COLOR);
    }

    /**
     * Fills from the left, {@code inset} pixels inside the bar's edges; {@code fraction} is clamped to 0–1.
     * The top row is lighter and the bottom row darker, so the bar looks rounded. With {@code inset} 0
     * the fill touches the bar's rounded corners, so its top and bottom rows leave them out too.
     */
    public static void drawFill(GuiGraphics graphics, int x, int y, int width, int height, int inset, float fraction, int color) {
        int fillWidth = Math.round((width - 2 * inset) * Mth.clamp(fraction, 0.0F, 1.0F));
        if (fillWidth <= 0) {
            return;
        }
        int left = x + inset;
        int right = left + fillWidth;
        int top = y + inset;
        int bottom = y + height - inset;
        if (bottom - top < 2) {
            graphics.fill(left, top, right, bottom, color);
            return;
        }
        int edgeLeft = inset == 0 ? Math.max(left, x + 1) : left;
        int edgeRight = inset == 0 ? Math.min(right, x + width - 1) : right;
        graphics.fill(edgeLeft, top, edgeRight, top + 1, mix(color, 0xFFFFFFFF, HIGHLIGHT));
        graphics.fill(left, top + 1, right, bottom - 1, color);
        graphics.fill(edgeLeft, bottom - 1, edgeRight, bottom, mix(color, 0xFF000000, SHADE));
    }

    /** Text on the left side of the bar, vertically centered (it overflows bars shorter than the font). */
    public static void drawLabel(GuiGraphics graphics, Font font, int x, int y, int height, String text) {
        // Digits are 7px tall; the shadow adds 1px below and doesn't count toward centering.
        int textY = y + Math.floorDiv(height - 7, 2);
        graphics.drawString(font, text, x + TEXT_PADDING, textY, TEXT_COLOR, true);
    }

    /** Blends two ARGB colors, keeping {@code from}'s alpha. */
    private static int mix(int from, int to, float amount) {
        int r = Math.round(Mth.lerp(amount, (from >> 16) & 0xFF, (to >> 16) & 0xFF));
        int g = Math.round(Mth.lerp(amount, (from >> 8) & 0xFF, (to >> 8) & 0xFF));
        int b = Math.round(Mth.lerp(amount, from & 0xFF, to & 0xFF));
        return (from & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
