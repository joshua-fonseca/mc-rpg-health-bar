package com.joshuafonseca.rpghealthbar.client;

/**
 * Positions of the bars in GUI-scaled pixels, at their default spot above the hotbar. Shared by
 * the HUD and the position editor.
 */
public record HudLayout(int x, int top, int iconRowY, int halfBarY, int healthY, int healthWidth, int manaX, int manaY, int manaWidth) {
    /** Same width as the hotbar. */
    public static final int WIDTH = 182;
    /** Health and mana bars: just tall enough for their 7px text. */
    public static final int MAIN_BAR_HEIGHT = 7;
    /** Armor and hunger bars: thin lines. */
    public static final int HALF_BAR_HEIGHT = 3;
    /** Gap between the armor and hunger bars. */
    public static final int HALF_BAR_GAP = 2;
    /** Each of the armor and hunger bars takes half the width, minus the gap between them. */
    public static final int HALF_BAR_WIDTH = (WIDTH - HALF_BAR_GAP) / 2;
    /** Vanilla armor and hunger icons are 9px tall. */
    public static final int ICON_ROW_HEIGHT = 9;
    /** Ten 9px icons, 8px apart. */
    public static final int ICON_ROW_WIDTH = 81;
    /** Vertical gap between the armor/hunger, health and mana rows; same as between the XP bar and hotbar. */
    private static final int ROW_GAP = 2;
    /** The XP bar's top edge is at guiHeight - 29; the bars sit 2px above it. */
    private static final int BOTTOM_MARGIN = 31;

    /**
     * Rows from top to bottom: armor/hunger, health, mana. Health and mana are each full or half
     * width ({@link #HALF_BAR_WIDTH}); with both half, they share one row, health on the left and
     * mana on the right, lined up with the armor and hunger bars.
     * <p>
     * The armor/hunger row holds our half bars and/or the vanilla icons, side by side. With any
     * vanilla icons in it, it's as tall as the icons and the bars are centered vertically in it;
     * with only bars it's as thin as the bars; with neither it's removed and the group shrinks.
     *
     * @param hasBars        whether armor or hunger is shown as a bar
     * @param hasIcons       whether armor or hunger is shown as vanilla icons
     * @param showManaRow    whether the mana bar is drawn (only with Iron's Spells installed)
     */
    public static HudLayout compute(int screenWidth, int screenHeight, boolean hasBars, boolean hasIcons, boolean showManaRow,
                                    boolean halfHealth, boolean halfMana) {
        int x = screenWidth / 2 - WIDTH / 2;
        int bottom = screenHeight - BOTTOM_MARGIN;
        int healthWidth = halfHealth ? HALF_BAR_WIDTH : WIDTH;
        int manaWidth = halfMana ? HALF_BAR_WIDTH : WIDTH;
        boolean sharedRow = halfHealth && halfMana;
        int manaX = sharedRow ? x + WIDTH - HALF_BAR_WIDTH : x;
        int manaY = bottom - MAIN_BAR_HEIGHT;
        int healthY = showManaRow && !sharedRow ? manaY - ROW_GAP - MAIN_BAR_HEIGHT : manaY;
        int rowHeight = hasIcons ? ICON_ROW_HEIGHT : hasBars ? HALF_BAR_HEIGHT : 0;
        int iconRowY = healthY - ROW_GAP - rowHeight;
        int halfBarY = iconRowY + (rowHeight - HALF_BAR_HEIGHT) / 2;
        int top = rowHeight > 0 ? iconRowY : healthY;
        return new HudLayout(x, top, iconRowY, halfBarY, healthY, healthWidth, manaX, manaY, manaWidth);
    }

    /** Left edge of the hunger bar (the armor bar starts at {@link #x()}). */
    public int hungerX() {
        return x + WIDTH - HALF_BAR_WIDTH;
    }
}
