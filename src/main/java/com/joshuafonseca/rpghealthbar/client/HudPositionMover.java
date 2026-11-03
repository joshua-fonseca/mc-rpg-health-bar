package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.AirPosition;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/**
 * Places the vanilla HUD elements that belong to the movable set at the same offset as our bars,
 * replacing any shift other mods gave them so the set always stays together.
 */
public final class HudPositionMover {
    /**
     * The vanilla overlays that move, and the piece each moves with. AppleSkin draws on the food
     * icons in the food overlay's Pre and Post, so it moves with them.
     */
    private static final Map<ResourceLocation, HudPiece> MOVED_OVERLAYS = Map.ofEntries(
            Map.entry(VanillaGuiOverlay.JUMP_BAR.id(), HudPiece.XP),
            Map.entry(VanillaGuiOverlay.ARMOR_LEVEL.id(), HudPiece.ARMOR),
            // Vanilla stacks air and mount health (which replaces food while riding) on the food side.
            Map.entry(VanillaGuiOverlay.FOOD_LEVEL.id(), HudPiece.HUNGER),
            Map.entry(VanillaGuiOverlay.AIR_LEVEL.id(), HudPiece.HUNGER),
            Map.entry(VanillaGuiOverlay.MOUNT_HEALTH.id(), HudPiece.HUNGER));

    /** Whether the overlay currently rendering was translated, so Post only pops what Pre pushed. */
    private static boolean pushed;

    /** Vanilla's rows are 9px icons plus a 1px gap. */
    private static final int ROW_HEIGHT = 10;
    /** Height of the air and mount health rows already drawn below hunger this frame. */
    private static int belowHeight;
    /** ForgeGui.rightHeight before a row drawn below hunger, or -1; put back after it, see {@link #onPost}. */
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

    /** Registered at high priority, after replaced overlays are cancelled and before other mods draw on them. */
    public static void onPre(RenderGuiOverlayEvent.Pre event) {
        ResourceLocation id = event.getOverlay().id();
        HudPiece piece = MOVED_OVERLAYS.get(id);
        if (piece == null || !ClientConfig.ENABLED.get()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        HudLayer.Frame frame = HudLayer.Frame.current(graphics.guiWidth(), graphics.guiHeight());
        graphics.pose().pushPose();
        HudPosition.moveTo(graphics.pose(), frame.pieceOffset(piece));
        boolean stackedRow = id.equals(VanillaGuiOverlay.AIR_LEVEL.id()) || id.equals(VanillaGuiOverlay.MOUNT_HEALTH.id());
        if (stackedRow && ClientConfig.AIR_POSITION.get() == AirPosition.BELOW && Minecraft.getInstance().gui instanceof ForgeGui gui) {
            HudPosition.Bounds hunger = frame.pieceBounds().get(HudPiece.HUNGER);
            if (hunger != null) {
                placeBelow(graphics, gui, hunger, id.equals(VanillaGuiOverlay.MOUNT_HEALTH.id()) ? vehicleRows() : 1);
            }
        }
        pushed = true;
    }

    /**
     * Moves a row (or mount health's rows) from where vanilla stacks it above hunger to under hunger
     * and the rows already put there. Vanilla draws the first row at the top of the free space and
     * further mount health rows upward from it.
     */
    private static void placeBelow(GuiGraphics graphics, ForgeGui gui, HudPosition.Bounds hunger, int rows) {
        int vanillaFirstRow = graphics.guiHeight() - gui.rightHeight;
        int firstRow = hunger.bottom() + 1 + belowHeight + (rows - 1) * ROW_HEIGHT;
        graphics.pose().translate(0, firstRow - vanillaFirstRow, 0);
        rightHeightBefore = gui.rightHeight;
    }

    /** Rows of mount health vanilla draws: 10 hearts a row, up to 30 hearts (as in ForgeGui.renderHealthMount). */
    private static int vehicleRows() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getVehicle() instanceof LivingEntity vehicle) {
            int hearts = Math.min((int) (vehicle.getMaxHealth() + 0.5F) / 2, 30);
            return Math.max(1, (hearts + 9) / 10);
        }
        return 1;
    }

    /**
     * Places the item name and action bar. Registered at the lowest priority, so it sees every move
     * other mods give the text in Pre, and every row they reserve above the hotbar for it.
     */
    public static void onTextPre(RenderGuiOverlayEvent.Pre event) {
        ResourceLocation id = event.getOverlay().id();
        boolean itemName = id.equals(VanillaGuiOverlay.ITEM_NAME.id());
        if (!itemName && !id.equals(VanillaGuiOverlay.RECORD_OVERLAY.id()) || !ClientConfig.ENABLED.get()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        HudLayer.Frame frame = HudLayer.Frame.current(graphics.guiWidth(), graphics.guiHeight());
        // Placed vertically from scratch: an upward move another mod gave the text is applied from
        // vanilla's spot, and replaced. Horizontal moves are kept.
        float foreignY = graphics.pose().last().pose().m31();
        int raise = Math.max(0, Math.round(-foreignY));
        int shift = itemName
                ? HudTextPlacer.itemNameShift(graphics.guiWidth(), graphics.guiHeight(), frame, raise)
                : HudTextPlacer.overlayShift(graphics.guiWidth(), graphics.guiHeight(), frame, raise);
        graphics.pose().pushPose();
        graphics.pose().translate(0, shift - foreignY, 0);
        pushed = true;
    }

    /** A mod cancelled the overlay after we shifted it; Forge won't send Post, so undo it here. */
    public static void onCancelled(RenderGuiOverlayEvent.Pre event) {
        if (event.isCanceled() && pushed) {
            event.getGuiGraphics().pose().popPose();
            pushed = false;
            rightHeightBefore = -1;
        }
    }

    /** Registered at the lowest priority, so mods drawing on the overlay in Post are shifted too. */
    public static void onPost(RenderGuiOverlayEvent.Post event) {
        if (rightHeightBefore >= 0 && Minecraft.getInstance().gui instanceof ForgeGui gui) {
            // Rows drawn below hunger take no room above it, so rows stacked above (other mods') and
            // the held item's name don't leave a gap for them.
            belowHeight += gui.rightHeight - rightHeightBefore;
            gui.rightHeight = rightHeightBefore;
        }
        rightHeightBefore = -1;
        if (pushed) {
            event.getGuiGraphics().pose().popPose();
            pushed = false;
        }
    }
}
