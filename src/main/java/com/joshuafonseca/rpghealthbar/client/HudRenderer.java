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
    /** At or below this much health (plus absorption), the bar pulses as a warning, like vanilla's shaking hearts. */
    private static final float LOW_HEALTH = 4.0F;
    private static final long LOW_HEALTH_PULSE_MILLIS = 900;
    private static final float LOW_HEALTH_PULSE_ALPHA = 0.35F;
    /** With Regeneration, a light band sweeps along the health fill once per period, like vanilla's heart wave. */
    private static final long REGEN_SWEEP_MILLIS = 1500;
    private static final int REGEN_BAND_WIDTH = 24;
    private static final float REGEN_BAND_ALPHA = 0.4F;
    /** When starving (no food left), the empty hunger bar pulses in the hunger color as a warning. */
    private static final long HUNGER_PULSE_MILLIS = 1200;
    private static final float HUNGER_PULSE_ALPHA = 0.6F;
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
        renderHalfBars(graphics, layout, values, showArmor, showHunger, live);
        renderHealth(graphics, font, layout, values, live);
        if (values.mana() != null) {
            renderMana(graphics, font, layout, values, live);
        }
    }

    /** Each bar keeps its own half when the other one isn't shown as a bar. */
    private static void renderHalfBars(GuiGraphics graphics, HudLayout layout, HudValues values, boolean showArmor, boolean showHunger, boolean live) {
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
            if (live && values.food() <= 0) {
                BarRenderer.drawSection(graphics, layout.hungerX(), y, width, height, false, 0.0F, 1.0F, values.hungerColor(), false,
                        HUNGER_PULSE_ALPHA * pulse(HUNGER_PULSE_MILLIS));
            }
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
            BarRenderer.drawSection(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, true, health / maxHealth, trail / maxHealth, StatusColors.lost(values.healthColor()), false, 1.0F);
            renderHealthEffects(graphics, x, y, values);
        }
        if (absorption > 0) {
            BarRenderer.drawFill(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, true, absorption / maxHealth, ABSORPTION_COLOR, absorption < maxHealth);
        }

        if (ClientConfig.SHOW_HEALTH_TEXT.get()) {
            BarRenderer.drawLabel(graphics, font, x, y, HudLayout.MAIN_BAR_HEIGHT, healthText(health, absorption, maxHealth));
        }
    }

    /** The low-health pulse and the Regeneration sweep over the health fill, like the vanilla hearts' movement. */
    private static void renderHealthEffects(GuiGraphics graphics, int x, int y, HudValues values) {
        int fill = BarRenderer.columns(HudLayout.WIDTH, true, values.health() / values.maxHealth());
        // Everything but the white tip, which stays as it is.
        int body = Math.max(0, fill - 1);
        if (values.health() + values.absorption() <= LOW_HEALTH) {
            BarRenderer.drawHighlight(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, true, 0, body, LOW_HEALTH_PULSE_ALPHA * pulse(LOW_HEALTH_PULSE_MILLIS));
        }
        if (values.regenerating() && body > 0) {
            renderRegenSweep(graphics, x, y, body);
        }
    }

    /** A soft light band moving left to right across the first {@code body} fill columns. */
    private static void renderRegenSweep(GuiGraphics graphics, int x, int y, int body) {
        float progress = (float) (Util.getMillis() % REGEN_SWEEP_MILLIS) / REGEN_SWEEP_MILLIS;
        // The band starts fully left of the fill and ends fully right of it.
        float center = -REGEN_BAND_WIDTH / 2.0F + progress * (body + REGEN_BAND_WIDTH);
        int from = Math.max(0, Mth.floor(center - REGEN_BAND_WIDTH / 2.0F));
        int to = Math.min(body, Mth.ceil(center + REGEN_BAND_WIDTH / 2.0F));
        for (int column = from; column < to; column++) {
            float distance = Math.abs(column + 0.5F - center) / (REGEN_BAND_WIDTH / 2.0F);
            float alpha = REGEN_BAND_ALPHA * (0.5F + 0.5F * Mth.cos(Math.min(1.0F, distance) * Mth.PI));
            BarRenderer.drawHighlight(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, true, column, column + 1, alpha);
        }
    }

    /** 0 to 1 and back, smoothly, once per period. */
    private static float pulse(long periodMillis) {
        return 0.5F - 0.5F * Mth.cos((float) (Util.getMillis() % periodMillis) / periodMillis * Mth.TWO_PI);
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
                BarRenderer.drawSection(graphics, x, y, HudLayout.WIDTH, HudLayout.MAIN_BAR_HEIGHT, true, (mana - cost) / maxMana, mana / maxMana, MANA_COST_COLOR, true, pulse(COST_PULSE_MILLIS));
            }
        }
        if (ClientConfig.SHOW_MANA_TEXT.get()) {
            BarRenderer.drawLabel(graphics, font, x, y, HudLayout.MAIN_BAR_HEIGHT,
                    NumberText.format(mana) + "/" + NumberText.format(maxMana));
        }
    }
}
