package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.BarSize;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.EditMode;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * How wide the health, mana and XP bars are. In Simple edit mode, the presets (full or half); in
 * Advanced, the widths set by dragging the bars' ends in the HUD Editor, where a bar not resized yet
 * keeps its preset.
 * <p>
 * Dragging a bar's left end also moves where it starts ({@link #left}). That's kept with the width,
 * not in the bar's position, so with the preset back (Simple mode) the bar is where it was.
 * <p>
 * The XP bar is measured in cells, like vanilla's: two halves, each a 1px border and 10px cells
 * (9px plus a 1px divider). Vanilla's 18 cells make it 182px, as wide as the hotbar.
 */
public final class BarWidths {
    /** The narrowest the health and mana bars can be made. */
    public static final int MIN_WIDTH = 20;
    public static final int XP_CELL_WIDTH = 10;
    public static final int VANILLA_XP_CELLS = 18;
    /** One cell in each half. */
    public static final int MIN_XP_CELLS = 2;

    /** A bar's custom width (cells for the XP bar; 0 for the preset) and how far left its left end was dragged. */
    public record Size(int width, int left) {
        public static final Size PRESET = new Size(0, 0);
    }

    /**
     * Where each resizable bar's size is stored: its custom width (cells for the XP bar), how far
     * left its left end was dragged, and its Simple mode preset (none for the XP bar).
     */
    private record Settings(ModConfigSpec.IntValue width, ModConfigSpec.IntValue left, @Nullable ModConfigSpec.EnumValue<BarSize> preset) {
    }

    private static final Map<HudPiece, Settings> SETTINGS = new EnumMap<>(Map.of(
            HudPiece.HEALTH, new Settings(ClientConfig.HEALTH_WIDTH, ClientConfig.HEALTH_LEFT, ClientConfig.HEALTH_SIZE),
            HudPiece.MANA, new Settings(ClientConfig.MANA_WIDTH, ClientConfig.MANA_LEFT, ClientConfig.MANA_SIZE),
            HudPiece.CONFLUENCE_MANA, new Settings(ClientConfig.CONFLUENCE_MANA_WIDTH, ClientConfig.CONFLUENCE_MANA_LEFT, ClientConfig.CONFLUENCE_MANA_SIZE),
            HudPiece.XP, new Settings(ClientConfig.XP_CELLS, ClientConfig.XP_LEFT, null)));

    /** The sizes while the HUD Editor is open, so the HUD follows the drag; null otherwise. */
    @Nullable
    private static Map<HudPiece, Size> preview;

    private BarWidths() {
    }

    /** The pieces whose ends can be dragged in Advanced mode. */
    public static boolean resizable(HudPiece piece) {
        return SETTINGS.containsKey(piece);
    }

    public static boolean advanced() {
        return ClientConfig.EDIT_MODE.get() == EditMode.ADVANCED;
    }

    /** The bar's width in GUI pixels. */
    public static int width(HudPiece piece) {
        if (piece == HudPiece.XP) {
            return xpWidth(xpCells());
        }
        int custom = advanced() ? custom(piece) : 0;
        return custom > 0 ? custom : HudLayout.width(preset(piece));
    }

    /** How many cells long the XP bar is. */
    public static int xpCells() {
        int custom = advanced() ? custom(HudPiece.XP) : 0;
        return custom > 0 ? custom : VANILLA_XP_CELLS;
    }

    /** Whether the XP bar isn't vanilla's length or spot, so the mod draws it. */
    public static boolean customXp() {
        return xpCells() != VANILLA_XP_CELLS || left(HudPiece.XP) != 0;
    }

    /** How far left of its usual spot the bar starts, from dragging its left end; 0 with the preset width. */
    public static int left(HudPiece piece) {
        if (!advanced()) {
            return 0;
        }
        Size size = size(piece);
        return size.width() > 0 ? size.left() : 0;
    }

    public static int xpWidth(int cells) {
        return 2 + cells * XP_CELL_WIDTH;
    }

    /** The stored width (cells for the XP bar), or 0 for the preset. */
    private static int custom(HudPiece piece) {
        return size(piece).width();
    }

    /** The stored size, or the editor's while it's open. */
    private static Size size(HudPiece piece) {
        if (preview != null) {
            return preview.getOrDefault(piece, Size.PRESET);
        }
        Settings settings = SETTINGS.get(piece);
        return settings == null ? Size.PRESET : new Size(settings.width().get(), settings.left().get());
    }

    /** The stored sizes, for the HUD Editor to start from. */
    public static Map<HudPiece, Size> saved() {
        Map<HudPiece, Size> sizes = new EnumMap<>(HudPiece.class);
        for (HudPiece piece : SETTINGS.keySet()) {
            sizes.put(piece, size(piece));
        }
        return sizes;
    }

    public static void setPreview(@Nullable Map<HudPiece, Size> sizes) {
        preview = sizes;
    }

    /** Stores the sizes; written to the file with the positions. */
    public static void save(Map<HudPiece, Size> sizes) {
        sizes.forEach((piece, size) -> {
            Settings settings = SETTINGS.get(piece);
            if (settings != null) {
                settings.width().set(size.width());
                settings.left().set(size.left());
            }
        });
    }

    private static BarSize preset(HudPiece piece) {
        return SETTINGS.get(piece).preset().get();
    }
}
