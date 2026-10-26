package com.joshuafonseca.rpghealthbar.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Draws bars in the style of the vanilla XP bar: opaque, with a dark tinted border, a bright middle
 * row, a darker bottom row and a faint pixel-noise texture. Corners are left out to round them.
 * <p>
 * Framed bars (health, mana) have a 1px border around the shaded rows; frameless ones (armor,
 * hunger) are just the shaded rows.
 */
public final class BarRenderer {
    /** The empty part, using the XP bar background's colors: dark border, lighter edges, darker middle. */
    private static final int TRACK_BORDER = 0xFF09100C;
    private static final int TRACK_EDGE = 0xFF29352F;
    private static final int TRACK_NEAR_EDGE = 0xFF202C25;
    private static final int TRACK_MIDDLE = 0xFF19241E;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_PADDING = 3;
    /** Brightness of each fill row relative to the bar's color, top to bottom, like the XP bar's rows. */
    private static final float[] ROWS_5 = {0.85F, 1.15F, 1.3F, 1.0F, 0.7F};
    private static final float[] ROWS_3 = {0.9F, 1.25F, 0.7F};
    /** Brightness of the fill's border pixels, which are a very dark shade of the bar's color. */
    private static final float BORDER_BRIGHTNESS = 0.22F;
    /** Each pixel's brightness varies by up to this much either way, for the XP bar's texture. */
    private static final float NOISE = 0.08F;
    /** The fill brightens gradually over its last few columns, by up to this much. */
    private static final int END_WIDTH = 6;
    private static final float END_BRIGHTEN = 0.18F;

    private BarRenderer() {
    }

    /** The empty bar, which fills are drawn over. */
    public static void drawTrack(GuiGraphics graphics, int x, int y, int width, int height, boolean framed) {
        graphics.drawManaged(() -> {
            int inset = framed ? 1 : 0;
            if (framed) {
                graphics.fill(x + 1, y, x + width - 1, y + 1, TRACK_BORDER);
                graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, TRACK_BORDER);
                graphics.fill(x, y + 1, x + 1, y + height - 1, TRACK_BORDER);
                graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, TRACK_BORDER);
            }
            int rows = height - 2 * inset;
            for (int row = 0; row < rows; row++) {
                int rowY = y + inset + row;
                // Frameless bars round their corners by leaving out the end pixels of the first and last rows.
                boolean corner = !framed && (row == 0 || row == rows - 1);
                int left = x + inset + (corner ? 1 : 0);
                int right = x + width - inset - (corner ? 1 : 0);
                graphics.fill(left, rowY, right, rowY + 1, trackRowColor(row, rows));
            }
        });
    }

    /**
     * Fills the bar from the left; {@code fraction} is clamped to 0–1. Draw it over {@link #drawTrack}
     * with the same position, size and {@code framed}.
     *
     * @param brightEnd whether the last few columns brighten slightly, like the leading edge of a fluid
     */
    public static void drawFill(GuiGraphics graphics, int x, int y, int width, int height, boolean framed, float fraction, int color, boolean brightEnd) {
        int inset = framed ? 1 : 0;
        int fillWidth = Math.round((width - 2 * inset) * Mth.clamp(fraction, 0.0F, 1.0F));
        if (fillWidth <= 0) {
            return;
        }
        int rows = height - 2 * inset;
        float[] shades = rows == 5 ? ROWS_5 : rows == 3 ? ROWS_3 : null;
        graphics.drawManaged(() -> {
            for (int i = 0; i < fillWidth; i++) {
                int column = x + inset + i;
                float end = brightEnd ? END_BRIGHTEN * Math.max(0, END_WIDTH - (fillWidth - 1 - i)) / END_WIDTH : 0.0F;
                for (int row = 0; row < rows; row++) {
                    boolean corner = !framed && (row == 0 || row == rows - 1) && (column == x || column == x + width - 1);
                    if (corner) {
                        continue;
                    }
                    float shade = (shades != null ? shades[row] : 1.0F) + end + noise(i, row);
                    int rowY = y + inset + row;
                    graphics.fill(column, rowY, column + 1, rowY + 1, scale(color, shade));
                }
                if (framed) {
                    // Tint the border along the filled part, as the XP bar's progress sprite does.
                    int border = scale(color, BORDER_BRIGHTNESS + noise(i, -1));
                    graphics.fill(column, y, column + 1, y + 1, border);
                    graphics.fill(column, y + height - 1, column + 1, y + height, scale(color, BORDER_BRIGHTNESS + noise(i, rows)));
                    if (i == 0) {
                        graphics.fill(x, y + 1, x + 1, y + height - 1, border);
                    }
                    if (column == x + width - 2) {
                        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, border);
                    }
                }
            }
        });
    }

    /** Text on the left side of the bar, vertically centered. */
    public static void drawLabel(GuiGraphics graphics, Font font, int x, int y, int height, String text) {
        // Digits are 7px tall; the shadow adds 1px below and doesn't count toward centering.
        int textY = y + Math.floorDiv(height - 7, 2);
        graphics.drawString(font, text, x + TEXT_PADDING, textY, TEXT_COLOR, true);
    }

    /** Lighter rows at the top and bottom of the track, darkest in the middle. */
    private static int trackRowColor(int row, int rows) {
        int fromEdge = Math.min(row, rows - 1 - row);
        if (fromEdge == 0) {
            return rows <= 2 ? TRACK_NEAR_EDGE : TRACK_EDGE;
        }
        return fromEdge == 1 && rows > 4 ? TRACK_NEAR_EDGE : TRACK_MIDDLE;
    }

    /**
     * A fixed brightness offset for each pixel of the bar, so the texture doesn't flicker or slide
     * as the fill changes.
     */
    private static float noise(int column, int row) {
        int hash = column * 73856093 ^ (row + 7) * 19349663;
        hash ^= hash >>> 13;
        hash *= 0x5BD1E995;
        hash ^= hash >>> 15;
        return ((hash & 0xFF) / 255.0F * 2.0F - 1.0F) * NOISE;
    }

    /** Multiplies an opaque color's brightness, clamping each channel. */
    private static int scale(int color, float factor) {
        int r = Mth.clamp(Math.round(((color >> 16) & 0xFF) * factor), 0, 255);
        int g = Mth.clamp(Math.round(((color >> 8) & 0xFF) * factor), 0, 255);
        int b = Mth.clamp(Math.round((color & 0xFF) * factor), 0, 255);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
