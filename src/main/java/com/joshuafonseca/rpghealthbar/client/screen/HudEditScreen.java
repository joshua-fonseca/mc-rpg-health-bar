package com.joshuafonseca.rpghealthbar.client.screen;

import com.joshuafonseca.rpghealthbar.client.HudLayer;
import com.joshuafonseca.rpghealthbar.client.HudLayout;
import com.joshuafonseca.rpghealthbar.client.HudPosition;
import com.joshuafonseca.rpghealthbar.client.HudRenderer;
import com.joshuafonseca.rpghealthbar.client.HudValues;
import com.joshuafonseca.rpghealthbar.compat.IronsSpellsCompat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Drag the whole HUD set to a new position, then Save or Cancel. In a world the real HUD follows
 * the drag; from the title screen a preview is drawn instead.
 */
public class HudEditScreen extends Screen {
    private static final int BUTTON_WIDTH = 70;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int OUTLINE_COLOR = 0xFFFFFF55;
    private static final int XP_BAR_PREVIEW_COLOR = 0xFF80FF20;

    /** Title, hint and buttons; moved to whichever half of the screen the HUD isn't in. */
    private static final int CONTROLS_HEIGHT = 44;
    private static final int CONTROLS_MARGIN = 8;

    private final Screen parent;
    private final List<Button> buttons = new ArrayList<>();
    private int controlsTop;
    /** Shift from the default spot; null until the screen size is known in init(). */
    private HudPosition.Offset offset;
    private boolean dragging;
    private double grabX;
    private double grabY;

    public HudEditScreen(Screen parent) {
        super(Component.translatable("rpghealthbar.edit.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int totalWidth = BUTTON_WIDTH * 3 + BUTTON_GAP * 2;
        int x = this.width / 2 - totalWidth / 2;
        buttons.clear();
        buttons.add(addRenderableWidget(Button.builder(Component.translatable("rpghealthbar.edit.save"), button -> save())
                .bounds(x, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build()));
        buttons.add(addRenderableWidget(Button.builder(Component.translatable("rpghealthbar.edit.reset"), button -> setOffset(HudPosition.defaultOffset(this.width, this.height, frame().bounds())))
                .bounds(x + BUTTON_WIDTH + BUTTON_GAP, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build()));
        buttons.add(addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(x + (BUTTON_WIDTH + BUTTON_GAP) * 2, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build()));

        // Start from the saved position, or re-clamp the dragged one if the window was resized.
        // This also places the controls.
        setOffset(offset != null ? offset : HudPosition.saved(this.width, this.height, frame().bounds()));
    }

    /** Keeps the controls out of the way: at the bottom when the HUD is in the top half, else at the top. */
    private void placeControls() {
        HudPosition.Bounds box = frame().bounds().offset(offset);
        boolean hudInTopHalf = (box.top() + box.bottom()) / 2 < this.height / 2;
        controlsTop = hudInTopHalf ? this.height - CONTROLS_MARGIN - CONTROLS_HEIGHT : CONTROLS_MARGIN;
        for (Button button : buttons) {
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

    private void save() {
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
            HudRenderer.render(graphics, this.font, frame.layout(), HudValues.placeholder(), frame.showArmor(), frame.showHunger());
            if (!inWorld) {
                int xpTop = frame.bounds().bottom() - 5;
                graphics.fill(frame.layout().x(), xpTop, frame.layout().x() + HudLayout.WIDTH, xpTop + 5, XP_BAR_PREVIEW_COLOR);
            }
            graphics.pose().popPose();
        }

        HudPosition.Bounds box = frame.bounds().offset(offset);
        graphics.renderOutline(box.left() - 2, box.top() - 2, box.right() - box.left() + 4, box.bottom() - box.top() + 4, OUTLINE_COLOR);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, controlsTop, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("rpghealthbar.edit.hint"), this.width / 2, controlsTop + 10, 0xC0C0C0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && frame().bounds().offset(offset).contains(mouseX, mouseY)) {
            dragging = true;
            grabX = mouseX - offset.x();
            grabY = mouseY - offset.y();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            setOffset(new HudPosition.Offset((int) Math.round(mouseX - grabX), (int) Math.round(mouseY - grabY)));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** Arrow keys nudge by 1 pixel, or 10 while holding Shift. */
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
            setOffset(new HudPosition.Offset(offset.x() + dx, offset.y() + dy));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
