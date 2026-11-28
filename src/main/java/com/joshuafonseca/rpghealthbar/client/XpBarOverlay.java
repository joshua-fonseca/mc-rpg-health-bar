package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Vanilla's XP bar, drawn like vanilla's (same texture, so resource packs apply) but at the XP
 * piece's position and length, with the level on the bar instead of above it: centered, or at the
 * end Level Text picks. Replaces the vanilla overlay, which draws both in one go.
 */
public class XpBarOverlay implements IGuiOverlay {
    /** Vanilla's HUD icons and XP bar. */
    public static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final int LEVEL_COLOR = 0x80FF20;
    /**
     * Vanilla draws the level's digits from 6px above the XP bar's top; moving them down 5px centers
     * them on the bar vertically.
     */
    private static final int LEVEL_DROP = 5;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = gui.getMinecraft();
        LocalPlayer player = minecraft.player;
        // Vanilla's conditions (see XpBarRenderer.xpBarShown), and hidden with F1.
        // Off, vanilla's own XP bar is drawn instead.
        if (!ClientConfig.ENABLED.get() || player == null || minecraft.options.hideGui) {
            return;
        }
        boolean bar = XpBarRenderer.xpBarShown(minecraft);
        // Without the bar, the level would cover the jump bar in the HUD Editor's mount view, and
        // none shows on a horse.
        boolean level = bar || XpBarRenderer.levelShown(minecraft) && !Boolean.TRUE.equals(HudPosition.previewMounted());
        if (!level) {
            return;
        }
        gui.setupOverlayRenderState(true, false);
        HudLayer.Frame frame = HudLayer.Frame.current(screenWidth, screenHeight);
        HudPosition.Bounds xp = frame.pieceBounds().get(HudPiece.XP);

        graphics.pose().pushPose();
        HudPosition.moveTo(graphics.pose(), frame.pieceOffset(HudPiece.XP));
        if (bar) {
            XpBarRenderer.renderXp(graphics, xp.left(), xp.top(), BarWidths.xpCells());
        }
        drawLevel(graphics, gui.getFont(), xp, player.experienceLevel);
        graphics.pose().popPose();
    }

    /**
     * The level, outlined like vanilla's, on the XP bar at {@code xp}, if above 0. Also used by the
     * config screen's preview.
     */
    public static void drawLevel(GuiGraphics graphics, Font font, HudPosition.Bounds xp, int level) {
        if (level <= 0) {
            return;
        }
        String text = String.valueOf(level);
        int textX = levelX(xp, font.width(text));
        int textY = xp.top() - 6 + LEVEL_DROP;
        graphics.drawString(font, text, textX + 1, textY, 0, false);
        graphics.drawString(font, text, textX - 1, textY, 0, false);
        graphics.drawString(font, text, textX, textY + 1, 0, false);
        graphics.drawString(font, text, textX, textY - 1, 0, false);
        graphics.drawString(font, text, textX, textY, LEVEL_COLOR, false);
    }

    /**
     * Where the level's number starts, for text {@code width} wide on the XP bar at {@code xp}:
     * centered, or {@link BarRenderer#TEXT_PADDING} in from the end Level Text picks.
     */
    static int levelX(HudPosition.Bounds xp, int width) {
        return switch (ClientConfig.LEVEL_TEXT.get()) {
            case LEFT -> xp.left() + BarRenderer.TEXT_PADDING;
            case RIGHT -> xp.right() - BarRenderer.TEXT_PADDING - width;
            case CENTER -> xp.left() + (xp.right() - xp.left() - width) / 2;
        };
    }
}
