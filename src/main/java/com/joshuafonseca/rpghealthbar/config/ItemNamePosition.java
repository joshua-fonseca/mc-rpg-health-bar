package com.joshuafonseca.rpghealthbar.config;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/** Where the held item's name goes when nothing is stacked right above the hotbar. */
public enum ItemNamePosition {
    /** No lower than vanilla puts it, leaving room for things other mods draw above the hotbar. */
    VANILLA,
    /** Right above the hotbar. */
    ABOVE_HOTBAR;

    public Component displayName() {
        return Component.translatable("rpghealthbar.config.itemNamePosition." + name().toLowerCase(Locale.ROOT));
    }
}
