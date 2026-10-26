package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.compat.IronsSpellsCompat;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Places the vanilla HUD elements that belong to the movable set at the same offset as our bars,
 * replacing any shift other mods gave them so the set always stays together.
 */
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
    /**
     * Vanilla draws the level's digits at guiHeight - 35 to - 29; the XP bar spans guiHeight - 29 to - 24.
     * Moving the digits down 5px centers them on the bar vertically.
     */
    private static final int LEVEL_DROP = 5;

    private HudPositionMover() {
    }

    /** Registered at the lowest priority, so it only runs if no other mod cancelled the layer. */
    public static void onPre(RenderGuiLayerEvent.Pre event) {
        if (!MOVED_LAYERS.contains(event.getName())) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        HudLayer.Frame frame = HudLayer.Frame.compute(graphics.guiWidth(), graphics.guiHeight(), IronsSpellsCompat.isAvailable());
        graphics.pose().pushPose();
        HudPosition.moveTo(graphics.pose(), frame.offset());
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
