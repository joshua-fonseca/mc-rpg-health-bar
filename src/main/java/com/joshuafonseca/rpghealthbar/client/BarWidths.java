package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.compat.ManaSource;
import com.joshuafonseca.rpghealthbar.config.BarSize;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.EditMode;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * How wide the health, mana and XP bars are, and how tall the upright hunger and thirst bars. In
 * Simple edit mode, the presets (full or half; upright bars have one height); in Advanced, the sizes
 * set by dragging the bars' ends in the HUD Editor, where a bar not resized yet keeps its preset.
 * <p>
 * Dragging a bar's left end also moves where it starts ({@link #left}). That's kept with the width,
 * not in the bar's position, so with the preset back (Simple mode) the bar is where it was.
 * <p>
 * The XP bar is measured in cells, like vanilla's: 10px cells (9px plus a 1px divider) between a
 * 1px rounded end on each side. Vanilla's 18 cells make it 182px, as wide as the hotbar.
 */
public final class BarWidths {
    /** The narrowest the health and mana bars can be made. */
    public static final int MIN_WIDTH = 20;
    public static final int XP_CELL_WIDTH = 10;
    public static final int VANILLA_XP_CELLS = 18;
    /** One cell in each half. */
    public static final int MIN_XP_CELLS = 2;
    /** The shortest the upright bars can be made. */
    public static final int MIN_HEIGHT = 10;

    /**
     * A bar's custom width (cells for the XP bar, the height for an upright bar; 0 for the preset)
     * and how far left its left end was dragged.
     */
    public record Size(int width, int left) {
        public static final Size PRESET = new Size(0, 0);
    }

    /**
     * Where each resizable bar's size is stored: its custom width (cells for the XP bar, the height
     * for an upright bar), how far left its left end was dragged (not for upright bars), and its
     * Simple mode preset (none for the XP bar and upright bars).
     */
    private record Settings(ForgeConfigSpec.IntValue width, @Nullable ForgeConfigSpec.IntValue left, @Nullable ForgeConfigSpec.EnumValue<BarSize> preset) {
    }

    private static final Map<HudPiece, Settings> SETTINGS = settings();

    private static Map<HudPiece, Settings> settings() {
        Map<HudPiece, Settings> settings = new EnumMap<>(HudPiece.class);
        settings.put(HudPiece.HEALTH, new Settings(ClientConfig.HEALTH_WIDTH, ClientConfig.HEALTH_LEFT, ClientConfig.HEALTH_SIZE));
        for (ManaSource source : ManaSource.values()) {
            settings.put(source.piece(), new Settings(source.width(), source.left(), source.size()));
        }
        settings.put(HudPiece.XP, new Settings(ClientConfig.XP_CELLS, ClientConfig.XP_LEFT, null));
        settings.put(HudPiece.HUNGER_VERTICAL, new Settings(ClientConfig.HUNGER_VERTICAL_HEIGHT, null, null));
        settings.put(HudPiece.THIRST_VERTICAL, new Settings(ClientConfig.THIRST_VERTICAL_HEIGHT, null, null));
        return settings;
    }

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

    /** An upright bar's height in GUI pixels. */
    public static int height(HudPiece piece) {
        int custom = advanced() ? custom(piece) : 0;
        return custom > 0 ? custom : HudLayout.VERTICAL_BAR_HEIGHT;
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
        if (settings == null) {
            return Size.PRESET;
        }
        return new Size(ClientConfig.read(settings.width()), settings.left() == null ? 0 : ClientConfig.read(settings.left()));
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

    /** Stores the sizes; written to the file with the positions, over the next few ticks. */
    public static void save(Map<HudPiece, Size> sizes) {
        sizes.forEach((piece, size) -> {
            Settings settings = SETTINGS.get(piece);
            if (settings != null) {
                ClientConfig.queue(settings.width(), size.width());
                if (settings.left() != null) {
                    ClientConfig.queue(settings.left(), size.left());
                }
            }
        });
    }

    private static BarSize preset(HudPiece piece) {
        return SETTINGS.get(piece).preset().get();
    }
}
