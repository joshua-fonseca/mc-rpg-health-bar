package com.joshuafonseca.rpghealthbar.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import java.util.EnumMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Stored in config/rpghealthbar-client.toml. Values are read every frame, so changes apply immediately. */
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Turns the mod on or off. Off, the HUD is fully vanilla (including Iron's own mana bar); the other settings are kept.")
            .translation("rpghealthbar.config.enabled")
            .define("enabled", true);

    public static final ModConfigSpec.BooleanValue SHOW_PREVIEW = BUILDER
            .comment("Whether the config screen shows a preview of the HUD beside its options. Set with the eye button by the screen's title.")
            .translation("rpghealthbar.config.preview")
            .define("showPreview", false);

    public static final ModConfigSpec.EnumValue<HalfBarMode> ARMOR_MODE = BUILDER
            .comment("How armor is shown: VANILLA (shown as Default: the armor icons, or another mod's armor display), BAR (white bar above the health bar) or HIDDEN.")
            .translation("rpghealthbar.config.armorMode")
            .defineEnum("armorMode", HalfBarMode.VANILLA, HalfBarMode.VANILLA, HalfBarMode.BAR, HalfBarMode.HIDDEN);

    public static final ModConfigSpec.EnumValue<HalfBarMode> HUNGER_MODE = BUILDER
            .comment("How hunger is shown: VANILLA (shown as Default: the hunger icons, or another mod's hunger display), BAR (orange bar above the health bar), VERTICAL (upright orange bar beside the hotbar) or HIDDEN.")
            .translation("rpghealthbar.config.hungerMode")
            .defineEnum("hungerMode", HalfBarMode.VANILLA);

    public static final ModConfigSpec.EnumValue<ThirstMode> THIRST_MODE = BUILDER
            .comment("How Thirst Was Taken 2's thirst is shown: VANILLA (shown as Default: its droplet icons), BAR (blue bar above the hunger bar), VERTICAL (upright blue bar beside the hotbar) or HIDDEN.")
            .translation("rpghealthbar.config.thirstMode")
            .defineEnum("thirstMode", ThirstMode.VANILLA);

    public static final ModConfigSpec.EnumValue<AirPosition> AIR_POSITION = BUILDER
            .comment("Where the air bubbles and mount health rows go: ABOVE hunger (stacked upward, like vanilla) or BELOW it (stacked downward).")
            .translation("rpghealthbar.config.airPosition")
            .defineEnum("airPosition", AirPosition.ABOVE);

    public static final ModConfigSpec.EnumValue<HealthUnit> HEALTH_UNIT = BUILDER
            .comment("Unit for the numbers on the health bar: HP (20 for a full vanilla bar) or HEARTS (HP / 2, so 10).")
            .translation("rpghealthbar.config.healthUnit")
            .defineEnum("healthUnit", HealthUnit.HP);

    public static final ModConfigSpec.EnumValue<AbsorptionDisplay> ABSORPTION_DISPLAY = BUILDER
            .comment("How absorption is written on the health bar: SEPARATE (16+4/20), COMBINED (20/20) or SEPARATE_RIGHT (16/20, with +4 at the bar's right end).")
            .translation("rpghealthbar.config.absorptionDisplay")
            .defineEnum("absorptionDisplay", AbsorptionDisplay.SEPARATE);

    public static final ModConfigSpec.EnumValue<BarText> HEALTH_TEXT = BUILDER
            .comment("Where the numbers go on the health bar: LEFT, RIGHT, CENTER or OFF.")
            .translation("rpghealthbar.config.healthText")
            .defineEnum("healthText", BarText.LEFT);

    public static final ModConfigSpec.EnumValue<BarText> MANA_TEXT = BUILDER
            .comment("Where the numbers go on Iron's mana bar: LEFT, RIGHT, CENTER or OFF.")
            .translation("rpghealthbar.config.manaText")
            .defineEnum("manaText", BarText.LEFT);

    // Before healthText and manaText: whether the numbers were shown. False still turns them off,
    // until the text is set in the config screen.
    public static final ModConfigSpec.BooleanValue SHOW_HEALTH_TEXT = BUILDER
            .comment("Replaced by healthText; false turns the numbers off, as before.")
            .define("showHealthText", true);

    public static final ModConfigSpec.BooleanValue SHOW_MANA_TEXT = BUILDER
            .comment("Replaced by manaText; false turns the numbers off, as before.")
            .define("showManaText", true);

    public static final ModConfigSpec.EnumValue<BarSize> HEALTH_SIZE = BUILDER
            .comment("Width of the health bar: FULL (as wide as the hotbar) or HALF (like the armor and hunger bars).")
            .translation("rpghealthbar.config.healthSize")
            .defineEnum("healthSize", BarSize.FULL);

    public static final ModConfigSpec.EnumValue<BarSize> MANA_SIZE = BUILDER
            .comment("Width of Iron's mana bar: FULL (as wide as the hotbar) or HALF (like the armor and hunger bars).")
            .translation("rpghealthbar.config.manaSize")
            .defineEnum("manaSize", BarSize.FULL);

    public static final ModConfigSpec.EnumValue<LevelText> LEVEL_TEXT = BUILDER
            .comment("Where the XP level's number goes on the XP bar: CENTER (like vanilla), LEFT or RIGHT.")
            .translation("rpghealthbar.config.levelText")
            .defineEnum("levelText", LevelText.CENTER);

    public static final ModConfigSpec.BooleanValue XP_BAR_SHOWN = BUILDER
            .comment("Whether the XP bar and its level number are shown. The jump bar still shows while riding.")
            .translation("rpghealthbar.config.xpBar")
            .define("xpBarShown", true);

    public static final ModConfigSpec.EnumValue<BarText> CONFLUENCE_MANA_TEXT = BUILDER
            .comment("Where the numbers go on Confluence's mana bar: LEFT, RIGHT, CENTER or OFF.")
            .translation("rpghealthbar.config.confluenceManaText")
            .defineEnum("confluenceManaText", BarText.LEFT);

    public static final ModConfigSpec.EnumValue<BarSize> CONFLUENCE_MANA_SIZE = BUILDER
            .comment("Width of Confluence's mana bar: FULL (as wide as the hotbar) or HALF (like the armor and hunger bars).")
            .translation("rpghealthbar.config.confluenceManaSize")
            .defineEnum("confluenceManaSize", BarSize.FULL);

    public static final ModConfigSpec.EnumValue<BarText> ARS_MANA_TEXT = BUILDER
            .comment("Where the numbers go on Ars's mana bar: LEFT, RIGHT, CENTER or OFF.")
            .translation("rpghealthbar.config.arsManaText")
            .defineEnum("arsManaText", BarText.LEFT);

    public static final ModConfigSpec.EnumValue<BarSize> ARS_MANA_SIZE = BUILDER
            .comment("Width of Ars's mana bar: FULL (as wide as the hotbar) or HALF (like the armor and hunger bars).")
            .translation("rpghealthbar.config.arsManaSize")
            .defineEnum("arsManaSize", BarSize.FULL);

    public static final ModConfigSpec.BooleanValue ARS_MANA_BUBBLES = BUILDER
            .comment("Whether bubbles rise through Ars's mana bar, like on Ars's own bar.")
            .translation("rpghealthbar.config.arsManaBubbles")
            .define("arsManaBubbles", true);

    public static final ModConfigSpec.EnumValue<EditMode> EDIT_MODE = BUILDER
            .comment("SIMPLE: the bars' widths are the presets above (healthSize, manaSize, confluenceManaSize, arsManaSize). ADVANCED: the widths and heights below, set by dragging the bars' ends in Config → HUD Editor.")
            .translation("rpghealthbar.config.editMode")
            .defineEnum("editMode", EditMode.SIMPLE);

    // Advanced widths: set in the HUD Editor. 0 keeps the preset (or vanilla's 18 cells for the XP bar).
    public static final ModConfigSpec.IntValue HEALTH_WIDTH = BUILDER
            .comment("Advanced only: width of the health bar in GUI pixels; 0 uses healthSize.")
            .translation("rpghealthbar.config.healthWidth")
            .defineInRange("healthWidth", 0, 0, 10000);

    public static final ModConfigSpec.IntValue MANA_WIDTH = BUILDER
            .comment("Advanced only: width of Iron's mana bar in GUI pixels; 0 uses manaSize.")
            .translation("rpghealthbar.config.manaWidth")
            .defineInRange("manaWidth", 0, 0, 10000);

    public static final ModConfigSpec.IntValue CONFLUENCE_MANA_WIDTH = BUILDER
            .comment("Advanced only: width of Confluence's mana bar in GUI pixels; 0 uses confluenceManaSize.")
            .translation("rpghealthbar.config.confluenceManaWidth")
            .defineInRange("confluenceManaWidth", 0, 0, 10000);

    public static final ModConfigSpec.IntValue ARS_MANA_WIDTH = BUILDER
            .comment("Advanced only: width of Ars's mana bar in GUI pixels; 0 uses arsManaSize.")
            .translation("rpghealthbar.config.arsManaWidth")
            .defineInRange("arsManaWidth", 0, 0, 10000);

    public static final ModConfigSpec.IntValue XP_CELLS = BUILDER
            .comment("Advanced only: how many cells long the XP bar is (vanilla's has 18); 0 keeps vanilla's.")
            .translation("rpghealthbar.config.xpCells")
            .defineInRange("xpCells", 0, 0, 1000);

    // How far each bar's left end was dragged out (negative: in), in GUI pixels; applied with the
    // width above, so Simple mode puts the bar back where it was.
    public static final ModConfigSpec.IntValue HEALTH_LEFT = BUILDER
            .comment("Advanced only: how far left of its usual spot the health bar starts, from dragging its left end in the HUD Editor.")
            .translation("rpghealthbar.config.healthLeft")
            .defineInRange("healthLeft", 0, -10000, 10000);

    public static final ModConfigSpec.IntValue MANA_LEFT = BUILDER
            .comment("Advanced only: how far left of its usual spot Iron's mana bar starts, from dragging its left end in the HUD Editor.")
            .translation("rpghealthbar.config.manaLeft")
            .defineInRange("manaLeft", 0, -10000, 10000);

    public static final ModConfigSpec.IntValue CONFLUENCE_MANA_LEFT = BUILDER
            .comment("Advanced only: how far left of its usual spot Confluence's mana bar starts, from dragging its left end in the HUD Editor.")
            .translation("rpghealthbar.config.confluenceManaLeft")
            .defineInRange("confluenceManaLeft", 0, -10000, 10000);

    public static final ModConfigSpec.IntValue ARS_MANA_LEFT = BUILDER
            .comment("Advanced only: how far left of its usual spot Ars's mana bar starts, from dragging its left end in the HUD Editor.")
            .translation("rpghealthbar.config.arsManaLeft")
            .defineInRange("arsManaLeft", 0, -10000, 10000);

    public static final ModConfigSpec.IntValue XP_LEFT = BUILDER
            .comment("Advanced only: how far left of its usual spot the XP bar starts, from dragging its left end in the HUD Editor.")
            .translation("rpghealthbar.config.xpLeft")
            .defineInRange("xpLeft", 0, -10000, 10000);

    public static final ModConfigSpec.IntValue HUNGER_VERTICAL_HEIGHT = BUILDER
            .comment("Advanced only: height of the vertical hunger bar in GUI pixels; 0 keeps the usual 33.")
            .translation("rpghealthbar.config.hungerVerticalHeight")
            .defineInRange("hungerVerticalHeight", 0, 0, 10000);

    public static final ModConfigSpec.IntValue THIRST_VERTICAL_HEIGHT = BUILDER
            .comment("Advanced only: height of the vertical thirst bar in GUI pixels; 0 keeps the usual 33.")
            .translation("rpghealthbar.config.thirstVerticalHeight")
            .defineInRange("thirstVerticalHeight", 0, 0, 10000);

    public static final int ITEM_NAME_OFFSET_MIN = -50;
    public static final int ITEM_NAME_OFFSET_MAX = 100;

    public static final ModConfigSpec.IntValue ITEM_NAME_OFFSET = BUILDER
            .comment("How far up (positive) or down (negative) the held item's name and action bar messages sit from where vanilla puts them, in GUI pixels, when the HUD isn't stacked right above the hotbar. 0 is vanilla's height, which leaves room for mods that draw above the hotbar; lower values bring it down to right above the hotbar.")
            .translation("rpghealthbar.config.itemNameOffset")
            .defineInRange("itemNameOffset", 0, ITEM_NAME_OFFSET_MIN, ITEM_NAME_OFFSET_MAX);

    // Position: set by dragging the HUD in Config → HUD Editor; not meant to be edited by hand.
    // Layout 2 (1.2.1 on): each piece has its own anchor and offset (below).
    // Layout 1 (before): the whole HUD's matching point (e.g. its bottom-center for BOTTOM_CENTER)
    // is placed at the anchor's screen point plus the offset, and each piece is shifted from there.
    // Layout 1 is still read, and saving in HUD Editor switches to layout 2.
    private static final int DEFAULT_OFFSET_X = 0;
    private static final int DEFAULT_OFFSET_Y = -24;

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
     * the whole HUD's position. Set with Config → HUD Editor.
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

    /** Where the health bar's numbers go, honoring an Off saved before healthText existed. */
    public static BarText healthText() {
        return SHOW_HEALTH_TEXT.get() ? HEALTH_TEXT.get() : BarText.OFF;
    }

    /** Where Iron's mana bar's numbers go, honoring an Off saved before manaText existed. */
    public static BarText manaText() {
        return SHOW_MANA_TEXT.get() ? MANA_TEXT.get() : BarText.OFF;
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

    /**
     * Puts every setting back to its default, as on a fresh install, HUD position and item name
     * height included, and writes the config file. Whether the config screen shows the preview is kept.
     */
    public static void resetToDefaults() {
        resetToDefaults(SPEC.getValues());
        SPEC.save();
    }

    private static void resetToDefaults(UnmodifiableConfig values) {
        for (Object value : values.valueMap().values()) {
            if (value instanceof UnmodifiableConfig section) {
                resetToDefaults(section);
            } else if (value instanceof ModConfigSpec.ConfigValue<?> configValue && configValue != SHOW_PREVIEW) {
                setDefault(configValue);
            }
        }
    }

    private static <T> void setDefault(ModConfigSpec.ConfigValue<T> value) {
        value.set(value.getDefault());
    }

    /** Updates a value and writes the config file. */
    public static <T> void set(ModConfigSpec.ConfigValue<T> value, T newValue) {
        value.set(newValue);
        value.save();
    }
}
