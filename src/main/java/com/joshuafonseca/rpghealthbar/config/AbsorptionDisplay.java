package com.joshuafonseca.rpghealthbar.config;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/** How absorption is written in the health bar's text. */
public enum AbsorptionDisplay {
    /** Health and absorption listed separately: 16+4/20. */
    SEPARATE,
    /** Health and absorption added together: 20/20. */
    COMBINED;

    public Component displayName() {
        return Component.translatable("rpghealthbar.config.absorptionDisplay." + name().toLowerCase(Locale.ROOT));
    }
}
