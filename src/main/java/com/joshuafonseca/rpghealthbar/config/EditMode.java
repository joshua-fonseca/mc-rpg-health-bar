package com.joshuafonseca.rpghealthbar.config;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/** How the bars' widths are set: presets in the config screen, or freely in the HUD Editor. */
public enum EditMode {
    /** Full or half width, picked in the config screen. */
    SIMPLE,
    /** Any width: the health, mana and XP bars' ends are dragged in the HUD Editor. */
    ADVANCED;

    public Component displayName() {
        return Component.translatable("rpghealthbar.config.editMode." + name().toLowerCase(Locale.ROOT));
    }
}
