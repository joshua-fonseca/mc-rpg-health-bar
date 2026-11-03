package com.joshuafonseca.rpghealthbar.client.screen;

import com.joshuafonseca.rpghealthbar.client.HudLayer;
import com.joshuafonseca.rpghealthbar.client.HudPosition;
import com.joshuafonseca.rpghealthbar.client.HudRenderer;
import com.joshuafonseca.rpghealthbar.client.HudValues;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A stand-in for the HUD on the Move HUD and Item Name pages, for when the real one isn't drawn:
 * without a survival player (title screen, creative), our bars with placeholder values, and with
 * no world at all, the XP bar (and the hotbar, if asked for) as plain boxes.
 */
final class HudPreview {
    private static final int XP_BAR_COLOR = 0xFF80FF20;
    private static final int HOTBAR_COLOR = 0xFF2A2A2A;
    private static final int HOTBAR_WIDTH = 182;
    private static final int HOTBAR_HEIGHT = 22;

    private HudPreview() {
    }

    static void drawIfHidden(GuiGraphics graphics, Minecraft minecraft, HudLayer.Frame frame, HudPosition.Pieces pieces, boolean hotbar) {
        boolean inWorld = minecraft.level != null;
        if (inWorld && HudLayer.isVisible(minecraft)) {
            return;
        }
        HudRenderer.render(graphics, minecraft.font, frame.layout(), HudValues.placeholder(), frame.showArmor(), frame.showHunger(), pieces, false);
        if (!inWorld) {
            HudPosition.Bounds xp = frame.pieceBounds().get(HudPiece.XP).offset(pieces.get(HudPiece.XP));
            graphics.fill(xp.left(), xp.top(), xp.right(), xp.bottom(), XP_BAR_COLOR);
            if (hotbar) {
                int left = graphics.guiWidth() / 2 - HOTBAR_WIDTH / 2;
                graphics.fill(left, graphics.guiHeight() - HOTBAR_HEIGHT, left + HOTBAR_WIDTH, graphics.guiHeight(), HOTBAR_COLOR);
            }
        }
    }
}
