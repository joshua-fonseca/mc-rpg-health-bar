package com.joshuafonseca.rpghealthbar.config;

/**
 * The parts of the HUD that can be moved on their own in Move HUD. The vanilla elements move with
 * the piece they belong to: the level and jump meter with XP, air bubbles and mount health with hunger.
 */
public enum HudPiece {
    ARMOR("armor"),
    HUNGER("hunger"),
    HEALTH("health"),
    /** Iron's Spells' mana bar. */
    MANA("mana"),
    /** Confluence's mana bar, below Iron's by default. */
    CONFLUENCE_MANA("confluenceMana"),
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
