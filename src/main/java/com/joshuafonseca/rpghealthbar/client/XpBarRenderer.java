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
 * vanilla's own sprites so texture packs still apply. Vanilla's bar is a 1px rounded end, nine cells
 * (9px, with a 1px divider between each), a 2px center piece, nine more cells and the other end. A
 * longer or shorter bar has the same ends and center with more or fewer cells on each side, so 18
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
    /** Where the sprite's pieces are: the center and its width, the right side's first cell and divider, and the right end. */
    private static final int CENTER = 90;
    private static final int CENTER_WIDTH = 2;
    private static final int RIGHT_CELL = 92;
    private static final int RIGHT_DIVIDER = 101;
    private static final int RIGHT_END = SPRITE_WIDTH - 1;
    private static final int CELL_BODY = 9;
    /** Each side has nine cells and eight dividers; more are repeated from them. */
    private static final int SIDE_CELLS = 9;
    private static final int SIDE_DIVIDERS = 8;

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

    /** The background, then the fill over {@code fraction} of it. */
    private static void draw(GuiGraphics graphics, int x, int y, int cells, ResourceLocation background, ResourceLocation fill, float fraction) {
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

    /**
     * The left end, the left side's cells, the center, the right side's cells and the right end, cut
     * off {@code visible} pixels in. Each side's cells count out from the center, so with nine on
     * each side every column is vanilla's own.
     */
    private static void drawCells(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int cells, int visible) {
        int step = BarWidths.XP_CELL_WIDTH;
        int leftCells = (cells + 1) / 2;
        int rightCells = cells - leftCells;
        int at = drawColumns(graphics, sprite, x, y, 0, 1, 0, visible);
        for (int i = 0; i < leftCells; i++) {
            // The i-th cell from the left end is the (leftCells - 1 - i)-th from the center.
            int fromCenter = leftCells - 1 - i;
            at = drawColumns(graphics, sprite, x, y, CENTER - step + 1 - (fromCenter % SIDE_CELLS) * step, CELL_BODY, at, visible);
            if (fromCenter > 0) {
                at = drawColumns(graphics, sprite, x, y, CENTER - step - ((fromCenter - 1) % SIDE_DIVIDERS) * step, 1, at, visible);
            }
        }
        at = drawColumns(graphics, sprite, x, y, CENTER, CENTER_WIDTH, at, visible);
        for (int i = 0; i < rightCells; i++) {
            if (i > 0) {
                at = drawColumns(graphics, sprite, x, y, RIGHT_DIVIDER + ((i - 1) % SIDE_DIVIDERS) * step, 1, at, visible);
            }
            at = drawColumns(graphics, sprite, x, y, RIGHT_CELL + (i % SIDE_CELLS) * step, CELL_BODY, at, visible);
        }
        drawColumns(graphics, sprite, x, y, RIGHT_END, 1, at, visible);
    }

    private static int drawColumns(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int spriteX, int width, int at, int visible) {
        int shown = Math.min(width, visible - at);
        if (shown > 0) {
            graphics.blitSprite(sprite, SPRITE_WIDTH, HEIGHT, spriteX, 0, x + at, y, shown, HEIGHT);
        }
        return at + width;
    }
}
