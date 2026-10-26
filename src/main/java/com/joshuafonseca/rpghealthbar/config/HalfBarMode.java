package com.joshuafonseca.rpghealthbar.config;

import net.minecraft.network.chat.Component;

/** How the armor and hunger stats are shown. */
public enum HalfBarMode {
    /** The vanilla icons, in the row above the health bar (the default). */
    VANILLA,
    /** Our half-width bar, in the row above the health bar. */
    BAR,
    /** Not shown at all. */
    HIDDEN;

    public Component displayName() {
        return Component.translatable("rpghealthbar.config.mode." + name().toLowerCase(java.util.Locale.ROOT));
    }
}
