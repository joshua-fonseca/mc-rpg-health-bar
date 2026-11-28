package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.compat.AppleSkinCompat;
import com.joshuafonseca.rpghealthbar.compat.ConfluenceCompat;
import com.joshuafonseca.rpghealthbar.compat.ThirstCompat;
import com.joshuafonseca.rpghealthbar.config.AirPosition;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import com.joshuafonseca.rpghealthbar.config.ThirstMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Places the vanilla HUD elements that belong to the movable set at the same offset as our bars,
 * replacing any shift other mods gave them so the set always stays together.
 */
public final class HudPositionMover {
    /** The vanilla (AppleSkin's and Confluence's) layers that move, and the piece each moves with. */
    private static final Map<ResourceLocation, HudPiece> MOVED_LAYERS = Map.ofEntries(
            Map.entry(VanillaGuiLayers.EXPERIENCE_BAR, HudPiece.XP),
            Map.entry(VanillaGuiLayers.EXPERIENCE_LEVEL, HudPiece.XP),
            Map.entry(VanillaGuiLayers.JUMP_METER, HudPiece.XP),
            Map.entry(VanillaGuiLayers.ARMOR_LEVEL, HudPiece.ARMOR),
            // Vanilla stacks air and mount health (which replaces food while riding) on the food side.
            Map.entry(VanillaGuiLayers.FOOD_LEVEL, HudPiece.HUNGER),
            Map.entry(VanillaGuiLayers.AIR_LEVEL, HudPiece.AIR),
            Map.entry(VanillaGuiLayers.VEHICLE_HEALTH, HudPiece.MOUNT),
            // AppleSkin draws over the food icons, at the spot vanilla's Gui.rightHeight gives it.
            Map.entry(AppleSkinCompat.SATURATION_LAYER, HudPiece.HUNGER),
            Map.entry(AppleSkinCompat.HUNGER_RESTORED_LAYER, HudPiece.HUNGER),
            Map.entry(AppleSkinCompat.EXHAUSTION_LAYER, HudPiece.HUNGER),
            // Confluence's own armor and hunger icons, shown with Armor or Hunger on Default, sit
            // where vanilla's would.
            Map.entry(ConfluenceCompat.ARMOR_LAYER, HudPiece.ARMOR),
            Map.entry(ConfluenceCompat.FOOD_LAYER, HudPiece.HUNGER));

    /** Whether the layer currently rendering was translated, so Post only pops what Pre pushed. */
    private static boolean pushed;
    /**
     * Vanilla draws the level's digits at guiHeight - 35 to - 29; the XP bar spans guiHeight - 29 to - 24.
     * Moving the digits down 5px centers them on the bar vertically.
     */
    private static final int LEVEL_DROP = 5;

    /** Vanilla's rows are 9px icons plus a 1px gap. */
    public static final int ROW_HEIGHT = 10;
    /** Where this frame's air and mount health rows were drawn, for those that took no room above the bars. */
    private static final List<HudPosition.Bounds> drawnRows = new ArrayList<>();
    /** Gui.rightHeight before an air or mount health layer, or -1; see {@link #onPost}. */
    private static int rightHeightBefore = -1;
    /** Where that layer's rows are drawn, and whether they're where rows above the bars normally stack. */
    private static HudPosition.Bounds rowsArea;
    private static boolean rowsStacked;

    /** Gui.rightHeight before Thirst Was Taken 2's layer on Default, or -1; see {@link #keepThirstRow}. */
    private static int rightHeightBeforeThirst = -1;

    private HudPositionMover() {
    }

    /**
     * The air and mount health rows drawn this frame anywhere but stacked above the bars, which the
     * held item's name also has to stay clear of.
     */
    public static List<HudPosition.Bounds> drawnRows() {
        return drawnRows;
    }

    /** Counts rows drawn away from the stack above the bars, for {@link #drawnRows}. */
    static void addDrawnRows(HudPosition.Bounds area) {
        drawnRows.add(area);
    }

    /** Called as the GUI starts drawing. */
    public static void newFrame() {
        drawnRows.clear();
    }

    /** Registered at the lowest priority, so it only runs if no other mod cancelled the layer. */
    public static void onPre(RenderGuiLayerEvent.Pre event) {
        if (!ClientConfig.ENABLED.get()) {
            return;
        }
        ResourceLocation name = event.getName();
        boolean text = name.equals(VanillaGuiLayers.SELECTED_ITEM_NAME) || name.equals(VanillaGuiLayers.OVERLAY_MESSAGE);
        if (name.equals(ThirstCompat.LAYER) && ThirstCompat.isAvailable()) {
            moveThirstIcons(event.getGuiGraphics());
            return;
        }
        HudPiece piece = MOVED_LAYERS.get(name);
        if (piece == null && !text) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        HudLayer.Frame frame = HudLayer.Frame.current(graphics.guiWidth(), graphics.guiHeight());
        if (text) {
            // Placed vertically from scratch: an upward move another mod gave the text is applied from
            // vanilla's spot, and replaced. Horizontal moves are kept.
            float foreignY = graphics.pose().last().pose().m31();
            int raise = Math.max(0, Math.round(-foreignY));
            int shift = name.equals(VanillaGuiLayers.SELECTED_ITEM_NAME)
                    ? HudTextPlacer.itemNameShift(graphics.guiWidth(), graphics.guiHeight(), frame, raise)
                    : HudTextPlacer.overlayShift(graphics.guiWidth(), graphics.guiHeight(), frame, raise);
            graphics.pose().pushPose();
            graphics.pose().translate(0, shift - foreignY, 0);
            pushed = true;
            return;
        }
        if (name.equals(VanillaGuiLayers.EXPERIENCE_LEVEL) && frame.pieceBounds().containsKey(HudPiece.XP_LEVEL)) {
            piece = HudPiece.XP_LEVEL;
        }
        graphics.pose().pushPose();
        HudPosition.moveTo(graphics.pose(), frame.pieceOffset(piece));
        if (event.getName().equals(VanillaGuiLayers.EXPERIENCE_LEVEL)) {
            // Vanilla draws the level above the XP bar; center it on the bar instead, and put it at
            // the end the Level Text setting asks for. On its own, it's centered in its box.
            Minecraft minecraft = Minecraft.getInstance();
            int levelX = 0;
            if (minecraft.player != null) {
                int width = minecraft.font.width(String.valueOf(minecraft.player.experienceLevel));
                int vanillaX = (graphics.guiWidth() - width) / 2;
                HudPosition.Bounds box = frame.pieceBounds().get(piece);
                levelX = (piece == HudPiece.XP_LEVEL ? centeredX(box, width) : levelX(box, width)) - vanillaX;
            }
            graphics.pose().translate(levelX, LEVEL_DROP, 0);
        }
        if (piece == HudPiece.AIR || piece == HudPiece.MOUNT) {
            placeRows(graphics, frame, piece);
        }
        pushed = true;
    }

    /**
     * Thirst Was Taken 2's droplets (Default): from where it stacks them above hunger to the thirst
     * piece. Its layer only runs this far on Default; otherwise it's hidden.
     */
    private static void moveThirstIcons(GuiGraphics graphics) {
        HudLayer.Frame frame = HudLayer.Frame.current(graphics.guiWidth(), graphics.guiHeight());
        if (frame.thirst() != ThirstMode.VANILLA) {
            return;
        }
        rightHeightBeforeThirst = Minecraft.getInstance().gui.rightHeight;
        int stackedTop = graphics.guiHeight() - rightHeightBeforeThirst;
        graphics.pose().pushPose();
        HudPosition.moveTo(graphics.pose(), frame.pieceOffset(HudPiece.THIRST)
                .plus(new HudPosition.Offset(0, frame.layout().thirstY() - stackedTop)));
        pushed = true;
    }

    /**
     * With a thirst row in the layout, starts the rows stacked above hunger (air bubbles, other
     * mods') above it, however far {@code rightHeight} had got before Thirst Was Taken 2's layer.
     * Without a thirst row (thirst not shown, as while riding a living entity), or with the bars
     * hidden (creative, spectator), the rows are left where they are.
     */
    public static void keepThirstRow(GuiGraphics graphics, int rightHeight) {
        HudLayer.Frame frame = HudLayer.Frame.current(graphics.guiWidth(), graphics.guiHeight());
        if (frame.pieceBounds().containsKey(HudPiece.THIRST) && HudLayer.isVisible(Minecraft.getInstance())) {
            Minecraft.getInstance().gui.rightHeight = Math.max(rightHeight, HudLayer.vanillaRowsStart(graphics.guiHeight(), frame));
        }
    }

    /**
     * Where the XP level's number starts, for text {@code width} wide on the XP bar at {@code xp}:
     * centered, or {@link BarRenderer#TEXT_PADDING} in from the end Level Text picks.
     */
    public static int levelX(HudPosition.Bounds xp, int width) {
        return switch (ClientConfig.LEVEL_TEXT.get()) {
            case LEFT -> xp.left() + BarRenderer.TEXT_PADDING;
            case RIGHT -> xp.right() - BarRenderer.TEXT_PADDING - width;
            case CENTER -> centeredX(xp, width);
        };
    }

    /** Where text {@code width} wide starts when centered in {@code box}. */
    public static int centeredX(HudPosition.Bounds box, int width) {
        return box.left() + (box.width() - width) / 2;
    }

    /**
     * Moves the air bubbles' or mount health's rows from where vanilla stacks them above hunger to
     * their piece. Vanilla draws the first row lowest and any more above it.
     */
    private static void placeRows(GuiGraphics graphics, HudLayer.Frame frame, HudPiece piece) {
        Gui gui = Minecraft.getInstance().gui;
        int mountRows = mountRows(Minecraft.getInstance());
        HudPosition.Bounds area;
        if (piece == HudPiece.MOUNT) {
            area = mountArea(frame, graphics.guiHeight(), Math.max(1, mountRows));
        } else {
            // Mount health is only in the way if it's drawn, which the HUD Editor's normal view doesn't.
            boolean mountDrawn = mountRows > 0 && !Boolean.FALSE.equals(HudPosition.previewMounted());
            area = airArea(frame, graphics.guiHeight(), mountDrawn ? mountArea(frame, graphics.guiHeight(), mountRows) : null);
        }
        int vanillaFirstRow = graphics.guiHeight() - gui.rightHeight;
        int firstRow = area.bottom() - HudLayout.ICON_ROW_HEIGHT;
        // The pose is already shifted by the piece's offset.
        graphics.pose().translate(0, firstRow - frame.pieceOffset(piece).y() - vanillaFirstRow, 0);
        rightHeightBefore = gui.rightHeight;
        rowsArea = area;
        // At their default spot above the bars, other mods' rows stack on top of them.
        rowsStacked = ClientConfig.AIR_POSITION.get() == AirPosition.ABOVE && frame.pieceOffset(piece).equals(HudPosition.Offset.ZERO);
    }

    /**
     * Where {@code rows} rows of mount health are drawn: from the mount piece upward, or downward with
     * Air Bubbles/Mount HP on Below, and moved back on screen if they'd reach past its edge.
     */
    public static HudPosition.Bounds mountArea(HudLayer.Frame frame, int screenHeight, int rows) {
        HudPosition.Bounds first = frame.placed(HudPiece.MOUNT);
        int extra = (rows - 1) * ROW_HEIGHT;
        int top = ClientConfig.AIR_POSITION.get() == AirPosition.BELOW ? first.top() : first.top() - extra;
        int height = first.height() + extra;
        top = Math.max(0, Math.min(top, screenHeight - height));
        return new HudPosition.Bounds(first.left(), top, first.right(), top + height);
    }

    /**
     * Where the air bubbles are drawn: at their piece, or past mount health's rows if those are in the
     * way, above them or, with Air Bubbles/Mount HP on Below, under them.
     *
     * @param mount where mount health's rows are drawn, or null if they aren't
     */
    public static HudPosition.Bounds airArea(HudLayer.Frame frame, int screenHeight, @Nullable HudPosition.Bounds mount) {
        HudPosition.Bounds air = frame.placed(HudPiece.AIR);
        if (mount == null || !air.intersects(mount)) {
            return air;
        }
        int gap = ROW_HEIGHT - air.height();
        int above = mount.top() - ROW_HEIGHT;
        int below = mount.bottom() + gap;
        boolean down = ClientConfig.AIR_POSITION.get() == AirPosition.BELOW;
        // The other way if there's no room.
        int top = down ? (below + air.height() <= screenHeight ? below : above) : (above >= 0 ? above : below);
        return new HudPosition.Bounds(air.left(), top, air.right(), top + air.height());
    }

    /** How many rows of mount health the player's mount has; 0 without a mount whose health is shown. */
    public static int mountRows(Minecraft minecraft) {
        LivingEntity mount = minecraft.player == null ? null : mountWithHealth(minecraft.player);
        return mount == null ? 0 : vehicleRows(mount);
    }

    /** Whether the game is drawing air bubbles right now. */
    public static boolean airShown(Minecraft minecraft) {
        return HudLayer.isVisible(minecraft) && minecraft.getCameraEntity() instanceof Player player
                && (player.isEyeInFluid(FluidTags.WATER) || player.getAirSupply() < player.getMaxAirSupply());
    }

    /** Whether the game is drawing mount health right now. */
    public static boolean mountShown(Minecraft minecraft) {
        return HudLayer.isVisible(minecraft) && minecraft.getCameraEntity() instanceof Player player && mountWithHealth(player) != null;
    }

    /** The mount the player rides, if vanilla shows its health (in hunger's place); null otherwise. */
    @Nullable
    public static LivingEntity mountWithHealth(Player player) {
        return player.getVehicle() instanceof LivingEntity vehicle && vehicle.showVehicleHealth() ? vehicle : null;
    }

    /** Hearts of mount health vanilla draws, up to 30 (as in Gui.getVehicleMaxHearts). */
    static int vehicleHearts(LivingEntity mount) {
        return Math.min((int) (mount.getMaxHealth() + 0.5F) / 2, 30);
    }

    /** Rows of mount health vanilla draws: 10 hearts a row. */
    static int vehicleRows(LivingEntity mount) {
        return Math.max(1, (vehicleHearts(mount) + 9) / 10);
    }

    public static void onPost(RenderGuiLayerEvent.Post event) {
        if (rightHeightBefore >= 0) {
            Gui gui = Minecraft.getInstance().gui;
            boolean drawn = gui.rightHeight != rightHeightBefore;
            if (drawn && rowsStacked) {
                int screenHeight = event.getGuiGraphics().guiHeight();
                gui.rightHeight = Math.max(rightHeightBefore, screenHeight - rowsArea.top() + ROW_HEIGHT);
            } else {
                // Rows drawn elsewhere take no room above the bars, so rows stacked there (other
                // mods') and the held item's name don't leave a gap for them.
                if (drawn) {
                    drawnRows.add(rowsArea);
                }
                gui.rightHeight = rightHeightBefore;
            }
            rightHeightBefore = -1;
        }
        if (rightHeightBeforeThirst >= 0) {
            keepThirstRow(event.getGuiGraphics(), rightHeightBeforeThirst);
            rightHeightBeforeThirst = -1;
        }
        if (pushed) {
            event.getGuiGraphics().pose().popPose();
            pushed = false;
        }
    }
}
