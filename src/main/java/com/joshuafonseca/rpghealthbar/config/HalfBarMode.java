package com.joshuafonseca.rpghealthbar.config;

import net.minecraft.network.chat.Component;

/** How the armor and hunger stats are shown. */
public enum HalfBarMode {
    /** Our half-width bar above the health bar. */
    BAR,
    /** The vanilla icons, drawn above our bars. */
    VANILLA,
    /** Not shown at all. */
    HIDDEN;

    public Component displayName() {
        return Component.translatable("rpghealthbar.config.mode." + name().toLowerCase(java.util.Locale.ROOT));
    }
}
