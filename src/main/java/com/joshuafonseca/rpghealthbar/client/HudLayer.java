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
        placeVanillaRows(minecraft.gui, graphics.guiHeight(), frame);

        graphics.pose().pushPose();
        HudPosition.moveTo(graphics.pose(), frame.offset());
        HudRenderer.render(graphics, minecraft.font, frame.layout(), values, frame.showArmor(), frame.showHunger(), true);
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
    public record Frame(HudLayout layout, HudPosition.Bounds bounds, HudPosition.Offset offset,
                        boolean showArmor, boolean showHunger, boolean vanillaArmor, boolean vanillaHunger) {
        public static Frame compute(int screenWidth, int screenHeight, boolean showMana) {
            HalfBarMode armorMode = ClientConfig.ARMOR_MODE.get();
            HalfBarMode hungerMode = ClientConfig.HUNGER_MODE.get();
            boolean showArmor = armorMode == HalfBarMode.BAR;
            boolean showHunger = hungerMode == HalfBarMode.BAR;
            boolean vanillaArmor = armorMode == HalfBarMode.VANILLA;
            boolean vanillaHunger = hungerMode == HalfBarMode.VANILLA;

            HudLayout layout = HudLayout.compute(screenWidth, screenHeight, showArmor || showHunger, vanillaArmor || vanillaHunger, showMana);
            HudPosition.Bounds bounds = HudPosition.bounds(screenHeight, layout);
            HudPosition.Offset offset = HudPosition.current(screenWidth, screenHeight, bounds);
            return new Frame(layout, bounds, offset, showArmor, showHunger, vanillaArmor, vanillaHunger);
        }
    }

    /**
     * Vanilla stacks the armor (left), food, air and mount health (right) rows upwards using
     * Gui.leftHeight and Gui.rightHeight: the next row's top edge is drawn at
     * {@code screenHeight - height}, and each row adds 10 (9px icons plus a 1px gap).
     * <p>
     * Vanilla armor/hunger icons go into our armor/hunger row, next to a half bar if there is one.
     * Everything else (air, mount health, other mods' rows) starts above our bars.
     * These positions are at the default spot; HudPositionMover shifts the vanilla rows with the set.
     */
    private static void placeVanillaRows(Gui gui, int screenHeight, Frame frame) {
        int inRow = screenHeight - frame.layout().iconRowY();
        int aboveBars = screenHeight - frame.layout().top() + 10;
        gui.leftHeight = frame.vanillaArmor() ? inRow : aboveBars;
        gui.rightHeight = frame.vanillaHunger() ? inRow : aboveBars;
    }
}
