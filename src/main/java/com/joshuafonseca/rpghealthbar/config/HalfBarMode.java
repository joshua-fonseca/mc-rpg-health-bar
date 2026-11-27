package com.joshuafonseca.rpghealthbar.config;

import net.minecraft.network.chat.Component;

/** How the armor and hunger stats are shown. */
public enum HalfBarMode {
    /**
     * Left to the game (the default): the vanilla icons, in the row above the health bar, or
     * another mod's display if one replaces them. Stored as VANILLA, its name before.
     */
    VANILLA,
    /** Our half-width bar, in the row above the health bar. */
    BAR,
    /** Our upright bar beside the hotbar. Hunger only. */
    VERTICAL,
    /** Not shown at all. */
    HIDDEN;

    public Component displayName() {
        return Component.translatable("rpghealthbar.config.mode." + name().toLowerCase(java.util.Locale.ROOT));
    }
}
