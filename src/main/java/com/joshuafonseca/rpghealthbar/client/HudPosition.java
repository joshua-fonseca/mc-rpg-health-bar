package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HudAnchor;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
    /** The level's number on its own: three digits and their outline, where vanilla's bar has it. */
    private static final int LEVEL_WIDTH = 20;
    private static final int LEVEL_HEIGHT = 9;
    private static final int LEVEL_BOTTOM_MARGIN = 22;
    /** Pieces closer than this share an anchor when saved (the default rows are 2px apart). */
    private static final int TOUCHING_GAP = 4;

    /** Piece shifts while the position editor is open, so the real HUD follows the drag; null otherwise. */
    @Nullable
    private static Pieces preview;
    /** While the position editor is open, whether it shows the HUD as it is while riding a mount; null otherwise. */
    @Nullable
    private static Boolean previewMounted;

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

    /** Each piece's box moved by its shift. */
    public static Map<HudPiece, Bounds> placed(Map<HudPiece, Bounds> shown, Function<HudPiece, Offset> shift) {
        Map<HudPiece, Bounds> placed = new EnumMap<>(HudPiece.class);
        shown.forEach((piece, box) -> placed.put(piece, box.offset(shift.apply(piece))));
        return placed;
    }

    /** The box around all the boxes; null if there are none. */
    @Nullable
    public static Bounds union(Collection<Bounds> boxes) {
        Bounds union = null;
        for (Bounds box : boxes) {
            union = union == null ? box : union.union(box);
        }
        return union;
    }

    /** The XP bar (with the level number on it) at its default position, as long as {@link BarWidths} says. */
    public static Bounds xpBar(int screenHeight, HudLayout layout) {
        int bottom = screenHeight - XP_BAR_BOTTOM_MARGIN;
        int left = layout.x() - BarWidths.left(HudPiece.XP);
        return new Bounds(left, bottom - XP_BAR_HEIGHT, left + BarWidths.width(HudPiece.XP), bottom);
    }

    /** The XP level's number on its own at its default position, in the middle of vanilla's XP bar. */
    public static Bounds xpLevel(int screenHeight, HudLayout layout) {
        int bottom = screenHeight - LEVEL_BOTTOM_MARGIN;
        int left = layout.x() + (HudLayout.WIDTH - LEVEL_WIDTH) / 2;
        return new Bounds(left, bottom - LEVEL_HEIGHT, left + LEVEL_WIDTH, bottom);
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
     * @param shown      each shown piece at its default spot
     * @param airDefault where the air bubbles go from their box until they're moved
     */
    public static Pieces pieces(int screenWidth, int screenHeight, Map<HudPiece, Bounds> shown, Offset airDefault) {
        return preview != null ? preview : saved(screenWidth, screenHeight, shown, airDefault);
    }

    /** The saved positions for this screen size, with each shown piece kept on screen. */
    private static Pieces saved(int screenWidth, int screenHeight, Map<HudPiece, Bounds> shown, Offset airDefault) {
        Map<HudPiece, Offset> shifts = new EnumMap<>(HudPiece.class);
        if (ClientConfig.layoutVersion() < 2) {
            Offset set = legacySetOffset(screenWidth, screenHeight, shown);
            for (HudPiece piece : HudPiece.values()) {
                // Upright bars stand apart from the set, beside the hotbar.
                shifts.put(piece, piece.isUpright() ? storedOffset(piece) : set.plus(storedOffset(piece)));
            }
            keepThirstOnScreen(shifts, shown);
        } else {
            for (HudPiece piece : HudPiece.values()) {
                shifts.put(piece, storedOffset(piece).plus(anchorShift(ClientConfig.pieceAnchor(piece), screenWidth, screenHeight)));
            }
        }
        if (!ClientConfig.rowsPlaced()) {
            // Saved before air and mount health had places of their own: they moved with hunger.
            shifts.put(HudPiece.MOUNT, shifts.get(HudPiece.HUNGER));
            shifts.put(HudPiece.AIR, shifts.get(HudPiece.HUNGER).plus(airDefault));
        } else if (isUnmoved(HudPiece.AIR)) {
            shifts.put(HudPiece.AIR, airDefault);
        }
        if (isUnmoved(HudPiece.XP_LEVEL)) {
            shifts.put(HudPiece.XP_LEVEL, shifts.get(HudPiece.XP));
        }
        for (Map.Entry<HudPiece, Bounds> entry : shown.entrySet()) {
            shifts.put(entry.getKey(), clamp(shifts.get(entry.getKey()), screenWidth, screenHeight, entry.getValue()));
        }
        return new Pieces(shifts);
    }

    /**
     * Saves every piece's position. Shown pieces are measured from the anchor nearest them (shared
     * by pieces that touch); hidden ones keep the anchor they had, unless the positions were reset.
     *
     * @param shown      each shown piece at its default spot
     * @param airDefault where the air bubbles go from their box until they're moved
     * @param reset      whether every piece was put back to its default spot since they were last saved
     */
    public static void save(Pieces pieces, int screenWidth, int screenHeight, Map<HudPiece, Bounds> shown, Offset airDefault, boolean reset) {
        Map<HudPiece, HudAnchor> anchors = anchors(pieces, screenWidth, screenHeight, shown);
        for (HudPiece piece : HudPiece.values()) {
            // A level never placed on its own keeps going where the XP bar does.
            if (piece == HudPiece.XP_LEVEL && !shown.containsKey(piece) && isUnmoved(piece)) {
                continue;
            }
            // The default spot is measured from the bottom center, so a hidden piece that was reset
            // stays there at any screen size.
            HudAnchor anchor = anchors.getOrDefault(piece, reset ? HudAnchor.BOTTOM_CENTER : ClientConfig.pieceAnchor(piece));
            Offset stored = pieces.get(piece).minus(anchorShift(anchor, screenWidth, screenHeight));
            if (piece == HudPiece.AIR && pieces.get(piece).equals(airDefault)) {
                // Saved as unmoved, so it goes where Air Bubbles/Mount HP says if that changes.
                anchor = HudAnchor.BOTTOM_CENTER;
                stored = Offset.ZERO;
            }
            if (piece == HudPiece.XP_LEVEL && pieces.get(piece).equals(pieces.get(HudPiece.XP))) {
                // Saved as unmoved, so it follows the XP bar.
                anchor = HudAnchor.BOTTOM_CENTER;
                stored = Offset.ZERO;
            }
            ClientConfig.setPiecePosition(piece, anchor, stored.x(), stored.y());
        }
        ClientConfig.savePositions();
    }

    /** Whether a piece's saved position is the untouched one. */
    private static boolean isUnmoved(HudPiece piece) {
        return ClientConfig.pieceAnchor(piece) == HudAnchor.BOTTOM_CENTER && storedOffset(piece).equals(Offset.ZERO);
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

    /** Whether the position editor is showing its positions on the HUD. */
    public static boolean isPreviewing() {
        return preview != null;
    }

    public static void setPreviewMounted(@Nullable Boolean mounted) {
        previewMounted = mounted;
    }

    @Nullable
    public static Boolean previewMounted() {
        return previewMounted;
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
        Map<HudPiece, Bounds> placed = placed(shown, pieces::get);
        List<HudPiece> keys = new ArrayList<>(placed.keySet());
        List<Bounds> boxes = new ArrayList<>(placed.values());
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
        // Left at its default spot, which is beside the hotbar, it stays there at any screen width.
        for (HudPiece piece : shown.keySet()) {
            if (piece.isUpright() && pieces.get(piece).equals(Offset.ZERO)) {
                anchors.put(piece, HudAnchor.BOTTOM_CENTER);
            }
        }
        return anchors;
    }

    /** Pieces layout 1 didn't have, which don't count as part of its set. */
    private static boolean newerThanLayoutOne(HudPiece piece) {
        return piece.isUpright() || piece == HudPiece.THIRST || piece == HudPiece.AIR || piece == HudPiece.MOUNT
                || piece == HudPiece.XP_LEVEL;
    }

    /**
     * Layout 1 had no thirst row: it goes above the set, as in the default layout, or below the set
     * if the set is too near the top of the screen for that.
     */
    private static void keepThirstOnScreen(Map<HudPiece, Offset> shifts, Map<HudPiece, Bounds> shown) {
        Bounds thirst = shown.get(HudPiece.THIRST);
        if (thirst == null || thirst.offset(shifts.get(HudPiece.THIRST)).top() >= 0) {
            return;
        }
        Map<HudPiece, Bounds> set = new EnumMap<>(shown);
        set.keySet().removeIf(HudPosition::newerThanLayoutOne);
        int below = union(placed(set, shifts::get).values()).bottom() + HudLayout.ROW_GAP;
        Offset shift = shifts.get(HudPiece.THIRST);
        shifts.put(HudPiece.THIRST, new Offset(shift.x(), below - thirst.top()));
    }

    /**
     * Layout 1 (before 1.2.1): one anchor for the whole HUD, measured to the matching point of the
     * box around all shown pieces (each with its own shift).
     */
    private static Offset legacySetOffset(int screenWidth, int screenHeight, Map<HudPiece, Bounds> shown) {
        Map<HudPiece, Bounds> set = new EnumMap<>(shown);
        // Newer than this layout: counting them would move everything else.
        set.keySet().removeIf(HudPosition::newerThanLayoutOne);
        Bounds bounds = union(placed(set, HudPosition::storedOffset).values());
        HudAnchor anchor = ClientConfig.ANCHOR.get();
        int left = anchor.x(screenWidth) - anchor.x(bounds.width()) + ClientConfig.OFFSET_X.get();
        int top = anchor.y(screenHeight) - anchor.y(bounds.height()) + ClientConfig.OFFSET_Y.get();
        return new Offset(left - bounds.left(), top - bounds.top());
    }
}
