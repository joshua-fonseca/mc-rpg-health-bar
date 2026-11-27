package com.joshuafonseca.rpghealthbar.client.screen;

import com.joshuafonseca.rpghealthbar.client.BarWidths;
import com.joshuafonseca.rpghealthbar.client.HudLayer;
import com.joshuafonseca.rpghealthbar.client.HudLayout;
import com.joshuafonseca.rpghealthbar.client.HudPosition;
import com.joshuafonseca.rpghealthbar.client.HudPositionMover;
import com.joshuafonseca.rpghealthbar.client.HudRenderer;
import com.joshuafonseca.rpghealthbar.client.HudValues;
import com.joshuafonseca.rpghealthbar.client.StatusColors;
import com.joshuafonseca.rpghealthbar.client.XpBarOverlay;
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

    private static final ResourceLocation ICONS = XpBarOverlay.ICONS;
    /** Where vanilla's icons are in {@link #ICONS}: u of each kind, and v of each row. */
    private static final int ARMOR_V = 9;
    private static final int ARMOR_EMPTY_U = 16;
    private static final int ARMOR_HALF_U = 25;
    private static final int ARMOR_FULL_U = 34;
    private static final int FOOD_V = 27;
    private static final int FOOD_EMPTY_U = 16;
    private static final int FOOD_FULL_U = 52;
    private static final int FOOD_HALF_U = 61;
    private static final int AIR_V = 18;
    private static final int AIR_U = 16;
    private static final int AIR_BURSTING_U = 25;
    private static final int HEART_V = 9;
    private static final int MOUNT_CONTAINER_U = 52;
    private static final int MOUNT_FULL_U = 88;
    /** Vanilla's hotbar in {@link #WIDGETS}, with the selected slot's frame in the row under it. */
    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
    private static final int HOTBAR_WIDTH = 182;
    private static final int HOTBAR_HEIGHT = 22;
    private static final int SELECTION_V = 22;
    private static final int SELECTION_WIDTH = 24;
    private static final int SELECTION_HEIGHT = 22;
    private static final int HOTBAR_LEFT = WIDTH / 2 - HOTBAR_WIDTH / 2;

    /**
     * A virtual screen the HUD is laid out on; only its height matters, as the HUD sits at the bottom.
     * Tall enough that no upright bar is cut to fit it.
     */
    private static final int VIRTUAL_HEIGHT = 10000;
    private static final int ICON = HudLayout.ICON_ROW_HEIGHT;
    private static final int ICON_SPACING = 8;

    /** Health a bit down with some absorption, and mana partly used, so the text and fills show. */
    private static final float HEALTH = 15;
    private static final float ABSORPTION = 4;
    private static final float ARMOR = 12;
    private static final float FOOD = 17;
    private static final int THIRST = 15;
    private static final int THIRST_ICONS_WIDTH = 41;
    private static final int AIR_BUBBLES = 7;
    private static final float XP_PROGRESS_FRACTION = 0.6F;
    private static final int LEVEL = 12;

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
        graphics.blit(WIDGETS, HOTBAR_LEFT, VIRTUAL_HEIGHT - HOTBAR_HEIGHT, 0, 0, HOTBAR_WIDTH, HOTBAR_HEIGHT);
        // As in vanilla, the frame sits a pixel above and to the left of the first slot.
        graphics.blit(WIDGETS, HOTBAR_LEFT - 1, VIRTUAL_HEIGHT - HOTBAR_HEIGHT - 1, 0, SELECTION_V, SELECTION_WIDTH, SELECTION_HEIGHT);

        HudValues values = new HudValues(HEALTH, 20, ABSORPTION, ARMOR, FOOD, 0, null, ManaSource.placeholders(60),
                new ThirstCompat.Thirst(THIRST, 0, 20), StatusColors.HEALTH, StatusColors.HUNGER, StatusColors.THIRST, false);
        HudRenderer.render(graphics, font, frame, values, false, HudPosition.Pieces.NONE, false);

        HudPosition.Bounds armor = frame.pieceBounds().get(HudPiece.ARMOR);
        if (frame.vanillaArmor() && armor != null) {
            for (int i = 0; i < 10; i++) {
                int points = i * 2 + 1;
                int u = points < ARMOR ? ARMOR_FULL_U : points == ARMOR ? ARMOR_HALF_U : ARMOR_EMPTY_U;
                graphics.blit(ICONS, armor.left() + i * ICON_SPACING, armor.top(), u, ARMOR_V, ICON, ICON);
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
        XpBarOverlay.drawLevel(graphics, font, xp, LEVEL);
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
            int x = box.right() - i * ICON_SPACING - ICON;
            // An empty container, then the food over it, as vanilla draws them.
            graphics.blit(ICONS, x, box.top(), FOOD_EMPTY_U, FOOD_V, ICON, ICON);
            if (points <= FOOD) {
                graphics.blit(ICONS, x, box.top(), points < FOOD ? FOOD_FULL_U : FOOD_HALF_U, FOOD_V, ICON, ICON);
            }
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

    /** A sample row of mount health ending at {@code right}. */
    static void drawMountHealth(GuiGraphics graphics, int right, int top) {
        for (int i = 0; i < 10; i++) {
            int x = right - i * ICON_SPACING - ICON;
            graphics.blit(ICONS, x, top, MOUNT_CONTAINER_U, HEART_V, ICON, ICON);
            graphics.blit(ICONS, x, top, MOUNT_FULL_U, HEART_V, ICON, ICON);
        }
    }

    /** Sample air bubbles in a row ending at {@code right}. */
    static void drawAir(GuiGraphics graphics, int right, int top) {
        // Right-aligned like the hunger icons, the last one bursting.
        for (int i = 0; i < AIR_BUBBLES; i++) {
            graphics.blit(ICONS, right - i * ICON_SPACING - ICON, top, i == AIR_BUBBLES - 1 ? AIR_BURSTING_U : AIR_U, AIR_V, ICON, ICON);
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
