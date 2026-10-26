package com.joshuafonseca.rpghealthbar.client;

/** Positions of the bars in GUI-scaled pixels. Shared by the HUD and (later) the position editor. */
public record HudLayout(int x, int top, int halfBarY, int healthY) {
    /** Same width as the hotbar. */
    public static final int WIDTH = 182;
    /** Health and mana bars: tall enough to fit a line of text. */
    public static final int MAIN_BAR_HEIGHT = 9;
    /** Armor and hunger bars: about half the height of the health bar. */
    public static final int HALF_BAR_HEIGHT = 5;
    /** Gap between the armor and hunger bars. */
    public static final int HALF_BAR_GAP = 2;
    /** Each of the armor and hunger bars takes half the width, minus the gap between them. */
    public static final int HALF_BAR_WIDTH = (WIDTH - HALF_BAR_GAP) / 2;
    /** Vertical gap between rows. */
    private static final int ROW_GAP = 1;
    /** The XP level number is drawn from guiHeight - 36 down; keep the group just above it. */
    private static final int BOTTOM_MARGIN = 37;

    public static HudLayout compute(int screenWidth, int screenHeight) {
        int x = screenWidth / 2 - WIDTH / 2;
        int bottom = screenHeight - BOTTOM_MARGIN;
        int healthY = bottom - MAIN_BAR_HEIGHT;
        int halfBarY = healthY - ROW_GAP - HALF_BAR_HEIGHT;
        return new HudLayout(x, halfBarY, halfBarY, healthY);
    }

    /** Left edge of the hunger bar (the armor bar starts at {@link #x()}). */
    public int hungerX() {
        return x + WIDTH - HALF_BAR_WIDTH;
    }
}
