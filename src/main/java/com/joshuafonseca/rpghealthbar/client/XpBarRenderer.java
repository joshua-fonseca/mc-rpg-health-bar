package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PlayerRideableJumping;

/**
 * Draws the XP bar, and the jump bar that replaces it while riding, at any number of cells, from
 * vanilla's own texture so texture packs still apply. Vanilla's bar is two halves, each a 1px border
 * then nine 10px cells; a longer or shorter bar has the same halves with more or fewer cells, so 18
 * cells is exactly vanilla's. The fill is stretched over the whole length.
 * <p>
 * On 1.20.1 the bars are rows of vanilla's HUD icons texture, each 182x5.
 */
public final class XpBarRenderer {
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    /** Where each bar's row starts in the icons texture, as in Gui.renderExperienceBar and renderJumpMeter. */
    private static final int XP_BACKGROUND = 64;
    private static final int XP_PROGRESS = 69;
    private static final int JUMP_COOLDOWN = 74;
    private static final int JUMP_BACKGROUND = 84;
    private static final int JUMP_PROGRESS = 89;
    private static final int HEIGHT = 5;
    /** Where the texture's right half starts, and how many cells each half has. */
    private static final int HALF = 91;
    private static final int HALF_CELLS = 9;

    private XpBarRenderer() {
    }

    /**
     * Whether the XP bar shows: with XP (not in creative), not riding something that jumps (its jump
     * bar takes the XP bar's place), and with XP Bar on Shown.
     */
    public static boolean xpBarShown(Minecraft minecraft) {
        return minecraft.player != null && minecraft.gameMode != null && minecraft.gameMode.hasExperience()
                && minecraft.player.jumpableVehicle() == null && ClientConfig.XP_BAR_SHOWN.get();
    }

    /** Whether the jump bar shows: riding something that jumps, like a horse. */
    public static boolean jumpBarShown(Minecraft minecraft) {
        return minecraft.player != null && minecraft.player.jumpableVehicle() != null;
    }

    /** The XP bar as vanilla shows it (only with XP to gain), with its top-left at {@code (x, y)}. */
    public static void renderXp(GuiGraphics graphics, int x, int y, int cells) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getXpNeededForNextLevel() > 0) {
            drawXp(graphics, x, y, cells, player.experienceProgress);
        }
    }

    /** The XP bar filled to {@code fraction}. */
    public static void drawXp(GuiGraphics graphics, int x, int y, int cells, float fraction) {
        draw(graphics, x, y, cells, XP_BACKGROUND, XP_PROGRESS, fraction);
    }

    /** The jump bar, charging or cooling down, like vanilla's. */
    public static void renderJump(GuiGraphics graphics, int x, int y, int cells, PlayerRideableJumping rideable) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (rideable.getJumpCooldown() > 0) {
            draw(graphics, x, y, cells, JUMP_BACKGROUND, JUMP_COOLDOWN, 1.0F);
        } else {
            draw(graphics, x, y, cells, JUMP_BACKGROUND, JUMP_PROGRESS, player.getJumpRidingScale());
        }
    }

    /** The background, then the fill over {@code fraction} of it. Drawn without blending, like vanilla's on 1.20.1. */
    private static void draw(GuiGraphics graphics, int x, int y, int cells, int background, int fill, float fraction) {
        int width = BarWidths.xpWidth(cells);
        RenderSystem.disableBlend();
        drawCells(graphics, background, x, y, cells, width);
        // As in vanilla, a full bar's fill reaches one pixel past the end, so it's never short.
        int filled = Math.min(width, (int) (fraction * (width + 1)));
        if (filled > 0) {
            drawCells(graphics, fill, x, y, cells, filled);
        }
        RenderSystem.enableBlend();
    }

    /** The bar's halves with {@code cells} cells between them, cut off {@code visible} pixels in. */
    private static void drawCells(GuiGraphics graphics, int row, int x, int y, int cells, int visible) {
        int leftCells = (cells + 1) / 2;
        int drawn = drawHalf(graphics, row, x, y, 0, leftCells, 0, visible);
        drawHalf(graphics, row, x, y, HALF, cells - leftCells, drawn, visible);
    }

    /** One half: its border, then its cells, repeating the texture's nine when there are more. */
    private static int drawHalf(GuiGraphics graphics, int row, int x, int y, int u, int cells, int at, int visible) {
        at = drawColumns(graphics, row, x, y, u, 1, at, visible);
        for (int i = 0; i < cells; i++) {
            at = drawColumns(graphics, row, x, y, u + 1 + (i % HALF_CELLS) * BarWidths.XP_CELL_WIDTH, BarWidths.XP_CELL_WIDTH, at, visible);
        }
        return at;
    }

    private static int drawColumns(GuiGraphics graphics, int row, int x, int y, int u, int width, int at, int visible) {
        int shown = Math.min(width, visible - at);
        if (shown > 0) {
            graphics.blit(ICONS, x + at, y, u, row, shown, HEIGHT);
        }
        return at + width;
    }
}
