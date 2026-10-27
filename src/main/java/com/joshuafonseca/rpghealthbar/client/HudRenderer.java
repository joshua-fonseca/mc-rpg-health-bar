package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.AbsorptionDisplay;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HealthUnit;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Draws the bar group at a layout. Used by the HUD layer and the position editor's preview. */
public final class HudRenderer {
    private static final int ABSORPTION_COLOR = 0xFFD8A820;
    private static final int MANA_COLOR = 0xFF2C88D0;
    private static final int ARMOR_COLOR = 0xFFB0B0B0;
    /** The section just lost, shown briefly before it shrinks away. */
    private static final int MANA_LOST_COLOR = 0xFF143C64;
    /** The hovered spell's cost blinks between the mana color and this, once per period. */
    private static final int MANA_COST_COLOR = 0xFF143C64;
    private static final long COST_PULSE_MILLIS = 800;
    private static final BarTrail HEALTH_TRAIL = new BarTrail();
    private static final BarChanges HEALTH_CHANGES = new BarChanges();
    /** When hit, the health bar flashes white, fading out over this long (vanilla's hearts blink white). */
    private static final float DAMAGE_FLASH_MILLIS = 350.0F;
    private static final float DAMAGE_FLASH_ALPHA = 0.6F;
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
            BarRenderer.drawTrack(graphics, layout.x(), y, width, height, false);
            BarRenderer.drawFill(graphics, layout.x(), y, width, height, false, values.armor() / MAX_ARMOR, ARMOR_COLOR, false);
        }
        if (showHunger) {
            BarRenderer.drawTrack(graphics, layout.hungerX(), y, width, height, false);
            BarRenderer.drawFill(graphics, layout.hungerX(), y, width, height, false, values.food() / MAX_FOOD, values.hungerColor(), false);
        }
    }

    private static void renderHealth(GuiGraphics graphics, Font font, HudLayout layout, HudValues values, boolean live) {
        float health = values.health();
        float maxHealth = values.maxHealth();
        float absorption = values.absorption();

        int x = layout.x();
        int y = layout.healthY();
        BarRenderer.drawTrack(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, true);
        BarRenderer.drawFill(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, true, health / maxHealth, values.healthColor(), true);
        if (live) {
            float trail = HEALTH_TRAIL.update(health);
            HEALTH_CHANGES.update(health);
            BarRenderer.drawSection(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, true, health / maxHealth, trail / maxHealth, StatusColors.lost(values.healthColor()), false, 1.0F);
        }
        if (live) {
            renderHealthEffects(graphics, x, y, health / maxHealth);
        }
        if (absorption > 0) {
            BarRenderer.drawFill(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, true, absorption / maxHealth, ABSORPTION_COLOR, absorption < maxHealth);
        }

        if (ClientConfig.SHOW_HEALTH_TEXT.get()) {
            BarRenderer.drawLabel(graphics, font, x, y, HudLayout.MAIN_BAR_HEIGHT, healthText(health, absorption, maxHealth));
        }
    }

    /** Flashes and highlights over the health fill, like the vanilla hearts' blinking. */
    private static void renderHealthEffects(GuiGraphics graphics, int x, int y, float fraction) {
        int fill = BarRenderer.columns(HudLayout.WIDTH, true, fraction);
        // Everything but the white tip, which stays as it is.
        int body = Math.max(0, fill - 1);
        float sinceHit = HEALTH_CHANGES.sinceDrop();
        if (sinceHit < DAMAGE_FLASH_MILLIS) {
            float alpha = DAMAGE_FLASH_ALPHA * (1.0F - sinceHit / DAMAGE_FLASH_MILLIS);
            BarRenderer.drawHighlight(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, true, 0, body, alpha);
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
        BarRenderer.drawTrack(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, true);
        if (maxMana > 0) {
            BarRenderer.drawFill(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, true, mana / maxMana, MANA_COLOR, true);
            if (live) {
                float trail = MANA_TRAIL.update(mana);
                BarRenderer.drawSection(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, true, mana / maxMana, trail / maxMana, MANA_LOST_COLOR, false, 1.0F);
            }
            float cost = values.mana().hoveredCost();
            if (cost > 0) {
                // The mana the hovered spell would use, pulsing between normal and dark blue.
                float pulse = 0.5F - 0.5F * Mth.cos((float) (Util.getMillis() % COST_PULSE_MILLIS) / COST_PULSE_MILLIS * Mth.TWO_PI);
                BarRenderer.drawSection(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, true, (mana - cost) / maxMana, mana / maxMana, MANA_COST_COLOR, true, pulse);
            }
        }
        if (ClientConfig.SHOW_MANA_TEXT.get()) {
            BarRenderer.drawLabel(graphics, font, x, y, HudLayout.MAIN_BAR_HEIGHT,
                    NumberText.format(mana) + "/" + NumberText.format(maxMana));
        }
    }
}
