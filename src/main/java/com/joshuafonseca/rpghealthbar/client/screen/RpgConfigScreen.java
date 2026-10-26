package com.joshuafonseca.rpghealthbar.client.screen;

import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
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
        int y = this.height / 4;

        addRenderableWidget(CycleButton.onOffBuilder(ClientConfig.SHOW_ARMOR_BAR.get())
                .create(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("rpghealthbar.config.showArmorBar"),
                        (button, value) -> ClientConfig.set(ClientConfig.SHOW_ARMOR_BAR, value)));
        y += ROW_SPACING;

        addRenderableWidget(CycleButton.onOffBuilder(ClientConfig.SHOW_HUNGER_BAR.get())
                .create(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("rpghealthbar.config.showHungerBar"),
                        (button, value) -> ClientConfig.set(ClientConfig.SHOW_HUNGER_BAR, value)));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(x, this.height - 28, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
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
