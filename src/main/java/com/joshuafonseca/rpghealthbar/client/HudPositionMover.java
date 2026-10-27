package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.compat.IronsSpellsCompat;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Places the vanilla HUD elements that belong to the movable set at the same offset as our bars,
 * replacing any shift other mods gave them so the set always stays together.
 */
public final class HudPositionMover {
    /** The vanilla layers that move, and the piece each moves with. */
    private static final Map<ResourceLocation, HudPiece> MOVED_LAYERS = Map.of(
            VanillaGuiLayers.EXPERIENCE_BAR, HudPiece.XP,
            VanillaGuiLayers.EXPERIENCE_LEVEL, HudPiece.XP,
            VanillaGuiLayers.JUMP_METER, HudPiece.XP,
            VanillaGuiLayers.ARMOR_LEVEL, HudPiece.ARMOR,
            // Vanilla stacks air and mount health (which replaces food while riding) on the food side.
            VanillaGuiLayers.FOOD_LEVEL, HudPiece.HUNGER,
            VanillaGuiLayers.AIR_LEVEL, HudPiece.HUNGER,
            VanillaGuiLayers.VEHICLE_HEALTH, HudPiece.HUNGER);

    /** Whether the layer currently rendering was translated, so Post only pops what Pre pushed. */
    private static boolean pushed;
    /**
     * Vanilla draws the level's digits at guiHeight - 35 to - 29; the XP bar spans guiHeight - 29 to - 24.
     * Moving the digits down 5px centers them on the bar vertically.
     */
    private static final int LEVEL_DROP = 5;

    private HudPositionMover() {
    }

    /** Registered at the lowest priority, so it only runs if no other mod cancelled the layer. */
    public static void onPre(RenderGuiLayerEvent.Pre event) {
        ResourceLocation name = event.getName();
        boolean text = name.equals(VanillaGuiLayers.SELECTED_ITEM_NAME) || name.equals(VanillaGuiLayers.OVERLAY_MESSAGE);
        HudPiece piece = MOVED_LAYERS.get(name);
        if (piece == null && !text) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        HudLayer.Frame frame = HudLayer.Frame.compute(graphics.guiWidth(), graphics.guiHeight(), IronsSpellsCompat.isAvailable());
        if (text) {
            // Only moved up or down, keeping any shift other mods gave the text.
            int shift = name.equals(VanillaGuiLayers.SELECTED_ITEM_NAME)
                    ? HudTextPlacer.itemNameShift(graphics.guiWidth(), graphics.guiHeight(), frame)
                    : HudTextPlacer.overlayShift(graphics.guiWidth(), graphics.guiHeight(), frame);
            graphics.pose().pushPose();
            graphics.pose().translate(0, shift, 0);
            pushed = true;
            return;
        }
        graphics.pose().pushPose();
        HudPosition.moveTo(graphics.pose(), frame.pieceOffset(piece));
        if (event.getName().equals(VanillaGuiLayers.EXPERIENCE_LEVEL)) {
            // Vanilla draws the level above the XP bar; center it on the bar instead.
            graphics.pose().translate(0, LEVEL_DROP, 0);
        }
        pushed = true;
    }

    public static void onPost(RenderGuiLayerEvent.Post event) {
        if (pushed) {
            event.getGuiGraphics().pose().popPose();
            pushed = false;
        }
    }
}
