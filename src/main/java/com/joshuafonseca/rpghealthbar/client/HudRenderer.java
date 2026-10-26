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
    /** The section just lost, shown briefly before it shrinks away. */
    private static final int HEALTH_LOST_COLOR = 0xFF801818;
    private static final int MANA_LOST_COLOR = 0xFF184A78;
    private static final BarTrail HEALTH_TRAIL = new BarTrail();
    private static final BarTrail MANA_TRAIL = new BarTrail();
    /** Vanilla's HUD shows at most 20 armor points and 20 food points. */
    private static final float MAX_ARMOR = 20.0F;
    private static final float MAX_FOOD = 20.0F;

    private HudRenderer() {
    }

    /**
     * @param live whether these are the player's real values; only then are losses shown as a
     *             shrinking darker section (the editor's placeholder values would otherwise
     *             look like a loss when switching back)
     */
    public static void render(GuiGraphics graphics, Font font, HudLayout layout, HudValues values, boolean showArmor, boolean showHunger, boolean live) {
        renderHalfBars(graphics, layout, values, showArmor, showHunger);
        renderHealth(graphics, font, layout, values, live);
        if (values.mana() != null) {
            renderMana(graphics, font, layout, values, live);
        }
    }

    /** Each bar keeps its own half when the other one isn't shown as a bar. */
    private static void renderHalfBars(GuiGraphics graphics, HudLayout layout, HudValues values, boolean showArmor, boolean showHunger) {
        int y = layout.halfBarY();
        int width = HudLayout.HALF_BAR_WIDTH;
        int height = HudLayout.HALF_BAR_HEIGHT;

        if (showArmor) {
            BarRenderer.drawTrack(graphics, layout.x(), y, width, height);
            BarRenderer.drawFill(graphics, layout.x(), y, width, height, 0, values.armor() / MAX_ARMOR, ARMOR_COLOR);
        }
        if (showHunger) {
            BarRenderer.drawTrack(graphics, layout.hungerX(), y, width, height);
            BarRenderer.drawFill(graphics, layout.hungerX(), y, width, height, 0, values.food() / MAX_FOOD, HUNGER_COLOR);
        }
    }

    private static void renderHealth(GuiGraphics graphics, Font font, HudLayout layout, HudValues values, boolean live) {
        float health = values.health();
        float maxHealth = values.maxHealth();
        float absorption = values.absorption();

        int x = layout.x();
        int y = layout.healthY();
        BarRenderer.drawFrame(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT);
        if (live) {
            float trail = HEALTH_TRAIL.update(health);
            BarRenderer.drawFill(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, 1, trail / maxHealth, HEALTH_LOST_COLOR);
        }
        BarRenderer.drawFill(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, 1, health / maxHealth, HEALTH_COLOR);
        BarRenderer.drawGlow(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, 1, health / maxHealth, HEALTH_COLOR);
        if (absorption > 0) {
            BarRenderer.drawFill(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, 1, absorption / maxHealth, ABSORPTION_COLOR);
        }

        if (ClientConfig.SHOW_HEALTH_TEXT.get()) {
            BarRenderer.drawLabel(graphics, font, x, y, HudLayout.MAIN_BAR_HEIGHT, healthText(health, absorption, maxHealth));
        }
    }

    /** E.g. "16+4/20", "20/20" with absorption combined, or "8+2/10" in hearts; rounded to 0.25. */
    private static String healthText(float health, float absorption, float maxHealth) {
        HealthUnit unit = ClientConfig.HEALTH_UNIT.get();
        String max = NumberText.formatQuarters(unit.fromHp(maxHealth));
        if (absorption <= 0) {
            return NumberText.formatQuarters(unit.fromHp(health)) + "/" + max;
        }
        if (ClientConfig.ABSORPTION_DISPLAY.get() == AbsorptionDisplay.COMBINED) {
            return NumberText.formatQuarters(unit.fromHp(health + absorption)) + "/" + max;
        }
        return NumberText.formatQuarters(unit.fromHp(health)) + "+" + NumberText.formatQuarters(unit.fromHp(absorption)) + "/" + max;
    }

    private static void renderMana(GuiGraphics graphics, Font font, HudLayout layout, HudValues values, boolean live) {
        float mana = values.mana().current();
        float maxMana = values.mana().max();

        int x = layout.x();
        int y = layout.manaY();
        BarRenderer.drawFrame(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT);
        if (maxMana > 0) {
            if (live) {
                float trail = MANA_TRAIL.update(mana);
                BarRenderer.drawFill(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, 1, trail / maxMana, MANA_LOST_COLOR);
            }
            BarRenderer.drawFill(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, 1, mana / maxMana, MANA_COLOR);
            BarRenderer.drawGlow(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, 1, mana / maxMana, MANA_COLOR);
        }
        if (ClientConfig.SHOW_MANA_TEXT.get()) {
            BarRenderer.drawLabel(graphics, font, x, y, HudLayout.MAIN_BAR_HEIGHT,
                    NumberText.format(mana) + "/" + NumberText.format(maxMana));
        }
    }
}
