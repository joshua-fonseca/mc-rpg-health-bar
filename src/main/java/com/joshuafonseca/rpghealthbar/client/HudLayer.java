package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;

/** Draws the whole bar group. */
public class HudLayer implements LayeredDraw.Layer {
    private static final int HEALTH_COLOR = 0xFFE03030;
    private static final int ABSORPTION_COLOR = 0xFFF0C030;
    private static final int ARMOR_COLOR = 0xFFE0E0E0;
    private static final int HUNGER_COLOR = 0xFFE08020;
    /** Vanilla's HUD shows at most 20 armor points and 20 food points. */
    private static final float MAX_ARMOR = 20.0F;
    private static final float MAX_FOOD = 20.0F;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        // Modded layers don't inherit the conditions of the vanilla group they're inserted into,
        // so repeat vanilla's checks: hidden with F1, and only in survival/adventure.
        if (minecraft.options.hideGui || minecraft.gameMode == null || !minecraft.gameMode.canHurtPlayer()) {
            return;
        }
        if (!(minecraft.getCameraEntity() instanceof Player player)) {
            return;
        }

        boolean showArmor = ClientConfig.SHOW_ARMOR_BAR.get();
        boolean showHunger = ClientConfig.SHOW_HUNGER_BAR.get();
        HudLayout layout = HudLayout.compute(graphics.guiWidth(), graphics.guiHeight(), showArmor || showHunger);
        renderHalfBars(graphics, player, layout, showArmor, showHunger);
        renderHealth(graphics, minecraft, player, layout);
        reserveVanillaSpace(minecraft.gui, graphics.guiHeight(), layout);
    }

    /** Each bar keeps its own half when the other one is turned off. */
    private static void renderHalfBars(GuiGraphics graphics, Player player, HudLayout layout, boolean showArmor, boolean showHunger) {
        int y = layout.halfBarY();
        int width = HudLayout.HALF_BAR_WIDTH;
        int height = HudLayout.HALF_BAR_HEIGHT;

        if (showArmor) {
            BarRenderer.drawFrame(graphics, layout.x(), y, width, height);
            BarRenderer.drawFill(graphics, layout.x(), y, width, height, player.getArmorValue() / MAX_ARMOR, ARMOR_COLOR);
        }
        if (showHunger) {
            BarRenderer.drawFrame(graphics, layout.hungerX(), y, width, height);
            BarRenderer.drawFill(graphics, layout.hungerX(), y, width, height, player.getFoodData().getFoodLevel() / MAX_FOOD, HUNGER_COLOR);
        }
    }

    private static void renderHealth(GuiGraphics graphics, Minecraft minecraft, Player player, HudLayout layout) {
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float absorption = player.getAbsorptionAmount();

        int x = layout.x();
        int y = layout.healthY();
        BarRenderer.drawFrame(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT);
        BarRenderer.drawFill(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, health / maxHealth, HEALTH_COLOR);
        if (absorption > 0) {
            BarRenderer.drawFill(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, absorption / maxHealth, ABSORPTION_COLOR);
        }

        String text = absorption > 0
                ? NumberText.format(health) + "+" + NumberText.format(absorption) + "/" + NumberText.format(maxHealth)
                : NumberText.format(health) + "/" + NumberText.format(maxHealth);
        BarRenderer.drawLabel(graphics, minecraft.font, x, y, HudLayout.MAIN_BAR_HEIGHT, text);
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
