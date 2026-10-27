package com.joshuafonseca.rpghealthbar.config;

/**
 * The parts of the HUD that can be moved on their own in Move HUD. The vanilla elements move with
 * the piece they belong to: the level and jump meter with XP, air bubbles and mount health with hunger.
 */
public enum HudPiece {
    ARMOR("armor"),
    HUNGER("hunger"),
    HEALTH("health"),
    MANA("mana"),
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
