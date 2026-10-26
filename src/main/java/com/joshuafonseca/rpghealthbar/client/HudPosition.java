package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HudAnchor;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;

/**
 * Where the movable HUD set is drawn. The set is our bars plus the vanilla elements that move with
 * them (XP bar, level, jump meter, and vanilla icons stacked above the bars).
 * <p>
 * Everything is laid out at the default spot above the hotbar and then shifted by an {@link Offset}.
 * The saved position is stored differently, relative to the nearest {@link HudAnchor}, so it holds
 * up when the window size or GUI scale changes.
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

        public int width() {
            return right - left;
        }

        public int height() {
            return bottom - top;
        }

        public boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }
    }

    public static Bounds bounds(int screenHeight, HudLayout layout, boolean vanillaIconsAbove) {
        int top = layout.top() - (vanillaIconsAbove ? VANILLA_ROW_HEIGHT : 0);
        return new Bounds(layout.x(), top, layout.x() + HudLayout.WIDTH, screenHeight - XP_BAR_BOTTOM_MARGIN);
    }

    /** The saved position (or the editor's preview), clamped so the whole set stays on screen. */
    public static Offset current(int screenWidth, int screenHeight, Bounds bounds) {
        Offset raw = preview != null ? preview : saved(screenWidth, screenHeight, bounds);
        return clamp(raw, screenWidth, screenHeight, bounds);
    }

    /** The saved position, as a shift from the default spot. */
    public static Offset saved(int screenWidth, int screenHeight, Bounds bounds) {
        return fromAnchor(ClientConfig.ANCHOR.get(), ClientConfig.OFFSET_X.get(), ClientConfig.OFFSET_Y.get(), screenWidth, screenHeight, bounds);
    }

    /** The anchor's point on screen, minus the matching point of the set, plus the offset. */
    private static Offset fromAnchor(HudAnchor anchor, int offsetX, int offsetY, int screenWidth, int screenHeight, Bounds bounds) {
        int left = anchor.x(screenWidth) - anchor.x(bounds.width()) + offsetX;
        int top = anchor.y(screenHeight) - anchor.y(bounds.height()) + offsetY;
        return new Offset(left - bounds.left(), top - bounds.top());
    }

    /** Saves a shift from the default spot, measured from whichever anchor is nearest the set. */
    public static void save(Offset offset, int screenWidth, int screenHeight, Bounds bounds) {
        Bounds moved = bounds.offset(offset);
        HudAnchor anchor = HudAnchor.nearest((moved.left() + moved.right()) / 2, (moved.top() + moved.bottom()) / 2, screenWidth, screenHeight);
        int offsetX = moved.left() - (anchor.x(screenWidth) - anchor.x(bounds.width()));
        int offsetY = moved.top() - (anchor.y(screenHeight) - anchor.y(bounds.height()));
        ClientConfig.setPosition(anchor, offsetX, offsetY);
    }

    /** The default spot above the hotbar, as a shift from the default spot (normally zero). */
    public static Offset defaultOffset(int screenWidth, int screenHeight, Bounds bounds) {
        return fromAnchor(HudAnchor.BOTTOM_CENTER, ClientConfig.DEFAULT_OFFSET_X, ClientConfig.DEFAULT_OFFSET_Y, screenWidth, screenHeight, bounds);
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
