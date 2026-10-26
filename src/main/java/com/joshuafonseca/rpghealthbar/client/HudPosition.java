package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;

/**
 * Where the movable HUD set is drawn: an offset from its default spot above the hotbar. The set is
 * our bars plus the vanilla elements that move with them (XP bar, level, jump meter, and vanilla
 * icons stacked above the bars).
 */
public final class HudPosition {
    /** The XP bar's bottom edge is this far above the bottom of the screen. */
    private static final int XP_BAR_BOTTOM_MARGIN = 24;
    /** Height of one row of vanilla icons (armor, hunger) stacked above the bars. */
    private static final int VANILLA_ROW_HEIGHT = 10;

    /** Offset while the position editor is open, so the real HUD follows the drag; null otherwise. */
    @Nullable
    private static Offset preview;

    private HudPosition() {
    }

    public record Offset(int x, int y) {
    }

    /** The bounding box of the movable set at its default position. */
    public record Bounds(int left, int top, int right, int bottom) {
        public Bounds offset(Offset offset) {
            return new Bounds(left + offset.x(), top + offset.y(), right + offset.x(), bottom + offset.y());
        }

        public boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }
    }

    public static Bounds bounds(int screenHeight, HudLayout layout, boolean vanillaIconsAbove) {
        int top = layout.top() - (vanillaIconsAbove ? VANILLA_ROW_HEIGHT : 0);
        return new Bounds(layout.x(), top, layout.x() + HudLayout.WIDTH, screenHeight - XP_BAR_BOTTOM_MARGIN);
    }

    /** The saved offset (or the editor's preview), clamped so the whole set stays on screen. */
    public static Offset current(int screenWidth, int screenHeight, Bounds bounds) {
        Offset raw = preview != null ? preview : new Offset(ClientConfig.OFFSET_X.get(), ClientConfig.OFFSET_Y.get());
        return clamp(raw, screenWidth, screenHeight, bounds);
    }

    public static Offset clamp(Offset offset, int screenWidth, int screenHeight, Bounds bounds) {
        int minX = -bounds.left();
        int maxX = screenWidth - bounds.right();
        int minY = -bounds.top();
        int maxY = screenHeight - bounds.bottom();
        return new Offset(Mth.clamp(offset.x(), minX, Math.max(minX, maxX)), Mth.clamp(offset.y(), minY, Math.max(minY, maxY)));
    }

    @Nullable
    public static Offset preview() {
        return preview;
    }

    public static void setPreview(@Nullable Offset offset) {
        preview = offset;
    }
}
