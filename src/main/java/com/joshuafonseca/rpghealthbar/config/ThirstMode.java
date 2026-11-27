package com.joshuafonseca.rpghealthbar.config;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/** How Thirst Was Taken 2's thirst is shown. */
public enum ThirstMode {
    /**
     * The mod's own droplet icons (the default), in the row above hunger. Stored as VANILLA, like
     * the armor and hunger modes' Default.
     */
    VANILLA,
    /** Our half-width bar, in that row. */
    BAR,
    /** An upright bar in the health bar's style, beside the hotbar. */
    VERTICAL,
    /** Not shown at all. */
    HIDDEN;

    public Component displayName() {
        return Component.translatable("rpghealthbar.config.mode." + name().toLowerCase(Locale.ROOT));
    }
}
