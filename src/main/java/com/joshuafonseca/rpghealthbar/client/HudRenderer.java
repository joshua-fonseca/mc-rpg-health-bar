package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.compat.AppleSkinCompat;
import com.joshuafonseca.rpghealthbar.compat.Mana;
import com.joshuafonseca.rpghealthbar.compat.ManaSource;
import com.joshuafonseca.rpghealthbar.compat.ThirstCompat;
import com.joshuafonseca.rpghealthbar.config.AbsorptionDisplay;
import com.joshuafonseca.rpghealthbar.config.BarText;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HealthUnit;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Draws the bar group at a layout. Used by the HUD layer and the position editor's preview. */
public final class HudRenderer {
    private static final int ABSORPTION_COLOR = 0xFFD8A820;
    /** A full layer of absorption behind the current one, when there's more than max health of it: 75% of {@link #ABSORPTION_COLOR}. */
    private static final int ABSORPTION_BACK_COLOR = 0xFFA27E18;
    /** What's left of the fill flashes this right after a drop, fading back to its color. */
    private static final int FLASH_COLOR = 0xFFFFFFFF;
    private static final int ARMOR_COLOR = 0xFFB0B0B0;
    /** Saturation is outlined on the hunger bar in this color, like AppleSkin's outlines on the food icons. */
    private static final int SATURATION_COLOR = 0xFFF0D030;
    /** Quenched is outlined on the thirst bar in a pale blue, as saturation is on the hunger bar. */
    private static final int QUENCHED_COLOR = 0xFFB8F0FF;
    /** With food held, what eating it would add flashes on the hunger bar, as fast as AppleSkin's preview on the icons. */
    private static final long FOOD_PREVIEW_FLASH_MILLIS = 1600;
    /** A spell's cost blinks between the mana color and its cost color, once per period. */
    private static final long COST_PULSE_MILLIS = 800;
    /** Ars Nouveau's reserved mana, a dark section at the bar's right end like on Ars's own bar. */
    private static final int RESERVED_MANA_COLOR = 0xFF101010;
    /** The cost of a spell that just failed for lack of mana, flashed like on Ars's own bar. */
    private static final int FAILED_COST_COLOR = 0xFFFF2020;
    /** Bubbles rising through a fill (Ars Nouveau), one row per step like Ars's own bar's animation. */
    private static final long BUBBLE_STEP_MILLIS = 150;
    /** One bubble per this many columns, each starting at its own time. */
    private static final int BUBBLE_SPACING = 8;
    /** Steps between one bubble's start and the next in the same place: rising takes as many steps as the fill has rows. */
    private static final int BUBBLE_CYCLE = 13;
    /** How far a bubble is mixed toward white, and how opaque it is. */
    private static final float BUBBLE_WHITE = 0.55F;
    private static final int BUBBLE_ALPHA = 0xB0;
    private static final BarTrail HEALTH_TRAIL = new BarTrail();
    /** Absorption grows in from the left when gained, rather than appearing at once. */
    private static final BarGrow ABSORPTION_GROW = new BarGrow();
    private static final BarTrail ABSORPTION_TRAIL = new BarTrail(500);
    /** Space between the health text and the absorption at the bar's right end, with Separate v2. */
    private static final int ABSORPTION_TEXT_GAP = 4;
    /** At or below this much health (plus absorption), the bar pulses as a warning, like vanilla's shaking hearts. */
    private static final float LOW_HEALTH = 4.0F;
    private static final long LOW_HEALTH_PULSE_MILLIS = 900;
    private static final float LOW_HEALTH_PULSE_ALPHA = 0.35F;
    /** With Regeneration, a light band sweeps along the health fill once per period, like vanilla's heart wave. */
    private static final long REGEN_SWEEP_MILLIS = 1500;
    private static final int REGEN_BAND_WIDTH = 24;
    private static final float REGEN_BAND_ALPHA = 0.4F;
    private static final Map<ManaSource, BarTrail> MANA_TRAILS = new EnumMap<>(ManaSource.class);
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
    public static void render(GuiGraphics graphics, Font font, HudLayer.Frame frame, HudValues values, boolean showHunger,
                              HudPosition.Pieces pieces, boolean live) {
        HudLayout layout = frame.layout();
        // Each bar keeps its own half when the other one isn't shown as a bar.
        if (frame.showArmor()) {
            inPiece(graphics, pieces, HudPiece.ARMOR, () -> renderHalfBar(graphics, layout.x(), layout, values.armor() / MAX_ARMOR, ARMOR_COLOR));
        }
        if (showHunger) {
            inPiece(graphics, pieces, HudPiece.HUNGER, () -> renderHunger(graphics, layout, values));
        }
        HudPiece thirstBar = frame.thirstBar();
        if (thirstBar != null && values.thirst() != null) {
            HudPosition.Bounds box = frame.pieceBounds().get(thirstBar);
            inPiece(graphics, pieces, thirstBar, () -> renderThirst(graphics, box, thirstBar == HudPiece.THIRST_VERTICAL, values));
        }
        inPiece(graphics, pieces, HudPiece.HEALTH, () -> renderHealth(graphics, font, layout, values, live));
        // Each mana bar in its own row from the layout, so one whose mana couldn't be read leaves
        // its row empty instead of the bars below moving up out of their pieces' places.
        List<ManaSource> manaBars = ManaSource.shownBars();
        for (int row = 0; row < manaBars.size(); row++) {
            ManaSource source = manaBars.get(row);
            Mana mana = values.mana().get(source);
            if (mana != null) {
                int y = layout.manaY(row);
                inPiece(graphics, pieces, source.piece(), () -> renderMana(graphics, font, layout, y, source, mana, live));
            }
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

    /**
     * The thirst bar in {@code box}, with quenched outlined like saturation: a thin half bar, or
     * upright in the health bar's style, filling from the bottom. The upright one has no empty part
     * and no bright tip: it's only as tall as the thirst left, bordered all around.
     */
    private static void renderThirst(GuiGraphics graphics, HudPosition.Bounds box, boolean vertical, HudValues values) {
        ThirstCompat.Thirst thirst = values.thirst();
        float fraction = thirst.thirst() / thirst.max();
        float quenched = thirst.quenched() / thirst.max();
        if (!vertical) {
            BarRenderer.drawTrack(graphics, box.left(), box.top(), box.width(), box.height(), false);
            BarRenderer.drawFill(graphics, box.left(), box.top(), box.width(), box.height(), false, fraction, values.thirstColor(), false);
            if (quenched > 0) {
                BarRenderer.drawOutline(graphics, box.left(), box.top(), box.width(), box.height(), 0, quenched, QUENCHED_COLOR, true);
            }
            return;
        }
        // Drawn lying down from the origin, then turned a quarter so its left end is at the bottom.
        int length = box.height();
        int thickness = box.width();
        graphics.pose().pushPose();
        graphics.pose().translate(box.left(), box.bottom(), 0);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(-90));
        int filled = BarRenderer.columns(length, true, fraction);
        if (filled > 0) {
            // A full bar of the filled length, so its border closes over the top.
            BarRenderer.drawFill(graphics, 0, 0, filled + 2, thickness, true, 1.0F, values.thirstColor(), false);
        }
        if (quenched > 0) {
            // On the border.
            BarRenderer.drawOutline(graphics, 0, 0, length, thickness, 0, quenched, QUENCHED_COLOR, true);
        }
        graphics.pose().popPose();
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
            String label = healthText(health, absorption, maxHealth);
            if (absorption > 0 && ClientConfig.ABSORPTION_DISPLAY.get() == AbsorptionDisplay.SEPARATE_RIGHT) {
                // The absorption at the right end, and the health text kept clear of it.
                String extra = "+" + NumberText.formatQuarters(ClientConfig.HEALTH_UNIT.get().fromHp(absorption));
                BarRenderer.drawLabel(graphics, font, x, y, width, HudLayout.MAIN_BAR_HEIGHT, extra, BarText.RIGHT);
                BarRenderer.drawLabel(graphics, font, x, y, width, HudLayout.MAIN_BAR_HEIGHT, label, text, font.width(extra) + ABSORPTION_TEXT_GAP);
            } else {
                BarRenderer.drawLabel(graphics, font, x, y, width, HudLayout.MAIN_BAR_HEIGHT, label, text);
            }
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

    /**
     * Light pixels rising from the bottom row of the fill to the top, then gone, like the bubbles on
     * Ars Nouveau's own bar. Each one has its own place and start time, kept off the white tip.
     */
    private static void renderBubbles(GuiGraphics graphics, int x, int y, int body, int color) {
        if (body <= 0) {
            return;
        }
        int rows = HudLayout.MAIN_BAR_HEIGHT - 2;
        long step = Util.getMillis() / BUBBLE_STEP_MILLIS;
        int bubble = (BUBBLE_ALPHA << 24) | (BarRenderer.mix(color, 0xFFFFFFFF, BUBBLE_WHITE) & 0xFFFFFF);
        graphics.drawManaged(() -> {
            for (int slot = 0; slot * BUBBLE_SPACING < body; slot++) {
                int hash = slot * 0x9E3779B1;
                int column = slot * BUBBLE_SPACING + Math.floorMod(hash >>> 8, BUBBLE_SPACING - 2);
                int age = (int) Math.floorMod(step + (hash >>> 16), BUBBLE_CYCLE);
                if (column >= body || age >= rows) {
                    continue;
                }
                int row = rows - 1 - age;
                // Two pixels wide near the bottom, one as it nears the top; it drifts a pixel sideways on the way.
                int left = x + 1 + column + (age >= 2 && (hash & 1) == 0 ? 1 : 0);
                int pixels = age < 2 && column + 1 < body ? 2 : 1;
                int top = y + 1 + row;
                graphics.fill(left, top, Math.min(left + pixels, x + 1 + body), top + 1, bubble);
            }
        });
    }

    /** 0 to 1 and back, smoothly, once per period. */
    private static float pulse(long periodMillis) {
        return 0.5F - 0.5F * Mth.cos((float) (Util.getMillis() % periodMillis) / periodMillis * Mth.TWO_PI);
    }

    /** E.g. "16+4/20", "20/20" with absorption combined, "16/20" with it at the right end, or "8+2/10" in hearts; rounded to 0.25. */
    private static String healthText(float health, float absorption, float maxHealth) {
        HealthUnit unit = ClientConfig.HEALTH_UNIT.get();
        String max = NumberText.formatQuarters(unit.fromHp(maxHealth));
        if (absorption <= 0 || ClientConfig.ABSORPTION_DISPLAY.get() == AbsorptionDisplay.SEPARATE_RIGHT) {
            return NumberText.formatQuarters(unit.fromHp(health)) + "/" + max;
        }
        if (ClientConfig.ABSORPTION_DISPLAY.get() == AbsorptionDisplay.COMBINED) {
            return NumberText.formatQuarters(unit.fromHp(health + absorption)) + "/" + max;
        }
        return NumberText.formatQuarters(unit.fromHp(health)) + "+" + NumberText.formatQuarters(unit.fromHp(absorption)) + "/" + max;
    }

    /** One mana bar, at row top {@code y}; Iron's and Confluence's differ in colors and their own settings. */
    /** One mana bar, at row top {@code y}, in its mod's colors and with its own settings. */
    private static void renderMana(GuiGraphics graphics, Font font, HudLayout layout, int y, ManaSource source, Mana values, boolean live) {
        int width = BarWidths.width(source.piece());
        BarText text = source.text();
        int color = source.color();
        float mana = values.current();
        float maxMana = values.max();

        int x = layout.x();
        BarRenderer.drawTrack(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true);
        if (maxMana > 0) {
            // With reserved mana (Ars Nouveau), the bar also holds it at its right end, so the rest is scaled to fit.
            float reserved = values.reserved();
            float scale = maxMana * (1 + reserved);
            BarRenderer.drawFill(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true, mana / scale, color, true);
            if (source.bubbles()) {
                renderBubbles(graphics, x, y, BarRenderer.columns(width, true, mana / scale) - 1, color);
            }
            if (live) {
                float trail = MANA_TRAILS.computeIfAbsent(source, s -> new BarTrail()).update(mana);
                BarRenderer.drawSection(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true, mana / scale, trail / scale, source.lostColor(), false, 1.0F);
            }
            if (reserved > 0) {
                // As on Ars's bar: the reserved share of max mana, dark, over the bar's right end.
                BarRenderer.drawSection(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true, Math.max(0, 1 - reserved), 1, RESERVED_MANA_COLOR, false, 1.0F);
            }
            float cost = values.hoveredCost();
            if (cost > 0) {
                // The mana the hovered spell would use, pulsing between normal and dark blue.
                BarRenderer.drawSection(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true, (mana - cost) / scale, mana / scale, source.costColor(), true, pulse(COST_PULSE_MILLIS));
            }
            if (values.failedCost() > 0 && values.failedAlpha() > 0) {
                // A spell just failed for lack of mana (Ars Nouveau): its cost flashes red from the left end, fading out, as on Ars's bar.
                BarRenderer.drawSection(graphics, x, y, width, HudLayout.MAIN_BAR_HEIGHT, true, 0, Math.min(1, values.failedCost() / maxMana), FAILED_COST_COLOR, false, values.failedAlpha());
            }
        }
        if (text != BarText.OFF) {
            BarRenderer.drawLabel(graphics, font, x, y, width, HudLayout.MAIN_BAR_HEIGHT,
                    NumberText.format(mana) + "/" + NumberText.format(maxMana), text);
        }
    }
}
