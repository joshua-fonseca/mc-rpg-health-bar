package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.AbsorptionDisplay;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HealthUnit;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Draws the bar group at a layout. Used by the HUD layer and the position editor's preview. */
public final class HudRenderer {
    private static final int HEALTH_COLOR = 0xFFE03030;
    private static final int ABSORPTION_COLOR = 0xFFF0C030;
    private static final int MANA_COLOR = 0xFF30A0F0;
    private static final int ARMOR_COLOR = 0xFFE0E0E0;
    private static final int HUNGER_COLOR = 0xFFE08020;
    /** Vanilla's HUD shows at most 20 armor points and 20 food points. */
    private static final float MAX_ARMOR = 20.0F;
    private static final float MAX_FOOD = 20.0F;

    private HudRenderer() {
    }

    public static void render(GuiGraphics graphics, Font font, HudLayout layout, HudValues values, boolean showArmor, boolean showHunger) {
        renderHalfBars(graphics, layout, values, showArmor, showHunger);
        renderHealth(graphics, font, layout, values);
        if (values.mana() != null) {
            renderMana(graphics, font, layout, values);
        }
    }

    /** Each bar keeps its own half when the other one isn't shown as a bar. */
    private static void renderHalfBars(GuiGraphics graphics, HudLayout layout, HudValues values, boolean showArmor, boolean showHunger) {
        int y = layout.halfBarY();
        int width = HudLayout.HALF_BAR_WIDTH;
        int height = HudLayout.HALF_BAR_HEIGHT;

        if (showArmor) {
            BarRenderer.drawFrame(graphics, layout.x(), y, width, height);
            BarRenderer.drawFill(graphics, layout.x(), y, width, height, values.armor() / MAX_ARMOR, ARMOR_COLOR);
        }
        if (showHunger) {
            BarRenderer.drawFrame(graphics, layout.hungerX(), y, width, height);
            BarRenderer.drawFill(graphics, layout.hungerX(), y, width, height, values.food() / MAX_FOOD, HUNGER_COLOR);
        }
    }

    private static void renderHealth(GuiGraphics graphics, Font font, HudLayout layout, HudValues values) {
        float health = values.health();
        float maxHealth = values.maxHealth();
        float absorption = values.absorption();

        int x = layout.x();
        int y = layout.healthY();
        BarRenderer.drawFrame(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT);
        BarRenderer.drawFill(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, health / maxHealth, HEALTH_COLOR);
        if (absorption > 0) {
            BarRenderer.drawFill(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, absorption / maxHealth, ABSORPTION_COLOR);
        }

        BarRenderer.drawLabel(graphics, font, x, y, HudLayout.MAIN_BAR_HEIGHT, healthText(health, absorption, maxHealth));
    }

    /** E.g. "16+4/20", "20/20" with absorption combined, or "8+2/10" in hearts; rounded to 0.5. */
    private static String healthText(float health, float absorption, float maxHealth) {
        HealthUnit unit = ClientConfig.HEALTH_UNIT.get();
        String max = NumberText.formatHalves(unit.fromHp(maxHealth));
        if (absorption <= 0) {
            return NumberText.formatHalves(unit.fromHp(health)) + "/" + max;
        }
        if (ClientConfig.ABSORPTION_DISPLAY.get() == AbsorptionDisplay.COMBINED) {
            return NumberText.formatHalves(unit.fromHp(health + absorption)) + "/" + max;
        }
        return NumberText.formatHalves(unit.fromHp(health)) + "+" + NumberText.formatHalves(unit.fromHp(absorption)) + "/" + max;
    }

    private static void renderMana(GuiGraphics graphics, Font font, HudLayout layout, HudValues values) {
        float mana = values.mana().current();
        float maxMana = values.mana().max();

        int x = layout.x();
        int y = layout.manaY();
        BarRenderer.drawFrame(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT);
        if (maxMana > 0) {
            BarRenderer.drawFill(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, mana / maxMana, MANA_COLOR);
        }
        BarRenderer.drawLabel(graphics, font, x, y, HudLayout.MAIN_BAR_HEIGHT,
                NumberText.format(mana) + "/" + NumberText.format(maxMana));
    }
}
