package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.HudPiece;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
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

    private HudPositionMover() {
    }

    /** Registered at high priority, after replaced overlays are cancelled and before other mods draw on them. */
    public static void onPre(RenderGuiOverlayEvent.Pre event) {
        HudPiece piece = MOVED_OVERLAYS.get(event.getOverlay().id());
        if (piece == null) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        HudLayer.Frame frame = HudLayer.Frame.current(graphics.guiWidth(), graphics.guiHeight());
        graphics.pose().pushPose();
        HudPosition.moveTo(graphics.pose(), frame.pieceOffset(piece));
        pushed = true;
    }

    /**
     * Places the item name and action bar. Registered at the lowest priority, so it sees every move
     * other mods give the text in Pre, and every row they reserve above the hotbar for it.
     */
    public static void onTextPre(RenderGuiOverlayEvent.Pre event) {
        ResourceLocation id = event.getOverlay().id();
        boolean itemName = id.equals(VanillaGuiOverlay.ITEM_NAME.id());
        if (!itemName && !id.equals(VanillaGuiOverlay.RECORD_OVERLAY.id())) {
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
        }
    }

    /** Registered at the lowest priority, so mods drawing on the overlay in Post are shifted too. */
    public static void onPost(RenderGuiOverlayEvent.Post event) {
        if (pushed) {
            event.getGuiGraphics().pose().popPose();
            pushed = false;
        }
    }
}
