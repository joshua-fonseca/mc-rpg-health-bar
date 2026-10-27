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
    /** Shift from the default spot; null until the screen size is known in init(). */
    private HudPosition.Offset offset;
    private HudPosition.Pieces pieces = HudPosition.savedPieces();
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

        // Start from the saved position, or re-clamp the dragged one if the window was resized.
        // This also places the controls.
        HudPosition.setPreviewPieces(pieces);
        setOffset(offset != null ? offset : HudPosition.saved(this.width, this.height, frame().bounds()));
    }

    /** Keeps the controls out of the way: at the bottom when the HUD is in the top half, else at the top. */
    private void placeControls() {
        HudPosition.Bounds box = frame().bounds().offset(offset);
        boolean hudInTopHalf = (box.top() + box.bottom()) / 2 < this.height / 2;
        controlsTop = hudInTopHalf ? this.height - CONTROLS_MARGIN - CONTROLS_HEIGHT : CONTROLS_MARGIN;
        for (AbstractButton button : buttons) {
            button.setY(controlsTop + CONTROLS_HEIGHT - BUTTON_HEIGHT);
        }
    }

    private HudLayer.Frame frame() {
        return HudLayer.Frame.compute(this.width, this.height, IronsSpellsCompat.isAvailable());
    }

    private void setOffset(HudPosition.Offset requested) {
        offset = HudPosition.clamp(requested, this.width, this.height, frame().bounds());
        HudPosition.setPreview(offset);
        placeControls();
    }

    /** Moves a piece on its own, keeping it on screen. */
    private void setPieceOffset(HudPiece piece, HudPosition.Offset requested) {
        HudPosition.Bounds atSetPosition = frame().pieceBounds().get(piece).offset(offset);
        pieces = pieces.with(piece, HudPosition.clamp(requested, this.width, this.height, atSetPosition));
        HudPosition.setPreviewPieces(pieces);
        setOffset(offset);
    }

    /** Puts the set back above the hotbar and every piece back into place. */
    private void reset() {
        pieces = HudPosition.Pieces.NONE;
        HudPosition.setPreviewPieces(pieces);
        setOffset(HudPosition.defaultOffset(this.width, this.height, frame().bounds()));
    }

    private void save() {
        HudPosition.savePieces(pieces);
        HudPosition.save(offset, this.width, this.height, frame().bounds());
        onClose();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void removed() {
        HudPosition.setPreview(null);
        HudPosition.setPreviewPieces(null);
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
            graphics.pose().pushPose();
            graphics.pose().translate(offset.x(), offset.y(), 0);
            HudRenderer.render(graphics, this.font, frame.layout(), HudValues.placeholder(), frame.showArmor(), frame.showHunger(), pieces, false);
            if (!inWorld) {
                HudPosition.Bounds xp = frame.pieceBounds().get(HudPiece.XP).offset(pieces.get(HudPiece.XP));
                graphics.fill(xp.left(), xp.top(), xp.right(), xp.bottom(), XP_BAR_PREVIEW_COLOR);
            }
            graphics.pose().popPose();
        }

        graphics.drawCenteredString(this.font, this.title, this.width / 2, controlsTop, 0xFFFFFF);
        if (dragging) {
            highlightGrabbed(graphics, frame);
        }

        String hint = mode == Mode.ALL ? "rpghealthbar.edit.hint" : "rpghealthbar.edit.hint.one";
        graphics.drawCenteredString(this.font, Component.translatable(hint), this.width / 2, controlsTop + 10, 0xC0C0C0);
    }

    /** Faintly lightens each piece being dragged: all of them, or the one grabbed. */
    private void highlightGrabbed(GuiGraphics graphics, HudLayer.Frame frame) {
        for (Map.Entry<HudPiece, HudPosition.Bounds> entry : frame.pieceBounds().entrySet()) {
            if (mode == Mode.ALL || entry.getKey() == selected) {
                HudPosition.Bounds box = entry.getValue().offset(frame.pieceOffset(entry.getKey())).grow(1);
                graphics.fill(box.left(), box.top(), box.right(), box.bottom(), GRABBED_HIGHLIGHT);
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
            if (frame().bounds().offset(offset).contains(mouseX, mouseY)) {
                dragging = true;
                grabX = mouseX - offset.x();
                grabY = mouseY - offset.y();
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
                setOffset(moved);
            } else if (selected != null) {
                setPieceOffset(selected, moved);
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
                setOffset(offset.plus(nudge));
            } else if (selected != null) {
                setPieceOffset(selected, pieces.get(selected).plus(nudge));
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
