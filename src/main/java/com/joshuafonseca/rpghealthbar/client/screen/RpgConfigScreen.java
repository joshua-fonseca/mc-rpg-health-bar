package com.joshuafonseca.rpghealthbar.client.screen;

import com.joshuafonseca.rpghealthbar.config.AbsorptionDisplay;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HalfBarMode;
import com.joshuafonseca.rpghealthbar.config.HealthUnit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;

/** Opened from Mods → RPG Health Bar → Config. Every change is saved and applied immediately. */
public class RpgConfigScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_SPACING = 24;

    private final Screen parent;

    public RpgConfigScreen(Screen parent) {
        super(Component.translatable("rpghealthbar.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - BUTTON_WIDTH / 2;
        int y = 36;

        // Each click cycles Vanilla → Bar → Hidden.
        addRenderableWidget(CycleButton.builder(HalfBarMode::displayName)
                .withValues(HalfBarMode.values())
                .withInitialValue(ClientConfig.ARMOR_MODE.get())
                .withTooltip(value -> tooltip("armorMode"))
                .create(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("rpghealthbar.config.armorMode"),
                        (button, value) -> ClientConfig.set(ClientConfig.ARMOR_MODE, value)));
        y += ROW_SPACING;

        addRenderableWidget(CycleButton.builder(HalfBarMode::displayName)
                .withValues(HalfBarMode.values())
                .withInitialValue(ClientConfig.HUNGER_MODE.get())
                .withTooltip(value -> tooltip("hungerMode"))
                .create(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("rpghealthbar.config.hungerMode"),
                        (button, value) -> ClientConfig.set(ClientConfig.HUNGER_MODE, value)));
        y += ROW_SPACING;

        addRenderableWidget(CycleButton.builder(HealthUnit::displayName)
                .withValues(HealthUnit.values())
                .withInitialValue(ClientConfig.HEALTH_UNIT.get())
                .withTooltip(value -> tooltip("healthUnit"))
                .create(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("rpghealthbar.config.healthUnit"),
                        (button, value) -> ClientConfig.set(ClientConfig.HEALTH_UNIT, value)));
        y += ROW_SPACING;

        addRenderableWidget(CycleButton.builder(AbsorptionDisplay::displayName)
                .withValues(AbsorptionDisplay.values())
                .withInitialValue(ClientConfig.ABSORPTION_DISPLAY.get())
                .withTooltip(value -> tooltip("absorptionDisplay"))
                .create(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("rpghealthbar.config.absorptionDisplay"),
                        (button, value) -> ClientConfig.set(ClientConfig.ABSORPTION_DISPLAY, value)));
        y += ROW_SPACING;

        addRenderableWidget(Button.builder(Component.translatable("rpghealthbar.config.moveHud"), button -> this.minecraft.setScreen(new HudEditScreen(this)))
                .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(tooltip("moveHud"))
                .build());
        y += ROW_SPACING;

        addRenderableWidget(Button.builder(Component.translatable("rpghealthbar.config.resetPosition"), button -> ClientConfig.resetPosition())
                .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(tooltip("resetPosition"))
                .build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(x, this.height - 28, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    /** Hover text listing the button's possible values and what each does. */
    private static Tooltip tooltip(String option) {
        return Tooltip.create(Component.translatable("rpghealthbar.config." + option + ".tooltip"));
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
