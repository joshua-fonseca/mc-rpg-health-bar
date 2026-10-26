package com.joshuafonseca.rpghealthbar;

import com.joshuafonseca.rpghealthbar.client.HudLayer;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import java.util.Set;
import org.slf4j.Logger;

// Client-only mod: this entry point is never loaded on a dedicated server.
@Mod(value = RpgHealthBar.MODID, dist = Dist.CLIENT)
public class RpgHealthBar {
    public static final String MODID = "rpghealthbar";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation HUD_LAYER = ResourceLocation.fromNamespaceAndPath(MODID, "hud");

    /** Vanilla HUD elements replaced by our bars. */
    private static final Set<ResourceLocation> REPLACED_LAYERS = Set.of(
            VanillaGuiLayers.PLAYER_HEALTH,
            VanillaGuiLayers.ARMOR_LEVEL,
            VanillaGuiLayers.FOOD_LEVEL);

    public RpgHealthBar(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(RpgHealthBar::registerGuiLayers);
        NeoForge.EVENT_BUS.addListener(RpgHealthBar::hideVanillaLayers);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        // Directly after the hearts, so vanilla layers drawn later (armor, food, air, mount health)
        // see the leftHeight/rightHeight we reserve and stack above our bars.
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, HUD_LAYER, new HudLayer());
    }

    private static void hideVanillaLayers(RenderGuiLayerEvent.Pre event) {
        if (REPLACED_LAYERS.contains(event.getName())) {
            event.setCanceled(true);
        }
    }
}
