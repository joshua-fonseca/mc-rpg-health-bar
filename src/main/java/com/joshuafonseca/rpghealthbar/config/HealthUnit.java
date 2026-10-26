package com.joshuafonseca.rpghealthbar.config;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/** Unit for the numbers on the health bar. */
public enum HealthUnit {
    /** Health points, as stored by the game: 20 for a full vanilla bar. */
    HP(1.0F),
    /** Hearts: one heart is 2 HP, so a full vanilla bar is 10. */
    HEARTS(0.5F);

    private final float perHp;

    HealthUnit(float perHp) {
        this.perHp = perHp;
    }

    public float fromHp(float hp) {
        return hp * perHp;
    }

    public Component displayName() {
        return Component.translatable("rpghealthbar.config.healthUnit." + name().toLowerCase(Locale.ROOT));
    }
}
