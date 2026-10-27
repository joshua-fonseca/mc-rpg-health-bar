package com.joshuafonseca.rpghealthbar.client;

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
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
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
        if (player == null || player.jumpableVehicle() != null || minecraft.options.hideGui
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
        if (player.getXpNeededForNextLevel() > 0) {
            int progress = (int) (player.experienceProgress * (WIDTH + 1));
            graphics.blit(ICONS, x, y, 0, 64, WIDTH, HEIGHT);
            if (progress > 0) {
                graphics.blit(ICONS, x, y, 0, 69, progress, HEIGHT);
            }
        }
        if (player.experienceLevel > 0) {
            Font font = gui.getFont();
            String level = String.valueOf(player.experienceLevel);
            int textX = (screenWidth - font.width(level)) / 2;
            int textY = screenHeight - 35 + LEVEL_DROP;
            graphics.drawString(font, level, textX + 1, textY, 0, false);
            graphics.drawString(font, level, textX - 1, textY, 0, false);
            graphics.drawString(font, level, textX, textY + 1, 0, false);
            graphics.drawString(font, level, textX, textY - 1, 0, false);
            graphics.drawString(font, level, textX, textY, LEVEL_COLOR, false);
        }
        RenderSystem.enableBlend();
        graphics.pose().popPose();
    }
}
