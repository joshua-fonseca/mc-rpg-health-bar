package com.joshuafonseca.rpghealthbar;

import com.joshuafonseca.rpghealthbar.client.HudLayer;
import com.joshuafonseca.rpghealthbar.client.HudPositionMover;
import com.joshuafonseca.rpghealthbar.client.screen.RpgConfigScreen;
import com.joshuafonseca.rpghealthbar.compat.AppleSkinCompat;
import com.joshuafonseca.rpghealthbar.compat.IronsSpellsCompat;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HalfBarMode;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

// Client-only mod: this entry point is never loaded on a dedicated server.
@Mod(value = RpgHealthBar.MODID, dist = Dist.CLIENT)
public class RpgHealthBar {
    public static final String MODID = "rpghealthbar";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation HUD_LAYER = ResourceLocation.fromNamespaceAndPath(MODID, "hud");

    public RpgHealthBar(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        // Mods → RPG Health Bar → Config, also reachable from the pause menu's Mods button.
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> new RpgConfigScreen(parent));
        modEventBus.addListener(RpgHealthBar::registerGuiLayers);
        // The HUD's layout and position are worked out once per frame, then shared by our layer and
        // the vanilla ones that move with it.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, (RenderGuiEvent.Pre event) -> HudLayer.Frame.clearCurrent());
        // Highest priority: hide replaced layers before other mods' Pre listeners run, so a mod that
        // pushes the pose in Pre (and pops in Post) never pushes for a layer that won't render.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, RpgHealthBar::hideVanillaLayers);
        // Lowest priority: only shift a layer if nothing else cancelled it, so every push is popped.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, HudPositionMover::onPre);
        NeoForge.EVENT_BUS.addListener(HudPositionMover::onPost);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        // Directly after the hearts, so vanilla layers drawn later (armor, food, air, mount health)
        // see the leftHeight/rightHeight we reserve and stack above our bars.
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, HUD_LAYER, new HudLayer());
    }

    private static void hideVanillaLayers(RenderGuiLayerEvent.Pre event) {
        ResourceLocation name = event.getName();
        if (name.equals(VanillaGuiLayers.PLAYER_HEALTH)
                || name.equals(IronsSpellsCompat.MANA_OVERLAY)
                // Its heart previews would draw over our bars.
                || name.equals(AppleSkinCompat.HEALTH_RESTORED_LAYER)
                // Its hunger overlays only fit the vanilla icons.
                || (AppleSkinCompat.isHungerOverlay(name) && ClientConfig.HUNGER_MODE.get() != HalfBarMode.VANILLA)
                || (name.equals(VanillaGuiLayers.ARMOR_LEVEL) && ClientConfig.ARMOR_MODE.get() != HalfBarMode.VANILLA)
                || (name.equals(VanillaGuiLayers.FOOD_LEVEL) && ClientConfig.HUNGER_MODE.get() != HalfBarMode.VANILLA)) {
            event.setCanceled(true);
        }
    }
}
