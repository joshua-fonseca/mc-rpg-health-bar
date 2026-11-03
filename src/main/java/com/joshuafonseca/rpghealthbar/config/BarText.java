package com.joshuafonseca.rpghealthbar.config;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/** Where the numbers go on the health or mana bar. */
public enum BarText {
    LEFT,
    RIGHT,
    CENTER,
    OFF;

    public Component displayName() {
        return Component.translatable("rpghealthbar.config.barText." + name().toLowerCase(Locale.ROOT));
    }
}
