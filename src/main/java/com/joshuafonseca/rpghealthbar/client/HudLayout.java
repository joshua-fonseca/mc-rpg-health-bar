package com.joshuafonseca.rpghealthbar.client;

/** Positions of the bars in GUI-scaled pixels. Shared by the HUD and (later) the position editor. */
public record HudLayout(int x, int top, int healthY) {
    /** Same width as the hotbar. */
    public static final int WIDTH = 182;
    /** Health and mana bars: tall enough to fit a line of text. */
    public static final int MAIN_BAR_HEIGHT = 9;
    /** The XP level number is drawn from guiHeight - 36 down; keep the group just above it. */
    private static final int BOTTOM_MARGIN = 37;

    public static HudLayout compute(int screenWidth, int screenHeight) {
        int x = screenWidth / 2 - WIDTH / 2;
        int bottom = screenHeight - BOTTOM_MARGIN;
        int healthY = bottom - MAIN_BAR_HEIGHT;
        return new HudLayout(x, healthY, healthY);
    }
}
