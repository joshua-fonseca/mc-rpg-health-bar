package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.BarSize;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HalfBarMode;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;

/** Draws the whole bar group. */
public class HudLayer implements LayeredDraw.Layer {
    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isVisible(minecraft) || !(minecraft.getCameraEntity() instanceof Player player)) {
            return;
        }

        HudValues values = HudValues.of(player);
        Frame frame = Frame.compute(graphics.guiWidth(), graphics.guiHeight(), values.mana() != null);
        // Reserved at the default position: the vanilla rows stacked above are shifted along with
        // everything else by HudPositionMover.
        placeVanillaRows(minecraft.gui, graphics.guiHeight(), frame);

        graphics.pose().pushPose();
        // Each piece is shifted on its own by HudRenderer; this only undoes other mods' shifts.
        HudPosition.moveTo(graphics.pose(), HudPosition.Offset.ZERO);
        HudRenderer.render(graphics, minecraft.font, frame.layout(), values, frame.showArmor(), frame.showHunger(), frame.pieces(), true);
        graphics.pose().popPose();
    }

    /**
     * Modded layers don't inherit the conditions of the vanilla group they're inserted into, so
     * repeat vanilla's checks: hidden with F1, and only in survival/adventure.
     */
    public static boolean isVisible(Minecraft minecraft) {
        return !minecraft.options.hideGui && minecraft.gameMode != null && minecraft.gameMode.canHurtPlayer()
                && minecraft.getCameraEntity() instanceof Player;
    }

    /**
     * Layout, display settings and position for the current frame.
     *
     * @param bounds      the box around every shown piece, where it's drawn
     * @param pieces      each piece's shift from its default position
     * @param pieceBounds each shown piece at its default position, without any shift
     */
    public record Frame(HudLayout layout, HudPosition.Bounds bounds, HudPosition.Pieces pieces,
                        Map<HudPiece, HudPosition.Bounds> pieceBounds,
                        boolean showArmor, boolean showHunger, boolean vanillaArmor, boolean vanillaHunger) {
        public static Frame compute(int screenWidth, int screenHeight, boolean showMana) {
            HalfBarMode armorMode = ClientConfig.ARMOR_MODE.get();
            HalfBarMode hungerMode = ClientConfig.HUNGER_MODE.get();
            boolean showArmor = armorMode == HalfBarMode.BAR;
            boolean showHunger = hungerMode == HalfBarMode.BAR;
            boolean vanillaArmor = armorMode == HalfBarMode.VANILLA;
            boolean vanillaHunger = hungerMode == HalfBarMode.VANILLA;

            HudLayout layout = HudLayout.compute(screenWidth, screenHeight, showArmor || showHunger, vanillaArmor || vanillaHunger, showMana,
                    ClientConfig.HEALTH_SIZE.get() == BarSize.HALF, ClientConfig.MANA_SIZE.get() == BarSize.HALF);
            Map<HudPiece, HudPosition.Bounds> pieceBounds = pieceBounds(screenHeight, layout, armorMode, hungerMode, showMana);
            HudPosition.Pieces pieces = HudPosition.pieces(screenWidth, screenHeight, pieceBounds);
            HudPosition.Bounds bounds = null;
            for (Map.Entry<HudPiece, HudPosition.Bounds> entry : pieceBounds.entrySet()) {
                HudPosition.Bounds moved = entry.getValue().offset(pieces.get(entry.getKey()));
                bounds = bounds == null ? moved : bounds.union(moved);
            }
            return new Frame(layout, bounds, pieces, pieceBounds, showArmor, showHunger, vanillaArmor, vanillaHunger);
        }

        /** Where a piece is drawn, as a shift from its default position. */
        public HudPosition.Offset pieceOffset(HudPiece piece) {
            return pieces.get(piece);
        }

        /** Each shown piece at its default position. Health and XP are always shown. */
        private static Map<HudPiece, HudPosition.Bounds> pieceBounds(int screenHeight, HudLayout layout, HalfBarMode armorMode, HalfBarMode hungerMode, boolean showMana) {
            Map<HudPiece, HudPosition.Bounds> bounds = new EnumMap<>(HudPiece.class);
            int left = layout.x();
            int right = left + HudLayout.WIDTH;
            int iconBottom = layout.iconRowY() + HudLayout.ICON_ROW_HEIGHT;
            int barBottom = layout.halfBarY() + HudLayout.HALF_BAR_HEIGHT;
            if (armorMode == HalfBarMode.VANILLA) {
                bounds.put(HudPiece.ARMOR, new HudPosition.Bounds(left, layout.iconRowY(), left + HudLayout.ICON_ROW_WIDTH, iconBottom));
            } else if (armorMode == HalfBarMode.BAR) {
                bounds.put(HudPiece.ARMOR, new HudPosition.Bounds(left, layout.halfBarY(), left + HudLayout.HALF_BAR_WIDTH, barBottom));
            }
            if (hungerMode == HalfBarMode.VANILLA) {
                bounds.put(HudPiece.HUNGER, new HudPosition.Bounds(right - HudLayout.ICON_ROW_WIDTH, layout.iconRowY(), right, iconBottom));
            } else if (hungerMode == HalfBarMode.BAR) {
                bounds.put(HudPiece.HUNGER, new HudPosition.Bounds(layout.hungerX(), layout.halfBarY(), right, barBottom));
            }
            bounds.put(HudPiece.HEALTH, new HudPosition.Bounds(left, layout.healthY(), left + layout.healthWidth(), layout.healthY() + HudLayout.MAIN_BAR_HEIGHT));
            if (showMana) {
                bounds.put(HudPiece.MANA, new HudPosition.Bounds(layout.manaX(), layout.manaY(), layout.manaX() + layout.manaWidth(), layout.manaY() + HudLayout.MAIN_BAR_HEIGHT));
            }
            bounds.put(HudPiece.XP, HudPosition.xpBar(screenHeight, layout));
            return bounds;
        }
    }

    /**
     * Vanilla stacks the armor (left), food, air and mount health (right) rows upwards using
     * Gui.leftHeight and Gui.rightHeight: the next row's top edge is drawn at
     * {@code screenHeight - height}, and each row adds 10 (9px icons plus a 1px gap).
     * <p>
     * Vanilla armor/hunger icons go into our armor/hunger row, next to a half bar if there is one.
     * Everything else (air, mount health, other mods' rows) starts above our bars.
     * These positions are at the default spot; HudPositionMover shifts the vanilla rows with the set.
     */
    private static void placeVanillaRows(Gui gui, int screenHeight, Frame frame) {
        gui.leftHeight = frame.vanillaArmor() ? inRow(screenHeight, frame) : aboveBars(screenHeight, frame);
        gui.rightHeight = frame.vanillaHunger() ? inRow(screenHeight, frame) : aboveBars(screenHeight, frame);
    }

    /**
     * Where rows stacked above the bars start, once the vanilla icons in our row are drawn (vanilla
     * adds 10 per row drawn). Anything Gui.leftHeight or Gui.rightHeight has grown past this is rows
     * above the bars: air, mount health, other mods' rows.
     */
    public static int vanillaRowsStart(int screenHeight, Frame frame) {
        return aboveBars(screenHeight, frame);
    }

    private static int inRow(int screenHeight, Frame frame) {
        return screenHeight - frame.layout().iconRowY();
    }

    private static int aboveBars(int screenHeight, Frame frame) {
        return screenHeight - frame.layout().top() + 10;
    }
}
