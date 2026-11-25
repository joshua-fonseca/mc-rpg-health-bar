package com.joshuafonseca.rpghealthbar.client.screen;

import com.joshuafonseca.rpghealthbar.client.BarWidths;
import com.joshuafonseca.rpghealthbar.compat.AppleSkinCompat;
import com.joshuafonseca.rpghealthbar.compat.CompatNotes;
import com.joshuafonseca.rpghealthbar.compat.ManaSource;
import com.joshuafonseca.rpghealthbar.config.AbsorptionDisplay;
import com.joshuafonseca.rpghealthbar.config.AirPosition;
import com.joshuafonseca.rpghealthbar.config.BarSize;
import com.joshuafonseca.rpghealthbar.config.BarText;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.EditMode;
import com.joshuafonseca.rpghealthbar.config.HalfBarMode;
import com.joshuafonseca.rpghealthbar.config.HealthUnit;
import com.joshuafonseca.rpghealthbar.config.LevelText;
import java.util.ArrayList;
import java.util.Arrays;
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
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.lwjgl.glfw.GLFW;

/**
 * Opened from Mods → RPG Health Bar → Config. The options are in sections, in a list that scrolls
 * when the window is too small. Changes show on the HUD right away; Save Settings keeps them, and
 * Back to Mods (or Esc) asks whether to keep them.
 */
public class RpgConfigScreen extends Screen {
    private static final int ROW_WIDTH = 310;
    /** Narrower, to the right, when there's room for the preview on the left. */
    private static final int PREVIEW_ROW_WIDTH = 260;
    /** Either side of the rows, inside the list: room for the scrollbar. */
    private static final int LIST_SIDE = 18;
    private static final int PREVIEW_MARGIN = 8;
    /** Between the preview and the list (whose rows have {@link #LIST_SIDE} more inside it). */
    private static final int PREVIEW_GAP = 8;
    /** Smaller than this and the preview is too small to read, so it's left out. */
    private static final float MIN_PREVIEW_SCALE = 0.7F;
    private static final ResourceLocation MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/menu_list_background.png");
    private static final ResourceLocation INWORLD_MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");
    private static final int ROW_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 20;
    /** Options are "Name: ... [value]": the name as text on the left, a button with the value on the right. */
    private static final int VALUE_WIDTH = 110;
    private static final int BUTTON_GAP = 10;
    /** Three footer buttons fit a window 400 GUI pixels wide. */
    private static final int FOOTER_BUTTON_WIDTH = 120;
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
    /** The Edit Mode button: room either side of its text, and from the screen's right edge. */
    private static final int EDIT_MODE_PADDING = 8;
    private static final int EDIT_MODE_MARGIN = 8;
    /** The compatibility notes: green when all is well, red for a problem. */
    private static final int COMPAT_OK_COLOR = 0x55FF55;
    private static final int COMPAT_PROBLEM_COLOR = 0xFF5555;
    /** Between the notes' + button and its label, and how far the notes are indented under it. */
    /** Notes that are just good to know, and their count when none is a problem. */
    private static final int COMPAT_INFO_COLOR = 0xFFFFFF;
    private static final int COMPAT_GAP = 6;
    /** How far the notes are indented: under the label, past the + button. */
    private static final int NOTE_INDENT = BUTTON_HEIGHT + COMPAT_GAP;
    /** Space between one compatibility note and the next. */
    private static final int BETWEEN_NOTES = 6;

    /** The settings this screen changes, put back by Back. Item Name and HUD Editor save on their own pages. */
    private static final List<ModConfigSpec.ConfigValue<?>> EDITED = edited();

    private final Screen parent;
    private final Map<ModConfigSpec.ConfigValue<?>, Object> opened = new HashMap<>();
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    @Nullable
    private OptionList list;
    /** The preview's scale, or 0 when there's no room for it; and where it's centered. */
    private float previewScale;
    private int previewX;
    private int previewY;
    @Nullable
    private PreviewButton previewButton;
    /** Top right: Simple or Advanced, with "Edit mode:" to its left. */
    @Nullable
    private CycleButton<EditMode> editModeButton;
    /** Whether the compatibility notes are shown; they start hidden. */
    private boolean compatOpen;
    /** The row width the open notes were wrapped to. */
    private int notesRowWidth;

    public RpgConfigScreen(Screen parent) {
        super(Component.translatable("rpghealthbar.config.title"));
        this.parent = parent;
        for (ModConfigSpec.ConfigValue<?> value : EDITED) {
            this.opened.put(value, value.get());
        }
    }

    @Override
    protected void init() {
        // Built once: on resize only repositionElements runs. Coming back from Item Name, HUD Editor
        // or the unsaved changes prompt, added() refills the rows and keeps the scroll position.
        this.layout.addTitleHeader(this.title, this.font);
        this.list = this.layout.addToContents(new OptionList(this.minecraft, this.width, this.layout));
        addOptions(this.list);
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(Component.translatable("rpghealthbar.config.back"), button -> onClose()).width(FOOTER_BUTTON_WIDTH).build());
        footer.addChild(Button.builder(Component.translatable("rpghealthbar.config.save"), button -> save()).width(FOOTER_BUTTON_WIDTH).build());
        footer.addChild(Button.builder(Component.translatable("rpghealthbar.config.reset"), button -> confirmReset()).width(FOOTER_BUTTON_WIDTH).build());
        this.layout.visitWidgets(this::addRenderableWidget);
        this.previewButton = addRenderableWidget(new PreviewButton(button -> {
            ClientConfig.set(ClientConfig.SHOW_PREVIEW, !ClientConfig.SHOW_PREVIEW.get());
            repositionElements();
        }));
        int editModeWidth = EDIT_MODE_PADDING * 2 + Math.max(this.font.width(EditMode.SIMPLE.displayName()), this.font.width(EditMode.ADVANCED.displayName()));
        this.editModeButton = addRenderableWidget(CycleButton.builder(EditMode::displayName)
                .withValues(EditMode.values())
                .withInitialValue(ClientConfig.EDIT_MODE.get())
                .withTooltip(value -> tooltip("editMode", "simple", "advanced"))
                .displayOnlyValue()
                .create(0, 0, editModeWidth, BUTTON_HEIGHT, Component.translatable("rpghealthbar.config.editMode"), (button, value) -> {
                    // The width rows change between presets and Edit in HUD Editor buttons.
                    ClientConfig.set(ClientConfig.EDIT_MODE, value);
                    refresh();
                }));
        repositionElements();
    }

    /**
     * Coming back from the HUD Editor and the like: the Compatibility notes may have changed (see
     * CompatNotes), and the bars' widths, which the preview is sized to.
     */
    @Override
    public void added() {
        super.added();
        refresh();
    }

    /** Fills the rows again and re-fits the preview, keeping the scroll position. */
    private void refresh() {
        if (this.list != null) {
            double scroll = this.list.getScrollAmount();
            rebuildOptions();
            repositionElements();
            this.list.setScrollAmount(scroll);
        }
    }

    /**
     * The notes, each wrapped to the list's width, laid out one after another with the same gap
     * between them, then split over as many rows as they need. Laying them out together keeps the
     * gaps even at any width, instead of each note rounding up to whole rows.
     */
    private void addNotes(OptionList list, List<Component> notes, List<Integer> colors) {
        List<NoteLine> lines = new ArrayList<>();
        // The first line sits where a one-line note centered in a row would.
        int y = (BUTTON_HEIGHT - this.font.lineHeight) / 2 + 1;
        for (int i = 0; i < notes.size(); i++) {
            if (i > 0) {
                y += BETWEEN_NOTES;
            }
            for (FormattedCharSequence line : this.font.split(ManaSource.colorNames(notes.get(i)), list.getRowWidth() - NOTE_INDENT)) {
                lines.add(new NoteLine(line, y, colors.get(i)));
                y += this.font.lineHeight;
            }
        }
        int rows = Math.max(1, Mth.positiveCeilDiv(y + NoteRow.BOTTOM, ROW_HEIGHT));
        for (int row = 0; row < rows; row++) {
            list.addEntry(new NoteRow(lines, row));
        }
    }

    /** Adds the rows again, showing the settings' current values. */
    private void rebuildOptions() {
        if (this.list != null) {
            this.list.clear();
            addOptions(this.list);
        }
    }

    private void addOptions(OptionList list) {
        // Compatibility notes: a + button and its label open them; a supported mod that's too
        // old, or that couldn't be read, is listed, otherwise a line says all is well.
        List<CompatNotes.Note> notes = CompatNotes.notes();
        list.addEntry(new CompatRow(notes.size(), CompatNotes.anyProblem(notes), this.compatOpen, () -> {
            this.compatOpen = !this.compatOpen;
            rebuildOptions();
        }));
        if (this.compatOpen) {
            List<Component> texts = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            if (!CompatNotes.anyProblem(notes)) {
                texts.add(Component.translatable("rpghealthbar.config.compat.none"));
                colors.add(COMPAT_OK_COLOR);
            }
            for (CompatNotes.Note note : notes) {
                texts.add(note.text());
                colors.add(note.problem() ? COMPAT_PROBLEM_COLOR : COMPAT_INFO_COLOR);
            }
            addNotes(list, texts, colors);
        }
        this.notesRowWidth = list.getRowWidth();

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
        list.addEntry(option("absorptionDisplay", AbsorptionDisplay.values(), ClientConfig.ABSORPTION_DISPLAY, AbsorptionDisplay::displayName, "separate", "combined", "separate_right"));
        list.addEntry(textOption("healthText", ClientConfig.healthText(), ClientConfig.HEALTH_TEXT, ClientConfig.SHOW_HEALTH_TEXT));
        list.addEntry(widthOption("healthSize", ClientConfig.HEALTH_SIZE));

        // The header names the mana mods found; each one's options are listed only with that mod.
        List<ManaSource> manaMods = Arrays.stream(ManaSource.values()).filter(ManaSource::available).toList();
        list.addEntry(new Header(Component.translatable("rpghealthbar.config.section.mana"), manaDetected(manaMods), IRONS_NOTE_COLOR));
        for (ManaSource source : manaMods) {
            list.addEntry(textOption(source.textKey(), source.text(), source.textValue(), source.legacyTextShown()));
            list.addEntry(widthOption(source.sizeKey(), source.size()));
            if (source.bubblesValue() != null) {
                list.addEntry(option("arsManaBubbles", new Boolean[]{true, false}, source.bubblesValue(),
                        on -> Component.translatable("rpghealthbar.config.arsManaBubbles." + (on ? "on" : "off")), "on", "off"));
            }
        }

        list.addEntry(new Header(Component.translatable("rpghealthbar.config.section.xp"), null, NOTE_COLOR));
        list.addEntry(option("xpBar", new Boolean[]{true, false}, ClientConfig.XP_BAR_SHOWN,
                shown -> Component.translatable("rpghealthbar.config.xpBar." + (shown ? "shown" : "hidden")), "shown", "hidden"));
        list.addEntry(option("levelText", LevelText.values(), ClientConfig.LEVEL_TEXT, LevelText::displayName, "center", "left", "right"));
        if (BarWidths.advanced()) {
            list.addEntry(widthOption("xpWidth", null));
        }

        list.addEntry(new Header(Component.translatable("rpghealthbar.config.section.positioning"), null, NOTE_COLOR));
        list.addEntry(new WidgetRow(null,
                Button.builder(Component.translatable("rpghealthbar.config.itemName"), button -> this.minecraft.setScreen(new ItemNameScreen(this)))
                        .size(0, BUTTON_HEIGHT)
                        .tooltip(tooltip("itemName"))
                        .build(),
                Button.builder(Component.translatable("rpghealthbar.config.moveHud"), button -> this.minecraft.setScreen(new HudEditScreen(this)))
                        .size(0, BUTTON_HEIGHT)
                        .tooltip(Tooltip.create(hudEditorTooltip()))
                        .build()));
    }

    /**
     * A bar's width: Full or Half in Simple edit mode; in Advanced, a button that opens the HUD
     * Editor, where the bar's ends are dragged.
     */
    private WidgetRow widthOption(String key, @Nullable ModConfigSpec.EnumValue<BarSize> preset) {
        if (preset == null || BarWidths.advanced()) {
            return new WidgetRow(label(key),
                    Button.builder(Component.translatable("rpghealthbar.config.editInHudEditor"), button -> this.minecraft.setScreen(new HudEditScreen(this)))
                            .size(VALUE_WIDTH, BUTTON_HEIGHT)
                            .tooltip(Tooltip.create(advancedWidthTooltip(key)))
                            .build());
        }
        return option(key, BarSize.values(), preset, BarSize::displayName, "full", "half");
    }

    /**
     * A width's tooltip in Advanced mode: "Advanced Mode:" in cyan, what the width is, and how to
     * change it in the HUD Editor.
     */
    private static Component advancedWidthTooltip(String key) {
        return Component.translatable("rpghealthbar.config.advancedModeTitle").withStyle(ChatFormatting.AQUA)
                .append(" ").append(Component.translatable("rpghealthbar.config." + key + ".tooltip").withStyle(ChatFormatting.WHITE))
                .append(" ").append(Component.translatable("rpghealthbar.config.editInHudEditor.tooltip").withStyle(ChatFormatting.WHITE));
    }

    /** The HUD Editor button's tooltip; in Advanced mode, with how to resize the bars ("advanced mode" in cyan). */
    private static Component hudEditorTooltip() {
        MutableComponent text = Component.translatable("rpghealthbar.config.moveHud.tooltip");
        if (BarWidths.advanced()) {
            Component mode = Component.translatable("rpghealthbar.config.advancedMode").withStyle(ChatFormatting.AQUA);
            text.append("\n\n").append(Component.translatable("rpghealthbar.config.moveHud.tooltip.advanced", mode));
        }
        return text;
    }

    /** An option's name with a colon, as written left of its button. */
    private static Component label(String key) {
        return ManaSource.colorNames(Component.translatable("rpghealthbar.config." + key).append(":"));
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
        return new WidgetRow(label(key), CycleButton.builder(displayName)
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
    private static List<ModConfigSpec.ConfigValue<?>> edited() {
        List<ModConfigSpec.ConfigValue<?>> edited = new ArrayList<>(List.of(ClientConfig.ENABLED, ClientConfig.ARMOR_MODE,
                ClientConfig.HUNGER_MODE, ClientConfig.AIR_POSITION, ClientConfig.HEALTH_UNIT, ClientConfig.ABSORPTION_DISPLAY,
                ClientConfig.HEALTH_TEXT, ClientConfig.SHOW_HEALTH_TEXT, ClientConfig.HEALTH_SIZE));
        for (ManaSource source : ManaSource.values()) {
            edited.add(source.textValue());
            if (source.legacyTextShown() != null) {
                edited.add(source.legacyTextShown());
            }
            edited.add(source.size());
            if (source.bubblesValue() != null) {
                edited.add(source.bubblesValue());
            }
        }
        edited.addAll(List.of(ClientConfig.LEVEL_TEXT, ClientConfig.EDIT_MODE, ClientConfig.XP_BAR_SHOWN));
        return edited;
    }

    /** "(Iron's + Confluence detected)", or that none was. */
    private static Component manaDetected(List<ManaSource> found) {
        if (found.isEmpty()) {
            return ManaSource.colorNames(Component.translatable("rpghealthbar.config.manaNotDetected"));
        }
        MutableComponent names = found.get(0).displayName().copy();
        for (ManaSource source : found.subList(1, found.size())) {
            names.append(" + ").append(source.displayName());
        }
        return ManaSource.colorNames(Component.translatable("rpghealthbar.config.manaDetected", names));
    }

    /** @param legacyShown the setting from before {@code value}, or null; it's turned back on with any choice */
    private static WidgetRow textOption(String key, BarText current, ModConfigSpec.EnumValue<BarText> value, @Nullable ModConfigSpec.BooleanValue legacyShown) {
        return option(key, BarText.values(), current, BarText::displayName, text -> {
            ClientConfig.set(value, text);
            if (legacyShown != null && !legacyShown.get()) {
                ClientConfig.set(legacyShown, true);
            }
        }, "left", "right", "center", "off");
    }

    /**
     * Hover text: a description, then each possible value on its own paragraph with its name highlighted.
     * The first value is the default. Also used by the Item Name and HUD Editor pages.
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
        return Tooltip.create(ManaSource.colorNames(text));
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.list == null || this.previewButton == null) {
            return;
        }
        this.previewButton.setPosition(this.width / 2 + this.font.width(this.title) / 2 + TITLE_GAP,
                (this.layout.getHeaderHeight() - BUTTON_HEIGHT) / 2);
        this.previewButton.update();
        if (this.editModeButton != null) {
            this.editModeButton.setPosition(this.width - EDIT_MODE_MARGIN - this.editModeButton.getWidth(), (this.layout.getHeaderHeight() - BUTTON_HEIGHT) / 2);
        }
        // The preview goes left of the list, scaled down to fit the room there; the two are
        // centered together, the preview taking only the width it needs.
        int listWidth = PREVIEW_ROW_WIDTH + LIST_SIDE * 2;
        int room = this.width - listWidth - PREVIEW_GAP - PREVIEW_MARGIN * 2;
        float scale = Math.min(1.0F, Math.min((float) room / ConfigPreview.width(), (float) this.layout.getContentHeight() / ConfigPreview.height()));
        this.list.screenWidth = this.width;
        if (ClientConfig.SHOW_PREVIEW.get() && scale >= MIN_PREVIEW_SCALE) {
            this.previewScale = scale;
            int previewWidth = Math.round(ConfigPreview.width() * scale);
            int left = (this.width - previewWidth - PREVIEW_GAP - listWidth) / 2;
            this.previewX = left + previewWidth / 2;
            this.previewY = this.layout.getHeaderHeight() + this.layout.getContentHeight() / 2;
            this.list.rowWidth = PREVIEW_ROW_WIDTH;
            this.list.updateSizeAndPosition(listWidth, this.layout.getContentHeight(), this.layout.getHeaderHeight());
            this.list.setX(left + previewWidth + PREVIEW_GAP);
        } else {
            this.previewScale = 0;
            this.list.rowWidth = ROW_WIDTH;
            this.list.updateSize(this.width, this.layout);
        }
        // The notes are wrapped to the row width, which changes with the preview.
        if (this.compatOpen && this.list.getRowWidth() != this.notesRowWidth) {
            double scroll = this.list.getScrollAmount();
            rebuildOptions();
            this.list.setScrollAmount(scroll);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.editModeButton != null && this.previewButton != null) {
            // Left out on a narrow window, where it would run into the title.
            Component label = Component.translatable("rpghealthbar.config.editMode").append(":");
            int labelX = this.editModeButton.getX() - TITLE_GAP / 2 - this.font.width(label);
            if (labelX >= this.previewButton.getX() + this.previewButton.getWidth() + TITLE_GAP) {
                graphics.drawString(this.font, label, labelX, this.editModeButton.getY() + (BUTTON_HEIGHT - 8) / 2, LABEL_COLOR);
            }
        }
        if (this.previewScale > 0) {
            ConfigPreview.draw(graphics, this.font, this.previewX, this.previewY, this.previewScale);
        }
    }

    /** Keeps the changes. */
    private void save() {
        this.opened.clear();
        this.minecraft.setScreen(this.parent);
    }

    /**
     * Back to Mods and Esc. With unsaved changes, asks whether to keep them: Yes saves, No puts
     * back the settings as they were when the screen opened, and Esc stays on this screen.
     */
    @Override
    public void onClose() {
        boolean changed = this.opened.entrySet().stream().anyMatch(entry -> !entry.getKey().get().equals(entry.getValue()));
        if (!changed) {
            this.minecraft.setScreen(this.parent);
            return;
        }
        this.minecraft.setScreen(new ConfirmScreen(save -> {
            if (save) {
                save();
            } else {
                this.opened.forEach((value, opened) -> restore(value, opened));
                this.minecraft.setScreen(this.parent);
            }
        }, Component.translatable("rpghealthbar.config.unsaved.title"), Component.translatable("rpghealthbar.config.unsaved.message")) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    this.minecraft.setScreen(RpgConfigScreen.this);
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        });
    }

    /**
     * Reset to Defaults: asks first. Yes puts every setting back to its default and saves it, HUD
     * position and item name height included; No, or Esc, leaves them.
     */
    private void confirmReset() {
        this.minecraft.setScreen(new ConfirmScreen(reset -> {
            if (reset) {
                ClientConfig.resetToDefaults();
                for (ModConfigSpec.ConfigValue<?> value : EDITED) {
                    this.opened.put(value, value.get());
                }
                if (this.editModeButton != null) {
                    this.editModeButton.setValue(ClientConfig.EDIT_MODE.get());
                }
                rebuildOptions();
                if (this.list != null) {
                    this.list.setScrollAmount(0);
                }
            }
            this.minecraft.setScreen(this);
        }, Component.translatable("rpghealthbar.config.reset.title"), Component.translatable("rpghealthbar.config.reset.message")));
    }

    @SuppressWarnings("unchecked")
    private static <T> void restore(ModConfigSpec.ConfigValue<T> value, Object opened) {
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

        /** Removes every row, so they can be added again with fresh values. */
        void clear() {
            clearEntries();
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
     * The compatibility notes' row: a + button (− while they're open) and the label beside it, both
     * of which open and close them. How many notes there are follows the label: red when any is a
     * problem, white otherwise.
     */
    private static class CompatRow extends Row {
        private final Button button;
        private final Component label;
        private final Runnable toggle;
        private int labelLeft;
        private int labelTop;

        CompatRow(int count, boolean problem, boolean open, Runnable toggle) {
            this.toggle = toggle;
            this.button = Button.builder(Component.literal(open ? "-" : "+"), b -> toggle.run()).size(BUTTON_HEIGHT, BUTTON_HEIGHT).build();
            MutableComponent label = Component.translatable("rpghealthbar.config.compat").copy();
            if (count > 0) {
                label.append(Component.literal(" (" + count + ")").withColor(problem ? COMPAT_PROBLEM_COLOR : COMPAT_INFO_COLOR));
            }
            this.label = label;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            this.button.setPosition(left, top);
            this.button.render(graphics, mouseX, mouseY, partialTick);
            this.labelLeft = left + NOTE_INDENT;
            this.labelTop = top;
            graphics.drawString(Minecraft.getInstance().font, this.label, this.labelLeft, top + (BUTTON_HEIGHT - 8) / 2, LABEL_COLOR);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
            if (super.mouseClicked(mouseX, mouseY, mouseButton)) {
                return true;
            }
            int labelWidth = Minecraft.getInstance().font.width(this.label);
            if (mouseButton == 0 && mouseX >= this.labelLeft && mouseX < this.labelLeft + labelWidth
                    && mouseY >= this.labelTop && mouseY < this.labelTop + BUTTON_HEIGHT) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                this.toggle.run();
                return true;
            }
            return false;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.button);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.button);
        }
    }

    /** One wrapped line of a note, {@code y} from the top of the notes' first row. */
    private record NoteLine(FormattedCharSequence text, int y, int color) {
    }

    /** One row of the notes, drawing the lines that start in it. */
    private static class NoteRow extends Row {
        /** Space kept under the last note before the next row. */
        static final int BOTTOM = 2;
        private final List<NoteLine> lines;
        private final int row;

        NoteRow(List<NoteLine> lines, int row) {
            this.lines = lines;
            this.row = row;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            Font font = Minecraft.getInstance().font;
            int rowTop = this.row * ROW_HEIGHT;
            for (NoteLine line : this.lines) {
                if (line.y() >= rowTop && line.y() < rowTop + ROW_HEIGHT) {
                    graphics.drawString(font, line.text(), left + NOTE_INDENT, top - rowTop + line.y(), line.color());
                }
            }
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
