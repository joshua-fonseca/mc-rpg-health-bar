package com.joshuafonseca.rpghealthbar.client.screen;

import com.joshuafonseca.rpghealthbar.client.BarWidths;
import com.joshuafonseca.rpghealthbar.client.HudLayer;
import com.joshuafonseca.rpghealthbar.client.HudPosition;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * The HUD Editor. Drag the whole HUD set, or one piece of it, to a new position, then Save (or Esc)
 * or Cancel. In Advanced edit mode, dragging either end of the health, mana or XP bar resizes it
 * (the XP bar a cell at a time), and dragging the top of an upright hunger or thirst bar changes its
 * height. In a world the real HUD follows the drag; from the title screen a
 * preview is drawn instead.
 */
public class HudEditScreen extends Screen {
    private static final int BUTTON_WIDTH = 70;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;

    /** Title, hint and buttons; moved to whichever half of the screen the HUD isn't in. */
    private static final int CONTROLS_HEIGHT = 44;
    private static final int LINE_HEIGHT = 10;
    /** A bar's end can be grabbed this far either side of it. */
    private static final int EDGE_MARGIN = 2;
    /** The system's resize cursors, shown over a bar's end; each made the first time it's needed. */
    private static long widthCursor;
    private static long heightCursor;
    private static final int CONTROLS_MARGIN = 8;
    /** Pieces can be grabbed this far outside their edges, so the thin bars are easy to pick up. */
    private static final int GRAB_MARGIN = 2;
    /** While dragging, what's being moved is lightened by this translucent white. */
    private static final int GRABBED_HIGHLIGHT = 0x30FFFFFF;
    /** What the cursor would grab is lightened more faintly. */
    private static final int HOVER_HIGHLIGHT = 0x18FFFFFF;

    /** What dragging moves: the whole set, or one piece of it. */
    private enum Mode {
        ALL,
        ONE;

        Component displayName() {
            return Component.translatable("rpghealthbar.edit.mode." + name().toLowerCase(Locale.ROOT));
        }
    }

    private final Screen parent;
    private final List<AbstractButton> buttons = new ArrayList<>();
    private int controlsTop;
    /** Each piece's shift from its default spot; null until the screen size is known in init(). */
    @Nullable
    private HudPosition.Pieces pieces;
    /** While dragging everything: the pieces where the drag started. */
    private HudPosition.Pieces grabbedPieces = HudPosition.Pieces.NONE;
    private Mode mode = Mode.ALL;
    private boolean dragging;
    /** In {@link Mode#ONE}, the piece last grabbed; the arrow keys move it. */
    @Nullable
    private HudPiece selected;
    private double grabX;
    private double grabY;
    /** The bars' sizes being edited, shown on the HUD as they change. */
    @Nullable
    private Map<HudPiece, BarWidths.Size> sizes;
    /** The bar end being dragged, and the bar's width (or height) and box when it was grabbed. */
    @Nullable
    private Edge resizing;
    private int resizeStartWidth;
    private int resizeStartHeight;
    /** How far left the bar's left end had been dragged when it was grabbed. */
    private int resizeStartLeft;
    @Nullable
    private HudPosition.Bounds resizeStartBox;
    /** The resize cursor being shown, or 0 for the normal one. */
    private long shownCursor;

    /** One end of a resizable bar; for an upright bar, its top, and {@code left} is false. */
    private record Edge(HudPiece piece, boolean left) {
    }

    public HudEditScreen(Screen parent) {
        super(Component.translatable("rpghealthbar.edit.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int totalWidth = BUTTON_WIDTH * 4 + BUTTON_GAP * 3;
        int x = this.width / 2 - totalWidth / 2;
        int step = BUTTON_WIDTH + BUTTON_GAP;
        buttons.clear();
        buttons.add(addRenderableWidget(CycleButton.builder(Mode::displayName)
                .withValues(Mode.values())
                .withInitialValue(mode)
                .withTooltip(value -> RpgConfigScreen.tooltip("moveMode", "all", "one"))
                .create(x, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("rpghealthbar.edit.mode"), (button, value) -> {
                    mode = value;
                    selected = null;
                })));
        buttons.add(addRenderableWidget(Button.builder(Component.translatable("rpghealthbar.edit.save"), button -> save())
                .bounds(x + step, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build()));
        buttons.add(addRenderableWidget(Button.builder(Component.translatable("rpghealthbar.edit.reset"), button -> reset())
                .bounds(x + step * 2, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build()));
        buttons.add(addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> close())
                .bounds(x + step * 3, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build()));

        if (sizes == null) {
            sizes = new EnumMap<>(BarWidths.saved());
            BarWidths.setPreview(sizes);
        }
        // Start from the saved positions, or re-clamp the dragged ones if the window was resized.
        // This also places the controls.
        setPieces(pieces != null ? pieces : frame().pieces());
    }

    /**
     * Keeps the controls out of the way: in the half of the screen away from the HUD, or else at
     * the other edge or in the middle, whichever covers the least of the pieces.
     */
    private void placeControls() {
        HudLayer.Frame frame = frame();
        int top = CONTROLS_MARGIN;
        int bottom = this.height - CONTROLS_MARGIN - controlsHeight();
        HudPosition.Bounds box = frame.bounds();
        boolean hudInTopHalf = (box.top() + box.bottom()) / 2 < this.height / 2;
        int[] candidates = {hudInTopHalf ? bottom : top, hudInTopHalf ? top : bottom, this.height / 2 - controlsHeight() / 2};
        controlsTop = candidates[0];
        int least = coveredArea(frame, candidates[0]);
        for (int i = 1; i < candidates.length && least > 0; i++) {
            int covered = coveredArea(frame, candidates[i]);
            if (covered < least) {
                least = covered;
                controlsTop = candidates[i];
            }
        }
        for (AbstractButton button : buttons) {
            button.setY(controlsTop + controlsHeight() - BUTTON_HEIGHT);
        }
    }

    /** With a line more in Advanced mode, on resizing the bars. */
    private static int controlsHeight() {
        return CONTROLS_HEIGHT + (BarWidths.advanced() ? LINE_HEIGHT : 0);
    }

    /** How many pixels of the pieces the title, hint and buttons would cover with their top at {@code y}. */
    private int coveredArea(HudLayer.Frame frame, int y) {
        int halfWidth = Math.max(BUTTON_WIDTH * 4 + BUTTON_GAP * 3, Math.max(this.font.width(Component.translatable("rpghealthbar.edit.hint.one")),
                this.font.width(Component.translatable("rpghealthbar.edit.hint.resize")))) / 2;
        HudPosition.Bounds controls = new HudPosition.Bounds(this.width / 2 - halfWidth, y, this.width / 2 + halfWidth, y + controlsHeight());
        int area = 0;
        for (HudPosition.Bounds box : frame.placedBounds().values()) {
            int w = Math.min(box.right(), controls.right()) - Math.max(box.left(), controls.left());
            int h = Math.min(box.bottom(), controls.bottom()) - Math.max(box.top(), controls.top());
            if (w > 0 && h > 0) {
                area += w * h;
            }
        }
        return area;
    }

    private HudLayer.Frame frame() {
        return HudLayer.Frame.compute(this.width, this.height);
    }

    /** Shows the pieces where they'd be saved, each kept on screen. */
    private void setPieces(HudPosition.Pieces requested) {
        HudPosition.Pieces clamped = requested;
        for (Map.Entry<HudPiece, HudPosition.Bounds> entry : frame().pieceBounds().entrySet()) {
            clamped = clamped.with(entry.getKey(), HudPosition.clamp(requested.get(entry.getKey()), this.width, this.height, entry.getValue()));
        }
        pieces = clamped;
        HudPosition.setPreview(pieces);
        placeControls();
    }

    /** Moves every piece by the same amount, as far as keeps them all on screen. */
    private void moveAll(HudPosition.Pieces from, HudPosition.Offset delta) {
        HudPosition.Bounds bounds = HudPosition.union(HudPosition.placed(frame().pieceBounds(), from::get).values());
        setPieces(from.shiftedBy(HudPosition.clamp(delta, this.width, this.height, bounds)));
    }

    /** Puts every piece back above the hotbar, and in Advanced mode each bar back to its preset width. */
    private void reset() {
        if (BarWidths.advanced()) {
            sizes.replaceAll((piece, size) -> BarWidths.Size.PRESET);
        }
        setPieces(HudPosition.Pieces.NONE);
    }

    private void save() {
        BarWidths.save(sizes);
        HudPosition.save(pieces, this.width, this.height, frame().pieceBounds());
        close();
    }

    /** Esc saves, like on the Item Name page; only Cancel leaves the old position. */
    @Override
    public void onClose() {
        save();
    }

    private void close() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void removed() {
        HudPosition.setPreview(null);
        BarWidths.setPreview(null);
        showResizeCursor(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // In a world, no background, so the real bars can be seen moving.
        if (this.minecraft.level == null) {
            renderBackground(graphics);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        HudLayer.Frame frame = frame();

        HudPreview.drawIfHidden(graphics, this.minecraft, frame, false);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, controlsTop, 0xFFFFFF);
        Edge hoveredEdge = resizing == null && !dragging && getChildAt(mouseX, mouseY).isEmpty() ? edgeAt(mouseX, mouseY) : null;
        showResizeCursor(resizing != null ? resizing : hoveredEdge);
        if (resizing != null) {
            highlight(graphics, frame, resizing.piece(), GRABBED_HIGHLIGHT);
        } else if (hoveredEdge != null) {
            highlight(graphics, frame, hoveredEdge.piece(), HOVER_HIGHLIGHT);
        } else if (dragging) {
            highlight(graphics, frame, mode == Mode.ALL ? null : selected, GRABBED_HIGHLIGHT);
        } else if (mode == Mode.ALL) {
            if (frame.bounds().contains(mouseX, mouseY)) {
                highlight(graphics, frame, null, HOVER_HIGHLIGHT);
            }
        } else {
            HudPiece hovered = pieceAt(mouseX, mouseY);
            if (hovered != null) {
                highlight(graphics, frame, hovered, HOVER_HIGHLIGHT);
            }
        }

        String hint = mode == Mode.ALL ? "rpghealthbar.edit.hint" : "rpghealthbar.edit.hint.one";
        graphics.drawCenteredString(this.font, Component.translatable(hint), this.width / 2, controlsTop + LINE_HEIGHT, 0xFFFFFF);
        if (BarWidths.advanced()) {
            graphics.drawCenteredString(this.font, Component.translatable("rpghealthbar.edit.hint.resize"), this.width / 2, controlsTop + LINE_HEIGHT * 2, 0xFFFFFF);
        }
    }

    /** Shows the resize cursor for the bar end, left-right or up-down, or the normal one for null. */
    private void showResizeCursor(@Nullable Edge edge) {
        long cursor = 0;
        if (edge != null && edge.piece().isUpright()) {
            if (heightCursor == 0) {
                heightCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR);
            }
            cursor = heightCursor;
        } else if (edge != null) {
            if (widthCursor == 0) {
                widthCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR);
            }
            cursor = widthCursor;
        }
        if (cursor != shownCursor) {
            GLFW.glfwSetCursor(this.minecraft.getWindow().getWindow(), cursor);
            shownCursor = cursor;
        }
    }

    /** In Advanced mode, the end of a resizable bar under the cursor. */
    @Nullable
    private Edge edgeAt(double mouseX, double mouseY) {
        if (!BarWidths.advanced()) {
            return null;
        }
        HudLayer.Frame frame = frame();
        for (Map.Entry<HudPiece, HudPosition.Bounds> entry : frame.placedBounds().entrySet()) {
            if (!BarWidths.resizable(entry.getKey())) {
                continue;
            }
            HudPosition.Bounds box = entry.getValue();
            if (entry.getKey().isUpright()) {
                if (mouseX >= box.left() && mouseX < box.right() && Math.abs(mouseY - box.top()) <= EDGE_MARGIN) {
                    return new Edge(entry.getKey(), false);
                }
                continue;
            }
            if (mouseY < box.top() - EDGE_MARGIN || mouseY >= box.bottom() + EDGE_MARGIN) {
                continue;
            }
            if (Math.abs(mouseX - box.left()) <= EDGE_MARGIN) {
                return new Edge(entry.getKey(), true);
            }
            if (Math.abs(mouseX - box.right()) <= EDGE_MARGIN) {
                return new Edge(entry.getKey(), false);
            }
        }
        return null;
    }

    /**
     * Resizes the grabbed bar to follow the cursor: the dragged end moves, the other stays put. Kept
     * between its smallest and the screen's edge; the XP bar snaps to whole cells.
     */
    private void resize(double mouseX, double mouseY) {
        HudPiece piece = resizing.piece();
        if (piece.isUpright()) {
            // Its bottom stays put.
            int height = Mth.clamp(resizeStartHeight - (int) Math.round(mouseY - grabY),
                    BarWidths.MIN_HEIGHT, Math.max(BarWidths.MIN_HEIGHT, resizeStartBox.bottom()));
            sizes.put(piece, new BarWidths.Size(height, 0));
            setPieces(pieces);
            return;
        }
        int moved = (int) Math.round(mouseX - grabX);
        int wanted = resizeStartWidth + (resizing.left() ? -moved : moved);
        int room = resizing.left() ? resizeStartBox.right() : this.width - resizeStartBox.left();
        int width;
        int stored;
        if (piece == HudPiece.XP) {
            int cells = Mth.clamp(Math.round((wanted - BarWidths.xpWidth(0)) / (float) BarWidths.XP_CELL_WIDTH),
                    BarWidths.MIN_XP_CELLS, Math.max(BarWidths.MIN_XP_CELLS, (room - BarWidths.xpWidth(0)) / BarWidths.XP_CELL_WIDTH));
            width = BarWidths.xpWidth(cells);
            stored = cells;
        } else {
            width = Mth.clamp(wanted, BarWidths.MIN_WIDTH, Math.max(BarWidths.MIN_WIDTH, room));
            stored = width;
        }
        // Dragging the left end moves the bar's start, keeping its right end where it was. That's
        // kept with the size rather than the position, so the preset width (Simple mode) is back
        // where it was.
        int left = resizing.left() ? resizeStartLeft + width - resizeStartWidth : resizeStartLeft;
        sizes.put(piece, new BarWidths.Size(stored, left));
        setPieces(pieces);
    }

    /**
     * Lightens one piece, or every piece if {@code only} is null, exactly over its own pixels so
     * it doesn't get in the way of lining things up.
     */
    private static void highlight(GuiGraphics graphics, HudLayer.Frame frame, @Nullable HudPiece only, int color) {
        for (Map.Entry<HudPiece, HudPosition.Bounds> entry : frame.placedBounds().entrySet()) {
            if (only == null || entry.getKey() == only) {
                HudPosition.Bounds box = entry.getValue();
                graphics.fill(box.left(), box.top(), box.right(), box.bottom(), color);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        Edge edge = edgeAt(mouseX, mouseY);
        if (edge != null) {
            HudLayer.Frame frame = frame();
            resizing = edge;
            resizeStartBox = frame.placed(edge.piece());
            resizeStartWidth = resizeStartBox.width();
            resizeStartHeight = resizeStartBox.height();
            resizeStartLeft = BarWidths.left(edge.piece());
            grabX = mouseX;
            grabY = mouseY;
            return true;
        }
        if (mode == Mode.ALL) {
            if (frame().bounds().contains(mouseX, mouseY)) {
                dragging = true;
                grabbedPieces = pieces;
                grabX = mouseX;
                grabY = mouseY;
                return true;
            }
            return false;
        }
        HudPiece piece = pieceAt(mouseX, mouseY);
        if (piece != null) {
            selected = piece;
            dragging = true;
            grabX = mouseX - pieces.get(piece).x();
            grabY = mouseY - pieces.get(piece).y();
            return true;
        }
        return false;
    }

    /** The piece under the cursor; one it's right on wins over one it's just next to. */
    @Nullable
    private HudPiece pieceAt(double mouseX, double mouseY) {
        HudLayer.Frame frame = frame();
        HudPiece near = null;
        for (Map.Entry<HudPiece, HudPosition.Bounds> entry : frame.placedBounds().entrySet()) {
            HudPosition.Bounds bounds = entry.getValue();
            if (bounds.contains(mouseX, mouseY)) {
                return entry.getKey();
            }
            if (near == null && bounds.grow(GRAB_MARGIN).contains(mouseX, mouseY)) {
                near = entry.getKey();
            }
        }
        return near;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (resizing != null) {
            resize(mouseX, mouseY);
            return true;
        }
        if (dragging) {
            HudPosition.Offset moved = new HudPosition.Offset((int) Math.round(mouseX - grabX), (int) Math.round(mouseY - grabY));
            if (mode == Mode.ALL) {
                moveAll(grabbedPieces, moved);
            } else if (selected != null) {
                setPieces(pieces.with(selected, moved));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        resizing = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** Arrow keys nudge the set (or the last grabbed piece) by 1 pixel, or 10 while holding Shift. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int step = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;
        int dx = switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> -step;
            case GLFW.GLFW_KEY_RIGHT -> step;
            default -> 0;
        };
        int dy = switch (keyCode) {
            case GLFW.GLFW_KEY_UP -> -step;
            case GLFW.GLFW_KEY_DOWN -> step;
            default -> 0;
        };
        if (dx != 0 || dy != 0) {
            HudPosition.Offset nudge = new HudPosition.Offset(dx, dy);
            if (mode == Mode.ALL) {
                moveAll(pieces, nudge);
            } else if (selected != null) {
                setPieces(pieces.with(selected, pieces.get(selected).plus(nudge)));
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
