package com.joshuafonseca.rpghealthbar.client.screen;

import com.joshuafonseca.rpghealthbar.compat.AppleSkinCompat;
import com.joshuafonseca.rpghealthbar.compat.IronsSpellsCompat;
import com.joshuafonseca.rpghealthbar.config.AbsorptionDisplay;
import com.joshuafonseca.rpghealthbar.config.AirPosition;
import com.joshuafonseca.rpghealthbar.config.BarSize;
import com.joshuafonseca.rpghealthbar.config.BarText;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HalfBarMode;
import com.joshuafonseca.rpghealthbar.config.HealthUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Opened from Mods → RPG Health Bar → Config. The options are in sections, in a list that scrolls
 * when the window is too small. Changes show on the HUD right away; Save Settings keeps them, and
 * Back (or Esc) puts back what they were when the screen opened.
 */
public class RpgConfigScreen extends Screen {
    private static final int ROW_WIDTH = 310;
    /** Narrower, to the right, when there's room for the preview on the left. */
    private static final int PREVIEW_ROW_WIDTH = 260;
    /** Either side of the rows, inside the list: room for the scrollbar. */
    private static final int LIST_SIDE = 18;
    private static final int PREVIEW_MARGIN = 8;
    /** Smaller than this and the preview is too small to read, so it's left out. */
    private static final float MIN_PREVIEW_SCALE = 0.7F;
    private static final ResourceLocation MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/menu_list_background.png");
    private static final ResourceLocation INWORLD_MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");
    private static final int ROW_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 20;
    /** Options are "Name: ... [value]": the name as text on the left, a button with the value on the right. */
    private static final int VALUE_WIDTH = 110;
    private static final int BUTTON_GAP = 10;
    private static final int FOOTER_BUTTON_WIDTH = 150;
    private static final int LABEL_COLOR = 0xFFFFFF;
    private static final int HEADER_COLOR = 0xFFFFFF;
    /** Dark yellow, for a note that a supported mod is installed. */
    private static final int NOTE_COLOR = 0xC8A43C;
    /** Darker, for the note on whether Iron's Spells is installed. */
    private static final int IRONS_NOTE_COLOR = 0x8C8C8C;
    /** Room either side of the Mod State button's text. */
    private static final int TOGGLE_PADDING = 24;
    private static final int DIVIDER_COLOR = 0x80FFFFFF;
    private static final int NOTE_GAP = 6;

    /** The settings this screen changes, put back by Back. Item Name and Move HUD save on their own pages. */
    private static final List<ModConfigSpec.ConfigValue<?>> EDITED = List.of(ClientConfig.ENABLED, ClientConfig.ARMOR_MODE,
            ClientConfig.HUNGER_MODE, ClientConfig.AIR_POSITION, ClientConfig.HEALTH_UNIT, ClientConfig.ABSORPTION_DISPLAY,
            ClientConfig.HEALTH_TEXT, ClientConfig.SHOW_HEALTH_TEXT, ClientConfig.HEALTH_SIZE, ClientConfig.MANA_TEXT, ClientConfig.SHOW_MANA_TEXT,
            ClientConfig.MANA_SIZE);

    private final Screen parent;
    private final Map<ModConfigSpec.ConfigValue<?>, Object> opened = new HashMap<>();
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    @Nullable
    private OptionList list;
    /** The preview's scale, or 0 when there's no room for it; and where it's centered. */
    private float previewScale;
    private int previewX;
    private int previewY;

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
        Component enabled = Component.translatable("rpghealthbar.config.enabled.on");
        Component disabled = Component.translatable("rpghealthbar.config.enabled.off");
        Component modState = Component.translatable("rpghealthbar.config.enabled");
        // As wide as its text with either value, so it doesn't change size when clicked.
        int toggleWidth = TOGGLE_PADDING * 2 + Math.max(
                this.font.width(CommonComponents.optionNameValue(modState, enabled)), this.font.width(CommonComponents.optionNameValue(modState, disabled)));
        list.addEntry(new WidgetRow(null, CycleButton.booleanBuilder(enabled, disabled)
                .withInitialValue(ClientConfig.ENABLED.get())
                .withTooltip(value -> tooltip("enabled", "on", "off"))
                .create(0, 0, toggleWidth, BUTTON_HEIGHT, modState, (button, value) -> ClientConfig.set(ClientConfig.ENABLED, value))));

        list.addEntry(new Header(Component.translatable("rpghealthbar.config.section.armorHunger"),
                AppleSkinCompat.isInstalled() ? Component.translatable("rpghealthbar.config.appleSkinDetected") : null, NOTE_COLOR));
        // Each click cycles Vanilla → Bar → Hidden.
        list.addEntry(option("armorMode", HalfBarMode.values(), ClientConfig.ARMOR_MODE, HalfBarMode::displayName, "vanilla", "bar", "hidden"));
        list.addEntry(option("hungerMode", HalfBarMode.values(), ClientConfig.HUNGER_MODE, HalfBarMode::displayName, "vanilla", "bar", "hidden"));
        list.addEntry(option("airPosition", AirPosition.values(), ClientConfig.AIR_POSITION, AirPosition::displayName, "above", "below"));

        list.addEntry(new Header(Component.translatable("rpghealthbar.config.section.health"), null, NOTE_COLOR));
        list.addEntry(option("healthUnit", HealthUnit.values(), ClientConfig.HEALTH_UNIT, HealthUnit::displayName, "hp", "hearts"));
        list.addEntry(option("absorptionDisplay", AbsorptionDisplay.values(), ClientConfig.ABSORPTION_DISPLAY, AbsorptionDisplay::displayName, "separate", "combined"));
        list.addEntry(textOption("healthText", ClientConfig.healthText(), ClientConfig.HEALTH_TEXT, ClientConfig.SHOW_HEALTH_TEXT));
        list.addEntry(option("healthSize", BarSize.values(), ClientConfig.HEALTH_SIZE, BarSize::displayName, "full", "half"));

        // The mana options only do something with Iron's Spells, so without it they can't be changed.
        boolean irons = IronsSpellsCompat.isAvailable();
        list.addEntry(new Header(Component.translatable("rpghealthbar.config.section.mana"),
                Component.translatable(irons ? "rpghealthbar.config.ironsDetected" : "rpghealthbar.config.ironsNotDetected"), IRONS_NOTE_COLOR));
        WidgetRow manaText = textOption("manaText", ClientConfig.manaText(), ClientConfig.MANA_TEXT, ClientConfig.SHOW_MANA_TEXT);
        WidgetRow manaSize = option("manaSize", BarSize.values(), ClientConfig.MANA_SIZE, BarSize::displayName, "full", "half");
        manaText.setActive(irons);
        manaSize.setActive(irons);
        list.addEntry(manaText);
        list.addEntry(manaSize);

        list.addEntry(new Header(Component.translatable("rpghealthbar.config.section.positioning"), null, NOTE_COLOR));
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

    /** A "Name: [value]" row, whose button cycles through the values and saves each change. */
    private static <T> WidgetRow option(String key, T[] values, ModConfigSpec.ConfigValue<T> value, Function<T, Component> displayName,
                                        String... tooltipValues) {
        return option(key, values, value.get(), displayName, newValue -> ClientConfig.set(value, newValue), tooltipValues);
    }

    /**
     * A "Name: [value]" row; {@code tooltipValues} name each value's paragraph in the hover text,
     * the default first.
     */
    private static <T> WidgetRow option(String key, T[] values, T current, Function<T, Component> displayName, Consumer<T> onChange,
                                        String... tooltipValues) {
        Component name = Component.translatable("rpghealthbar.config." + key);
        Tooltip tooltip = tooltip(key, tooltipValues);
        return new WidgetRow(name.copy().append(":"), CycleButton.builder(displayName)
                .withValues(values)
                .withInitialValue(current)
                .withTooltip(value -> tooltip)
                .displayOnlyValue()
                .create(0, 0, VALUE_WIDTH, BUTTON_HEIGHT, name, (button, newValue) -> onChange.accept(newValue)));
    }

    /**
     * Where a bar's numbers go. Setting it also clears the Off saved before this option existed
     * ({@code legacyShown}), so the choice takes effect.
     */
    private static WidgetRow textOption(String key, BarText current, ModConfigSpec.EnumValue<BarText> value, ModConfigSpec.BooleanValue legacyShown) {
        return option(key, BarText.values(), current, BarText::displayName, text -> {
            ClientConfig.set(value, text);
            if (!legacyShown.get()) {
                ClientConfig.set(legacyShown, true);
            }
        }, "left", "right", "center", "off");
    }

    /**
     * Hover text: a description, then each possible value on its own paragraph with its name highlighted.
     * The first value is the default. Also used by the Item Name and Move HUD pages.
     */
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
        if (this.list == null) {
            return;
        }
        // The preview goes left of the list, scaled down to fit the room there.
        int listWidth = PREVIEW_ROW_WIDTH + LIST_SIDE * 2;
        int room = this.width - listWidth - PREVIEW_MARGIN * 2;
        float scale = Math.min(1.0F, Math.min((float) room / ConfigPreview.WIDTH, (float) this.layout.getContentHeight() / ConfigPreview.height()));
        this.list.screenWidth = this.width;
        if (scale >= MIN_PREVIEW_SCALE) {
            this.previewScale = scale;
            this.previewX = PREVIEW_MARGIN + room / 2;
            this.previewY = this.layout.getHeaderHeight() + this.layout.getContentHeight() / 2;
            this.list.rowWidth = PREVIEW_ROW_WIDTH;
            this.list.updateSizeAndPosition(listWidth, this.layout.getContentHeight(), this.layout.getHeaderHeight());
            this.list.setX(this.width - listWidth - PREVIEW_MARGIN);
        } else {
            this.previewScale = 0;
            this.list.rowWidth = ROW_WIDTH;
            this.list.updateSize(this.width, this.layout);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.previewScale > 0) {
            ConfigPreview.draw(graphics, this.font, this.previewX, this.previewY, this.previewScale);
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

    /**
     * The options. With the preview shown it covers only the right of the screen, but its
     * background and the lines above and below it still span the whole width, around the preview too.
     */
    private static class OptionList extends ContainerObjectSelectionList<Row> {
        private int rowWidth = ROW_WIDTH;
        private int screenWidth;

        OptionList(Minecraft minecraft, int width, HeaderAndFooterLayout layout) {
            super(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight(), ROW_HEIGHT);
            this.centerListVertically = false;
            this.screenWidth = width;
        }

        @Override
        protected void renderListBackground(GuiGraphics graphics) {
            RenderSystem.enableBlend();
            ResourceLocation texture = this.minecraft.level == null ? MENU_LIST_BACKGROUND : INWORLD_MENU_LIST_BACKGROUND;
            graphics.blit(texture, 0, getY(), this.screenWidth, getBottom() + (int) getScrollAmount(), this.screenWidth, getHeight(), 32, 32);
            RenderSystem.disableBlend();
        }

        @Override
        protected void renderListSeparators(GuiGraphics graphics) {
            RenderSystem.enableBlend();
            ResourceLocation header = this.minecraft.level == null ? Screen.HEADER_SEPARATOR : Screen.INWORLD_HEADER_SEPARATOR;
            ResourceLocation footer = this.minecraft.level == null ? Screen.FOOTER_SEPARATOR : Screen.INWORLD_FOOTER_SEPARATOR;
            graphics.blit(header, 0, getY() - 2, 0, 0, this.screenWidth, 2, 32, 2);
            graphics.blit(footer, 0, getBottom(), 0, 0, this.screenWidth, 2, 32, 2);
            RenderSystem.disableBlend();
        }

        @Override
        public int addEntry(Row row) {
            return super.addEntry(row);
        }

        @Override
        public int getRowWidth() {
            return this.rowWidth;
        }
    }

    private abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
    }

    /** A section's name and an optional note beside it, centered, with a divider under them. */
    private static class Header extends Row {
        private final Component name;
        @Nullable
        private final Component note;
        private final int noteColor;

        Header(Component name, @Nullable Component note, int noteColor) {
            this.name = name;
            this.note = note == null ? null : note.copy().withStyle(ChatFormatting.ITALIC);
            this.noteColor = noteColor;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            int textTop = top + 6;
            Font font = Minecraft.getInstance().font;
            int textWidth = font.width(this.name) + (this.note != null ? NOTE_GAP + font.width(this.note) : 0);
            int x = left + (width - textWidth) / 2;
            graphics.drawString(font, this.name, x, textTop, HEADER_COLOR);
            if (this.note != null) {
                graphics.drawString(font, this.note, x + font.width(this.name) + NOTE_GAP, textTop, this.noteColor);
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
     * With a name: the name on the left and one button on the right. Without: one button is centered
     * at its own width, and more share the row's width.
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
            } else if (this.widgets.size() == 1) {
                AbstractWidget button = this.widgets.get(0);
                button.setPosition(left + (width - button.getWidth()) / 2, top);
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

        /** Inactive buttons are greyed out and can't be clicked. */
        void setActive(boolean active) {
            for (AbstractWidget widget : this.widgets) {
                widget.active = active;
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
