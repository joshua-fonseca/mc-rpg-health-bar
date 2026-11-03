package com.joshuafonseca.rpghealthbar.config;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/** Where the air bubbles and mount health rows go, relative to hunger. */
public enum AirPosition {
    /** Stacked upward from above hunger, like vanilla. */
    ABOVE,
    /** Stacked downward from under hunger. */
    BELOW;

    public Component displayName() {
        return Component.translatable("rpghealthbar.config.airPosition." + name().toLowerCase(Locale.ROOT));
    }
}
