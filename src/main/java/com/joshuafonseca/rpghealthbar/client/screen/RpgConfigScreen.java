package com.joshuafonseca.rpghealthbar.client.screen;

import com.joshuafonseca.rpghealthbar.compat.AppleSkinCompat;
import com.joshuafonseca.rpghealthbar.config.AbsorptionDisplay;
import com.joshuafonseca.rpghealthbar.config.AirPosition;
import com.joshuafonseca.rpghealthbar.config.BarSize;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HalfBarMode;
import com.joshuafonseca.rpghealthbar.config.HealthUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Opened from Mods → RPG Health Bar → Config. The options are in sections, in a list that scrolls
 * when the window is too small. Changes show on the HUD right away; Save Settings keeps them, and
 * Back (or Esc) puts back what they were when the screen opened.
 */
public class RpgConfigScreen extends Screen {
    private static final int ROW_WIDTH = 310;
    private static final int ROW_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 20;
    /** Options are "Name: ... [value]": the name as text on the left, a button with the value on the right. */
    private static final int VALUE_WIDTH = 110;
    private static final int BUTTON_GAP = 10;
    private static final int FOOTER_BUTTON_WIDTH = 150;
    private static final int LABEL_COLOR = 0xFFFFFF;
    private static final int HEADER_COLOR = 0xFFFFFF;
    /** Dark yellow, for notes beside a section's name. */
    private static final int NOTE_COLOR = 0xC8A43C;
    private static final int DIVIDER_COLOR = 0x80FFFFFF;

    /** The settings this screen changes, put back by Back. Item Name and Move HUD save on their own pages. */
    private static final List<ModConfigSpec.ConfigValue<?>> EDITED = List.of(ClientConfig.ENABLED, ClientConfig.ARMOR_MODE,
            ClientConfig.HUNGER_MODE, ClientConfig.AIR_POSITION, ClientConfig.HEALTH_UNIT, ClientConfig.ABSORPTION_DISPLAY,
            ClientConfig.SHOW_HEALTH_TEXT, ClientConfig.HEALTH_SIZE, ClientConfig.SHOW_MANA_TEXT, ClientConfig.MANA_SIZE);

    private final Screen parent;
    private final Map<ModConfigSpec.ConfigValue<?>, Object> opened = new HashMap<>();
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    @Nullable
    private OptionList list;

    public RpgConfigScreen(Screen parent) {
        super(Component.translatable("rpghealthbar.config.title"));
        this.parent = parent;
        for (ModConfigSpec.ConfigValue<?> value : EDITED) {
            this.opened.put(value, value.get());
        }
    }

    @Override
    protected void init() {
        // Rebuilt on resize and when coming back from Item Name or Move HUD; keep the scroll position.
        double scroll = this.list != null ? this.list.getScrollAmount() : 0;
        this.layout.addTitleHeader(this.title, this.font);
        this.list = this.layout.addToContents(new OptionList(this.minecraft, this.width, this.layout));
        addOptions(this.list);
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(CommonComponents.GUI_BACK, button -> onClose()).width(FOOTER_BUTTON_WIDTH).build());
        footer.addChild(Button.builder(Component.translatable("rpghealthbar.config.save"), button -> save()).width(FOOTER_BUTTON_WIDTH).build());
        this.layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
        this.list.setScrollAmount(scroll);
    }

    private void addOptions(OptionList list) {
        list.addEntry(new WidgetRow(null, CycleButton.booleanBuilder(Component.translatable("rpghealthbar.config.enabled.on"),
                        Component.translatable("rpghealthbar.config.enabled.off"))
                .withInitialValue(ClientConfig.ENABLED.get())
                .withTooltip(value -> tooltip("enabled", "on", "off"))
                .create(0, 0, ROW_WIDTH, BUTTON_HEIGHT, Component.translatable("rpghealthbar.config.enabled"),
                        (button, value) -> ClientConfig.set(ClientConfig.ENABLED, value))));

        list.addEntry(new Header(Component.translatable("rpghealthbar.config.section.armorHunger"),
                AppleSkinCompat.isInstalled() ? Component.translatable("rpghealthbar.config.appleSkinDetected") : null));
        // Each click cycles Vanilla → Bar → Hidden.
        list.addEntry(option("armorMode", CycleButton.builder(HalfBarMode::displayName)
                .withValues(HalfBarMode.values())
                .withInitialValue(ClientConfig.ARMOR_MODE.get())
                .withTooltip(value -> tooltip("armorMode", "vanilla", "bar", "hidden")), ClientConfig.ARMOR_MODE));
        list.addEntry(option("hungerMode", CycleButton.builder(HalfBarMode::displayName)
                .withValues(HalfBarMode.values())
                .withInitialValue(ClientConfig.HUNGER_MODE.get())
                .withTooltip(value -> tooltip("hungerMode", "vanilla", "bar", "hidden")), ClientConfig.HUNGER_MODE));
        list.addEntry(option("airPosition", CycleButton.builder(AirPosition::displayName)
                .withValues(AirPosition.values())
                .withInitialValue(ClientConfig.AIR_POSITION.get())
                .withTooltip(value -> tooltip("airPosition", "above", "below")), ClientConfig.AIR_POSITION));

        list.addEntry(new Header(Component.translatable("rpghealthbar.config.section.health"), null));
        list.addEntry(option("healthUnit", CycleButton.builder(HealthUnit::displayName)
                .withValues(HealthUnit.values())
                .withInitialValue(ClientConfig.HEALTH_UNIT.get())
                .withTooltip(value -> tooltip("healthUnit", "hp", "hearts")), ClientConfig.HEALTH_UNIT));
        list.addEntry(option("absorptionDisplay", CycleButton.builder(AbsorptionDisplay::displayName)
                .withValues(AbsorptionDisplay.values())
                .withInitialValue(ClientConfig.ABSORPTION_DISPLAY.get())
                .withTooltip(value -> tooltip("absorptionDisplay", "separate", "combined")), ClientConfig.ABSORPTION_DISPLAY));
        list.addEntry(option("showHealthText", CycleButton.onOffBuilder(ClientConfig.SHOW_HEALTH_TEXT.get())
                .withTooltip(value -> tooltip("showHealthText", "on", "off")), ClientConfig.SHOW_HEALTH_TEXT));
        list.addEntry(option("healthSize", CycleButton.builder(BarSize::displayName)
                .withValues(BarSize.values())
                .withInitialValue(ClientConfig.HEALTH_SIZE.get())
                .withTooltip(value -> tooltip("healthSize", "full", "half")), ClientConfig.HEALTH_SIZE));

        list.addEntry(new Header(Component.translatable("rpghealthbar.config.section.mana"), null));
        list.addEntry(option("showManaText", CycleButton.onOffBuilder(ClientConfig.SHOW_MANA_TEXT.get())
                .withTooltip(value -> tooltip("showManaText", "on", "off")), ClientConfig.SHOW_MANA_TEXT));
        list.addEntry(option("manaSize", CycleButton.builder(BarSize::displayName)
                .withValues(BarSize.values())
                .withInitialValue(ClientConfig.MANA_SIZE.get())
                .withTooltip(value -> tooltip("manaSize", "full", "half")), ClientConfig.MANA_SIZE));

        list.addEntry(new Header(Component.translatable("rpghealthbar.config.section.positioning"), null));
        list.addEntry(new WidgetRow(null,
                Button.builder(Component.translatable("rpghealthbar.config.itemName"), button -> this.minecraft.setScreen(new ItemNameScreen(this)))
                        .size(0, BUTTON_HEIGHT)
                        .tooltip(tooltip("itemName"))
                        .build(),
                Button.builder(Component.translatable("rpghealthbar.config.moveHud"), button -> this.minecraft.setScreen(new HudEditScreen(this)))
                        .size(0, BUTTON_HEIGHT)
                        .tooltip(tooltip("moveHud"))
                        .build()));
    }

    /** A "Name: [value]" row, whose button saves each change. */
    private static <T> WidgetRow option(String key, CycleButton.Builder<T> builder, ModConfigSpec.ConfigValue<T> value) {
        Component name = Component.translatable("rpghealthbar.config." + key);
        return new WidgetRow(name.copy().append(":"), builder.displayOnlyValue()
                .create(0, 0, VALUE_WIDTH, BUTTON_HEIGHT, name, (button, newValue) -> ClientConfig.set(value, newValue)));
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
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.list != null) {
            this.list.updateSize(this.width, this.layout);
        }
    }

    /** Keeps the changes. */
    private void save() {
        this.opened.clear();
        this.minecraft.setScreen(this.parent);
    }

    /** Back and Esc: puts back the settings as they were when the screen opened. */
    @Override
    public void onClose() {
        for (Map.Entry<ModConfigSpec.ConfigValue<?>, Object> entry : this.opened.entrySet()) {
            restore(entry.getKey(), entry.getValue());
        }
        this.minecraft.setScreen(this.parent);
    }

    @SuppressWarnings("unchecked")
    private static <T> void restore(ModConfigSpec.ConfigValue<T> value, Object opened) {
        if (!value.get().equals(opened)) {
            ClientConfig.set(value, (T) opened);
        }
    }

    private static class OptionList extends ContainerObjectSelectionList<Row> {
        OptionList(Minecraft minecraft, int width, HeaderAndFooterLayout layout) {
            super(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight(), ROW_HEIGHT);
            this.centerListVertically = false;
        }

        @Override
        public int addEntry(Row row) {
            return super.addEntry(row);
        }

        @Override
        public int getRowWidth() {
            return ROW_WIDTH;
        }
    }

    private abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
    }

    /** A section's name, an optional note beside it, and a divider under them. */
    private class Header extends Row {
        private final Component name;
        @Nullable
        private final Component note;

        Header(Component name, @Nullable Component note) {
            this.name = name;
            this.note = note == null ? null : note.copy().withStyle(ChatFormatting.ITALIC);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            int textTop = top + 6;
            graphics.drawString(RpgConfigScreen.this.font, this.name, left, textTop, HEADER_COLOR);
            if (this.note != null) {
                graphics.drawString(RpgConfigScreen.this.font, this.note, left + RpgConfigScreen.this.font.width(this.name) + 6, textTop, NOTE_COLOR);
            }
            graphics.fill(left, textTop + 11, left + width, textTop + 12, DIVIDER_COLOR);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }

    /**
     * With a name: the name on the left and one button on the right. Without: the buttons share the
     * row's width.
     */
    private static class WidgetRow extends Row {
        @Nullable
        private final Component label;
        private final List<AbstractWidget> widgets;

        WidgetRow(@Nullable Component label, AbstractWidget... widgets) {
            this.label = label;
            this.widgets = List.of(widgets);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            if (this.label != null) {
                graphics.drawString(Minecraft.getInstance().font, this.label, left, top + (BUTTON_HEIGHT - 8) / 2, LABEL_COLOR);
                AbstractWidget button = this.widgets.get(0);
                button.setPosition(left + width - button.getWidth(), top);
            } else {
                int each = (width - BUTTON_GAP * (this.widgets.size() - 1)) / this.widgets.size();
                for (int i = 0; i < this.widgets.size(); i++) {
                    AbstractWidget widget = this.widgets.get(i);
                    widget.setWidth(each);
                    widget.setPosition(left + i * (each + BUTTON_GAP), top);
                }
            }
            for (AbstractWidget widget : this.widgets) {
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.widgets;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.widgets;
        }
    }
}
