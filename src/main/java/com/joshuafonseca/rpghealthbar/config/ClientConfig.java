package com.joshuafonseca.rpghealthbar.config;

import java.util.EnumMap;
import java.util.Map;
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

    public static final ModConfigSpec.BooleanValue SHOW_HEALTH_TEXT = BUILDER
            .comment("Show the numbers on the health bar.")
            .translation("rpghealthbar.config.showHealthText")
            .define("showHealthText", true);

    public static final ModConfigSpec.BooleanValue SHOW_MANA_TEXT = BUILDER
            .comment("Show the numbers on the mana bar (Iron's Spells 'n Spellbooks only).")
            .translation("rpghealthbar.config.showManaText")
            .define("showManaText", true);

    // Position: set by dragging the HUD in Config → Move HUD; not meant to be edited by hand.
    // Layout 2 (1.2.1 on): each piece has its own anchor and offset (below).
    // Layout 1 (before): the whole HUD's matching point (e.g. its bottom-center for BOTTOM_CENTER)
    // is placed at the anchor's screen point plus the offset, and each piece is shifted from there.
    // Layout 1 is still read, and saving in Move HUD switches to layout 2.
    public static final int DEFAULT_OFFSET_X = 0;
    public static final int DEFAULT_OFFSET_Y = -24;

    public static final ModConfigSpec.IntValue LAYOUT_VERSION = BUILDER
            .comment("How the HUD position is stored: 1 = one anchor for the whole HUD (anchor, offsetX, offsetY), 2 = an anchor per piece. Set automatically.")
            .translation("rpghealthbar.config.layoutVersion")
            .defineInRange("layoutVersion", 1, 1, 2);

    public static final ModConfigSpec.EnumValue<HudAnchor> ANCHOR = BUILDER
            .comment("Layout 1 only: screen spot the HUD position is measured from.")
            .translation("rpghealthbar.config.anchor")
            .defineEnum("anchor", HudAnchor.BOTTOM_CENTER);

    public static final ModConfigSpec.IntValue OFFSET_X = BUILDER
            .comment("Layout 1 only: horizontal offset of the HUD from its anchor, in GUI pixels.")
            .translation("rpghealthbar.config.offsetX")
            .defineInRange("offsetX", DEFAULT_OFFSET_X, -10000, 10000);

    public static final ModConfigSpec.IntValue OFFSET_Y = BUILDER
            .comment("Layout 1 only: vertical offset of the HUD from its anchor, in GUI pixels.")
            .translation("rpghealthbar.config.offsetY")
            .defineInRange("offsetY", DEFAULT_OFFSET_Y, -10000, 10000);

    /**
     * Layout 2: each piece's anchor, and its shift from its default spot above the hotbar, in GUI
     * pixels, for a screen where the anchor is at the bottom-center; when the screen size changes,
     * the piece keeps its distance from its anchor. Layout 1: only the shifts are used, on top of
     * the whole HUD's position. Set with Config → Move HUD.
     */
    private static final Map<HudPiece, ModConfigSpec.EnumValue<HudAnchor>> PIECE_ANCHORS = new EnumMap<>(HudPiece.class);
    private static final Map<HudPiece, ModConfigSpec.IntValue> PIECE_OFFSETS_X = new EnumMap<>(HudPiece.class);
    private static final Map<HudPiece, ModConfigSpec.IntValue> PIECE_OFFSETS_Y = new EnumMap<>(HudPiece.class);

    static {
        for (HudPiece piece : HudPiece.values()) {
            PIECE_ANCHORS.put(piece, BUILDER
                    .comment("Layout 2: screen spot the " + piece.key() + " piece's position is measured from.")
                    .translation("rpghealthbar.config." + piece.key() + "Anchor")
                    .defineEnum(piece.key() + "Anchor", HudAnchor.BOTTOM_CENTER));
            PIECE_OFFSETS_X.put(piece, BUILDER
                    .comment("Horizontal shift of the " + piece.key() + " piece from its default spot, in GUI pixels.")
                    .translation("rpghealthbar.config." + piece.key() + "OffsetX")
                    .defineInRange(piece.key() + "OffsetX", 0, -10000, 10000));
            PIECE_OFFSETS_Y.put(piece, BUILDER
                    .comment("Vertical shift of the " + piece.key() + " piece from its default spot, in GUI pixels.")
                    .translation("rpghealthbar.config." + piece.key() + "OffsetY")
                    .defineInRange(piece.key() + "OffsetY", 0, -10000, 10000));
        }
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }

    /** Moves every piece back to its default spot above the hotbar, and writes the config file. */
    public static void resetPosition() {
        for (HudPiece piece : HudPiece.values()) {
            setPiecePosition(piece, HudAnchor.BOTTOM_CENTER, 0, 0);
        }
        ANCHOR.set(HudAnchor.BOTTOM_CENTER);
        OFFSET_X.set(DEFAULT_OFFSET_X);
        OFFSET_Y.set(DEFAULT_OFFSET_Y);
        savePositions();
    }

    public static HudAnchor pieceAnchor(HudPiece piece) {
        return PIECE_ANCHORS.get(piece).get();
    }

    public static int pieceOffsetX(HudPiece piece) {
        return PIECE_OFFSETS_X.get(piece).get();
    }

    public static int pieceOffsetY(HudPiece piece) {
        return PIECE_OFFSETS_Y.get(piece).get();
    }

    /** Updates a piece's position; written to the file by {@link #savePositions}. */
    public static void setPiecePosition(HudPiece piece, HudAnchor anchor, int x, int y) {
        PIECE_ANCHORS.get(piece).set(anchor);
        PIECE_OFFSETS_X.get(piece).set(x);
        PIECE_OFFSETS_Y.get(piece).set(y);
    }

    /** Marks the positions as layout 2 and writes the config file. */
    public static void savePositions() {
        LAYOUT_VERSION.set(2);
        SPEC.save();
    }

    /** Updates a value and writes the config file. */
    public static <T> void set(ModConfigSpec.ConfigValue<T> value, T newValue) {
        value.set(newValue);
        value.save();
    }
}
