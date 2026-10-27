package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HudAnchor;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Where the movable HUD pieces are drawn: our bars plus the vanilla elements that move with them
 * (XP bar, level, jump meter, vanilla armor/hunger icons and the rows stacked above them).
 * <p>
 * Every {@link HudPiece} is laid out at its default spot above the hotbar and then shifted by its
 * own {@link Offset} ({@link Pieces}). The saved position is stored differently: each piece
 * relative to the nearest {@link HudAnchor}, so a piece put in a corner stays in that corner, and
 * one left above the hotbar stays above the hotbar, when the window size or GUI scale changes.
 * Pieces that touch share one anchor, so they don't come apart.
 */
public final class HudPosition {
    /** The XP bar's bottom edge is this far above the bottom of the screen. */
    private static final int XP_BAR_BOTTOM_MARGIN = 24;
    private static final int XP_BAR_HEIGHT = 5;
    /** Pieces closer than this share an anchor when saved (the default rows are 2px apart). */
    private static final int TOUCHING_GAP = 4;

    /** Piece shifts while the position editor is open, so the real HUD follows the drag; null otherwise. */
    @Nullable
    private static Pieces preview;

    private HudPosition() {
    }

    public record Offset(int x, int y) {
        public static final Offset ZERO = new Offset(0, 0);

        public Offset plus(Offset other) {
            return new Offset(x + other.x, y + other.y);
        }

        public Offset minus(Offset other) {
            return new Offset(x - other.x, y - other.y);
        }
    }

    /** Each piece's shift from its default spot. */
    public record Pieces(Map<HudPiece, Offset> offsets) {
        public static final Pieces NONE = new Pieces(Map.of());

        public Offset get(HudPiece piece) {
            return offsets.getOrDefault(piece, Offset.ZERO);
        }

        public Pieces with(HudPiece piece, Offset offset) {
            Map<HudPiece, Offset> copy = new EnumMap<>(HudPiece.class);
            copy.putAll(offsets);
            copy.put(piece, offset);
            return new Pieces(copy);
        }

        /** Every piece, shown or not, moved by the same amount. */
        public Pieces shiftedBy(Offset delta) {
            Map<HudPiece, Offset> moved = new EnumMap<>(HudPiece.class);
            for (HudPiece piece : HudPiece.values()) {
                moved.put(piece, get(piece).plus(delta));
            }
            return new Pieces(moved);
        }
    }

    /** A box on screen, in GUI pixels; right and bottom are exclusive. */
    public record Bounds(int left, int top, int right, int bottom) {
        public Bounds offset(Offset offset) {
            return new Bounds(left + offset.x(), top + offset.y(), right + offset.x(), bottom + offset.y());
        }

        public Bounds grow(int amount) {
            return new Bounds(left - amount, top - amount, right + amount, bottom + amount);
        }

        public Bounds union(Bounds other) {
            return new Bounds(Math.min(left, other.left), Math.min(top, other.top), Math.max(right, other.right), Math.max(bottom, other.bottom));
        }

        public boolean intersects(Bounds other) {
            return left < other.right && other.left < right && top < other.bottom && other.top < bottom;
        }

        public int width() {
            return right - left;
        }

        public int height() {
            return bottom - top;
        }

        public boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }
    }

    /** The XP bar (with the level number centered on it) at its default position. */
    public static Bounds xpBar(int screenHeight, HudLayout layout) {
        int bottom = screenHeight - XP_BAR_BOTTOM_MARGIN;
        return new Bounds(layout.x(), bottom - XP_BAR_HEIGHT, layout.x() + HudLayout.WIDTH, bottom);
    }

    /**
     * Positions the pose at the given shift from the default spot, replacing any horizontal or
     * vertical shift other mods applied to this layer. Every piece is placed only by this mod, so
     * other mods can't pull the HUD apart (e.g. a hotbar mod shifting the XP bar but not our bars).
     * Depth (z) is left as it is.
     */
    public static void moveTo(PoseStack pose, Offset offset) {
        Matrix4f matrix = pose.last().pose();
        pose.translate(offset.x() - matrix.m30(), offset.y() - matrix.m31(), 0);
    }

    /**
     * Each piece's shift: the editor's preview, or the saved positions.
     *
     * @param shown each shown piece at its default spot
     */
    public static Pieces pieces(int screenWidth, int screenHeight, Map<HudPiece, Bounds> shown) {
        return preview != null ? preview : saved(screenWidth, screenHeight, shown);
    }

    /** The saved positions for this screen size, with each shown piece kept on screen. */
    public static Pieces saved(int screenWidth, int screenHeight, Map<HudPiece, Bounds> shown) {
        Map<HudPiece, Offset> shifts = new EnumMap<>(HudPiece.class);
        if (ClientConfig.LAYOUT_VERSION.get() < 2) {
            Offset set = legacySetOffset(screenWidth, screenHeight, shown);
            for (HudPiece piece : HudPiece.values()) {
                shifts.put(piece, set.plus(storedOffset(piece)));
            }
        } else {
            for (HudPiece piece : HudPiece.values()) {
                shifts.put(piece, storedOffset(piece).plus(anchorShift(ClientConfig.pieceAnchor(piece), screenWidth, screenHeight)));
            }
        }
        for (Map.Entry<HudPiece, Bounds> entry : shown.entrySet()) {
            shifts.put(entry.getKey(), clamp(shifts.get(entry.getKey()), screenWidth, screenHeight, entry.getValue()));
        }
        return new Pieces(shifts);
    }

    /**
     * Saves every piece's position. Shown pieces are measured from the anchor nearest them (shared
     * by pieces that touch); hidden ones keep the anchor they had.
     *
     * @param shown each shown piece at its default spot
     */
    public static void save(Pieces pieces, int screenWidth, int screenHeight, Map<HudPiece, Bounds> shown) {
        Map<HudPiece, HudAnchor> anchors = anchors(pieces, screenWidth, screenHeight, shown);
        for (HudPiece piece : HudPiece.values()) {
            HudAnchor anchor = anchors.getOrDefault(piece, ClientConfig.pieceAnchor(piece));
            Offset stored = pieces.get(piece).minus(anchorShift(anchor, screenWidth, screenHeight));
            ClientConfig.setPiecePosition(piece, anchor, stored.x(), stored.y());
        }
        ClientConfig.savePositions();
    }

    public static Offset clamp(Offset offset, int screenWidth, int screenHeight, Bounds bounds) {
        int minX = -bounds.left();
        int maxX = screenWidth - bounds.right();
        int minY = -bounds.top();
        int maxY = screenHeight - bounds.bottom();
        return new Offset(Mth.clamp(offset.x(), minX, Math.max(minX, maxX)), Mth.clamp(offset.y(), minY, Math.max(minY, maxY)));
    }

    public static void setPreview(@Nullable Pieces pieces) {
        preview = pieces;
    }

    private static Offset storedOffset(HudPiece piece) {
        return new Offset(ClientConfig.pieceOffsetX(piece), ClientConfig.pieceOffsetY(piece));
    }

    /**
     * How far the anchor's point is from the bottom-center, where the default layout is measured
     * from. A piece measured from an anchor moves with it when the screen size changes.
     */
    private static Offset anchorShift(HudAnchor anchor, int screenWidth, int screenHeight) {
        return new Offset(anchor.x(screenWidth) - screenWidth / 2, anchor.y(screenHeight) - screenHeight);
    }

    /**
     * Groups shown pieces that touch (closer than {@link #TOUCHING_GAP}) and gives each group the
     * anchor nearest its center, so a group stays together when the screen size changes.
     */
    private static Map<HudPiece, HudAnchor> anchors(Pieces pieces, int screenWidth, int screenHeight, Map<HudPiece, Bounds> shown) {
        List<HudPiece> keys = new ArrayList<>(shown.keySet());
        List<Bounds> boxes = new ArrayList<>();
        for (HudPiece piece : keys) {
            boxes.add(shown.get(piece).offset(pieces.get(piece)));
        }
        int[] group = new int[keys.size()];
        for (int i = 0; i < group.length; i++) {
            group[i] = i;
        }
        for (int i = 0; i < keys.size(); i++) {
            for (int j = i + 1; j < keys.size(); j++) {
                if (boxes.get(i).grow(TOUCHING_GAP).intersects(boxes.get(j))) {
                    int from = group[j];
                    int to = group[i];
                    for (int k = 0; k < group.length; k++) {
                        if (group[k] == from) {
                            group[k] = to;
                        }
                    }
                }
            }
        }
        Map<HudPiece, HudAnchor> anchors = new EnumMap<>(HudPiece.class);
        for (int i = 0; i < keys.size(); i++) {
            Bounds united = null;
            for (int k = 0; k < keys.size(); k++) {
                if (group[k] == group[i]) {
                    united = united == null ? boxes.get(k) : united.union(boxes.get(k));
                }
            }
            anchors.put(keys.get(i), HudAnchor.nearest((united.left() + united.right()) / 2, (united.top() + united.bottom()) / 2, screenWidth, screenHeight));
        }
        return anchors;
    }

    /**
     * Layout 1 (before 1.2.1): one anchor for the whole HUD, measured to the matching point of the
     * box around all shown pieces (each with its own shift).
     */
    private static Offset legacySetOffset(int screenWidth, int screenHeight, Map<HudPiece, Bounds> shown) {
        Bounds bounds = null;
        for (Map.Entry<HudPiece, Bounds> entry : shown.entrySet()) {
            Bounds moved = entry.getValue().offset(storedOffset(entry.getKey()));
            bounds = bounds == null ? moved : bounds.union(moved);
        }
        HudAnchor anchor = ClientConfig.ANCHOR.get();
        int left = anchor.x(screenWidth) - anchor.x(bounds.width()) + ClientConfig.OFFSET_X.get();
        int top = anchor.y(screenHeight) - anchor.y(bounds.height()) + ClientConfig.OFFSET_Y.get();
        return new Offset(left - bounds.left(), top - bounds.top());
    }
}
