package com.joshuafonseca.rpghealthbar.client.screen;

import com.joshuafonseca.rpghealthbar.config.AbsorptionDisplay;
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

/** Opened from Mods → RPG Health Bar → Config. Every change is saved and applied immediately. */
public class RpgConfigScreen extends Screen {
    /** Two columns of buttons, so the screen fits small windows. */
    private static final int BUTTON_WIDTH = 155;
    private static final int COLUMN_GAP = 10;
    private static final int DONE_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_SPACING = 24;

    private final Screen parent;

    public RpgConfigScreen(Screen parent) {
        super(Component.translatable("rpghealthbar.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - COLUMN_GAP / 2 - BUTTON_WIDTH;
        int right = this.width / 2 + COLUMN_GAP / 2;
        int y = 36;

        // Each click cycles Vanilla → Bar → Hidden.
        addRenderableWidget(CycleButton.builder(HalfBarMode::displayName)
                .withValues(HalfBarMode.values())
                .withInitialValue(ClientConfig.ARMOR_MODE.get())
                .withTooltip(value -> tooltip("armorMode", "vanilla", "bar", "hidden"))
                .create(left, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("rpghealthbar.config.armorMode"),
                        (button, value) -> ClientConfig.set(ClientConfig.ARMOR_MODE, value)));
        addRenderableWidget(CycleButton.builder(HalfBarMode::displayName)
                .withValues(HalfBarMode.values())
                .withInitialValue(ClientConfig.HUNGER_MODE.get())
                .withTooltip(value -> tooltip("hungerMode", "vanilla", "bar", "hidden"))
                .create(right, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("rpghealthbar.config.hungerMode"),
                        (button, value) -> ClientConfig.set(ClientConfig.HUNGER_MODE, value)));
        y += ROW_SPACING;

        addRenderableWidget(CycleButton.builder(HealthUnit::displayName)
                .withValues(HealthUnit.values())
                .withInitialValue(ClientConfig.HEALTH_UNIT.get())
                .withTooltip(value -> tooltip("healthUnit", "hp", "hearts"))
                .create(left, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("rpghealthbar.config.healthUnit"),
                        (button, value) -> ClientConfig.set(ClientConfig.HEALTH_UNIT, value)));
        addRenderableWidget(CycleButton.builder(AbsorptionDisplay::displayName)
                .withValues(AbsorptionDisplay.values())
                .withInitialValue(ClientConfig.ABSORPTION_DISPLAY.get())
                .withTooltip(value -> tooltip("absorptionDisplay", "separate", "combined"))
                .create(right, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("rpghealthbar.config.absorptionDisplay"),
                        (button, value) -> ClientConfig.set(ClientConfig.ABSORPTION_DISPLAY, value)));
        y += ROW_SPACING;

        addRenderableWidget(CycleButton.onOffBuilder(ClientConfig.SHOW_HEALTH_TEXT.get())
                .withTooltip(value -> tooltip("showHealthText", "on", "off"))
                .create(left, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("rpghealthbar.config.showHealthText"),
                        (button, value) -> ClientConfig.set(ClientConfig.SHOW_HEALTH_TEXT, value)));
        addRenderableWidget(CycleButton.onOffBuilder(ClientConfig.SHOW_MANA_TEXT.get())
                .withTooltip(value -> tooltip("showManaText", "on", "off"))
                .create(right, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("rpghealthbar.config.showManaText"),
                        (button, value) -> ClientConfig.set(ClientConfig.SHOW_MANA_TEXT, value)));
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

    /**
     * Hover text: a description, then each possible value on its own paragraph with its name highlighted.
     * The first value is the default.
     */
    private static Tooltip tooltip(String option, String... values) {
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
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
