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

    /** Widths (cells for the XP bar) while the HUD Editor is open, so the HUD follows the drag; null otherwise. */
    @Nullable
    private static Map<HudPiece, Integer> preview;

    private BarWidths() {
    }

    /** The pieces whose ends can be dragged in Advanced mode. */
    public static boolean resizable(HudPiece piece) {
        return piece == HudPiece.HEALTH || piece == HudPiece.MANA || piece == HudPiece.CONFLUENCE_MANA || piece == HudPiece.XP;
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

    /** Whether the XP bar isn't vanilla's length, so the mod draws it. */
    public static boolean customXp() {
        return xpCells() != VANILLA_XP_CELLS;
    }

    public static int xpWidth(int cells) {
        return 2 + cells * XP_CELL_WIDTH;
    }

    /** The stored width (cells for the XP bar), or 0 for the preset; the editor's while it's open. */
    public static int custom(HudPiece piece) {
        if (preview != null) {
            return preview.getOrDefault(piece, 0);
        }
        ModConfigSpec.IntValue value = config(piece);
        return value == null ? 0 : value.get();
    }

    /** The stored widths, for the HUD Editor to start from. */
    public static Map<HudPiece, Integer> saved() {
        Map<HudPiece, Integer> widths = new EnumMap<>(HudPiece.class);
        for (HudPiece piece : HudPiece.values()) {
            ModConfigSpec.IntValue value = config(piece);
            if (value != null) {
                widths.put(piece, value.get());
            }
        }
        return widths;
    }

    public static void setPreview(@Nullable Map<HudPiece, Integer> widths) {
        preview = widths;
    }

    /** Stores the widths; written to the file with the positions. */
    public static void save(Map<HudPiece, Integer> widths) {
        widths.forEach((piece, width) -> {
            ModConfigSpec.IntValue value = config(piece);
            if (value != null) {
                value.set(width);
            }
        });
    }

    private static BarSize preset(HudPiece piece) {
        return switch (piece) {
            case MANA -> ClientConfig.MANA_SIZE.get();
            case CONFLUENCE_MANA -> ClientConfig.CONFLUENCE_MANA_SIZE.get();
            default -> ClientConfig.HEALTH_SIZE.get();
        };
    }

    @Nullable
    private static ModConfigSpec.IntValue config(HudPiece piece) {
        return switch (piece) {
            case HEALTH -> ClientConfig.HEALTH_WIDTH;
            case MANA -> ClientConfig.MANA_WIDTH;
            case CONFLUENCE_MANA -> ClientConfig.CONFLUENCE_MANA_WIDTH;
            case XP -> ClientConfig.XP_CELLS;
            default -> null;
        };
    }
}
