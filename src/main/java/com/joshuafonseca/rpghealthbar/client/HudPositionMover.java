package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.compat.AppleSkinCompat;
import com.joshuafonseca.rpghealthbar.config.AirPosition;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Places the vanilla HUD elements that belong to the movable set at the same offset as our bars,
 * replacing any shift other mods gave them so the set always stays together.
 */
public final class HudPositionMover {
    /** The vanilla (and AppleSkin) layers that move, and the piece each moves with. */
    private static final Map<ResourceLocation, HudPiece> MOVED_LAYERS = Map.ofEntries(
            Map.entry(VanillaGuiLayers.EXPERIENCE_BAR, HudPiece.XP),
            Map.entry(VanillaGuiLayers.EXPERIENCE_LEVEL, HudPiece.XP),
            Map.entry(VanillaGuiLayers.JUMP_METER, HudPiece.XP),
            Map.entry(VanillaGuiLayers.ARMOR_LEVEL, HudPiece.ARMOR),
            // Vanilla stacks air and mount health (which replaces food while riding) on the food side.
            Map.entry(VanillaGuiLayers.FOOD_LEVEL, HudPiece.HUNGER),
            Map.entry(VanillaGuiLayers.AIR_LEVEL, HudPiece.HUNGER),
            Map.entry(VanillaGuiLayers.VEHICLE_HEALTH, HudPiece.HUNGER),
            // AppleSkin draws over the food icons, at the spot vanilla's Gui.rightHeight gives it.
            Map.entry(AppleSkinCompat.SATURATION_LAYER, HudPiece.HUNGER),
            Map.entry(AppleSkinCompat.HUNGER_RESTORED_LAYER, HudPiece.HUNGER),
            Map.entry(AppleSkinCompat.EXHAUSTION_LAYER, HudPiece.HUNGER));

    /** Whether the layer currently rendering was translated, so Post only pops what Pre pushed. */
    private static boolean pushed;
    /**
     * Vanilla draws the level's digits at guiHeight - 35 to - 29; the XP bar spans guiHeight - 29 to - 24.
     * Moving the digits down 5px centers them on the bar vertically.
     */
    private static final int LEVEL_DROP = 5;

    /** Vanilla's rows are 9px icons plus a 1px gap. */
    private static final int ROW_HEIGHT = 10;
    /** Height of the air and mount health rows already drawn below hunger this frame. */
    private static int belowHeight;
    /** Gui.rightHeight before a row drawn below hunger, or -1; put back after it, see {@link #onPost}. */
    private static int rightHeightBefore = -1;

    private HudPositionMover() {
    }

    /** How far the air and mount health rows drawn below hunger this frame reach under it. */
    public static int belowHeight() {
        return belowHeight;
    }

    /** Called as the GUI starts drawing. */
    public static void newFrame() {
        belowHeight = 0;
    }

    /** Registered at the lowest priority, so it only runs if no other mod cancelled the layer. */
    public static void onPre(RenderGuiLayerEvent.Pre event) {
        if (!ClientConfig.ENABLED.get()) {
            return;
        }
        ResourceLocation name = event.getName();
        boolean text = name.equals(VanillaGuiLayers.SELECTED_ITEM_NAME) || name.equals(VanillaGuiLayers.OVERLAY_MESSAGE);
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
        graphics.pose().pushPose();
        HudPosition.moveTo(graphics.pose(), frame.pieceOffset(piece));
        if (event.getName().equals(VanillaGuiLayers.EXPERIENCE_LEVEL)) {
            // Vanilla draws the level above the XP bar; center it on the bar instead.
            graphics.pose().translate(0, LEVEL_DROP, 0);
        }
        boolean stackedRow = name.equals(VanillaGuiLayers.AIR_LEVEL) || name.equals(VanillaGuiLayers.VEHICLE_HEALTH);
        HudPosition.Bounds hunger = frame.pieceBounds().get(HudPiece.HUNGER);
        if (stackedRow && hunger != null && ClientConfig.AIR_POSITION.get() == AirPosition.BELOW) {
            placeBelow(graphics, hunger, name.equals(VanillaGuiLayers.VEHICLE_HEALTH) ? vehicleRows() : 1);
        }
        pushed = true;
    }

    /**
     * Moves a row (or mount health's rows) from where vanilla stacks it above hunger to under hunger
     * and the rows already put there. Vanilla draws the first row at the top of the free space and
     * further mount health rows upward from it.
     */
    private static void placeBelow(GuiGraphics graphics, HudPosition.Bounds hunger, int rows) {
        Gui gui = Minecraft.getInstance().gui;
        int vanillaFirstRow = graphics.guiHeight() - gui.rightHeight;
        int firstRow = hunger.bottom() + 1 + belowHeight + (rows - 1) * ROW_HEIGHT;
        graphics.pose().translate(0, firstRow - vanillaFirstRow, 0);
        rightHeightBefore = gui.rightHeight;
    }

    /** Rows of mount health vanilla draws: 10 hearts a row, up to 30 hearts (as in Gui.getVehicleMaxHearts). */
    private static int vehicleRows() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getVehicle() instanceof LivingEntity vehicle && vehicle.showVehicleHealth()) {
            int hearts = Math.min((int) (vehicle.getMaxHealth() + 0.5F) / 2, 30);
            return Math.max(1, (hearts + 9) / 10);
        }
        return 1;
    }

    public static void onPost(RenderGuiLayerEvent.Post event) {
        if (rightHeightBefore >= 0) {
            // Rows drawn below hunger take no room above it, so rows stacked above (other mods') and
            // the held item's name don't leave a gap for them.
            Gui gui = Minecraft.getInstance().gui;
            belowHeight += gui.rightHeight - rightHeightBefore;
            gui.rightHeight = rightHeightBefore;
            rightHeightBefore = -1;
        }
        if (pushed) {
            event.getGuiGraphics().pose().popPose();
            pushed = false;
        }
    }
}
