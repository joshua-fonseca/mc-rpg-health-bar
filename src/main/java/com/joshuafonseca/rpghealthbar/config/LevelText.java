package com.joshuafonseca.rpghealthbar.config;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/** Where the XP level's number goes on the XP bar. */
public enum LevelText {
    CENTER,
    LEFT,
    RIGHT;

    public Component displayName() {
        return Component.translatable("rpghealthbar.config.barText." + name().toLowerCase(Locale.ROOT));
    }
}
