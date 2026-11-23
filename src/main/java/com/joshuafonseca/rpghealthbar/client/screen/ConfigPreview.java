package com.joshuafonseca.rpghealthbar.client.screen;

import com.joshuafonseca.rpghealthbar.client.BarWidths;
import com.joshuafonseca.rpghealthbar.client.HudLayer;
import com.joshuafonseca.rpghealthbar.client.HudLayout;
import com.joshuafonseca.rpghealthbar.client.HudPosition;
import com.joshuafonseca.rpghealthbar.client.HudPositionMover;
import com.joshuafonseca.rpghealthbar.client.HudRenderer;
import com.joshuafonseca.rpghealthbar.client.HudValues;
import com.joshuafonseca.rpghealthbar.client.StatusColors;
import com.joshuafonseca.rpghealthbar.client.XpBarRenderer;
import com.joshuafonseca.rpghealthbar.compat.ManaBars;
import com.joshuafonseca.rpghealthbar.config.AirPosition;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The HUD as the config screen's settings would draw it, at its default spot, with sample values:
 * the bars or vanilla icons, a row of air bubbles where they'd go, and the XP bar with its level.
 * Drawn every frame from the current settings, so it follows each click.
 */
final class ConfigPreview {
    /** The virtual screen's width: the default HUD fills it. */
    private static final int WIDTH = HudLayout.WIDTH;

    private static final ResourceLocation ARMOR_FULL = ResourceLocation.withDefaultNamespace("hud/armor_full");
    private static final ResourceLocation ARMOR_HALF = ResourceLocation.withDefaultNamespace("hud/armor_half");
    private static final ResourceLocation ARMOR_EMPTY = ResourceLocation.withDefaultNamespace("hud/armor_empty");
    private static final ResourceLocation FOOD_FULL = ResourceLocation.withDefaultNamespace("hud/food_full");
    private static final ResourceLocation FOOD_HALF = ResourceLocation.withDefaultNamespace("hud/food_half");
    private static final ResourceLocation FOOD_EMPTY = ResourceLocation.withDefaultNamespace("hud/food_empty");
    private static final ResourceLocation AIR = ResourceLocation.withDefaultNamespace("hud/air");
    private static final ResourceLocation AIR_BURSTING = ResourceLocation.withDefaultNamespace("hud/air_bursting");

    /** A virtual screen the HUD is laid out on; only its height matters, as the HUD sits at the bottom. */
    private static final int VIRTUAL_HEIGHT = 200;
    private static final int ICON = HudLayout.ICON_ROW_HEIGHT;
    private static final int ICON_SPACING = 8;
    private static final int ROW_HEIGHT = HudPositionMover.ROW_HEIGHT;
    private static final int LEVEL_COLOR = 0x80FF20;

    /** Health a bit down with some absorption, and mana partly used, so the text and fills show. */
    private static final float HEALTH = 15;
    private static final float ABSORPTION = 4;
    private static final float ARMOR = 12;
    private static final float FOOD = 17;
    private static final int AIR_BUBBLES = 7;
    private static final float XP_PROGRESS_FRACTION = 0.6F;
    private static final String LEVEL = "12";

    private ConfigPreview() {
    }

    /** The preview's height at full size, for the current settings. */
    static int height() {
        Layout layout = layout();
        return layout.bottom() - layout.top();
    }

    /** The preview's width at full size: the HUD's, or wider with bars made longer in Advanced mode. */
    static int width() {
        Layout layout = layout();
        return layout.right() - layout.left();
    }

    /** Draws the preview centered on {@code (centerX, centerY)}, scaled by {@code scale}; nothing while the mod is off. */
    static void draw(GuiGraphics graphics, Font font, int centerX, int centerY, float scale) {
        if (!ClientConfig.ENABLED.get()) {
            return;
        }
        Layout preview = layout();
        HudLayer.Frame frame = preview.frame();
        HudLayout layout = frame.layout();
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.pose().translate(-(preview.left() + preview.right()) / 2.0F, -(preview.top() + preview.bottom()) / 2.0F, 0);

        HudValues values = new HudValues(HEALTH, 20, ABSORPTION, ARMOR, FOOD, 0, null, ManaBars.placeholderIrons(60), ManaBars.placeholderConfluence(60),
                StatusColors.HEALTH, StatusColors.HUNGER, false);
        HudRenderer.render(graphics, font, layout, values, frame.showArmor(), frame.showHunger(), HudPosition.Pieces.NONE, false);

        HudPosition.Bounds armor = frame.pieceBounds().get(HudPiece.ARMOR);
        if (frame.vanillaArmor() && armor != null) {
            for (int i = 0; i < 10; i++) {
                int points = i * 2 + 1;
                ResourceLocation sprite = points < ARMOR ? ARMOR_FULL : points == ARMOR ? ARMOR_HALF : ARMOR_EMPTY;
                graphics.blitSprite(sprite, armor.left() + i * ICON_SPACING, armor.top(), ICON, ICON);
            }
        }
        HudPosition.Bounds hunger = frame.pieceBounds().get(HudPiece.HUNGER);
        if (frame.vanillaHunger() && hunger != null) {
            for (int i = 0; i < 10; i++) {
                int points = i * 2 + 1;
                ResourceLocation sprite = points < FOOD ? FOOD_FULL : points == FOOD ? FOOD_HALF : FOOD_EMPTY;
                graphics.blitSprite(sprite, hunger.right() - i * ICON_SPACING - ICON, hunger.top(), ICON, ICON);
            }
        }
        // Right-aligned like the hunger icons, the last one bursting.
        for (int i = 0; i < AIR_BUBBLES; i++) {
            graphics.blitSprite(i == AIR_BUBBLES - 1 ? AIR_BURSTING : AIR, WIDTH - i * ICON_SPACING - ICON, preview.airTop(), ICON, ICON);
        }

        if (!ClientConfig.XP_BAR_SHOWN.get()) {
            graphics.pose().popPose();
            return;
        }
        HudPosition.Bounds xp = frame.pieceBounds().get(HudPiece.XP);
        XpBarRenderer.drawXp(graphics, xp.left(), xp.top(), BarWidths.xpCells(), XP_PROGRESS_FRACTION);
        // Outlined like vanilla's, and centered on the bar like the mod draws it.
        int levelX = HudPositionMover.levelX(xp, font.width(LEVEL));
        int levelY = xp.top() - 1;
        graphics.drawString(font, LEVEL, levelX + 1, levelY, 0, false);
        graphics.drawString(font, LEVEL, levelX - 1, levelY, 0, false);
        graphics.drawString(font, LEVEL, levelX, levelY + 1, 0, false);
        graphics.drawString(font, LEVEL, levelX, levelY - 1, 0, false);
        graphics.drawString(font, LEVEL, levelX, levelY, LEVEL_COLOR, false);
        graphics.pose().popPose();
    }

    /**
     * The HUD laid out on the virtual screen, and the air bubbles' row: stacked above everything
     * (Above, or with no hunger to go under), or right under hunger (Below).
     */
    private record Layout(HudLayer.Frame frame, int airTop, int left, int top, int right, int bottom) {
    }

    private static Layout layout() {
        HudLayer.Frame frame = HudLayer.Frame.compute(WIDTH, VIRTUAL_HEIGHT);
        HudPosition.Bounds hunger = frame.pieceBounds().get(HudPiece.HUNGER);
        int airTop = ClientConfig.AIR_POSITION.get() == AirPosition.BELOW && hunger != null
                ? hunger.bottom() + 1
                : frame.layout().top() - ROW_HEIGHT;
        // The pieces at their default spots, ignoring where they've been moved.
        int left = 0;
        int top = airTop;
        int right = WIDTH;
        int bottom = 0;
        for (HudPosition.Bounds piece : frame.pieceBounds().values()) {
            left = Math.min(left, piece.left());
            top = Math.min(top, piece.top());
            right = Math.max(right, piece.right());
            bottom = Math.max(bottom, piece.bottom());
        }
        return new Layout(frame, airTop, left, top, right, bottom);
    }
}
