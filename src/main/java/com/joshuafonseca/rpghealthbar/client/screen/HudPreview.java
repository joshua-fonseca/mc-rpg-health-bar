package com.joshuafonseca.rpghealthbar.client.screen;

import com.joshuafonseca.rpghealthbar.client.HudLayer;
import com.joshuafonseca.rpghealthbar.client.HudLayout;
import com.joshuafonseca.rpghealthbar.client.HudPosition;
import com.joshuafonseca.rpghealthbar.client.HudRenderer;
import com.joshuafonseca.rpghealthbar.client.HudValues;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import com.joshuafonseca.rpghealthbar.config.XpBarMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A stand-in for the HUD on the HUD Editor and Item Name pages, for when the real one isn't drawn:
 * without a survival player (title screen, creative), our bars with placeholder values, and with
 * no world at all, the XP bar (and the hotbar, if asked for) as plain boxes.
 */
final class HudPreview {
    private static final int XP_BAR_COLOR = 0xFF80FF20;
    private static final int HOTBAR_COLOR = 0xFF2A2A2A;

    private HudPreview() {
    }

    static void drawIfHidden(GuiGraphics graphics, Minecraft minecraft, HudLayer.Frame frame, boolean hotbar, boolean mounted) {
        boolean inWorld = minecraft.level != null;
        if (inWorld && HudLayer.isVisible(minecraft)) {
            return;
        }
        HudRenderer.render(graphics, minecraft.font, frame, HudValues.placeholder(), mounted, frame.pieces(), false);
        if (!inWorld && !mounted && ClientConfig.xpBar() == XpBarMode.LEVEL_ONLY) {
            ConfigPreview.drawSampleLevel(graphics, minecraft.font, frame.placed(HudPiece.XP));
        }
        if (!inWorld && ClientConfig.xpBar() == XpBarMode.SHOWN) {
            HudPosition.Bounds xp = frame.placed(HudPiece.XP);
            graphics.fill(xp.left(), xp.top(), xp.right(), xp.bottom(), XP_BAR_COLOR);
        }
        if (!inWorld && hotbar) {
            int left = graphics.guiWidth() / 2 - HudLayout.WIDTH / 2;
            graphics.fill(left, graphics.guiHeight() - HudLayout.HOTBAR_HEIGHT, left + HudLayout.WIDTH, graphics.guiHeight(), HOTBAR_COLOR);
        }
    }
}
