package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.RpgHealthBar;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws bars in the style of the vanilla XP bar: opaque, with a dark tinted border, a bright middle
 * row, a darker bottom row and a faint pixel-noise texture. Corners are left out to round them.
 * <p>
 * Framed bars (health, mana) have a 1px border around the shaded rows; frameless ones (armor,
 * hunger) are just the shaded rows.
 * <p>
 * Each full bar is painted once into a small texture per color, so a frame only draws a few
 * textured strips. Only the fill's brighter end, which moves with the fill, is drawn pixel by pixel.
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
    /** The last column is mixed this much toward white, keeping the rows' shading. */
    private static final float TIP_WHITE = 0.7F;
    /** Flashes and highlights blend toward white, keeping the rows' shading. */
    private static final int HIGHLIGHT_COLOR = 0xFFFFFFFF;

    /** A full bar of one color and size, painted once and reused every frame. */
    private record Style(int color, int width, int height, boolean framed) {
    }

    private static final Map<Style, ResourceLocation> TEXTURES = new HashMap<>();

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
     * @param brightEnd whether the last few columns brighten slightly, ending in a near-white tip
     */
    public static void drawFill(GuiGraphics graphics, int x, int y, int width, int height, boolean framed, float fraction, int color, boolean brightEnd) {
        int fillWidth = columns(width, framed, fraction);
        if (fillWidth <= 0) {
            return;
        }
        drawColumns(graphics, x, y, width, height, framed, 0, fillWidth, color, 1.0F);
        if (brightEnd) {
            drawBrightEnd(graphics, x, y, width, height, framed, fillWidth, color);
        }
    }

    /**
     * Draws the fill's style in another color from {@code fromFraction} to {@code toFraction}, e.g.
     * the darker section just lost, or a spell's cost.
     *
     * @param keepTip leave the last column alone, so a fill's tip ending at {@code toFraction} stays visible
     * @param alpha   opacity over what's already drawn (1 for opaque)
     */
    public static void drawSection(GuiGraphics graphics, int x, int y, int width, int height, boolean framed,
                                   float fromFraction, float toFraction, int color, boolean keepTip, float alpha) {
        int start = columns(width, framed, fromFraction);
        int end = columns(width, framed, toFraction) - (keepTip ? 1 : 0);
        if (end > start) {
            drawColumns(graphics, x, y, width, height, framed, start, end, color, alpha);
        }
    }

    /**
     * Lightens fill columns {@code start} to {@code end} (exclusive) toward white by {@code alpha},
     * leaving the border alone. Used for flashes and highlights over a fill drawn with {@link #drawFill}.
     */
    public static void drawHighlight(GuiGraphics graphics, int x, int y, int width, int height, boolean framed, int start, int end, float alpha) {
        if (end <= start || alpha <= 0) {
            return;
        }
        int inset = framed ? 1 : 0;
        ResourceLocation texture = texture(new Style(HIGHLIGHT_COLOR, width, height, framed));
        RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, alpha));
        graphics.blit(texture, x + inset + start, y + inset, inset + start, inset, end - start, height - 2 * inset, width, height);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    /** How many fill columns a fraction covers. */
    public static int columns(int width, boolean framed, float fraction) {
        int inner = width - (framed ? 2 : 0);
        return Math.round(inner * Mth.clamp(fraction, 0.0F, 1.0F));
    }

    /** Text on the left side of the bar, vertically centered. */
    public static void drawLabel(GuiGraphics graphics, Font font, int x, int y, int height, String text) {
        // Digits are 7px tall; the shadow adds 1px below and doesn't count toward centering.
        int textY = y + Math.floorDiv(height - 7, 2);
        graphics.drawString(font, text, x + TEXT_PADDING, textY, TEXT_COLOR, true);
    }

    /**
     * Draws fill columns {@code start} to {@code end} (exclusive) from the bar's texture. Framed bars
     * include the border around those columns: the left border with the first column, and the right
     * border once the fill reaches it.
     */
    private static void drawColumns(GuiGraphics graphics, int x, int y, int width, int height, boolean framed,
                                    int start, int end, int color, float alpha) {
        int u0 = start;
        int u1 = end;
        if (framed) {
            int inner = width - 2;
            u0 = start == 0 ? 0 : start + 1;
            u1 = end == inner ? width : end + 1;
        }
        ResourceLocation texture = texture(new Style(color, width, height, framed));
        RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(texture, x + u0, y, u0, 0, u1 - u0, height, width, height);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    /** Redraws the fill's last few columns brighter, ending in a near-white tip. */
    private static void drawBrightEnd(GuiGraphics graphics, int x, int y, int width, int height, boolean framed, int fillWidth, int color) {
        int inset = framed ? 1 : 0;
        int rows = height - 2 * inset;
        float[] shades = shades(rows);
        graphics.drawManaged(() -> {
            for (int i = Math.max(0, fillWidth - END_WIDTH); i < fillWidth; i++) {
                int column = x + inset + i;
                float brighten = END_BRIGHTEN * (END_WIDTH - (fillWidth - 1 - i)) / END_WIDTH;
                boolean tip = i == fillWidth - 1;
                for (int row = 0; row < rows; row++) {
                    if (!framed && (row == 0 || row == rows - 1) && (column == x || column == x + width - 1)) {
                        continue;
                    }
                    int pixel = tip
                            ? scale(mix(color, 0xFFFFFFFF, TIP_WHITE), shades[row])
                            : scale(color, shades[row] + brighten + noise(i, row));
                    int rowY = y + inset + row;
                    graphics.fill(column, rowY, column + 1, rowY + 1, pixel);
                }
            }
        });
    }

    /** The texture for a style, painted on first use. */
    private static ResourceLocation texture(Style style) {
        ResourceLocation location = TEXTURES.get(style);
        if (location == null) {
            location = ResourceLocation.fromNamespaceAndPath(RpgHealthBar.MODID, String.format("bar/%08x_%dx%d%s",
                    style.color(), style.width(), style.height(), style.framed() ? "_framed" : ""));
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(paint(style)));
            TEXTURES.put(style, location);
        }
        return location;
    }

    /** A completely full bar of the style; transparent where the corners are rounded off. */
    private static NativeImage paint(Style style) {
        int width = style.width();
        int height = style.height();
        int color = style.color();
        int inset = style.framed() ? 1 : 0;
        int rows = height - 2 * inset;
        int inner = width - 2 * inset;
        float[] shades = shades(rows);
        NativeImage image = new NativeImage(width, height, true);
        for (int px = 0; px < width; px++) {
            for (int py = 0; py < height; py++) {
                boolean cornerX = px == 0 || px == width - 1;
                boolean cornerY = py == 0 || py == height - 1;
                int pixel;
                if (cornerX && cornerY) {
                    pixel = 0;
                } else if (style.framed() && cornerY) {
                    // Top and bottom border, tinted along the fill as the XP bar's progress sprite is.
                    pixel = scale(color, BORDER_BRIGHTNESS + noise(px - 1, py == 0 ? -1 : rows));
                } else if (style.framed() && cornerX) {
                    pixel = scale(color, BORDER_BRIGHTNESS + noise(px == 0 ? 0 : inner - 1, -1));
                } else {
                    int row = py - inset;
                    int column = px - inset;
                    pixel = scale(color, shades[row] + noise(column, row));
                }
                image.setPixelRGBA(px, py, toAbgr(pixel));
            }
        }
        return image;
    }

    private static float[] shades(int rows) {
        if (rows == 5) {
            return ROWS_5;
        }
        if (rows == 3) {
            return ROWS_3;
        }
        float[] flat = new float[rows];
        java.util.Arrays.fill(flat, 1.0F);
        return flat;
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

    /** Blends two opaque colors. */
    private static int mix(int from, int to, float amount) {
        int r = Math.round(Mth.lerp(amount, (from >> 16) & 0xFF, (to >> 16) & 0xFF));
        int g = Math.round(Mth.lerp(amount, (from >> 8) & 0xFF, (to >> 8) & 0xFF));
        int b = Math.round(Mth.lerp(amount, from & 0xFF, to & 0xFF));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Multiplies an opaque color's brightness, clamping each channel. */
    private static int scale(int color, float factor) {
        int r = Mth.clamp(Math.round(((color >> 16) & 0xFF) * factor), 0, 255);
        int g = Mth.clamp(Math.round(((color >> 8) & 0xFF) * factor), 0, 255);
        int b = Mth.clamp(Math.round((color & 0xFF) * factor), 0, 255);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** NativeImage stores pixels as ABGR. */
    private static int toAbgr(int argb) {
        return (argb & 0xFF00FF00) | ((argb & 0xFF) << 16) | ((argb >> 16) & 0xFF);
    }
}
