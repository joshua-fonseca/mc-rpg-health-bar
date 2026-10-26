package com.joshuafonseca.rpghealthbar.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Stored in config/rpghealthbar-client.toml. Values are read every frame, so changes apply immediately. */
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<HalfBarMode> ARMOR_MODE = BUILDER
            .comment("How armor is shown: VANILLA (vanilla armor icons), BAR (white bar above the health bar) or HIDDEN.")
            .translation("rpghealthbar.config.armorMode")
            .defineEnum("armorMode", HalfBarMode.VANILLA);

    public static final ModConfigSpec.EnumValue<HalfBarMode> HUNGER_MODE = BUILDER
            .comment("How hunger is shown: VANILLA (vanilla hunger icons), BAR (orange bar above the health bar) or HIDDEN.")
            .translation("rpghealthbar.config.hungerMode")
            .defineEnum("hungerMode", HalfBarMode.VANILLA);

    public static final ModConfigSpec.EnumValue<HealthUnit> HEALTH_UNIT = BUILDER
            .comment("Unit for the numbers on the health bar: HP (20 for a full vanilla bar) or HEARTS (HP / 2, so 10).")
            .translation("rpghealthbar.config.healthUnit")
            .defineEnum("healthUnit", HealthUnit.HP);

    public static final ModConfigSpec.EnumValue<AbsorptionDisplay> ABSORPTION_DISPLAY = BUILDER
            .comment("How absorption is written on the health bar: SEPARATE (16+4/20) or COMBINED (20/20).")
            .translation("rpghealthbar.config.absorptionDisplay")
            .defineEnum("absorptionDisplay", AbsorptionDisplay.SEPARATE);

    // Position: set by dragging the HUD in Config → Move HUD; not meant to be edited by hand.
    // The HUD's matching point (e.g. its bottom-center for BOTTOM_CENTER) is placed at the anchor's
    // screen point plus the offset. The default sits just above the hotbar.
    public static final int DEFAULT_OFFSET_X = 0;
    public static final int DEFAULT_OFFSET_Y = -24;

    public static final ModConfigSpec.EnumValue<HudAnchor> ANCHOR = BUILDER
            .comment("Screen spot the HUD position is measured from; picked automatically when saving in Config → Move HUD.")
            .translation("rpghealthbar.config.anchor")
            .defineEnum("anchor", HudAnchor.BOTTOM_CENTER);

    public static final ModConfigSpec.IntValue OFFSET_X = BUILDER
            .comment("Horizontal offset of the HUD from its anchor, in GUI pixels. Set with Config → Move HUD.")
            .translation("rpghealthbar.config.offsetX")
            .defineInRange("offsetX", DEFAULT_OFFSET_X, -10000, 10000);

    public static final ModConfigSpec.IntValue OFFSET_Y = BUILDER
            .comment("Vertical offset of the HUD from its anchor, in GUI pixels. Set with Config → Move HUD.")
            .translation("rpghealthbar.config.offsetY")
            .defineInRange("offsetY", DEFAULT_OFFSET_Y, -10000, 10000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }

    /** Saves the HUD position and writes the config file. */
    public static void setPosition(HudAnchor anchor, int offsetX, int offsetY) {
        ANCHOR.set(anchor);
        OFFSET_X.set(offsetX);
        OFFSET_Y.set(offsetY);
        SPEC.save();
    }

    public static void resetPosition() {
        setPosition(HudAnchor.BOTTOM_CENTER, DEFAULT_OFFSET_X, DEFAULT_OFFSET_Y);
    }

    /** Updates a value and writes the config file. */
    public static <T> void set(ModConfigSpec.ConfigValue<T> value, T newValue) {
        value.set(newValue);
        value.save();
    }
}
