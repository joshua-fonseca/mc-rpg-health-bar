package com.joshuafonseca.rpghealthbar.config;

/**
 * The parts of the HUD that can be moved on their own in HUD Editor. The vanilla elements move with
 * the piece they belong to: the level and jump meter with XP.
 */
public enum HudPiece {
    ARMOR("armor"),
    HUNGER("hunger"),
    /** The upright hunger bar, beside the hotbar by default; placed apart from {@link #HUNGER}. */
    HUNGER_VERTICAL("hungerVertical"),
    /** Thirst Was Taken 2's thirst, as its icons or our half bar, above hunger by default. */
    THIRST("thirst"),
    /** The upright thirst bar, beside the hotbar by default; placed apart from {@link #THIRST}. */
    THIRST_VERTICAL("thirstVertical"),
    /** The air bubbles, above hunger by default, or under it with Air Bubbles/Mount HP on Below. */
    AIR("air"),
    /** Mount health, shown while riding in hunger's place by default; its rows grow up, or down on Below. */
    MOUNT("mount"),
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

    /** An upright bar, which stands beside the hotbar, apart from the rest of the HUD. */
    public boolean isUpright() {
        return this == HUNGER_VERTICAL || this == THIRST_VERTICAL;
    }

    /** Used in the config file's key names. */
    public String key() {
        return key;
    }
}
