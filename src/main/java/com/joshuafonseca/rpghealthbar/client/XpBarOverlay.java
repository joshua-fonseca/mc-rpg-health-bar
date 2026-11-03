package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Vanilla's XP bar, drawn the way vanilla does (same texture, so resource packs apply) but at the
 * XP piece's position and with the level centered on the bar instead of above it. Replaces the
 * vanilla overlay, which draws both in one go.
 */
public class XpBarOverlay implements IGuiOverlay {
    /** Vanilla's HUD icons and XP bar. */
    public static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final int WIDTH = 182;
    private static final int HEIGHT = 5;
    private static final int LEVEL_COLOR = 0x80FF20;
    /**
     * Vanilla draws the level's digits at screenHeight - 35 to - 29; the XP bar spans screenHeight - 29 to - 24.
     * Moving the digits down 5px centers them on the bar vertically.
     */
    private static final int LEVEL_DROP = 5;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = gui.getMinecraft();
        LocalPlayer player = minecraft.player;
        // Vanilla's conditions: not while riding something you can make jump (the jump meter takes
        // its place), hidden with F1, and only in modes with experience.
        // Off, vanilla's own XP bar is drawn instead.
        if (!ClientConfig.ENABLED.get() || player == null || player.jumpableVehicle() != null || minecraft.options.hideGui
                || minecraft.gameMode == null || !minecraft.gameMode.hasExperience()) {
            return;
        }
        gui.setupOverlayRenderState(true, false);
        HudLayer.Frame frame = HudLayer.Frame.current(screenWidth, screenHeight);
        int x = screenWidth / 2 - WIDTH / 2;
        int y = screenHeight - 29;

        graphics.pose().pushPose();
        HudPosition.moveTo(graphics.pose(), frame.pieceOffset(HudPiece.XP));
        RenderSystem.disableBlend();
        boolean withBar = player.getXpNeededForNextLevel() > 0;
        draw(graphics, gui.getFont(), x, y, withBar, (int) (player.experienceProgress * (WIDTH + 1)), player.experienceLevel);
        RenderSystem.enableBlend();
        graphics.pose().popPose();
    }

    /**
     * The bar with its top left at {@code (x, y)}, filled {@code progress} pixels, if {@code withBar};
     * and the level, outlined like vanilla's and centered on the bar, if above 0. Also used by the
     * config screen's preview.
     */
    public static void draw(GuiGraphics graphics, Font font, int x, int y, boolean withBar, int progress, int level) {
        if (withBar) {
            graphics.blit(ICONS, x, y, 0, 64, WIDTH, HEIGHT);
            if (progress > 0) {
                graphics.blit(ICONS, x, y, 0, 69, progress, HEIGHT);
            }
        }
        if (level > 0) {
            String text = String.valueOf(level);
            int textX = x + (WIDTH - font.width(text)) / 2;
            int textY = y - 6 + LEVEL_DROP;
            graphics.drawString(font, text, textX + 1, textY, 0, false);
            graphics.drawString(font, text, textX - 1, textY, 0, false);
            graphics.drawString(font, text, textX, textY + 1, 0, false);
            graphics.drawString(font, text, textX, textY - 1, 0, false);
            graphics.drawString(font, text, textX, textY, LEVEL_COLOR, false);
        }
    }
}
