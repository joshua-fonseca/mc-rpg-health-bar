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
import com.joshuafonseca.rpghealthbar.compat.ManaSource;
import com.joshuafonseca.rpghealthbar.compat.ThirstCompat;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import com.joshuafonseca.rpghealthbar.config.ThirstMode;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The HUD as the config screen's settings would draw it, at its default spot, with sample values:
 * the bars or vanilla icons, a row of air bubbles where they'd go, the XP bar with its level, and
 * a stand-in for the hotbar.
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
    private static final ResourceLocation HOTBAR = ResourceLocation.withDefaultNamespace("hud/hotbar");
    private static final ResourceLocation HOTBAR_SELECTION = ResourceLocation.withDefaultNamespace("hud/hotbar_selection");
    /** The selected slot's frame, a pixel larger than the hotbar above and to the left of the first slot. */
    private static final int SELECTION_WIDTH = 24;
    private static final int SELECTION_HEIGHT = 23;
    private static final int HOTBAR_LEFT = WIDTH / 2 - HudLayout.WIDTH / 2;

    /**
     * A virtual screen the HUD is laid out on; only its height matters, as the HUD sits at the bottom.
     * Tall enough that no upright bar is cut to fit it.
     */
    private static final int VIRTUAL_HEIGHT = 10000;
    private static final int ICON = HudLayout.ICON_ROW_HEIGHT;
    private static final int ICON_SPACING = 8;
    private static final int LEVEL_COLOR = 0x80FF20;

    /** Health a bit down with some absorption, and mana partly used, so the text and fills show. */
    private static final float HEALTH = 15;
    private static final float ABSORPTION = 4;
    private static final float ARMOR = 12;
    private static final float FOOD = 17;
    private static final int THIRST = 15;
    private static final int THIRST_ICONS_WIDTH = 41;
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

    /**
     * The width the config screen keeps beside its options: the preview's with every upright bar
     * showing. It doesn't follow the settings, so changing them never moves the options.
     */
    static int reservedWidth() {
        int last = ThirstCompat.isAvailable() ? 1 : 0;
        return HudLayer.Frame.uprightLeft(WIDTH, last) + HudLayout.MAIN_BAR_HEIGHT - (HOTBAR_LEFT - 1);
    }

    /** The preview's width with nothing beside or past the hotbar, which decides whether there's room for it. */
    static int leastWidth() {
        return WIDTH - (HOTBAR_LEFT - 1);
    }

    /** Draws the preview centered on {@code (centerX, centerY)}, scaled by {@code scale}; nothing while the mod is off. */
    static void draw(GuiGraphics graphics, Font font, int centerX, int centerY, float scale) {
        if (!ClientConfig.ENABLED.get()) {
            return;
        }
        Layout preview = layout();
        HudLayer.Frame frame = preview.frame();
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0);
        graphics.pose().scale(scale, scale, 1);
        // Whole pixels: on a half pixel, the vanilla icons pick up the texture rows beside them.
        graphics.pose().translate(-((preview.left() + preview.right()) / 2), -((preview.top() + preview.bottom()) / 2), 0);

        // The empty hotbar with its first slot selected, so the HUD is seen where it sits around it.
        graphics.blitSprite(HOTBAR, HOTBAR_LEFT, VIRTUAL_HEIGHT - HudLayout.HOTBAR_HEIGHT, HudLayout.WIDTH, HudLayout.HOTBAR_HEIGHT);
        graphics.blitSprite(HOTBAR_SELECTION, HOTBAR_LEFT - 1, VIRTUAL_HEIGHT - SELECTION_HEIGHT, SELECTION_WIDTH, SELECTION_HEIGHT);

        HudValues values = new HudValues(HEALTH, 20, ABSORPTION, ARMOR, FOOD, 0, null, ManaSource.placeholders(60),
                new ThirstCompat.Thirst(THIRST, 0, 20), StatusColors.HEALTH, StatusColors.HUNGER, StatusColors.THIRST, false);
        HudRenderer.render(graphics, font, frame, values, false, HudPosition.Pieces.NONE, false);

        HudPosition.Bounds armor = frame.pieceBounds().get(HudPiece.ARMOR);
        if (frame.vanillaArmor() && armor != null) {
            for (int i = 0; i < 10; i++) {
                int points = i * 2 + 1;
                ResourceLocation sprite = points < ARMOR ? ARMOR_FULL : points == ARMOR ? ARMOR_HALF : ARMOR_EMPTY;
                graphics.blitSprite(sprite, armor.left() + i * ICON_SPACING, armor.top(), ICON, ICON);
            }
        }
        drawFood(graphics, frame);
        HudPosition.Bounds air = frame.defaultAir();
        drawAir(graphics, air.right(), air.top());

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

    /** Sample hunger and thirst icons where the pieces are on Default, as laid out in {@code frame}, unmoved. */
    private static void drawFood(GuiGraphics graphics, HudLayer.Frame frame) {
        HudPosition.Bounds hunger = frame.pieceBounds().get(HudPiece.HUNGER);
        if (frame.vanillaHunger() && hunger != null) {
            drawHungerIcons(graphics, hunger);
        }
        HudPosition.Bounds thirst = frame.pieceBounds().get(HudPiece.THIRST);
        if (frame.thirst() == ThirstMode.VANILLA && thirst != null) {
            drawThirstIcons(graphics, thirst);
        }
    }

    /** Sample hunger icons filling {@code box}. */
    static void drawHungerIcons(GuiGraphics graphics, HudPosition.Bounds box) {
        for (int i = 0; i < 10; i++) {
            int points = i * 2 + 1;
            ResourceLocation sprite = points < FOOD ? FOOD_FULL : points == FOOD ? FOOD_HALF : FOOD_EMPTY;
            graphics.blitSprite(sprite, box.right() - i * ICON_SPACING - ICON, box.top(), ICON, ICON);
        }
    }

    /** Sample thirst droplets filling {@code box}. */
    static void drawThirstIcons(GuiGraphics graphics, HudPosition.Bounds box) {
        for (int i = 0; i < 10; i++) {
            int points = i * 2 + 1;
            int x = box.right() - i * ICON_SPACING - ICON;
            // The mod's strip: the empty droplet, then fills over it, half at 16 and full at 32.
            graphics.blit(ThirstCompat.ICONS, x, box.top(), 0, 0, ICON, ICON, THIRST_ICONS_WIDTH, ICON);
            if (points <= THIRST) {
                graphics.blit(ThirstCompat.ICONS, x, box.top(), points < THIRST ? 32 : 16, 0, ICON, ICON, THIRST_ICONS_WIDTH, ICON);
            }
        }
    }

    /** Sample air bubbles in a row ending at {@code right}. */
    static void drawAir(GuiGraphics graphics, int right, int top) {
        // Right-aligned like the hunger icons, the last one bursting.
        for (int i = 0; i < AIR_BUBBLES; i++) {
            graphics.blitSprite(i == AIR_BUBBLES - 1 ? AIR_BURSTING : AIR, right - i * ICON_SPACING - ICON, top, ICON, ICON);
        }
    }

    /** The HUD laid out on the virtual screen, and the box around everything in it. */
    private record Layout(HudLayer.Frame frame, int left, int top, int right, int bottom) {
    }

    private static Layout layout() {
        HudLayer.Frame frame = HudLayer.Frame.preview(WIDTH, VIRTUAL_HEIGHT);
        // The pieces at their default spots, ignoring where they've been moved.
        HudPosition.Bounds air = frame.defaultAir();
        int left = HOTBAR_LEFT - 1;
        int top = air.top();
        int right = WIDTH;
        int bottom = VIRTUAL_HEIGHT;
        for (Map.Entry<HudPiece, HudPosition.Bounds> entry : frame.pieceBounds().entrySet()) {
            if (entry.getKey() == HudPiece.AIR || entry.getKey() == HudPiece.MOUNT) {
                continue;
            }
            HudPosition.Bounds piece = entry.getValue();
            left = Math.min(left, piece.left());
            top = Math.min(top, piece.top());
            right = Math.max(right, piece.right());
            bottom = Math.max(bottom, piece.bottom());
        }
        return new Layout(frame, left, top, right, bottom);
    }
}
