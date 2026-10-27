package com.joshuafonseca.rpghealthbar.config;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;

/** Stored in config/rpghealthbar-client.toml. Values are read every frame, so changes apply immediately. */
public final class ClientConfig {
    /**
     * Position values waiting to be written, applied in memory straight away. On Forge 1.20.1 every
     * {@code set} writes the file and Forge then reloads it on another thread, so a reload landing
     * between two quick sets undoes the earlier one. Writing one value per tick ({@link #writePending})
     * lets each reload finish first.
     */
    private static final Map<ForgeConfigSpec.ConfigValue<?>, Object> PENDING = new LinkedHashMap<>();

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.EnumValue<HalfBarMode> ARMOR_MODE = BUILDER
            .comment("How armor is shown: VANILLA (vanilla armor icons), BAR (white bar above the health bar) or HIDDEN.")
            .translation("rpghealthbar.config.armorMode")
            .defineEnum("armorMode", HalfBarMode.VANILLA);

    public static final ForgeConfigSpec.EnumValue<HalfBarMode> HUNGER_MODE = BUILDER
            .comment("How hunger is shown: VANILLA (vanilla hunger icons), BAR (orange bar above the health bar) or HIDDEN.")
            .translation("rpghealthbar.config.hungerMode")
            .defineEnum("hungerMode", HalfBarMode.VANILLA);

    public static final ForgeConfigSpec.EnumValue<HealthUnit> HEALTH_UNIT = BUILDER
            .comment("Unit for the numbers on the health bar: HP (20 for a full vanilla bar) or HEARTS (HP / 2, so 10).")
            .translation("rpghealthbar.config.healthUnit")
            .defineEnum("healthUnit", HealthUnit.HP);

    public static final ForgeConfigSpec.EnumValue<AbsorptionDisplay> ABSORPTION_DISPLAY = BUILDER
            .comment("How absorption is written on the health bar: SEPARATE (16+4/20) or COMBINED (20/20).")
            .translation("rpghealthbar.config.absorptionDisplay")
            .defineEnum("absorptionDisplay", AbsorptionDisplay.SEPARATE);

    public static final ForgeConfigSpec.BooleanValue SHOW_HEALTH_TEXT = BUILDER
            .comment("Show the numbers on the health bar.")
            .translation("rpghealthbar.config.showHealthText")
            .define("showHealthText", true);

    public static final ForgeConfigSpec.BooleanValue SHOW_MANA_TEXT = BUILDER
            .comment("Show the numbers on the mana bar (Iron's Spells 'n Spellbooks only).")
            .translation("rpghealthbar.config.showManaText")
            .define("showManaText", true);

    public static final ForgeConfigSpec.EnumValue<BarSize> HEALTH_SIZE = BUILDER
            .comment("Width of the health bar: FULL (as wide as the hotbar) or HALF (like the armor and hunger bars).")
            .translation("rpghealthbar.config.healthSize")
            .defineEnum("healthSize", BarSize.FULL);

    public static final ForgeConfigSpec.EnumValue<BarSize> MANA_SIZE = BUILDER
            .comment("Width of the mana bar: FULL or HALF. With both bars HALF, they share a row, health on the left (Iron's Spells 'n Spellbooks only).")
            .translation("rpghealthbar.config.manaSize")
            .defineEnum("manaSize", BarSize.FULL);

    public static final int ITEM_NAME_OFFSET_MIN = -50;
    public static final int ITEM_NAME_OFFSET_MAX = 100;

    public static final ForgeConfigSpec.IntValue ITEM_NAME_OFFSET = BUILDER
            .comment("How far up (positive) or down (negative) the held item's name and action bar messages sit from where vanilla puts them, in GUI pixels, when the HUD isn't stacked right above the hotbar. 0 is vanilla's height, which leaves room for mods that draw above the hotbar; lower values bring it down to right above the hotbar.")
            .translation("rpghealthbar.config.itemNameOffset")
            .defineInRange("itemNameOffset", 0, ITEM_NAME_OFFSET_MIN, ITEM_NAME_OFFSET_MAX);

    // Position: set by dragging the HUD in Config → Move HUD; not meant to be edited by hand.
    // Layout 2 (1.2.1 on): each piece has its own anchor and offset (below).
    // Layout 1 (before): the whole HUD's matching point (e.g. its bottom-center for BOTTOM_CENTER)
    // is placed at the anchor's screen point plus the offset, and each piece is shifted from there.
    // Layout 1 is still read, and saving in Move HUD switches to layout 2.
    public static final int DEFAULT_OFFSET_X = 0;
    public static final int DEFAULT_OFFSET_Y = -24;

    public static final ForgeConfigSpec.IntValue LAYOUT_VERSION = BUILDER
            .comment("How the HUD position is stored: 1 = one anchor for the whole HUD (anchor, offsetX, offsetY), 2 = an anchor per piece. Set automatically.")
            .translation("rpghealthbar.config.layoutVersion")
            .defineInRange("layoutVersion", 1, 1, 2);

    public static final ForgeConfigSpec.EnumValue<HudAnchor> ANCHOR = BUILDER
            .comment("Layout 1 only: screen spot the HUD position is measured from.")
            .translation("rpghealthbar.config.anchor")
            .defineEnum("anchor", HudAnchor.BOTTOM_CENTER);

    public static final ForgeConfigSpec.IntValue OFFSET_X = BUILDER
            .comment("Layout 1 only: horizontal offset of the HUD from its anchor, in GUI pixels.")
            .translation("rpghealthbar.config.offsetX")
            .defineInRange("offsetX", DEFAULT_OFFSET_X, -10000, 10000);

    public static final ForgeConfigSpec.IntValue OFFSET_Y = BUILDER
            .comment("Layout 1 only: vertical offset of the HUD from its anchor, in GUI pixels.")
            .translation("rpghealthbar.config.offsetY")
            .defineInRange("offsetY", DEFAULT_OFFSET_Y, -10000, 10000);

    /**
     * Layout 2: each piece's anchor, and its shift from its default spot above the hotbar, in GUI
     * pixels, for a screen where the anchor is at the bottom-center; when the screen size changes,
     * the piece keeps its distance from its anchor. Layout 1: only the shifts are used, on top of
     * the whole HUD's position. Set with Config → Move HUD.
     */
    private static final Map<HudPiece, ForgeConfigSpec.EnumValue<HudAnchor>> PIECE_ANCHORS = new EnumMap<>(HudPiece.class);
    private static final Map<HudPiece, ForgeConfigSpec.IntValue> PIECE_OFFSETS_X = new EnumMap<>(HudPiece.class);
    private static final Map<HudPiece, ForgeConfigSpec.IntValue> PIECE_OFFSETS_Y = new EnumMap<>(HudPiece.class);

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

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }

    /** Moves every piece back to its default spot above the hotbar, and writes the config file. */
    public static void resetPosition() {
        for (HudPiece piece : HudPiece.values()) {
            setPiecePosition(piece, HudAnchor.BOTTOM_CENTER, 0, 0);
        }
        queue(ANCHOR, HudAnchor.BOTTOM_CENTER);
        queue(OFFSET_X, DEFAULT_OFFSET_X);
        queue(OFFSET_Y, DEFAULT_OFFSET_Y);
        savePositions();
    }

    public static int layoutVersion() {
        return read(LAYOUT_VERSION);
    }

    public static HudAnchor pieceAnchor(HudPiece piece) {
        return read(PIECE_ANCHORS.get(piece));
    }

    public static int pieceOffsetX(HudPiece piece) {
        return read(PIECE_OFFSETS_X.get(piece));
    }

    public static int pieceOffsetY(HudPiece piece) {
        return read(PIECE_OFFSETS_Y.get(piece));
    }

    /** Updates a piece's position; written to the file by {@link #savePositions}. */
    public static void setPiecePosition(HudPiece piece, HudAnchor anchor, int x, int y) {
        queue(PIECE_ANCHORS.get(piece), anchor);
        queue(PIECE_OFFSETS_X.get(piece), x);
        queue(PIECE_OFFSETS_Y.get(piece), y);
    }

    /** Marks the positions as layout 2; they're written to the file over the next few ticks. */
    public static void savePositions() {
        queue(LAYOUT_VERSION, 2);
    }

    /** Updates a value; Forge writes the config file on every change. */
    public static <T> void set(ForgeConfigSpec.ConfigValue<T> value, T newValue) {
        value.set(newValue);
    }

    /** Writes one waiting position value; called every client tick. */
    public static void writePending() {
        Iterator<Map.Entry<ForgeConfigSpec.ConfigValue<?>, Object>> it = PENDING.entrySet().iterator();
        if (it.hasNext()) {
            Map.Entry<ForgeConfigSpec.ConfigValue<?>, Object> entry = it.next();
            it.remove();
            write(entry.getKey(), entry.getValue());
        }
    }

    /** Writes every waiting value at once, when the game closes. */
    public static void writeAllPending() {
        while (!PENDING.isEmpty()) {
            writePending();
        }
    }

    private static <T> void queue(ForgeConfigSpec.ConfigValue<T> value, T newValue) {
        // Re-inserted, so a value queued again is written after the others queued with it.
        PENDING.remove(value);
        PENDING.put(value, newValue);
    }

    @SuppressWarnings("unchecked")
    private static <T> T read(ForgeConfigSpec.ConfigValue<T> value) {
        Object pending = PENDING.get(value);
        return pending != null ? (T) pending : value.get();
    }

    @SuppressWarnings("unchecked")
    private static <T> void write(ForgeConfigSpec.ConfigValue<T> value, Object newValue) {
        value.set((T) newValue);
    }
}
