package com.joshuafonseca.rpghealthbar.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Stored in config/rpghealthbar-client.toml. Values are read every frame, so changes apply immediately. */
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_ARMOR_BAR = BUILDER
            .comment("Show the white armor bar above the health bar.")
            .translation("rpghealthbar.config.showArmorBar")
            .define("showArmorBar", true);

    public static final ModConfigSpec.BooleanValue SHOW_HUNGER_BAR = BUILDER
            .comment("Show the orange hunger bar above the health bar.")
            .translation("rpghealthbar.config.showHungerBar")
            .define("showHungerBar", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }

    /** Updates a value and writes the config file. */
    public static <T> void set(ModConfigSpec.ConfigValue<T> value, T newValue) {
        value.set(newValue);
        value.save();
    }
}
