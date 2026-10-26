package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.compat.IronsSpellsCompat;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Shifts the vanilla HUD elements that belong to the movable set by the same offset as our bars. */
public final class HudPositionMover {
    private static final Set<ResourceLocation> MOVED_LAYERS = Set.of(
            VanillaGuiLayers.EXPERIENCE_BAR,
            VanillaGuiLayers.EXPERIENCE_LEVEL,
            VanillaGuiLayers.JUMP_METER,
            VanillaGuiLayers.ARMOR_LEVEL,
            VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.AIR_LEVEL,
            VanillaGuiLayers.VEHICLE_HEALTH);

    /** Whether the layer currently rendering was translated, so Post only pops what Pre pushed. */
    private static boolean pushed;

    private HudPositionMover() {
    }

    /** Registered at the lowest priority, so it only runs if no other mod cancelled the layer. */
    public static void onPre(RenderGuiLayerEvent.Pre event) {
        if (!MOVED_LAYERS.contains(event.getName())) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        HudLayer.Frame frame = HudLayer.Frame.compute(graphics.guiWidth(), graphics.guiHeight(), IronsSpellsCompat.isAvailable());
        if (frame.offset().x() == 0 && frame.offset().y() == 0) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(frame.offset().x(), frame.offset().y(), 0);
        pushed = true;
    }

    public static void onPost(RenderGuiLayerEvent.Post event) {
        if (pushed) {
            event.getGuiGraphics().pose().popPose();
            pushed = false;
        }
    }
}
