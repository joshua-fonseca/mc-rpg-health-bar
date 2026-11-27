package com.joshuafonseca.rpghealthbar;

import com.joshuafonseca.rpghealthbar.client.BarWidths;
import com.joshuafonseca.rpghealthbar.client.HudLayer;
import com.joshuafonseca.rpghealthbar.client.HudPosition;
import com.joshuafonseca.rpghealthbar.client.XpBarRenderer;
import com.joshuafonseca.rpghealthbar.client.HudPositionMover;
import com.joshuafonseca.rpghealthbar.client.screen.RpgConfigScreen;
import com.joshuafonseca.rpghealthbar.compat.AppleSkinCompat;
import com.joshuafonseca.rpghealthbar.compat.ConfluenceCompat;
import com.joshuafonseca.rpghealthbar.compat.ManaSource;
import com.joshuafonseca.rpghealthbar.compat.ThirstCompat;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HalfBarMode;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import com.joshuafonseca.rpghealthbar.config.ThirstMode;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, (RenderGuiEvent.Pre event) -> {
            HudLayer.Frame.clearCurrent();
            HudPositionMover.newFrame();
        });
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
        // Confluence's own displays replace these vanilla ones, and Confluence hides them later, at
        // normal priority. Hidden here first, even with the mod off (it doesn't change what shows),
        // so mods that shift them in Pre and back in Post, like Double Hotbar, never shift them
        // without shifting back: the leftover shift would move everything drawn after, menus included.
        if ((name.equals(VanillaGuiLayers.PLAYER_HEALTH) && ConfluenceCompat.showsOwnHealth())
                || (name.equals(VanillaGuiLayers.ARMOR_LEVEL) && ConfluenceCompat.showsOwnArmor())
                || (name.equals(VanillaGuiLayers.FOOD_LEVEL) && ConfluenceCompat.showsOwnFood())) {
            event.setCanceled(true);
            return;
        }
        if (!ClientConfig.ENABLED.get()) {
            return;
        }
        // Thirst Was Taken 2's droplets show only on Default; the thirst row still counts for the
        // rows stacked above it.
        if (name.equals(ThirstCompat.LAYER) && ThirstCompat.isAvailable() && ClientConfig.THIRST_MODE.get() != ThirstMode.VANILLA) {
            event.setCanceled(true);
            HudPositionMover.keepThirstRow(event.getGuiGraphics(), Minecraft.getInstance().gui.rightHeight);
            return;
        }
        // Armor and hunger on Default leave whatever the game shows: the vanilla icons, or a mod's
        // display that replaces them, like Confluence's. Otherwise ours replaces both.
        boolean armorReplaced = ClientConfig.ARMOR_MODE.get() != HalfBarMode.VANILLA;
        boolean hungerReplaced = ClientConfig.HUNGER_MODE.get() != HalfBarMode.VANILLA;
        if ((name.equals(VanillaGuiLayers.EXPERIENCE_BAR) || name.equals(VanillaGuiLayers.EXPERIENCE_LEVEL)) && !ClientConfig.XP_BAR_SHOWN.get()) {
            // XP Bar set to Hidden; the jump bar still shows while riding.
            event.setCanceled(true);
            return;
        }
        if ((name.equals(VanillaGuiLayers.EXPERIENCE_BAR) || name.equals(VanillaGuiLayers.JUMP_METER)) && BarWidths.customXp()) {
            // Not vanilla's length: drawn here instead, at its place in the set.
            drawXpBar(event.getGuiGraphics(), name.equals(VanillaGuiLayers.JUMP_METER));
            event.setCanceled(true);
            return;
        }
        if (name.equals(VanillaGuiLayers.PLAYER_HEALTH)
                || name.equals(ConfluenceCompat.HEALTH_LAYER)
                || (name.equals(ConfluenceCompat.ARMOR_LAYER) && armorReplaced)
                || (name.equals(ConfluenceCompat.FOOD_LAYER) && hungerReplaced)
                // The mana mods' own displays, which our bars replace.
                || ManaSource.hidesAnyOwnDisplay(name)
                // Its heart previews would draw over our bars.
                || name.equals(AppleSkinCompat.HEALTH_RESTORED_LAYER)
                // Its hunger overlays only fit the vanilla icons.
                || (AppleSkinCompat.isHungerOverlay(name) && hungerReplaced)
                || (name.equals(VanillaGuiLayers.ARMOR_LEVEL) && armorReplaced)
                || (name.equals(VanillaGuiLayers.FOOD_LEVEL) && hungerReplaced)) {
            event.setCanceled(true);
        }
    }

    /** The XP bar, or the jump bar while riding, with the cells set in the HUD Editor; shown when vanilla's would be. */
    private static void drawXpBar(GuiGraphics graphics, boolean jump) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(jump ? XpBarRenderer.jumpBarShown(minecraft) : XpBarRenderer.xpBarShown(minecraft))) {
            return;
        }
        HudLayer.Frame frame = HudLayer.Frame.current(graphics.guiWidth(), graphics.guiHeight());
        HudPosition.Bounds xp = frame.pieceBounds().get(HudPiece.XP);
        graphics.pose().pushPose();
        HudPosition.moveTo(graphics.pose(), frame.pieceOffset(HudPiece.XP));
        if (jump) {
            XpBarRenderer.renderJump(graphics, xp.left(), xp.top(), BarWidths.xpCells(), minecraft.player.jumpableVehicle());
        } else {
            XpBarRenderer.renderXp(graphics, xp.left(), xp.top(), BarWidths.xpCells());
        }
        graphics.pose().popPose();
    }
}
