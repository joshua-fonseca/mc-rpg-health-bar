package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.BarSize;

/**
 * Positions of the bars in GUI-scaled pixels, at their default spot above the hotbar. Shared by
 * the HUD and the position editor.
 */
public record HudLayout(int x, int top, int iconRowY, int halfBarY, int healthY, int healthWidth, int manaY, int manaBars, int thirstY, int rowsTop) {
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
    /** The vertical hunger and thirst bars are, unless resized, one and a half times as tall as the hotbar (22px). */
    public static final int VERTICAL_BAR_HEIGHT = 33;
    /** The first vertical bar is this far from what's beside it, as the offhand slot is from the hotbar. */
    private static final int BESIDE_GAP = 7;
    /** A left-handed player's offhand slot reaches this far right of the hotbar. */
    private static final int OFFHAND_SLOT_WIDTH = 29;
    /** The hotbar attack indicator, right of a right-handed player's hotbar, ends this far from it. */
    private static final int ATTACK_INDICATOR_END = 24;
    /** Vertical gap between the armor/hunger, health and mana rows; same as between the XP bar and hotbar. */
    static final int ROW_GAP = 2;
    /** The XP bar's top edge is at guiHeight - 29; the bars sit 2px above it. */
    private static final int BOTTOM_MARGIN = 31;

    /**
     * Rows from top to bottom: thirst, armor/hunger, health, then the mana bars (Iron's Spells', then
     * Confluence's). Health and each mana bar are as wide as {@link BarWidths} says, lined up on
     * the left.
     * <p>
     * The armor/hunger row holds our half bars and/or the vanilla icons, side by side. With any
     * vanilla icons in it, it's as tall as the icons and the bars are centered vertically in it;
     * with only bars it's as thin as the bars; with neither it's removed and the group shrinks.
     *
     * @param hasBars  whether armor or hunger is shown as a bar
     * @param hasIcons whether armor or hunger is shown as vanilla icons
     * @param thirstHeight the thirst row's height (its icons' or bar's), or 0 for no thirst row
     * @param manaBars how many mana bars are drawn, see {@link com.joshuafonseca.rpghealthbar.compat.ManaSource}
     */
    public static HudLayout compute(int screenWidth, int screenHeight, boolean hasBars, boolean hasIcons, int manaBars,
                                    int healthWidth, int thirstHeight) {
        int x = screenWidth / 2 - WIDTH / 2;
        int bottom = screenHeight - BOTTOM_MARGIN;
        int manaY = bottom - manaBars * (MAIN_BAR_HEIGHT + ROW_GAP) + ROW_GAP;
        int healthY = manaBars > 0 ? manaY - ROW_GAP - MAIN_BAR_HEIGHT : bottom - MAIN_BAR_HEIGHT;
        int rowHeight = hasIcons ? ICON_ROW_HEIGHT : hasBars ? HALF_BAR_HEIGHT : 0;
        int iconRowY = healthY - ROW_GAP - rowHeight;
        int halfBarY = iconRowY + (rowHeight - HALF_BAR_HEIGHT) / 2;
        int rowsTop = rowHeight > 0 ? iconRowY : healthY;
        int thirstY = rowsTop - ROW_GAP - thirstHeight;
        int top = thirstHeight > 0 ? thirstY : rowsTop;
        return new HudLayout(x, top, iconRowY, halfBarY, healthY, healthWidth, manaY, manaBars, thirstY, rowsTop);
    }

    /**
     * A vertical bar's left edge: right of the hotbar, or of what vanilla draws on that side, as far
     * from it as the offhand slot is from the hotbar. A second bar follows the first with the same
     * gap as between the health bar and the XP bar. Their bottom edges are the screen's, like the
     * hotbar's.
     *
     * @param offhandSlot     whether the offhand slot goes on the right, as for a left-handed player
     * @param attackIndicator whether the hotbar attack indicator goes on the right
     * @param index           0 for the bar nearest the hotbar, 1 for the one after it
     */
    public static int verticalBarX(int screenWidth, boolean offhandSlot, boolean attackIndicator, int index) {
        int beside = offhandSlot ? OFFHAND_SLOT_WIDTH : attackIndicator ? ATTACK_INDICATOR_END : 0;
        return screenWidth / 2 + WIDTH / 2 + beside + BESIDE_GAP + index * (MAIN_BAR_HEIGHT + ROW_GAP);
    }

    /** Full or half width: as wide as the hotbar, or as the armor and hunger bars. */
    public static int width(BarSize size) {
        return size == BarSize.HALF ? HALF_BAR_WIDTH : WIDTH;
    }

    /** Top edge of mana bar {@code row}, counting from 0 at the top. */
    public int manaY(int row) {
        return manaY + row * (MAIN_BAR_HEIGHT + ROW_GAP);
    }

    /** Left edge of the hunger bar (the armor bar starts at {@link #x()}). */
    public int hungerX() {
        return x + WIDTH - HALF_BAR_WIDTH;
    }
}
