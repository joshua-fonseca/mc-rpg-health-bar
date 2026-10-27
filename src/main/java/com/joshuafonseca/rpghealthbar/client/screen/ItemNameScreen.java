package com.joshuafonseca.rpghealthbar.client.screen;

import com.joshuafonseca.rpghealthbar.client.HudLayer;
import com.joshuafonseca.rpghealthbar.client.HudPosition;
import com.joshuafonseca.rpghealthbar.client.HudRenderer;
import com.joshuafonseca.rpghealthbar.client.HudTextPlacer;
import com.joshuafonseca.rpghealthbar.client.HudValues;
import com.joshuafonseca.rpghealthbar.compat.IronsSpellsCompat;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * Sets the Item Name Height, with a preview: the held item's name is drawn where it would go and
 * follows the slider and the arrow keys. In a world the real HUD is shown behind it; from the title
 * screen a stand-in.
 */
public class ItemNameScreen extends Screen {
    private static final int SLIDER_WIDTH = 200;
    private static final int BUTTON_WIDTH = 98;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int HOTBAR_WIDTH = 182;
    private static final int HOTBAR_HEIGHT = 22;
    private static final int HOTBAR_PREVIEW_COLOR = 0xFF2A2A2A;
    private static final int XP_BAR_PREVIEW_COLOR = 0xFF80FF20;
    /** The previewed name is lightened like the piece being dragged in Move HUD. */
    private static final int NAME_HIGHLIGHT = 0x30FFFFFF;
    private static final int HINT_COLOR = 0xC0C0C0;
    /** Title, slider, buttons and two lines of tips. */
    private static final int CONTROLS_HEIGHT = 92;
    private static final int CONTROLS_MARGIN = 8;
    /** The arrow keys move the name this much, or {@link #SHIFT_STEP} with Shift held, as in Move HUD. */
    private static final int STEP = 1;
    private static final int SHIFT_STEP = 10;

    private final Screen parent;
    private HeightSlider slider;
    private int controlsTop;

    public ItemNameScreen(Screen parent) {
        super(Component.translatable("rpghealthbar.itemName.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // At the top, unless the HUD has been moved there; then in the middle.
        HudLayer.Frame frame = HudLayer.Frame.compute(this.width, this.height, IronsSpellsCompat.isAvailable());
        this.controlsTop = frame.bounds().top() < CONTROLS_MARGIN + CONTROLS_HEIGHT ? (this.height - CONTROLS_HEIGHT) / 2 : CONTROLS_MARGIN;
        int y = this.controlsTop + 14;
        this.slider = new HeightSlider(this.width / 2 - SLIDER_WIDTH / 2, y, SLIDER_WIDTH, BUTTON_HEIGHT,
                this.slider != null ? this.slider.offset() : ClientConfig.ITEM_NAME_OFFSET.get());
        this.slider.setTooltip(RpgConfigScreen.tooltip("itemNameOffset", "vanilla", "lower"));
        addRenderableWidget(this.slider);
        y += BUTTON_HEIGHT + BUTTON_GAP;
        int left = this.width / 2 - BUTTON_WIDTH - BUTTON_GAP / 2;
        addRenderableWidget(Button.builder(Component.translatable("rpghealthbar.edit.reset"), button -> this.slider.set(0))
                .bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left + BUTTON_WIDTH + BUTTON_GAP, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // In a world, keep the HUD visible and unblurred, so the name is seen against it.
        if (this.minecraft.level == null) {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        HudLayer.Frame frame = HudLayer.Frame.compute(this.width, this.height, IronsSpellsCompat.isAvailable());

        // Without a survival player the HUD isn't drawn, so draw a stand-in, as Move HUD does.
        boolean inWorld = this.minecraft.level != null;
        if (!inWorld || !HudLayer.isVisible(this.minecraft)) {
            HudRenderer.render(graphics, this.font, frame.layout(), HudValues.placeholder(), frame.showArmor(), frame.showHunger(), frame.pieces(), false);
            if (!inWorld) {
                HudPosition.Bounds xp = frame.pieceBounds().get(HudPiece.XP).offset(frame.pieceOffset(HudPiece.XP));
                graphics.fill(xp.left(), xp.top(), xp.right(), xp.bottom(), XP_BAR_PREVIEW_COLOR);
                int hotbarLeft = this.width / 2 - HOTBAR_WIDTH / 2;
                graphics.fill(hotbarLeft, this.height - HOTBAR_HEIGHT, hotbarLeft + HOTBAR_WIDTH, this.height, HOTBAR_PREVIEW_COLOR);
            }
        }

        Component name = Component.translatable("rpghealthbar.itemName.sample");
        if (this.minecraft.player != null) {
            ItemStack held = this.minecraft.player.getMainHandItem();
            if (!held.isEmpty()) {
                name = held.getHoverName();
            }
        }
        int top = HudTextPlacer.previewNameTop(this.width, this.height, frame, this.slider.offset());
        int x = (this.width - this.font.width(name)) / 2;
        graphics.fill(x - 2, top - 2, x + this.font.width(name) + 2, top + 10, NAME_HIGHLIGHT);
        graphics.drawString(this.font, name, x, top, 0xFFFFFF);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.controlsTop, 0xFFFFFF);
        int tipsTop = this.controlsTop + 14 + (BUTTON_HEIGHT + BUTTON_GAP) * 2 + 2;
        graphics.drawCenteredString(this.font, Component.translatable("rpghealthbar.itemName.keys"), this.width / 2, tipsTop, HINT_COLOR);
        int aboveHotbar = HudTextPlacer.previewAboveHotbarOffset(this.height, frame);
        graphics.drawCenteredString(this.font, Component.translatable("rpghealthbar.itemName.values", String.valueOf(aboveHotbar)),
                this.width / 2, tipsTop + 12, HINT_COLOR);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int direction = switch (keyCode) {
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_RIGHT -> 1;
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_LEFT -> -1;
            default -> 0;
        };
        if (direction != 0) {
            int step = hasShiftDown() ? SHIFT_STEP : STEP;
            this.slider.set(Math.max(ClientConfig.ITEM_NAME_OFFSET_MIN, Math.min(ClientConfig.ITEM_NAME_OFFSET_MAX, this.slider.offset() + direction * step)));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        this.slider.save();
        this.minecraft.setScreen(this.parent);
    }

    /**
     * The Item Name Height, in whole pixels. Saved when let go (or changed with the keys) rather than
     * on every step of a drag, so the config file isn't written over and over; the preview follows
     * the slider as it moves.
     */
    private static class HeightSlider extends AbstractSliderButton {
        private static final int MIN = ClientConfig.ITEM_NAME_OFFSET_MIN;
        private static final int MAX = ClientConfig.ITEM_NAME_OFFSET_MAX;

        private boolean dragging;

        HeightSlider(int x, int y, int width, int height, int offset) {
            super(x, y, width, height, Component.empty(), toSlider(offset));
            updateMessage();
        }

        private static double toSlider(int offset) {
            return (double) (offset - MIN) / (MAX - MIN);
        }

        int offset() {
            return MIN + (int) Math.round(this.value * (MAX - MIN));
        }

        void set(int offset) {
            this.value = toSlider(offset);
            updateMessage();
            save();
        }

        @Override
        protected void updateMessage() {
            int offset = offset();
            Component value = offset == 0 ? Component.translatable("rpghealthbar.config.itemNameOffset.vanilla")
                    : Component.literal(offset > 0 ? "+" + offset : String.valueOf(offset));
            setMessage(Component.translatable("rpghealthbar.itemName.height", value));
        }

        @Override
        protected void applyValue() {
            // Snap to whole pixels, so the knob sits where the number says.
            this.value = toSlider(offset());
            if (!this.dragging) {
                save();
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            this.dragging = true;
            super.onClick(mouseX, mouseY);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            this.dragging = false;
            save();
        }

        void save() {
            if (offset() != ClientConfig.ITEM_NAME_OFFSET.get()) {
                ClientConfig.set(ClientConfig.ITEM_NAME_OFFSET, offset());
            }
        }
    }
}
