package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.compat.AppleSkinCompat;
import com.joshuafonseca.rpghealthbar.compat.Mana;
import com.joshuafonseca.rpghealthbar.config.AbsorptionDisplay;
import com.joshuafonseca.rpghealthbar.config.BarText;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HealthUnit;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Draws the bar group at a layout. Used by the HUD layer and the position editor's preview. */
public final class HudRenderer {
    private static final int ABSORPTION_COLOR = 0xFFD8A820;
    /** A full layer of absorption behind the current one, when there's more than max health of it: 75% of {@link #ABSORPTION_COLOR}. */
    private static final int ABSORPTION_BACK_COLOR = 0xFFA27E18;
    /** What's left of the fill flashes this right after a drop, fading back to its color. */
    private static final int FLASH_COLOR = 0xFFFFFFFF;
    private static final int MANA_COLOR = 0xFF2C88D0;
    /** Confluence's mana bar: a darker, purplish blue like its mana stars, so it stands apart from Iron's. */
    private static final int CONFLUENCE_MANA_COLOR = 0xFF3A50B8;
    private static final int CONFLUENCE_MANA_DARK_COLOR = 0xFF1A2456;
    private static final int ARMOR_COLOR = 0xFFB0B0B0;
    /** Saturation is outlined on the hunger bar in this color, like AppleSkin's outlines on the food icons. */
    private static final int SATURATION_COLOR = 0xFFF0D030;
    /** With food held, what eating it would add flashes on the hunger bar, as fast as AppleSkin's preview on the icons. */
    private static final long FOOD_PREVIEW_FLASH_MILLIS = 1600;
    /** The section just lost, shown briefly before it shrinks away. */
    private static final int MANA_LOST_COLOR = 0xFF143C64;
    /** The hovered spell's cost blinks between the mana color and this, once per period. */
    private static final int MANA_COST_COLOR = 0xFF143C64;
    private static final long COST_PULSE_MILLIS = 800;
    private static final BarTrail HEALTH_TRAIL = new BarTrail();
    /** Absorption grows in from the left when gained, rather than appearing at once. */
    private static final BarGrow ABSORPTION_GROW = new BarGrow();
    private static final BarTrail ABSORPTION_TRAIL = new BarTrail(500);
    /** At or below this much health (plus absorption), the bar pulses as a warning, like vanilla's shaking hearts. */
    private static final float LOW_HEALTH = 4.0F;
    private static final long LOW_HEALTH_PULSE_MILLIS = 900;
    private static final float LOW_HEALTH_PULSE_ALPHA = 0.35F;
    /** With Regeneration, a light band sweeps along the health fill once per period, like vanilla's heart wave. */
    private static final long REGEN_SWEEP_MILLIS = 1500;
    private static final int REGEN_BAND_WIDTH = 24;
    private static final float REGEN_BAND_ALPHA = 0.4F;
    private static final BarTrail MANA_TRAIL = new BarTrail();
    private static final BarTrail CONFLUENCE_MANA_TRAIL = new BarTrail();
    /** Vanilla's HUD shows at most 20 armor points and 20 food points. */
    private static final float MAX_ARMOR = 20.0F;
    private static final float MAX_FOOD = 20.0F;

    private HudRenderer() {
    }

    /**
     * @param live whether these are the player's real values; only then are losses shown as a
     *             shrinking darker section (the editor's placeholder values would otherwise
     *             look like a loss when switching back)
     * @param pieces each piece's shift from its place in the layout
     */
    public static void render(GuiGraphics graphics, Font font, HudLayout layout, HudValues values, boolean showArmor, boolean showHunger,
                              HudPosition.Pieces pieces, boolean live) {
        // Each bar keeps its own half when the other one isn't shown as a bar.
        if (showArmor) {
            inPiece(graphics, pieces, HudPiece.ARMOR, () -> renderHalfBar(graphics, layout.x(), layout, values.armor() / MAX_ARMOR, ARMOR_COLOR));
        }
        if (showHunger) {
            inPiece(graphics, pieces, HudPiece.HUNGER, () -> renderHunger(graphics, layout, values));
        }
        inPiece(graphics, pieces, HudPiece.HEALTH, () -> renderHealth(graphics, font, layout, values, live));
        // Confluence's bar goes in the row under Iron's, or the first when it's the only one.
        int confluenceRow = 0;
        if (values.ironsMana() != null) {
            confluenceRow = 1;
            inPiece(graphics, pieces, HudPiece.MANA, () -> renderMana(graphics, font, layout, layout.manaY(0), BarWidths.width(HudPiece.MANA),
                    ClientConfig.manaText(), values.ironsMana(), MANA_COLOR, MANA_LOST_COLOR, MANA_COST_COLOR, MANA_TRAIL, live));
        }
        if (values.confluenceMana() != null) {
            int row = confluenceRow;
            inPiece(graphics, pieces, HudPiece.CONFLUENCE_MANA, () -> renderMana(graphics, font, layout, layout.manaY(row),
                    BarWidths.width(HudPiece.CONFLUENCE_MANA), ClientConfig.CONFLUENCE_MANA_TEXT.get(), values.confluenceMana(), CONFLUENCE_MANA_COLOR, CONFLUENCE_MANA_DARK_COLOR, CONFLUENCE_MANA_DARK_COLOR, CONFLUENCE_MANA_TRAIL, live));
        }
    }

    /**
     * Draws with the pose shifted by the piece's own offset, and for a bar whose left end was
     * dragged in Advanced mode, by how far it was dragged.
     */
    private static void inPiece(GuiGraphics graphics, HudPosition.Pieces pieces, HudPiece piece, Runnable draw) {
        HudPosition.Offset offset = pieces.get(piece).minus(new HudPosition.Offset(BarWidths.left(piece), 0));
        if (offset.equals(HudPosition.Offset.ZERO)) {
            draw.run();
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(offset.x(), offset.y(), 0);
        draw.run();
        graphics.pose().popPose();
    }

    private static void renderHalfBar(GuiGraphics graphics, int x, HudLayout layout, float fraction, int color) {
        int y = layout.halfBarY();
        BarRenderer.drawTrack(graphics, x, y, HudLayout.HALF_BAR_WIDTH, HudLayout.HALF_BAR_HEIGHT, false);
        BarRenderer.drawFill(graphics, x, y, HudLayout.HALF_BAR_WIDTH, HudLayout.HALF_BAR_HEIGHT, false, fraction, color, false);
    }

    /** The hunger bar, with AppleSkin's saturation outline and held-food preview when it's installed. */
    private static void renderHunger(GuiGraphics graphics, HudLayout layout, HudValues values) {
        int x = layout.hungerX();
        int y = layout.halfBarY();
        int width = HudLayout.HALF_BAR_WIDTH;
        int height = HudLayout.HALF_BAR_HEIGHT;
        renderHalfBar(graphics, x, layout, values.food() / MAX_FOOD, values.hungerColor());
        AppleSkinCompat.HeldFood held = values.heldFood();
        float flash = held == null ? 0 : held.flashAlpha() * pulse(FOOD_PREVIEW_FLASH_MILLIS);
        if (held != null && held.food() > values.food()) {
            BarRenderer.drawSection(graphics, x, y, width, height, false, values.food() / MAX_FOOD, held.food() / MAX_FOOD, values.hungerColor(), false, flash);
        }
        if (values.saturation() > 0) {
            BarRenderer.drawOutline(graphics, x, y, width, height, 0, values.saturation() / MAX_FOOD, SATURATION_COLOR, true);
        }
        if (held != null && held.saturation() > values.saturation()) {
            int color = ((int) (flash * 255) << 24) | (SATURATION_COLOR & 0xFFFFFF);
            BarRenderer.drawOutline(graphics, x, y, width, height, values.saturation() / MAX_FOOD, held.saturation() / MAX_FOOD, color, values.saturation() <= 0);
        }
    }

    private static void renderHealth(GuiGraphics graphics, Font font, HudLayout layout, HudValues values, boolean live) {
        float health = values.health();
        float maxHealth = values.maxHealth();
        float absorption = values.absorption();

        int x = layout.x();
        int y = layout.healthY();
        int width = layout.healthWidth();
        BarRenderer.drawTrack(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true);
        BarRenderer.drawFill(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true, health / maxHealth, values.healthColor(), true);
        if (live) {
            float trail = HEALTH_TRAIL.update(health);
            BarRenderer.drawSection(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true, health / maxHealth, trail / maxHealth, StatusColors.lost(values.healthColor()), false, 1.0F);
            renderHealthEffects(graphics, x, y, width, values);
            flash(graphics, x, y, width, health / maxHealth, HEALTH_TRAIL.flash());
        }
        // Absorption comes in layers of max health each: the current one in front, bright, and a full
        // darker one behind it while there's more. When the front layer runs out, the one behind takes its place.
        float shownAbsorption = live ? ABSORPTION_GROW.update(absorption) : absorption;
        int layers = shownAbsorption > 0 ? (int) Math.ceil(shownAbsorption / maxHealth) : 0;
        float layerBase = Math.max(0, layers - 1) * maxHealth;
        if (layers > 1) {
            BarRenderer.drawFill(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true, 1.0F, ABSORPTION_BACK_COLOR, false);
        }
        if (shownAbsorption > 0) {
            float front = shownAbsorption - layerBase;
            BarRenderer.drawFill(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true, front / maxHealth, ABSORPTION_COLOR, front < maxHealth);
        }
        if (live) {
            // absorption lost fades like health does, within the front layer; while it grows the trail is at the value, so nothing shows
            float trail = ABSORPTION_TRAIL.update(absorption);
            if (trail > absorption) {
                float from = (absorption - layerBase) / maxHealth;
                float to = Math.min(trail - layerBase, maxHealth) / maxHealth;
                BarRenderer.drawSection(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true, from, to, StatusColors.lost(ABSORPTION_COLOR), false, 1.0F);
            }
            flash(graphics, x, y, width, (absorption - layerBase) / maxHealth, ABSORPTION_TRAIL.flash());
        }

        BarText text = ClientConfig.healthText();
        if (text != BarText.OFF) {
            BarRenderer.drawLabel(graphics, font, x, y, width, HudLayout.MAIN_BAR_HEIGHT, healthText(health, absorption, maxHealth), text);
        }
    }

    /** Right after a drop, what's left of the fill flashes white and quickly fades back to its color. */
    private static void flash(GuiGraphics graphics, int x, int y, int width, float toFraction, float alpha) {
        if (alpha > 0) {
            BarRenderer.drawSection(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true, 0, toFraction, FLASH_COLOR, false, alpha);
        }
    }

    /** The low-health pulse and the Regeneration sweep over the health fill, like the vanilla hearts' movement. */
    private static void renderHealthEffects(GuiGraphics graphics, int x, int y, int width, HudValues values) {
        int fill = BarRenderer.columns(width, true, values.health() / values.maxHealth());
        // Everything but the white tip, which stays as it is.
        int body = Math.max(0, fill - 1);
        if (values.health() + values.absorption() <= LOW_HEALTH) {
            BarRenderer.drawHighlight(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true, 0, body, LOW_HEALTH_PULSE_ALPHA * pulse(LOW_HEALTH_PULSE_MILLIS));
        }
        if (values.regenerating() && body > 0) {
            renderRegenSweep(graphics, x, y, width, body);
        }
    }

    /** A soft light band moving left to right across the first {@code body} fill columns. */
    private static void renderRegenSweep(GuiGraphics graphics, int x, int y, int width, int body) {
        float progress = (float) (Util.getMillis() % REGEN_SWEEP_MILLIS) / REGEN_SWEEP_MILLIS;
        // The band starts fully left of the fill and ends fully right of it.
        float center = -REGEN_BAND_WIDTH / 2.0F + progress * (body + REGEN_BAND_WIDTH);
        int from = Math.max(0, Mth.floor(center - REGEN_BAND_WIDTH / 2.0F));
        int to = Math.min(body, Mth.ceil(center + REGEN_BAND_WIDTH / 2.0F));
        if (to <= from) {
            return;
        }
        float[] alphas = new float[to - from];
        for (int i = 0; i < alphas.length; i++) {
            float distance = Math.abs(from + i + 0.5F - center) / (REGEN_BAND_WIDTH / 2.0F);
            alphas[i] = REGEN_BAND_ALPHA * (0.5F + 0.5F * Mth.cos(Math.min(1.0F, distance) * Mth.PI));
        }
        BarRenderer.drawHighlightColumns(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true, from, alphas);
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

    /** One mana bar, at row top {@code y}; Iron's and Confluence's differ in colors and their own settings. */
    private static void renderMana(GuiGraphics graphics, Font font, HudLayout layout, int y, int width, BarText text, Mana values,
                                   int color, int lostColor, int costColor, BarTrail manaTrail, boolean live) {
        float mana = values.current();
        float maxMana = values.max();

        int x = layout.x();
        BarRenderer.drawTrack(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true);
        if (maxMana > 0) {
            BarRenderer.drawFill(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true, mana / maxMana, color, true);
            if (live) {
                float trail = manaTrail.update(mana);
                BarRenderer.drawSection(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true, mana / maxMana, trail / maxMana, lostColor, false, 1.0F);
            }
            float cost = values.hoveredCost();
            if (cost > 0) {
                // The mana the hovered spell would use, pulsing between normal and dark blue.
                BarRenderer.drawSection(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true, (mana - cost) / maxMana, mana / maxMana, costColor, true, pulse(COST_PULSE_MILLIS));
            }
        }
        if (text != BarText.OFF) {
            BarRenderer.drawLabel(graphics, font, x, y, width, HudLayout.MAIN_BAR_HEIGHT,
                    NumberText.format(mana) + "/" + NumberText.format(maxMana), text);
        }
    }
}
