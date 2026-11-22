package com.joshuafonseca.rpghealthbar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PlayerRideableJumping;

/**
 * Draws the XP bar, and the jump bar that replaces it while riding, at any number of cells, from
 * vanilla's own sprites so texture packs still apply. Vanilla's bar is two halves, each a 1px border
 * then nine 10px cells; a longer or shorter bar has the same halves with more or fewer cells, so 18
 * cells is exactly vanilla's. The fill is stretched over the whole length.
 */
public final class XpBarRenderer {
    private static final ResourceLocation XP_BACKGROUND = ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
    private static final ResourceLocation XP_PROGRESS = ResourceLocation.withDefaultNamespace("hud/experience_bar_progress");
    private static final ResourceLocation JUMP_BACKGROUND = ResourceLocation.withDefaultNamespace("hud/jump_bar_background");
    private static final ResourceLocation JUMP_COOLDOWN = ResourceLocation.withDefaultNamespace("hud/jump_bar_cooldown");
    private static final ResourceLocation JUMP_PROGRESS = ResourceLocation.withDefaultNamespace("hud/jump_bar_progress");
    /** The sprites' size, in the 182x5 pixels vanilla draws them at. */
    private static final int SPRITE_WIDTH = 182;
    private static final int HEIGHT = 5;
    /** Where the sprite's right half starts, and how many cells each half has. */
    private static final int HALF = 91;
    private static final int HALF_CELLS = 9;

    private XpBarRenderer() {
    }

    /** The XP bar as vanilla shows it (only with XP to gain), with its top-left at {@code (x, y)}. */
    public static void renderXp(GuiGraphics graphics, int x, int y, int cells) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getXpNeededForNextLevel() > 0) {
            draw(graphics, x, y, cells, XP_BACKGROUND, XP_PROGRESS, player.experienceProgress);
        }
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

    /** The background, then the fill over {@code fraction} of it. */
    public static void draw(GuiGraphics graphics, int x, int y, int cells, ResourceLocation background, ResourceLocation fill, float fraction) {
        int width = BarWidths.xpWidth(cells);
        RenderSystem.enableBlend();
        drawCells(graphics, background, x, y, cells, width);
        // As in vanilla, a full bar's fill reaches one pixel past the end, so it's never short.
        int filled = Math.min(width, (int) (fraction * (width + 1)));
        if (filled > 0) {
            drawCells(graphics, fill, x, y, cells, filled);
        }
        RenderSystem.disableBlend();
    }

    /** The sprite's halves with {@code cells} cells between them, cut off {@code visible} pixels in. */
    private static void drawCells(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int cells, int visible) {
        int leftCells = (cells + 1) / 2;
        int drawn = drawHalf(graphics, sprite, x, y, 0, leftCells, 0, visible);
        drawHalf(graphics, sprite, x, y, HALF, cells - leftCells, drawn, visible);
    }

    /** One half: its border, then its cells, repeating the sprite's nine when there are more. */
    private static int drawHalf(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int spriteX, int cells, int at, int visible) {
        at = drawColumns(graphics, sprite, x, y, spriteX, 1, at, visible);
        for (int i = 0; i < cells; i++) {
            at = drawColumns(graphics, sprite, x, y, spriteX + 1 + (i % HALF_CELLS) * BarWidths.XP_CELL_WIDTH, BarWidths.XP_CELL_WIDTH, at, visible);
        }
        return at;
    }

    private static int drawColumns(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int spriteX, int width, int at, int visible) {
        int shown = Math.min(width, visible - at);
        if (shown > 0) {
            graphics.blitSprite(sprite, SPRITE_WIDTH, HEIGHT, spriteX, 0, x + at, y, shown, HEIGHT);
        }
        return at + width;
    }
}
