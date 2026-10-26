package com.joshuafonseca.rpghealthbar.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Draws the pieces of a single bar: frame, fills and label. */
public final class BarRenderer {
    private static final int BORDER_COLOR = 0xFF000000;
    private static final int BACKGROUND_COLOR = 0xB0303030;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_PADDING = 3;

    private BarRenderer() {
    }

    /** 1px black border with a dark, semi-transparent background inside. */
    public static void drawFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + 1, BORDER_COLOR);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER_COLOR);
        graphics.fill(x, y + 1, x + 1, y + height - 1, BORDER_COLOR);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, BORDER_COLOR);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, BACKGROUND_COLOR);
    }

    /** Fills the inside of the frame from the left; {@code fraction} is clamped to 0–1. */
    public static void drawFill(GuiGraphics graphics, int x, int y, int width, int height, float fraction, int color) {
        int innerWidth = width - 2;
        int fillWidth = Math.round(innerWidth * Mth.clamp(fraction, 0.0F, 1.0F));
        if (fillWidth > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + height - 1, color);
        }
    }

    /** Text on the left side of the bar, vertically centered. */
    public static void drawLabel(GuiGraphics graphics, Font font, int x, int y, int height, String text) {
        // Font glyphs are 7px tall plus a 1px shadow.
        int textY = y + (height - 7) / 2;
        graphics.drawString(font, text, x + TEXT_PADDING, textY, TEXT_COLOR, true);
    }
}
