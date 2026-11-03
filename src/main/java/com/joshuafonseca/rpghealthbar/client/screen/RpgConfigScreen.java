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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.common.ForgeConfigSpec;

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
    private static final int EDGE_SHADOW = 4;
    private static final int ROW_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 20;
    /** Options are "Name: ... [value]": the name as text on the left, a button with the value on the right. */
    private static final int VALUE_WIDTH = 110;
    private static final int BUTTON_GAP = 10;
    private static final int FOOTER_BUTTON_WIDTH = 150;
    private static final int FOOTER_BUTTON_GAP = 8;
    /** The title above the list and the buttons below it, as in vanilla's option screens. */
    private static final int HEADER_HEIGHT = 32;
    private static final int FOOTER_HEIGHT = 32;
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
    /** Gap between the title and the preview button beside it. */
    private static final int TITLE_GAP = 8;

    /** The settings this screen changes, put back by Back. Item Name and Move HUD save on their own pages. */
    private static final List<ForgeConfigSpec.ConfigValue<?>> EDITED = List.of(ClientConfig.ENABLED, ClientConfig.ARMOR_MODE,
            ClientConfig.HUNGER_MODE, ClientConfig.AIR_POSITION, ClientConfig.HEALTH_UNIT, ClientConfig.ABSORPTION_DISPLAY,
            ClientConfig.HEALTH_TEXT, ClientConfig.SHOW_HEALTH_TEXT, ClientConfig.HEALTH_SIZE, ClientConfig.MANA_TEXT, ClientConfig.SHOW_MANA_TEXT,
            ClientConfig.MANA_SIZE);

    private final Screen parent;
    private final Map<ForgeConfigSpec.ConfigValue<?>, Object> opened = new HashMap<>();
    @Nullable
    private OptionList list;
    /** The preview's scale, or 0 when there's no room for it; and where it's centered. */
    private float previewScale;
    private int previewX;
    private int previewY;

    public RpgConfigScreen(Screen parent) {
        super(Component.translatable("rpghealthbar.config.title"));
        this.parent = parent;
        for (ForgeConfigSpec.ConfigValue<?> value : EDITED) {
            this.opened.put(value, value.get());
        }
    }

    @Override
    protected void init() {
        // Rebuilt on resize and when coming back from Item Name or Move HUD; keep the scroll position.
        double scroll = this.list != null ? this.list.getScrollAmount() : 0;
        // The preview goes left of the list, scaled down to fit the room there.
        int contentHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT;
        int listWidth = PREVIEW_ROW_WIDTH + LIST_SIDE * 2;
        int room = this.width - listWidth - PREVIEW_MARGIN * 2;
        float scale = Math.min(1.0F, Math.min((float) room / ConfigPreview.WIDTH, (float) contentHeight / ConfigPreview.height()));
        if (ClientConfig.SHOW_PREVIEW.get() && scale >= MIN_PREVIEW_SCALE) {
            this.previewScale = scale;
            this.previewX = PREVIEW_MARGIN + room / 2;
            this.previewY = HEADER_HEIGHT + contentHeight / 2;
            this.list = new OptionList(this.minecraft, listWidth, this.height, HEADER_HEIGHT, this.height - FOOTER_HEIGHT, PREVIEW_ROW_WIDTH);
            this.list.setLeftPos(this.width - listWidth - PREVIEW_MARGIN);
        } else {
            this.previewScale = 0;
            this.list = new OptionList(this.minecraft, this.width, this.height, HEADER_HEIGHT, this.height - FOOTER_HEIGHT, ROW_WIDTH);
        }
        addOptions(this.list);
        addWidget(this.list);
        int footerY = this.height - FOOTER_HEIGHT + (FOOTER_HEIGHT - BUTTON_HEIGHT) / 2;
        int footerLeft = this.width / 2 - FOOTER_BUTTON_WIDTH - FOOTER_BUTTON_GAP / 2;
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> onClose())
                .bounds(footerLeft, footerY, FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("rpghealthbar.config.save"), button -> save())
                .bounds(footerLeft + FOOTER_BUTTON_WIDTH + FOOTER_BUTTON_GAP, footerY, FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(new PreviewButton(button -> {
            ClientConfig.set(ClientConfig.SHOW_PREVIEW, !ClientConfig.SHOW_PREVIEW.get());
            rebuildWidgets();
        })).setPosition(this.width / 2 + this.font.width(this.title) / 2 + TITLE_GAP, (HEADER_HEIGHT - BUTTON_HEIGHT) / 2);
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
    private static <T> WidgetRow option(String key, T[] values, ForgeConfigSpec.ConfigValue<T> value, Function<T, Component> displayName,
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
    private static WidgetRow textOption(String key, BarText current, ForgeConfigSpec.EnumValue<BarText> value, ForgeConfigSpec.BooleanValue legacyShown) {
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        // The list draws only its rows; its background and the header and footer span the whole
        // width, around the preview too.
        int top = HEADER_HEIGHT;
        int bottom = this.height - FOOTER_HEIGHT;
        graphics.setColor(0.125F, 0.125F, 0.125F, 1.0F);
        graphics.blit(BACKGROUND_LOCATION, 0, top, this.width, bottom + (int) this.list.getScrollAmount(), this.width, bottom - top, 32, 32);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        this.list.render(graphics, mouseX, mouseY, partialTick);
        graphics.setColor(0.25F, 0.25F, 0.25F, 1.0F);
        graphics.blit(BACKGROUND_LOCATION, 0, 0, 0, 0, this.width, top, 32, 32);
        graphics.blit(BACKGROUND_LOCATION, 0, bottom, 0, bottom, this.width, this.height - bottom, 32, 32);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.fillGradient(RenderType.guiOverlay(), 0, top, this.width, top + EDGE_SHADOW, 0xFF000000, 0, 0);
        graphics.fillGradient(RenderType.guiOverlay(), 0, bottom - EDGE_SHADOW, this.width, bottom, 0, 0xFF000000, 0);
        if (this.previewScale > 0) {
            ConfigPreview.draw(graphics, this.font, this.previewX, this.previewY, this.previewScale);
        }
        graphics.drawCenteredString(this.font, this.title, this.width / 2, (HEADER_HEIGHT - 8) / 2, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** Keeps the changes. */
    private void save() {
        this.opened.clear();
        this.minecraft.setScreen(this.parent);
    }

    /** Back and Esc: puts back the settings as they were when the screen opened. */
    @Override
    public void onClose() {
        for (Map.Entry<ForgeConfigSpec.ConfigValue<?>, Object> entry : this.opened.entrySet()) {
            restore(entry.getKey(), entry.getValue());
        }
        this.minecraft.setScreen(this.parent);
    }

    @SuppressWarnings("unchecked")
    private static <T> void restore(ForgeConfigSpec.ConfigValue<T> value, Object opened) {
        if (!value.get().equals(opened)) {
            ClientConfig.set(value, (T) opened);
        }
    }

    /**
     * Beside the title: shows or hides the preview. An eye, in the style of vanilla's Blindness
     * icon, with a red slash over it while the preview is off.
     */
    private static class PreviewButton extends Button {
        /** The eye, one character a pixel: outline, white, shade, iris, pupil, highlight; '.' is clear. */
        private static final String[] EYE = {
                "......LLLLLL......",
                "....LLEEEEEELL....",
                "...LEEEKHHKEEEL...",
                "..LEEEKHHHHKEEEL..",
                ".LEEEEHHJHHHEEEEL.",
                ".LDEEEHHHHHHEEEDL.",
                "..LDEEKHHHHKEEDL..",
                "...LDDDKHHKDDDL...",
                "....LLDDDDDDLL....",
                "......LLLLLL......",
        };
        private static final int ICON_SIZE = 18;
        private static final int SLASH_COLOR = 0xFFFF0606;
        private static final int SLASH_SHADOW = 0xFFC80404;
        private static final int SLASH_INSET = 3;

        PreviewButton(OnPress onPress) {
            super(0, 0, BUTTON_HEIGHT, BUTTON_HEIGHT, Component.empty(), onPress, DEFAULT_NARRATION);
            update();
        }

        /** Matches the label and tip, read by narration and shown on hover, to the setting. */
        void update() {
            String state = ClientConfig.SHOW_PREVIEW.get() ? "on" : "off";
            setMessage(Component.translatable("rpghealthbar.config.preview." + state));
            setTooltip(Tooltip.create(Component.translatable("rpghealthbar.config.preview.tooltip." + state)));
        }

        /** The icon in place of the text. */
        @Override
        public void renderString(GuiGraphics graphics, Font font, int color) {
            int x = getX() + (this.width - ICON_SIZE) / 2;
            int y = getY() + (this.height - ICON_SIZE) / 2;
            int eyeTop = y + (ICON_SIZE - EYE.length) / 2;
            for (int row = 0; row < EYE.length; row++) {
                for (int column = 0; column < ICON_SIZE; column++) {
                    int pixel = eyeColor(EYE[row].charAt(column));
                    if (pixel != 0) {
                        graphics.fill(x + column, eyeTop + row, x + column + 1, eyeTop + row + 1, pixel);
                    }
                }
            }
            if (!ClientConfig.SHOW_PREVIEW.get()) {
                // Top left to bottom right, two pixels wide with a darker edge.
                for (int i = SLASH_INSET; i < ICON_SIZE - SLASH_INSET; i++) {
                    graphics.fill(x + i, y + i, x + i + 1, y + i + 1, SLASH_COLOR);
                    graphics.fill(x + i + 1, y + i, x + i + 2, y + i + 1, SLASH_SHADOW);
                }
            }
        }

        private static int eyeColor(char pixel) {
            return switch (pixel) {
                case 'L' -> 0xFF121C29;
                case 'E' -> 0xFFFFFFFF;
                case 'D' -> 0xFF9ABBB7;
                case 'K' -> 0xFF506B68;
                case 'H' -> 0xFF212121;
                case 'J' -> 0xFFE7E7E7;
                default -> 0;
            };
        }
    }

    private static class OptionList extends ContainerObjectSelectionList<Row> {
        private final int rowWidth;

        OptionList(Minecraft minecraft, int width, int height, int top, int bottom, int rowWidth) {
            super(minecraft, width, height, top, bottom, ROW_HEIGHT);
            this.centerListVertically = false;
            this.rowWidth = rowWidth;
            // Drawn by the screen, across the whole width.
            setRenderBackground(false);
            setRenderTopAndBottom(false);
        }

        @Override
        public int addEntry(Row row) {
            return super.addEntry(row);
        }

        @Override
        public int getRowWidth() {
            return this.rowWidth;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.x0 + this.width / 2 + this.rowWidth / 2 + 10;
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
