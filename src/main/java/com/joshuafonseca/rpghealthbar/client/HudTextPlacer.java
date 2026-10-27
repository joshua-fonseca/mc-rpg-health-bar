package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.HudPiece;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

/**
 * Where the held item's name and the action bar message go: just above whatever is stacked directly
 * above the hotbar, or right above the hotbar when nothing is. Vanilla puts them above the hearts and
 * armor rows instead, and never lower than a fixed height, which doesn't fit once pieces are moved.
 */
public final class HudTextPlacer {
    private static final int HOTBAR_WIDTH = 182;
    private static final int HOTBAR_HEIGHT = 22;
    /**
     * A piece whose bottom is closer than this above the current top leaves no room for the item
     * name (9px plus a 1px gap) below it, so it counts as stacked.
     */
    private static final int TEXT_ROOM = 10;
    /** The action bar sits this much higher than the item name, as in vanilla. */
    private static final int OVERLAY_ABOVE_NAME = 9;
    /** Vanilla's lowest positions, measured from the bottom of the screen. */
    private static final int VANILLA_NAME_MIN = 59;
    private static final int VANILLA_OVERLAY_MIN = 68;
    /** Vanilla draws the item name this much lower when the player can't be hurt (creative). */
    private static final int VANILLA_NAME_CREATIVE_DROP = 14;

    private HudTextPlacer() {
    }

    /** How far to move the item name down (positive) or up from where vanilla would draw it. */
    public static int itemNameShift(int screenWidth, int screenHeight, HudLayer.Frame frame) {
        int vanillaTop = screenHeight - Math.max(rowsHeight(), VANILLA_NAME_MIN);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null && !minecraft.gameMode.canHurtPlayer()) {
            vanillaTop += VANILLA_NAME_CREATIVE_DROP;
        }
        return nameTop(screenWidth, screenHeight, frame) - vanillaTop;
    }

    /** How far to move the action bar message from where vanilla would draw it. */
    public static int overlayShift(int screenWidth, int screenHeight, HudLayer.Frame frame) {
        int vanillaTop = screenHeight - Math.max(rowsHeight() + OVERLAY_ABOVE_NAME, VANILLA_OVERLAY_MIN);
        return nameTop(screenWidth, screenHeight, frame) - OVERLAY_ABOVE_NAME - vanillaTop;
    }

    /** Vanilla's yShift: the taller of the two stacks of rows above the hotbar. */
    private static int rowsHeight() {
        Gui gui = Minecraft.getInstance().gui;
        return Math.max(gui.leftHeight, gui.rightHeight);
    }

    /** The item name's top edge: 1px above the top of the stack over the hotbar. */
    private static int nameTop(int screenWidth, int screenHeight, HudLayer.Frame frame) {
        int hotbarLeft = screenWidth / 2 - HOTBAR_WIDTH / 2;
        int hotbarRight = hotbarLeft + HOTBAR_WIDTH;
        Minecraft minecraft = Minecraft.getInstance();
        boolean barsShown = HudLayer.isVisible(minecraft);
        // The XP piece's row holds the XP bar, or the jump meter while riding; creative has neither.
        boolean xpRowShown = minecraft.player != null && minecraft.gameMode != null
                && (minecraft.gameMode.hasExperience() || minecraft.player.jumpableVehicle() != null);
        int top = screenHeight - HOTBAR_HEIGHT;
        boolean stacked = false;
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Map.Entry<HudPiece, HudPosition.Bounds> entry : frame.pieceBounds().entrySet()) {
                boolean shown = entry.getKey() == HudPiece.XP ? xpRowShown : barsShown;
                if (!shown) {
                    continue;
                }
                HudPosition.Bounds box = entry.getValue().offset(frame.pieceOffset(entry.getKey()));
                boolean overHotbar = box.right() > hotbarLeft && box.left() < hotbarRight;
                if (overHotbar && box.top() < top && box.bottom() > top - TEXT_ROOM) {
                    top = box.top();
                    stacked = true;
                    grew = true;
                }
            }
        }
        if (stacked && barsShown) {
            // Rows vanilla and other mods stacked above the bars (air bubbles, mount health, ...).
            top -= Math.max(0, rowsHeight() - HudLayer.vanillaRowsStart(screenHeight, frame));
        }
        return top - TEXT_ROOM;
    }
}
