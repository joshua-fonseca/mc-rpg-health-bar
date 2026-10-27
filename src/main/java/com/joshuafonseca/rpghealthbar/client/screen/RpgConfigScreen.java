package com.joshuafonseca.rpghealthbar.client.screen;

import com.joshuafonseca.rpghealthbar.config.AbsorptionDisplay;
import com.joshuafonseca.rpghealthbar.config.BarSize;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HalfBarMode;
import com.joshuafonseca.rpghealthbar.config.HealthUnit;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.MutableComponent;
import java.util.ArrayList;
import java.util.List;

/** Opened from Mods → RPG Health Bar → Config. Every change is saved and applied immediately. */
public class RpgConfigScreen extends Screen {
    /** Two columns of buttons, so the screen fits small windows. */
    private static final int BUTTON_WIDTH = 155;
    private static final int COLUMN_GAP = 10;
    private static final int DONE_WIDTH = 200;
    /** Options are "Name: [value]": the name as text, then a button showing only the value. */
    private static final int LABEL_WIDTH = 75;
    private static final int VALUE_WIDTH = BUTTON_WIDTH - LABEL_WIDTH;
    private static final int LABEL_COLOR = 0xFFFFFF;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_SPACING = 24;

    private final Screen parent;
    private final List<Label> labels = new ArrayList<>();

    private record Label(Component text, int x, int y) {
    }

    public RpgConfigScreen(Screen parent) {
        super(Component.translatable("rpghealthbar.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.labels.clear();
        int left = this.width / 2 - COLUMN_GAP / 2 - BUTTON_WIDTH;
        int right = this.width / 2 + COLUMN_GAP / 2;
        int y = 36;

        // Each click cycles Vanilla → Bar → Hidden.
        addRenderableWidget(CycleButton.builder(HalfBarMode::displayName)
                .withValues(HalfBarMode.values())
                .withInitialValue(ClientConfig.ARMOR_MODE.get())
                .withTooltip(value -> tooltip("armorMode", "vanilla", "bar", "hidden"))
                .displayOnlyValue()
                .create(left + LABEL_WIDTH, y, VALUE_WIDTH, BUTTON_HEIGHT, label(left, y, "rpghealthbar.config.armorMode"),
                        (button, value) -> ClientConfig.set(ClientConfig.ARMOR_MODE, value)));
        addRenderableWidget(CycleButton.builder(HalfBarMode::displayName)
                .withValues(HalfBarMode.values())
                .withInitialValue(ClientConfig.HUNGER_MODE.get())
                .withTooltip(value -> tooltip("hungerMode", "vanilla", "bar", "hidden"))
                .displayOnlyValue()
                .create(right + LABEL_WIDTH, y, VALUE_WIDTH, BUTTON_HEIGHT, label(right, y, "rpghealthbar.config.hungerMode"),
                        (button, value) -> ClientConfig.set(ClientConfig.HUNGER_MODE, value)));
        y += ROW_SPACING;

        addRenderableWidget(CycleButton.builder(HealthUnit::displayName)
                .withValues(HealthUnit.values())
                .withInitialValue(ClientConfig.HEALTH_UNIT.get())
                .withTooltip(value -> tooltip("healthUnit", "hp", "hearts"))
                .displayOnlyValue()
                .create(left + LABEL_WIDTH, y, VALUE_WIDTH, BUTTON_HEIGHT, label(left, y, "rpghealthbar.config.healthUnit"),
                        (button, value) -> ClientConfig.set(ClientConfig.HEALTH_UNIT, value)));
        addRenderableWidget(CycleButton.builder(AbsorptionDisplay::displayName)
                .withValues(AbsorptionDisplay.values())
                .withInitialValue(ClientConfig.ABSORPTION_DISPLAY.get())
                .withTooltip(value -> tooltip("absorptionDisplay", "separate", "combined"))
                .displayOnlyValue()
                .create(right + LABEL_WIDTH, y, VALUE_WIDTH, BUTTON_HEIGHT, label(right, y, "rpghealthbar.config.absorptionDisplay"),
                        (button, value) -> ClientConfig.set(ClientConfig.ABSORPTION_DISPLAY, value)));
        y += ROW_SPACING;

        addRenderableWidget(CycleButton.onOffBuilder(ClientConfig.SHOW_HEALTH_TEXT.get())
                .withTooltip(value -> tooltip("showHealthText", "on", "off"))
                .displayOnlyValue()
                .create(left + LABEL_WIDTH, y, VALUE_WIDTH, BUTTON_HEIGHT, label(left, y, "rpghealthbar.config.showHealthText"),
                        (button, value) -> ClientConfig.set(ClientConfig.SHOW_HEALTH_TEXT, value)));
        addRenderableWidget(CycleButton.onOffBuilder(ClientConfig.SHOW_MANA_TEXT.get())
                .withTooltip(value -> tooltip("showManaText", "on", "off"))
                .displayOnlyValue()
                .create(right + LABEL_WIDTH, y, VALUE_WIDTH, BUTTON_HEIGHT, label(right, y, "rpghealthbar.config.showManaText"),
                        (button, value) -> ClientConfig.set(ClientConfig.SHOW_MANA_TEXT, value)));
        y += ROW_SPACING;

        addRenderableWidget(CycleButton.builder(BarSize::displayName)
                .withValues(BarSize.values())
                .withInitialValue(ClientConfig.HEALTH_SIZE.get())
                .withTooltip(value -> tooltip("healthSize", "full", "half"))
                .displayOnlyValue()
                .create(left + LABEL_WIDTH, y, VALUE_WIDTH, BUTTON_HEIGHT, label(left, y, "rpghealthbar.config.healthSize"),
                        (button, value) -> ClientConfig.set(ClientConfig.HEALTH_SIZE, value)));
        addRenderableWidget(CycleButton.builder(BarSize::displayName)
                .withValues(BarSize.values())
                .withInitialValue(ClientConfig.MANA_SIZE.get())
                .withTooltip(value -> tooltip("manaSize", "full", "half"))
                .displayOnlyValue()
                .create(right + LABEL_WIDTH, y, VALUE_WIDTH, BUTTON_HEIGHT, label(right, y, "rpghealthbar.config.manaSize"),
                        (button, value) -> ClientConfig.set(ClientConfig.MANA_SIZE, value)));
        y += ROW_SPACING;

        addRenderableWidget(Button.builder(Component.translatable("rpghealthbar.config.itemName"), button -> this.minecraft.setScreen(new ItemNameScreen(this)))
                .bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(tooltip("itemName"))
                .build());
        y += ROW_SPACING;

        addRenderableWidget(Button.builder(Component.translatable("rpghealthbar.config.moveHud"), button -> this.minecraft.setScreen(new HudEditScreen(this)))
                .bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(tooltip("moveHud"))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("rpghealthbar.config.resetPosition"), button -> ClientConfig.resetPosition())
                .bounds(right, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(tooltip("resetPosition"))
                .build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 - DONE_WIDTH / 2, this.height - 28, DONE_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    /** Adds the option's name as text left of its button, and returns it (for narration). */
    private Component label(int x, int y, String key) {
        Component text = Component.translatable(key).append(":");
        this.labels.add(new Label(text, x, y + (BUTTON_HEIGHT - 8) / 2));
        return Component.translatable(key);
    }

    /**
     * Hover text: a description, then each possible value on its own paragraph with its name highlighted.
     * The first value is the default.
     */
    /** Also used by the position editor. */
    static Tooltip tooltip(String option, String... values) {
        String key = "rpghealthbar.config." + option + ".tooltip";
        MutableComponent text = Component.translatable(key);
        for (int i = 0; i < values.length; i++) {
            String valueKey = key + "." + values[i];
            text.append("\n\n").append(Component.translatable(valueKey + ".name").withStyle(ChatFormatting.GOLD));
            if (i == 0) {
                text.append(" ").append(Component.translatable("rpghealthbar.config.tooltip.default").withStyle(ChatFormatting.GRAY));
            }
            text.append(Component.literal(": ").withStyle(ChatFormatting.GOLD)).append(Component.translatable(valueKey));
        }
        return Tooltip.create(text);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        for (Label label : this.labels) {
            graphics.drawString(this.font, label.text(), label.x(), label.y(), LABEL_COLOR);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
