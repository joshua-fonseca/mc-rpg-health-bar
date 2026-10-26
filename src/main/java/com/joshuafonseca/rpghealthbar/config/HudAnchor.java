package com.joshuafonseca.rpghealthbar.config;

/**
 * The screen spot the HUD's saved position is measured from. Chosen automatically when the
 * position is saved: whichever spot is nearest the HUD. Measuring from the nearest spot keeps the
 * HUD where the player put it when the window size or GUI scale changes.
 */
public enum HudAnchor {
    TOP_LEFT(0, 0), TOP_CENTER(1, 0), TOP_RIGHT(2, 0),
    MIDDLE_LEFT(0, 1), CENTER(1, 1), MIDDLE_RIGHT(2, 1),
    BOTTOM_LEFT(0, 2), BOTTOM_CENTER(1, 2), BOTTOM_RIGHT(2, 2);

    /** Position across the screen in halves: 0 = left/top edge, 1 = middle, 2 = right/bottom edge. */
    private final int halvesX;
    private final int halvesY;

    HudAnchor(int halvesX, int halvesY) {
        this.halvesX = halvesX;
        this.halvesY = halvesY;
    }

    /** The anchor point along a length, e.g. 0, length / 2 or length. */
    public int x(int length) {
        return length * halvesX / 2;
    }

    public int y(int length) {
        return length * halvesY / 2;
    }

    /** The anchor whose third of the screen contains the point. */
    public static HudAnchor nearest(int x, int y, int screenWidth, int screenHeight) {
        int column = Math.min(2, Math.max(0, x * 3 / Math.max(1, screenWidth)));
        int row = Math.min(2, Math.max(0, y * 3 / Math.max(1, screenHeight)));
        for (HudAnchor anchor : values()) {
            if (anchor.halvesX == column && anchor.halvesY == row) {
                return anchor;
            }
        }
        return BOTTOM_CENTER;
    }
}
