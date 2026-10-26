package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HalfBarMode;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;

/** Draws the whole bar group. */
public class HudLayer implements LayeredDraw.Layer {
    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isVisible(minecraft) || !(minecraft.getCameraEntity() instanceof Player player)) {
            return;
        }

        HudValues values = HudValues.of(player);
        Frame frame = Frame.compute(graphics.guiWidth(), graphics.guiHeight(), values.mana() != null);
        // Reserved at the default position: the vanilla rows stacked above are shifted along with
        // everything else by HudPositionMover.
        reserveVanillaSpace(minecraft.gui, graphics.guiHeight(), frame.layout());

        graphics.pose().pushPose();
        graphics.pose().translate(frame.offset().x(), frame.offset().y(), 0);
        HudRenderer.render(graphics, minecraft.font, frame.layout(), values, frame.showArmor(), frame.showHunger());
        graphics.pose().popPose();
    }

    /**
     * Modded layers don't inherit the conditions of the vanilla group they're inserted into, so
     * repeat vanilla's checks: hidden with F1, and only in survival/adventure.
     */
    public static boolean isVisible(Minecraft minecraft) {
        return !minecraft.options.hideGui && minecraft.gameMode != null && minecraft.gameMode.canHurtPlayer()
                && minecraft.getCameraEntity() instanceof Player;
    }

    /** Layout, display settings and position for the current frame. */
    public record Frame(HudLayout layout, HudPosition.Bounds bounds, HudPosition.Offset offset, boolean showArmor, boolean showHunger) {
        public static Frame compute(int screenWidth, int screenHeight, boolean showMana) {
            HalfBarMode armorMode = ClientConfig.ARMOR_MODE.get();
            HalfBarMode hungerMode = ClientConfig.HUNGER_MODE.get();
            boolean showArmor = armorMode == HalfBarMode.BAR;
            boolean showHunger = hungerMode == HalfBarMode.BAR;
            boolean vanillaIconsAbove = armorMode == HalfBarMode.VANILLA || hungerMode == HalfBarMode.VANILLA;

            HudLayout layout = HudLayout.compute(screenWidth, screenHeight, showArmor || showHunger, showMana);
            HudPosition.Bounds bounds = HudPosition.bounds(screenHeight, layout, vanillaIconsAbove);
            HudPosition.Offset offset = HudPosition.current(screenWidth, screenHeight, bounds);
            return new Frame(layout, bounds, offset, showArmor, showHunger);
        }
    }

    /**
     * Vanilla stacks the armor, food, air and mount health rows upwards using Gui.leftHeight and
     * Gui.rightHeight: the next row's top edge is drawn at {@code screenHeight - height}, and rows
     * are 9px tall with a 1px gap. Push both above our bars so anything still drawn by vanilla or
     * other mods doesn't overlap them.
     */
    private static void reserveVanillaSpace(Gui gui, int screenHeight, HudLayout layout) {
        int height = screenHeight - layout.top() + 10;
        gui.leftHeight = Math.max(gui.leftHeight, height);
        gui.rightHeight = Math.max(gui.rightHeight, height);
    }
}
