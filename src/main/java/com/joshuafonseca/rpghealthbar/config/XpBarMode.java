package com.joshuafonseca.rpghealthbar.config;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/** What of the XP bar is shown. The jump bar shows while riding whichever is picked. */
public enum XpBarMode {
    /** The bar with the level's number on it. */
    SHOWN,
    /** Just the level's number, placed on its own. */
    LEVEL_ONLY,
    HIDDEN;

    public Component displayName() {
        return Component.translatable("rpghealthbar.config.xpBar." + name().toLowerCase(Locale.ROOT));
    }
}
