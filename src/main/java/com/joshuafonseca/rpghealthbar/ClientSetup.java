package com.joshuafonseca.rpghealthbar;

import com.joshuafonseca.rpghealthbar.client.HudLayer;
import com.joshuafonseca.rpghealthbar.client.HudPositionMover;
import com.joshuafonseca.rpghealthbar.client.XpBarOverlay;
import com.joshuafonseca.rpghealthbar.client.screen.RpgConfigScreen;
import com.joshuafonseca.rpghealthbar.compat.IronsSpellsCompat;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HalfBarMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Registers everything on the client. */
final class ClientSetup {
    private ClientSetup() {
    }

    static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        // Mods → RPG Health Bar → Config.
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new RpgConfigScreen(parent)));
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientSetup::registerOverlays);
        // Highest priority: hide replaced overlays before other mods' Pre listeners run, so a mod that
        // pushes the pose in Pre (and pops in Post) never pushes for an overlay that won't render.
        // Forge doesn't send Post for a cancelled overlay, and AppleSkin draws its hunger and health
        // previews in the Pre and Post of the overlays it goes with, so they're hidden along with them.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ClientSetup::hideVanillaOverlays);
        // Right after: shift the overlays that move with the set, before other mods (like AppleSkin)
        // draw on them in Pre, and undo it in Post once they've drawn.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, HudPositionMover::onPre);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, HudPositionMover::onCancelled);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, HudPositionMover::onPost);
        // Saved positions are written one value per tick; see ClientConfig.
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                ClientConfig.writePending();
            }
        });
        MinecraftForge.EVENT_BUS.addListener((GameShuttingDownEvent event) -> ClientConfig.writeAllPending());
    }

    private static void registerOverlays(RegisterGuiOverlaysEvent event) {
        // Directly after the hearts, so vanilla overlays drawn later (armor, food, air, mount health)
        // see the leftHeight/rightHeight we reserve and stack above our bars.
        event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), "hud", new HudLayer());
        // Vanilla draws the level above the XP bar in the same overlay; ours centers it on the bar.
        event.registerAbove(VanillaGuiOverlay.EXPERIENCE_BAR.id(), "experience_bar", new XpBarOverlay());
    }

    private static void hideVanillaOverlays(RenderGuiOverlayEvent.Pre event) {
        ResourceLocation id = event.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
                || id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())
                || id.equals(IronsSpellsCompat.MANA_OVERLAY)
                || (id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id()) && ClientConfig.ARMOR_MODE.get() != HalfBarMode.VANILLA)
                || (id.equals(VanillaGuiOverlay.FOOD_LEVEL.id()) && ClientConfig.HUNGER_MODE.get() != HalfBarMode.VANILLA)) {
            event.setCanceled(true);
        }
    }
}
