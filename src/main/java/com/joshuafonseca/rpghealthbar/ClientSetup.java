package com.joshuafonseca.rpghealthbar;

import com.joshuafonseca.rpghealthbar.client.BarWidths;
import com.joshuafonseca.rpghealthbar.client.HudLayer;
import com.joshuafonseca.rpghealthbar.client.HudPosition;
import com.joshuafonseca.rpghealthbar.client.HudPositionMover;
import com.joshuafonseca.rpghealthbar.client.XpBarOverlay;
import com.joshuafonseca.rpghealthbar.client.XpBarRenderer;
import com.joshuafonseca.rpghealthbar.client.screen.RpgConfigScreen;
import com.joshuafonseca.rpghealthbar.compat.ConfluenceCompat;
import com.joshuafonseca.rpghealthbar.compat.IronsSpellsCompat;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HalfBarMode;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
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
        // The HUD's layout and position are worked out once per frame, then shared by our overlays and
        // the vanilla ones that move with them.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, (RenderGuiEvent.Pre event) -> {
            HudLayer.Frame.clearCurrent();
            HudPositionMover.newFrame();
        });
        // Highest priority: hide replaced overlays before other mods' Pre listeners run, so a mod that
        // pushes the pose in Pre (and pops in Post) never pushes for an overlay that won't render.
        // Forge doesn't send Post for a cancelled overlay, and AppleSkin draws its hunger and health
        // previews in the Pre and Post of the overlays it goes with, so they're hidden along with them.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ClientSetup::hideVanillaOverlays);
        // Right after: shift the overlays that move with the set, before other mods (like AppleSkin)
        // draw on them in Pre, and undo it in Post once they've drawn.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, HudPositionMover::onPre);
        // The item name and action bar are placed last, once other mods have moved them or reserved
        // rows above the hotbar for them.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, HudPositionMover::onTextPre);
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
        // Confluence's own displays replace these vanilla ones, and Confluence hides them later, at
        // normal priority. Hidden here first, even with the mod off (it doesn't change what shows),
        // so mods that shift them in Pre and back in Post, like Double Hotbar, never shift them
        // without shifting back: the leftover shift would move everything drawn after, menus included.
        if ((id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id()) && ConfluenceCompat.showsOwnHealth())
                || (id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id()) && ConfluenceCompat.showsOwnArmor())
                || (id.equals(VanillaGuiOverlay.FOOD_LEVEL.id()) && ConfluenceCompat.showsOwnFood())) {
            event.setCanceled(true);
            return;
        }
        if (!ClientConfig.ENABLED.get()) {
            return;
        }
        // Armor and hunger on Default leave whatever the game shows: the vanilla icons, or a mod's
        // display that replaces them, like Confluence's. Otherwise ours replaces both.
        boolean armorReplaced = ClientConfig.ARMOR_MODE.get() != HalfBarMode.VANILLA;
        boolean hungerReplaced = ClientConfig.HUNGER_MODE.get() != HalfBarMode.VANILLA;
        if (id.equals(VanillaGuiOverlay.JUMP_BAR.id()) && BarWidths.customXp()) {
            // Not vanilla's length or spot: drawn here instead, at its place in the set.
            drawJumpBar(event.getGuiGraphics());
            event.setCanceled(true);
            return;
        }
        if (id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
                // Ours draws it; see XpBarOverlay.
                || id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())
                || id.equals(ConfluenceCompat.HEALTH_LAYER)
                || (id.equals(ConfluenceCompat.ARMOR_LAYER) && armorReplaced)
                || (id.equals(ConfluenceCompat.FOOD_LAYER) && hungerReplaced)
                || id.equals(IronsSpellsCompat.MANA_OVERLAY)
                // Our bar shows its mana; in soul mode its display shows soul instead, so it's kept.
                || (id.equals(ConfluenceCompat.MANA_LAYER) && ConfluenceCompat.isAvailable()
                        && !ConfluenceCompat.isSoulMode(Minecraft.getInstance().player))
                || (id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id()) && armorReplaced)
                || (id.equals(VanillaGuiOverlay.FOOD_LEVEL.id()) && hungerReplaced)) {
            event.setCanceled(true);
        }
    }

    /** The jump bar at the XP bar's length and place, as vanilla shows it (not with F1). */
    private static void drawJumpBar(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!XpBarRenderer.jumpBarShown(minecraft) || minecraft.options.hideGui) {
            return;
        }
        HudLayer.Frame frame = HudLayer.Frame.current(graphics.guiWidth(), graphics.guiHeight());
        HudPosition.Bounds xp = frame.pieceBounds().get(HudPiece.XP);
        graphics.pose().pushPose();
        HudPosition.moveTo(graphics.pose(), frame.pieceOffset(HudPiece.XP));
        XpBarRenderer.renderJump(graphics, xp.left(), xp.top(), BarWidths.xpCells(), minecraft.player.jumpableVehicle());
        graphics.pose().popPose();
    }
}
