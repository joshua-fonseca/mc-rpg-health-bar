package com.joshuafonseca.rpghealthbar.config;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/** Width of the health or mana bar. */
public enum BarSize {
    /** As wide as the hotbar. */
    FULL,
    /** Half as wide, like the armor and hunger bars. */
    HALF;

    public Component displayName() {
        return Component.translatable("rpghealthbar.config.barSize." + name().toLowerCase(Locale.ROOT));
    }
}
