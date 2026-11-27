package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.compat.ManaSource;
import com.joshuafonseca.rpghealthbar.compat.ThirstCompat;
import com.joshuafonseca.rpghealthbar.config.AirPosition;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HalfBarMode;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import com.joshuafonseca.rpghealthbar.config.ThirstMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

/** Draws the whole bar group. */
public class HudLayer implements LayeredDraw.Layer {
    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientConfig.ENABLED.get() || !isVisible(minecraft) || !(minecraft.getCameraEntity() instanceof Player player)) {
            return;
        }

        HudValues values = HudValues.of(player);
        // After HudValues.of, which turns the mana bar off if mana can't be read.
        Frame frame = Frame.current(graphics.guiWidth(), graphics.guiHeight());
        // Reserved at the default position: the vanilla rows stacked above are shifted along with
        // everything else by HudPositionMover.
        placeVanillaRows(minecraft.gui, graphics.guiHeight(), frame);

        graphics.pose().pushPose();
        // Each piece is shifted on its own by HudRenderer; this only undoes other mods' shifts.
        HudPosition.moveTo(graphics.pose(), HudPosition.Offset.ZERO);
        // Riding a mount whose health vanilla shows, its hearts take hunger's place, as in vanilla;
        // the HUD Editor shows whichever of the two its view asks for.
        Boolean editorView = HudPosition.previewMounted();
        boolean mounted = editorView != null ? editorView : HudPositionMover.mountWithHealth(player) != null;
        if (editorView != null && !editorView && values.thirst() == null && frame.thirstBar() != null) {
            values = values.withThirst(HudValues.placeholder().thirst());
        }
        HudRenderer.render(graphics, minecraft.font, frame, values, mounted, frame.pieces(), true);
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
     * @param pieces      each piece's shift from its default position
     * @param pieceBounds each shown piece at its default position, without any shift; mount health
     *                    and the air bubbles are always among them
     */
    public record Frame(HudLayout layout, HudPosition.Pieces pieces,
                        Map<HudPiece, HudPosition.Bounds> pieceBounds,
                        boolean showArmor, boolean showHunger, boolean vanillaArmor, boolean vanillaHunger,
                        ThirstMode thirst) {
        /** The HUD's frame, shared by every layer that needs it; cleared when the GUI starts drawing. */
        @Nullable
        private static Frame current;

        /** The frame for the HUD being drawn, worked out once per frame. */
        public static Frame current(int screenWidth, int screenHeight) {
            if (current == null) {
                current = compute(screenWidth, screenHeight);
            }
            return current;
        }

        /** Called as the GUI starts drawing, so settings and positions changed since are picked up. */
        public static void clearCurrent() {
            current = null;
        }

        /**
         * The frame for the current settings, with the mana bars {@link ManaSource} shows. With the
         * real HUD drawn, there's only a thirst row while the player's thirst is shown, except in
         * the HUD Editor.
         */
        public static Frame compute(int screenWidth, int screenHeight) {
            Minecraft minecraft = Minecraft.getInstance();
            boolean thirstRow = HudPosition.isPreviewing() || !(isVisible(minecraft) && minecraft.getCameraEntity() instanceof Player player)
                    || ThirstCompat.isShown(player);
            return compute(screenWidth, screenHeight, thirstRow);
        }

        /**
         * The frame with every row the settings ask for, shown right now or not: for the config
         * screen's preview and the HUD Editor.
         */
        public static Frame preview(int screenWidth, int screenHeight) {
            return compute(screenWidth, screenHeight, true);
        }

        private static Frame compute(int screenWidth, int screenHeight, boolean thirstRow) {
            HalfBarMode armorMode = ClientConfig.ARMOR_MODE.get();
            HalfBarMode hungerMode = ClientConfig.HUNGER_MODE.get();
            boolean showArmor = armorMode == HalfBarMode.BAR;
            boolean showHunger = hungerMode == HalfBarMode.BAR;
            boolean vanillaArmor = armorMode == HalfBarMode.VANILLA;
            boolean vanillaHunger = hungerMode == HalfBarMode.VANILLA;

            // Hidden without Thirst Was Taken 2.
            ThirstMode thirst = ThirstCompat.isAvailable() ? ClientConfig.THIRST_MODE.get() : ThirstMode.HIDDEN;
            int fullThirstHeight = switch (thirst) {
                case VANILLA -> HudLayout.ICON_ROW_HEIGHT;
                case BAR -> HudLayout.HALF_BAR_HEIGHT;
                case VERTICAL, HIDDEN -> 0;
            };
            int thirstHeight = thirstRow ? fullThirstHeight : 0;

            HudLayout layout = HudLayout.compute(screenWidth, screenHeight, showArmor || showHunger, vanillaArmor || vanillaHunger, ManaSource.shownBars().size(),
                    BarWidths.width(HudPiece.HEALTH), thirstHeight);
            Map<HudPiece, HudPosition.Bounds> pieceBounds = pieceBounds(screenWidth, screenHeight, layout, armorMode, hungerMode, thirst, thirstRow,
                    fullThirstHeight);
            HudPosition.Pieces pieces = HudPosition.pieces(screenWidth, screenHeight, pieceBounds, airDefault(pieceBounds));
            return new Frame(layout, pieces, pieceBounds, showArmor, showHunger, vanillaArmor, vanillaHunger, thirst);
        }

        /**
         * From the air bubbles' box to where they go until moved: nowhere (above hunger), or under
         * hunger with Air Bubbles/Mount HP on Below and a horizontal hunger bar or icons.
         */
        private static HudPosition.Offset airDefault(Map<HudPiece, HudPosition.Bounds> pieceBounds) {
            HudPosition.Bounds hunger = pieceBounds.get(HudPiece.HUNGER);
            if (ClientConfig.AIR_POSITION.get() != AirPosition.BELOW || hunger == null) {
                return HudPosition.Offset.ZERO;
            }
            return new HudPosition.Offset(0, hunger.bottom() + 1 - pieceBounds.get(HudPiece.AIR).top());
        }

        public HudPosition.Offset airDefault() {
            return airDefault(pieceBounds);
        }

        /** Every piece at its default spot. */
        public HudPosition.Pieces defaultPieces() {
            return HudPosition.Pieces.NONE.with(HudPiece.AIR, airDefault());
        }

        /** The air bubbles where they are until moved. */
        public HudPosition.Bounds defaultAir() {
            return pieceBounds.get(HudPiece.AIR).offset(airDefault());
        }

        /** The piece our thirst bar is drawn in, or null with no thirst bar of ours. */
        @Nullable
        public HudPiece thirstBar() {
            return switch (thirst) {
                case BAR -> HudPiece.THIRST;
                case VERTICAL -> HudPiece.THIRST_VERTICAL;
                case VANILLA, HIDDEN -> null;
            };
        }

        /** Each shown piece where it's drawn: its default box moved by its shift. */
        public Map<HudPiece, HudPosition.Bounds> placedBounds() {
            return HudPosition.placed(pieceBounds, pieces::get);
        }

        /** The box around every shown piece, where it's drawn. */
        public HudPosition.Bounds bounds() {
            return HudPosition.union(placedBounds().values());
        }

        /** A shown piece where it's drawn. */
        public HudPosition.Bounds placed(HudPiece piece) {
            return pieceBounds.get(piece).offset(pieces.get(piece));
        }

        /** Where a piece is drawn, as a shift from its default position. */
        public HudPosition.Offset pieceOffset(HudPiece piece) {
            return pieces.get(piece);
        }

        /** Each shown piece at its default position. Health and XP are always shown; each mana bar is its own piece. */
        private static Map<HudPiece, HudPosition.Bounds> pieceBounds(int screenWidth, int screenHeight, HudLayout layout, HalfBarMode armorMode,
                                                                    HalfBarMode hungerMode, ThirstMode thirst, boolean thirstRow,
                                                                    int fullThirstHeight) {
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
            if (thirstRow && thirst == ThirstMode.VANILLA) {
                bounds.put(HudPiece.THIRST, new HudPosition.Bounds(right - HudLayout.ICON_ROW_WIDTH, layout.thirstY(), right, layout.thirstY() + HudLayout.ICON_ROW_HEIGHT));
            } else if (thirstRow && thirst == ThirstMode.BAR) {
                bounds.put(HudPiece.THIRST, new HudPosition.Bounds(layout.hungerX(), layout.thirstY(), right, layout.thirstY() + HudLayout.HALF_BAR_HEIGHT));
            }
            // Upright bars, side by side from the hotbar outward: hunger, then thirst.
            int upright = 0;
            if (hungerMode == HalfBarMode.VERTICAL) {
                bounds.put(HudPiece.HUNGER_VERTICAL, uprightBounds(screenWidth, screenHeight, HudPiece.HUNGER_VERTICAL, upright++));
            }
            if (thirst == ThirstMode.VERTICAL) {
                bounds.put(HudPiece.THIRST_VERTICAL, uprightBounds(screenWidth, screenHeight, HudPiece.THIRST_VERTICAL, upright));
            }
            // Above the rows, and above the thirst row whether or not it's shown right now, as it
            // comes and goes.
            int airTop = layout.rowsTop() - (fullThirstHeight > 0 ? HudLayout.ROW_GAP + fullThirstHeight : 0) - HudPositionMover.ROW_HEIGHT;
            bounds.put(HudPiece.AIR, new HudPosition.Bounds(right - HudLayout.ICON_ROW_WIDTH, airTop, right, airTop + HudLayout.ICON_ROW_HEIGHT));
            // Mount health takes hunger's place; without a hunger row, it goes above the rows.
            HudPosition.Bounds hunger = bounds.get(HudPiece.HUNGER);
            int mountTop = hunger != null ? hunger.bottom() - HudLayout.ICON_ROW_HEIGHT : layout.rowsTop() - HudPositionMover.ROW_HEIGHT;
            bounds.put(HudPiece.MOUNT, new HudPosition.Bounds(right - HudLayout.ICON_ROW_WIDTH, mountTop, right, mountTop + HudLayout.ICON_ROW_HEIGHT));
            int healthLeft = left - BarWidths.left(HudPiece.HEALTH);
            bounds.put(HudPiece.HEALTH, new HudPosition.Bounds(healthLeft, layout.healthY(), healthLeft + layout.healthWidth(), layout.healthY() + HudLayout.MAIN_BAR_HEIGHT));
            List<ManaSource> manaBars = ManaSource.shownBars();
            for (int row = 0; row < manaBars.size(); row++) {
                HudPiece piece = manaBars.get(row).piece();
                bounds.put(piece, manaBounds(layout, row, piece));
            }
            bounds.put(HudPiece.XP, HudPosition.xpBar(screenHeight, layout));
            return bounds;
        }

        /** The {@code index}th upright bar's default left edge, counting from the hotbar. */
        public static int uprightLeft(int screenWidth, int index) {
            // By the player's settings rather than what's on screen, so the bars don't jump around.
            Options options = Minecraft.getInstance().options;
            boolean leftHanded = options.mainHand().get() == HumanoidArm.LEFT;
            return HudLayout.verticalBarX(screenWidth, leftHanded,
                    !leftHanded && options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR, index);
        }

        /** The {@code index}th upright bar's default box, counting from the hotbar. */
        private static HudPosition.Bounds uprightBounds(int screenWidth, int screenHeight, HudPiece piece, int index) {
            int left = uprightLeft(screenWidth, index);
            // A height set on a taller screen is cut to fit this one.
            int height = Math.min(BarWidths.height(piece), screenHeight);
            return new HudPosition.Bounds(left, screenHeight - height, left + HudLayout.MAIN_BAR_HEIGHT, screenHeight);
        }

        private static HudPosition.Bounds manaBounds(HudLayout layout, int row, HudPiece piece) {
            int top = layout.manaY(row);
            int left = layout.x() - BarWidths.left(piece);
            return new HudPosition.Bounds(left, top, left + BarWidths.width(piece), top + HudLayout.MAIN_BAR_HEIGHT);
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
