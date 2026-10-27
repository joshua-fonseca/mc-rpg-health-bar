package com.joshuafonseca.rpghealthbar.client.screen;

import com.joshuafonseca.rpghealthbar.client.HudLayer;
import com.joshuafonseca.rpghealthbar.client.HudPosition;
import com.joshuafonseca.rpghealthbar.client.HudRenderer;
import com.joshuafonseca.rpghealthbar.client.HudValues;
import com.joshuafonseca.rpghealthbar.compat.IronsSpellsCompat;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import java.util.ArrayList;
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
import org.lwjgl.glfw.GLFW;

/**
 * Drag the whole HUD set, or one piece of it, to a new position, then Save or Cancel. In a world
 * the real HUD follows the drag; from the title screen a preview is drawn instead.
 */
public class HudEditScreen extends Screen {
    private static final int BUTTON_WIDTH = 70;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int XP_BAR_PREVIEW_COLOR = 0xFF80FF20;

    /** Title, hint and buttons; moved to whichever half of the screen the HUD isn't in. */
    private static final int CONTROLS_HEIGHT = 44;
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
        buttons.add(addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(x + step * 3, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build()));

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
        int bottom = this.height - CONTROLS_MARGIN - CONTROLS_HEIGHT;
        HudPosition.Bounds box = frame.bounds();
        boolean hudInTopHalf = (box.top() + box.bottom()) / 2 < this.height / 2;
        int[] candidates = {hudInTopHalf ? bottom : top, hudInTopHalf ? top : bottom, this.height / 2 - CONTROLS_HEIGHT / 2};
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
            button.setY(controlsTop + CONTROLS_HEIGHT - BUTTON_HEIGHT);
        }
    }

    /** How many pixels of the pieces the title, hint and buttons would cover with their top at {@code y}. */
    private int coveredArea(HudLayer.Frame frame, int y) {
        int halfWidth = Math.max(BUTTON_WIDTH * 4 + BUTTON_GAP * 3, this.font.width(Component.translatable("rpghealthbar.edit.hint.one"))) / 2;
        HudPosition.Bounds controls = new HudPosition.Bounds(this.width / 2 - halfWidth, y, this.width / 2 + halfWidth, y + CONTROLS_HEIGHT);
        int area = 0;
        for (Map.Entry<HudPiece, HudPosition.Bounds> entry : frame.pieceBounds().entrySet()) {
            HudPosition.Bounds box = entry.getValue().offset(frame.pieceOffset(entry.getKey()));
            int w = Math.min(box.right(), controls.right()) - Math.max(box.left(), controls.left());
            int h = Math.min(box.bottom(), controls.bottom()) - Math.max(box.top(), controls.top());
            if (w > 0 && h > 0) {
                area += w * h;
            }
        }
        return area;
    }

    private HudLayer.Frame frame() {
        return HudLayer.Frame.compute(this.width, this.height, IronsSpellsCompat.isAvailable());
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
        HudPosition.Bounds bounds = null;
        for (Map.Entry<HudPiece, HudPosition.Bounds> entry : frame().pieceBounds().entrySet()) {
            HudPosition.Bounds moved = entry.getValue().offset(from.get(entry.getKey()));
            bounds = bounds == null ? moved : bounds.union(moved);
        }
        setPieces(from.shiftedBy(HudPosition.clamp(delta, this.width, this.height, bounds)));
    }

    /** Puts every piece back above the hotbar. */
    private void reset() {
        setPieces(HudPosition.Pieces.NONE);
    }

    private void save() {
        HudPosition.save(pieces, this.width, this.height, frame().pieceBounds());
        onClose();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void removed() {
        HudPosition.setPreview(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // In a world, keep the HUD visible and unblurred so the real bars can be seen moving.
        if (this.minecraft.level == null) {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        HudLayer.Frame frame = frame();

        // The HUD layer isn't drawn without a survival player (title screen, creative), so draw a
        // preview of our bars, plus a stand-in for the XP bar when there's no world at all.
        boolean inWorld = this.minecraft.level != null;
        if (!inWorld || !HudLayer.isVisible(this.minecraft)) {
            HudRenderer.render(graphics, this.font, frame.layout(), HudValues.placeholder(), frame.showArmor(), frame.showHunger(), pieces, false);
            if (!inWorld) {
                HudPosition.Bounds xp = frame.pieceBounds().get(HudPiece.XP).offset(pieces.get(HudPiece.XP));
                graphics.fill(xp.left(), xp.top(), xp.right(), xp.bottom(), XP_BAR_PREVIEW_COLOR);
            }
        }

        graphics.drawCenteredString(this.font, this.title, this.width / 2, controlsTop, 0xFFFFFF);
        if (dragging) {
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
        graphics.drawCenteredString(this.font, Component.translatable(hint), this.width / 2, controlsTop + 10, 0xC0C0C0);
    }

    /**
     * Lightens one piece, or every piece if {@code only} is null, exactly over its own pixels so
     * it doesn't get in the way of lining things up.
     */
    private static void highlight(GuiGraphics graphics, HudLayer.Frame frame, @Nullable HudPiece only, int color) {
        for (Map.Entry<HudPiece, HudPosition.Bounds> entry : frame.pieceBounds().entrySet()) {
            if (only == null || entry.getKey() == only) {
                HudPosition.Bounds box = entry.getValue().offset(frame.pieceOffset(entry.getKey()));
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
        for (Map.Entry<HudPiece, HudPosition.Bounds> entry : frame.pieceBounds().entrySet()) {
            HudPosition.Bounds bounds = entry.getValue().offset(frame.pieceOffset(entry.getKey()));
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
