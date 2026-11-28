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
        // The HUD Editor's mount view doesn't move a level on its own, as none shows on a horse.
        boolean levelAlone = XpBarRenderer.levelShown(minecraft) && !Boolean.TRUE.equals(HudPosition.previewMounted());
        if (!bar && !levelAlone) {
            return;
        }
        gui.setupOverlayRenderState(true, false);
        HudLayer.Frame frame = HudLayer.Frame.current(screenWidth, screenHeight);
        HudPiece piece = bar ? HudPiece.XP : HudPiece.XP_LEVEL;
        HudPosition.Bounds box = frame.pieceBounds().get(piece);

        graphics.pose().pushPose();
        HudPosition.moveTo(graphics.pose(), frame.pieceOffset(piece));
        if (bar) {
            XpBarRenderer.renderXp(graphics, box.left(), box.top(), BarWidths.xpCells());
            drawLevel(graphics, gui.getFont(), box, player.experienceLevel);
        } else {
            drawLevelAlone(graphics, gui.getFont(), box, player.experienceLevel);
        }
        graphics.pose().popPose();
    }

    /**
     * The level, outlined like vanilla's, on the XP bar at {@code xp}, if above 0. Also used by the
     * config screen's preview.
     */
    public static void drawLevel(GuiGraphics graphics, Font font, HudPosition.Bounds xp, int level) {
        draw(graphics, font, level, levelX(xp, font.width(String.valueOf(level))), xp.top() - 6 + LEVEL_DROP);
    }

    /** The level on its own, centered in {@code box}, if above 0. */
    public static void drawLevelAlone(GuiGraphics graphics, Font font, HudPosition.Bounds box, int level) {
        draw(graphics, font, level, box.left() + (box.width() - font.width(String.valueOf(level))) / 2, box.top() + 1);
    }

    private static void draw(GuiGraphics graphics, Font font, int level, int textX, int textY) {
        if (level <= 0) {
            return;
        }
        String text = String.valueOf(level);
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
