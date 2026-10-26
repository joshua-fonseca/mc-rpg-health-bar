package com.joshuafonseca.rpghealthbar.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Stored in config/rpghealthbar-client.toml. Values are read every frame, so changes apply immediately. */
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<HalfBarMode> ARMOR_MODE = BUILDER
            .comment("How armor is shown: BAR (white bar above the health bar), VANILLA (vanilla armor icons) or HIDDEN.")
            .translation("rpghealthbar.config.armorMode")
            .defineEnum("armorMode", HalfBarMode.BAR);

    public static final ModConfigSpec.EnumValue<HalfBarMode> HUNGER_MODE = BUILDER
            .comment("How hunger is shown: BAR (orange bar above the health bar), VANILLA (vanilla hunger icons) or HIDDEN.")
            .translation("rpghealthbar.config.hungerMode")
            .defineEnum("hungerMode", HalfBarMode.BAR);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }

    /** Updates a value and writes the config file. */
    public static <T> void set(ModConfigSpec.ConfigValue<T> value, T newValue) {
        value.set(newValue);
        value.save();
    }
}
