package com.joshuafonseca.rpghealthbar.config;

/**
 * The parts of the HUD that can be moved on their own in HUD Editor. The vanilla elements move with
 * the piece they belong to: the level and jump meter with XP, air bubbles and mount health with hunger.
 */
public enum HudPiece {
    ARMOR("armor"),
    HUNGER("hunger"),
    /** Thirst Was Taken 2's thirst, as its icons or our half bar, above hunger by default. */
    THIRST("thirst"),
    /** The upright thirst bar, beside the hotbar by default; placed apart from {@link #THIRST}. */
    THIRST_VERTICAL("thirstVertical"),
    HEALTH("health"),
    /** Iron's Spells' mana bar. */
    MANA("mana"),
    /** Confluence's mana bar, below Iron's by default. */
    CONFLUENCE_MANA("confluenceMana"),
    /** Ars Nouveau's mana bar, below the others. */
    ARS_MANA("arsMana"),
    XP("xp");

    private final String key;

    HudPiece(String key) {
        this.key = key;
    }

    /** Used in the config file's key names. */
    public String key() {
        return key;
    }
}
