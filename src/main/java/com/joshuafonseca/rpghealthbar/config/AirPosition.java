package com.joshuafonseca.rpghealthbar.config;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/** Where the air bubbles start, and which way mount health rows stack. */
public enum AirPosition {
    /** Above hunger, stacked upward, like vanilla. */
    ABOVE,
    /** Under hunger, stacked downward. */
    BELOW;

    public Component displayName() {
        return Component.translatable("rpghealthbar.config.airPosition." + name().toLowerCase(Locale.ROOT));
    }
}
