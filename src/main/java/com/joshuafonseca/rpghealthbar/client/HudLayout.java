package com.joshuafonseca.rpghealthbar.client;

/** Positions of the bars in GUI-scaled pixels. Shared by the HUD and (later) the position editor. */
public record HudLayout(int x, int top, int iconRowY, int halfBarY, int healthY, int manaY) {
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
    /** Vertical gap between the armor/hunger, health and mana rows. */
    private static final int ROW_GAP = 3;
    /** The XP bar's top edge is at guiHeight - 29; the bars sit 2px above it. */
    private static final int BOTTOM_MARGIN = 31;

    /**
     * Rows from top to bottom: armor/hunger, health, mana.
     * <p>
     * The armor/hunger row holds our half bars and/or the vanilla icons, side by side. With any
     * vanilla icons in it, it's as tall as the icons and the bars are centered vertically in it;
     * with only bars it's as thin as the bars; with neither it's removed and the group shrinks.
     *
     * @param hasBars        whether armor or hunger is shown as a bar
     * @param hasIcons       whether armor or hunger is shown as vanilla icons
     * @param showManaRow    whether the mana bar is drawn (only with Iron's Spells installed)
     */
    public static HudLayout compute(int screenWidth, int screenHeight, boolean hasBars, boolean hasIcons, boolean showManaRow) {
        int x = screenWidth / 2 - WIDTH / 2;
        int bottom = screenHeight - BOTTOM_MARGIN;
        int manaY = bottom - MAIN_BAR_HEIGHT;
        int healthY = showManaRow ? manaY - ROW_GAP - MAIN_BAR_HEIGHT : manaY;
        int rowHeight = hasIcons ? ICON_ROW_HEIGHT : hasBars ? HALF_BAR_HEIGHT : 0;
        int iconRowY = healthY - ROW_GAP - rowHeight;
        int halfBarY = iconRowY + (rowHeight - HALF_BAR_HEIGHT) / 2;
        int top = rowHeight > 0 ? iconRowY : healthY;
        return new HudLayout(x, top, iconRowY, halfBarY, healthY, manaY);
    }

    /** Left edge of the hunger bar (the armor bar starts at {@link #x()}). */
    public int hungerX() {
        return x + WIDTH - HALF_BAR_WIDTH;
    }
}
