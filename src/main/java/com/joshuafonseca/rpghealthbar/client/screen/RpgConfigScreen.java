package com.joshuafonseca.rpghealthbar.client.screen;

import com.joshuafonseca.rpghealthbar.client.BarWidths;
import com.joshuafonseca.rpghealthbar.compat.AppleSkinCompat;
import com.joshuafonseca.rpghealthbar.compat.CompatNotes;
import com.joshuafonseca.rpghealthbar.compat.ManaSource;
import com.joshuafonseca.rpghealthbar.compat.ThirstCompat;
import com.joshuafonseca.rpghealthbar.config.AbsorptionDisplay;
import com.joshuafonseca.rpghealthbar.config.AirPosition;
import com.joshuafonseca.rpghealthbar.config.BarSize;
import com.joshuafonseca.rpghealthbar.config.BarText;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.EditMode;
import com.joshuafonseca.rpghealthbar.config.HalfBarMode;
import com.joshuafonseca.rpghealthbar.config.ThirstMode;
import com.joshuafonseca.rpghealthbar.config.HealthUnit;
import com.joshuafonseca.rpghealthbar.config.LevelText;
import com.joshuafonseca.rpghealthbar.config.XpBarMode;
import java.util.ArrayList;
import java.util.Arrays;
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
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.common.ForgeConfigSpec;
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
    /** How much of the height beside the list is the preview's; the description is under it. */
    private static final float PREVIEW_HEIGHT_SHARE = 0.4F;
    /** The widest a description beside the list is wrapped, and the room above it. */
    private static final int DESCRIPTION_MAX_WIDTH = 220;
    private static final int DESCRIPTION_TOP = 8;
    private static final int DESCRIPTION_BOTTOM = 4;
    private static final int EDGE_SHADOW = 4;
    private static final int ROW_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 20;
    /** Options are "Name: ... [value]": the name as text on the left, a button with the value on the right. */
    private static final int VALUE_WIDTH = 110;
    private static final int BUTTON_GAP = 10;
    /** Three footer buttons fit a window 400 GUI pixels wide. */
    private static final int FOOTER_BUTTON_WIDTH = 120;
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
    /** The Edit Mode button: room either side of its text, and from the screen's right edge. */
    private static final int EDIT_MODE_PADDING = 8;
    private static final int EDIT_MODE_MARGIN = 8;
    /** The compatibility notes: green when all is well, red for a problem. */
    private static final int COMPAT_OK_COLOR = 0x55FF55;
    private static final int COMPAT_PROBLEM_COLOR = 0xFF5555;
    /** Notes that are just good to know, and their count when none is a problem. */
    private static final int COMPAT_INFO_COLOR = 0xFFFFFF;
    /** Between the notes' + button and its label. */
    private static final int COMPAT_GAP = 6;
    /** How far the notes are indented: under the label, past the + button. */
    private static final int NOTE_INDENT = BUTTON_HEIGHT + COMPAT_GAP;
    /** Space between one compatibility note and the next. */
    private static final int BETWEEN_NOTES = 6;

    /** The settings this screen changes, put back by Back. Item Name and HUD Editor save on their own pages. */
    private static final List<ForgeConfigSpec.ConfigValue<?>> EDITED = edited();

    private final Screen parent;
    private final Map<ForgeConfigSpec.ConfigValue<?>, Object> opened = new HashMap<>();
    @Nullable
    private OptionList list;
    /**
     * The column left of the list, holding the preview and the descriptions: its width, or 0 with the
     * preview off or no room for it, when the descriptions are tooltips. Centered at previewX.
     */
    private int sideWidth;
    private int descriptionY;
    /** The preview's scale, or 0 when it isn't drawn; and its height's middle. */
    private float previewScale;
    private int previewX;
    private int previewY;
    /** Top right: Simple or Advanced, with "Edit mode:" to its left. */
    @Nullable
    private CycleButton<EditMode> editModeButton;
    @Nullable
    private PreviewButton previewButton;
    private final Tooltip editModeTooltip = tooltip("editMode", "simple", "advanced");
    /** Whether the compatibility notes are shown; they start hidden. */
    private boolean compatOpen;
    /** {@link ClientConfig#repairs} as of the options last being shown. */
    private int shownRepairs;

    public RpgConfigScreen(Screen parent) {
        super(Component.translatable("rpghealthbar.config.title"));
        this.parent = parent;
        for (ForgeConfigSpec.ConfigValue<?> value : EDITED) {
            this.opened.put(value, ClientConfig.read(value));
        }
    }

    @Override
    protected void init() {
        this.shownRepairs = ClientConfig.repairs();
        // Rebuilt on resize and when coming back from Item Name or the HUD Editor (whose bar widths
        // the preview is sized to, and which can change the Compatibility notes); keep the scroll position.
        double scroll = this.list != null ? this.list.getScrollAmount() : 0;
        // The preview and the hovered option's description go left of the list. The list is placed
        // as if centered together with a column of a set width, so it stays put as settings change
        // the preview's size.
        int contentHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT;
        int listWidth = PREVIEW_ROW_WIDTH + LIST_SIDE * 2;
        int room = this.width - listWidth - PREVIEW_GAP - PREVIEW_MARGIN * 2;
        int reserved = Math.min(room, ConfigPreview.reservedWidth());
        this.previewScale = 0;
        if (ClientConfig.SHOW_PREVIEW.get() && room >= ConfigPreview.leastWidth() * MIN_PREVIEW_SCALE) {
            int left = (this.width - reserved - PREVIEW_GAP - listWidth) / 2;
            int listX = left + reserved + PREVIEW_GAP;
            // In the middle of the space beside the rows, not up against them.
            this.previewX = (listX + LIST_SIDE) / 2;
            this.sideWidth = listX + LIST_SIDE - PREVIEW_MARGIN * 2;
            int previewHeight = Math.round(contentHeight * PREVIEW_HEIGHT_SHARE);
            this.previewScale = Math.min(1.0F, Math.min((float) this.sideWidth / ConfigPreview.width(),
                    (float) previewHeight / ConfigPreview.height()));
            this.previewY = HEADER_HEIGHT + previewHeight / 2;
            this.descriptionY = HEADER_HEIGHT + previewHeight + DESCRIPTION_TOP;
            this.list = new OptionList(this.minecraft, listWidth, this.height, HEADER_HEIGHT, this.height - FOOTER_HEIGHT, PREVIEW_ROW_WIDTH);
            this.list.setLeftPos(listX);
        } else {
            this.sideWidth = 0;
            this.list = new OptionList(this.minecraft, this.width, this.height, HEADER_HEIGHT, this.height - FOOTER_HEIGHT, ROW_WIDTH);
        }
        addOptions(this.list);
        addWidget(this.list);
        int footerY = this.height - FOOTER_HEIGHT + (FOOTER_HEIGHT - BUTTON_HEIGHT) / 2;
        int footerLeft = this.width / 2 - FOOTER_BUTTON_WIDTH * 3 / 2 - FOOTER_BUTTON_GAP;
        addRenderableWidget(Button.builder(Component.translatable("rpghealthbar.config.back"), button -> onClose())
                .bounds(footerLeft, footerY, FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("rpghealthbar.config.save"), button -> save())
                .bounds(footerLeft + FOOTER_BUTTON_WIDTH + FOOTER_BUTTON_GAP, footerY, FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("rpghealthbar.config.reset"), button -> confirmReset())
                .bounds(footerLeft + (FOOTER_BUTTON_WIDTH + FOOTER_BUTTON_GAP) * 2, footerY, FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        this.previewButton = addRenderableWidget(new PreviewButton(button -> {
            ClientConfig.set(ClientConfig.SHOW_PREVIEW, !ClientConfig.read(ClientConfig.SHOW_PREVIEW));
            rebuildWidgets();
        }));
        this.previewButton.setPosition(this.width / 2 + this.font.width(this.title) / 2 + TITLE_GAP, (HEADER_HEIGHT - BUTTON_HEIGHT) / 2);
        this.list.setScrollAmount(scroll);
        int editModeWidth = EDIT_MODE_PADDING * 2 + Math.max(this.font.width(EditMode.SIMPLE.displayName()), this.font.width(EditMode.ADVANCED.displayName()));
        this.editModeButton = addRenderableWidget(CycleButton.builder(EditMode::displayName)
                .withValues(EditMode.values())
                .withInitialValue(ClientConfig.EDIT_MODE.get())
                .displayOnlyValue()
                .create(this.width - EDIT_MODE_MARGIN - editModeWidth, (HEADER_HEIGHT - BUTTON_HEIGHT) / 2, editModeWidth, BUTTON_HEIGHT,
                        Component.translatable("rpghealthbar.config.editMode"), (button, value) -> {
                            // The width rows change between presets and Edit in HUD Editor buttons.
                            ClientConfig.set(ClientConfig.EDIT_MODE, value);
                            rebuildWidgets();
                        }));
    }

    @Override
    public void tick() {
        // An option may be showing a value from before it was written again.
        if (this.shownRepairs != ClientConfig.repairs()) {
            rebuildWidgets();
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
            // Mod names are colored, except in a problem note, which stays all red.
            Component text = colors.get(i) == COMPAT_PROBLEM_COLOR ? notes.get(i) : ManaSource.colorNames(notes.get(i));
            for (FormattedCharSequence line : this.font.split(text, list.getRowWidth() - NOTE_INDENT)) {
                lines.add(new NoteLine(line, y, colors.get(i)));
                y += this.font.lineHeight;
            }
        }
        int rows = Math.max(1, Mth.positiveCeilDiv(y + NoteRow.BOTTOM, ROW_HEIGHT));
        for (int row = 0; row < rows; row++) {
            list.addEntry(new NoteRow(lines, row));
        }
    }

    private void addOptions(OptionList list) {
        // Compatibility notes: a + button and its label open them; a supported mod that's too
        // old, or that couldn't be read, is listed, otherwise a line says all is well.
        List<CompatNotes.Note> notes = CompatNotes.notes();
        list.addEntry(new CompatRow(notes.size(), this.compatOpen, () -> {
            this.compatOpen = !this.compatOpen;
            rebuildWidgets();
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

        Component enabled = Component.translatable("rpghealthbar.config.enabled.on");
        Component disabled = Component.translatable("rpghealthbar.config.enabled.off");
        Component modState = Component.translatable("rpghealthbar.config.enabled");
        // As wide as its text with either value, so it doesn't change size when clicked.
        int toggleWidth = TOGGLE_PADDING * 2 + Math.max(
                this.font.width(CommonComponents.optionNameValue(modState, enabled)), this.font.width(CommonComponents.optionNameValue(modState, disabled)));
        list.addEntry(new WidgetRow(null, CycleButton.booleanBuilder(enabled, disabled)
                .withInitialValue(ClientConfig.ENABLED.get())
                .create(0, 0, toggleWidth, BUTTON_HEIGHT, modState, (button, value) -> ClientConfig.set(ClientConfig.ENABLED, value)))
                .described(description("enabled", "on", "off")));

        list.addEntry(new Header(Component.translatable("rpghealthbar.config.section.armorHunger"),
                survivalDetected(), NOTE_COLOR));
        // Each click cycles Vanilla → Bar → Hidden.
        list.addEntry(option("armorMode", new HalfBarMode[] {HalfBarMode.VANILLA, HalfBarMode.BAR, HalfBarMode.HIDDEN}, ClientConfig.ARMOR_MODE, HalfBarMode::displayName, "vanilla", "bar", "hidden"));
        // An upright bar's height row comes and goes with the mode, so changing it refills the rows.
        list.addEntry(option("hungerMode", HalfBarMode.values(), ClientConfig.HUNGER_MODE.get(), HalfBarMode::displayName, mode -> {
            ClientConfig.set(ClientConfig.HUNGER_MODE, mode);
            rebuildWidgets();
        }, "vanilla", "bar", "vertical", "hidden"));
        if (BarWidths.advanced() && ClientConfig.HUNGER_MODE.get() == HalfBarMode.VERTICAL) {
            list.addEntry(heightRow("hungerHeight"));
        }
        if (ThirstCompat.isAvailable()) {
            list.addEntry(option("thirstMode", ThirstMode.values(), ClientConfig.THIRST_MODE.get(), ThirstMode::displayName, mode -> {
                ClientConfig.set(ClientConfig.THIRST_MODE, mode);
                rebuildWidgets();
            }, "vanilla", "bar", "vertical", "hidden"));
            if (BarWidths.advanced() && ClientConfig.THIRST_MODE.get() == ThirstMode.VERTICAL) {
                list.addEntry(heightRow("thirstHeight"));
            }
        }
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
        list.addEntry(option("xpBar", XpBarMode.values(), ClientConfig.xpBar(), XpBarMode::displayName, mode -> {
            ClientConfig.set(ClientConfig.XP_BAR, mode);
            // The choice replaces a Hidden saved before this option existed.
            if (!ClientConfig.read(ClientConfig.XP_BAR_SHOWN)) {
                ClientConfig.set(ClientConfig.XP_BAR_SHOWN, true);
            }
            rebuildWidgets();
        }, "shown", "level_only", "hidden"));
        // Without a level there's no place for it to set, so it's greyed out.
        list.addEntry(option("levelText", LevelText.values(), ClientConfig.LEVEL_TEXT, LevelText::displayName, "center", "left", "right")
                .active(ClientConfig.xpBar() != XpBarMode.HIDDEN));
        // Listed whatever XP Bar is on, as the jump bar has this width too.
        list.addEntry(widthOption("xpWidth", ClientConfig.XP_SIZE));

        list.addEntry(new Header(Component.translatable("rpghealthbar.config.section.positioning"), null, NOTE_COLOR));
        list.addEntry(new WidgetRow(null,
                Button.builder(Component.translatable("rpghealthbar.config.itemName"), button -> this.minecraft.setScreen(new ItemNameScreen(this)))
                        .size(0, BUTTON_HEIGHT)
                        .build(),
                Button.builder(Component.translatable("rpghealthbar.config.moveHud"), button -> this.minecraft.setScreen(new HudEditScreen(this)))
                        .size(0, BUTTON_HEIGHT)
                        .build())
                .described(description("itemName"), hudEditorTooltip()));
    }

    /**
     * A bar's width: Full or Half in Simple edit mode; in Advanced, a button that opens the HUD
     * Editor, where the bar's ends are dragged.
     */
    private WidgetRow widthOption(String key, @Nullable ForgeConfigSpec.EnumValue<BarSize> preset) {
        if (preset == null || BarWidths.advanced()) {
            return editorRow(key, "rpghealthbar.config.editInHudEditor.tooltip");
        }
        return option(key, BarSize.values(), preset, BarSize::displayName, "full", "half");
    }

    /** Advanced mode only: an upright bar's height, changed in the HUD Editor. */
    private WidgetRow heightRow(String key) {
        return editorRow(key, "rpghealthbar.config.editInHudEditor.tooltip.height");
    }

    private WidgetRow editorRow(String key, String howKey) {
        return new WidgetRow(label(key),
                Button.builder(Component.translatable("rpghealthbar.config.editInHudEditor"), button -> this.minecraft.setScreen(editorFor(key)))
                        .size(VALUE_WIDTH, BUTTON_HEIGHT)
                        .build())
                .described(advancedTooltip(key, howKey));
    }

    /** The HUD Editor for a size's button: in Mount View for the XP bar's width with XP Bar on Hidden, to show the jump bar. */
    private HudEditScreen editorFor(String key) {
        return key.equals("xpWidth") && ClientConfig.xpBar() == XpBarMode.HIDDEN ? HudEditScreen.mountView(this) : new HudEditScreen(this);
    }

    /**
     * A size's tooltip in Advanced mode: "Advanced Mode:" in cyan, what the size is, and how to
     * change it in the HUD Editor.
     */
    private static Component advancedTooltip(String key, String howKey) {
        return Component.translatable("rpghealthbar.config.advancedModeTitle").withStyle(ChatFormatting.AQUA)
                .append(" ").append(Component.translatable("rpghealthbar.config." + key + ".tooltip").withStyle(ChatFormatting.WHITE))
                .append(" ").append(Component.translatable(howKey).withStyle(ChatFormatting.WHITE));
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
        return Component.translatable("rpghealthbar.config." + key).append(":");
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
        return new WidgetRow(label(key), CycleButton.builder(displayName)
                .withValues(values)
                .withInitialValue(current)
                .displayOnlyValue()
                .create(0, 0, VALUE_WIDTH, BUTTON_HEIGHT, name, (button, newValue) -> onChange.accept(newValue)))
                .described(description(key, tooltipValues));
    }

    private static List<ForgeConfigSpec.ConfigValue<?>> edited() {
        List<ForgeConfigSpec.ConfigValue<?>> edited = new ArrayList<>(List.of(ClientConfig.ENABLED, ClientConfig.ARMOR_MODE,
                ClientConfig.HUNGER_MODE, ClientConfig.THIRST_MODE, ClientConfig.AIR_POSITION, ClientConfig.HEALTH_UNIT, ClientConfig.ABSORPTION_DISPLAY,
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
        edited.addAll(List.of(ClientConfig.LEVEL_TEXT, ClientConfig.EDIT_MODE, ClientConfig.XP_BAR,
                ClientConfig.XP_BAR_SHOWN, ClientConfig.XP_SIZE));
        return edited;
    }

    /** Which of AppleSkin and Thirst Was Taken 2 were found, for the Armor + Hunger header; null for neither. */
    @Nullable
    private static Component survivalDetected() {
        boolean appleSkin = AppleSkinCompat.isInstalled();
        if (ThirstCompat.isAvailable()) {
            return Component.translatable(appleSkin ? "rpghealthbar.config.appleSkinThirstDetected" : "rpghealthbar.config.thirstDetected");
        }
        return appleSkin ? Component.translatable("rpghealthbar.config.appleSkinDetected") : null;
    }

    /** "(Iron's + Confluence detected)", or that none was. */
    private static Component manaDetected(List<ManaSource> found) {
        if (found.isEmpty()) {
            return Component.translatable("rpghealthbar.config.manaNotDetected");
        }
        MutableComponent names = found.get(0).displayName().copy();
        for (ManaSource source : found.subList(1, found.size())) {
            names.append(" + ").append(source.displayName());
        }
        return Component.translatable("rpghealthbar.config.manaDetected", names);
    }

    /**
     * Where a bar's numbers go. Setting it also clears the Off saved before this option existed
     * ({@code legacyShown}), so the choice takes effect.
     *
     * @param legacyShown the setting from before {@code value}, or null
     */
    private static WidgetRow textOption(String key, BarText current, ForgeConfigSpec.EnumValue<BarText> value, @Nullable ForgeConfigSpec.BooleanValue legacyShown) {
        return option(key, BarText.values(), current, BarText::displayName, text -> {
            ClientConfig.set(value, text);
            if (legacyShown != null && !ClientConfig.read(legacyShown)) {
                ClientConfig.set(legacyShown, true);
            }
        }, "left", "right", "center", "off");
    }

    /**
     * Hover text: a description, then each possible value on its own paragraph with its name highlighted.
     * The first value is the default. Also used by the Item Name and HUD Editor pages.
     */
    static Tooltip tooltip(String option, String... values) {
        return Tooltip.create(description(option, values));
    }

    private static Component description(String option, String... values) {
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
        return ManaSource.colorNames(text);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        boolean side = this.sideWidth > 0;
        // Set every frame: a clicked button drops its tooltip.
        if (this.list != null) {
            this.list.showTooltips(!side);
        }
        if (this.editModeButton != null) {
            this.editModeButton.setTooltip(side ? null : this.editModeTooltip);
        }
        if (this.previewButton != null) {
            this.previewButton.setTooltip(side ? null : this.previewButton.tooltip);
        }
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
        graphics.drawCenteredString(this.font, this.title, this.width / 2, (HEADER_HEIGHT - 8) / 2, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (side) {
            drawDescription(graphics, mouseX, mouseY);
        }
    }

    /**
     * What the option under the mouse, or reached with the keyboard, does, under the preview.
     * Anywhere on an option's row counts.
     */
    private void drawDescription(GuiGraphics graphics, int mouseX, int mouseY) {
        Component text = headerDescription(mouseX, mouseY);
        if (text == null && this.list != null) {
            text = this.list.description(mouseX, mouseY);
        }
        if (text == null) {
            return;
        }
        int width = Math.min(this.sideWidth, DESCRIPTION_MAX_WIDTH);
        int room = this.height - FOOTER_HEIGHT - DESCRIPTION_BOTTOM - this.descriptionY;
        // Shrunk until it fits above the footer.
        float scale = 1.0F;
        List<FormattedCharSequence> lines = this.font.split(text, width);
        while (lines.size() * this.font.lineHeight * scale > room && scale > 0.5F) {
            scale -= 0.05F;
            lines = this.font.split(text, (int) (width / scale));
        }
        graphics.pose().pushPose();
        graphics.pose().translate(this.previewX - width / 2, this.descriptionY, 0);
        graphics.pose().scale(scale, scale, 1);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(this.font, lines.get(i), 0, i * this.font.lineHeight, LABEL_COLOR);
        }
        graphics.pose().popPose();
    }

    /** The description of the preview or Edit Mode button under the mouse or focused, or null. */
    @Nullable
    private Component headerDescription(int mouseX, int mouseY) {
        boolean keyboard = this.minecraft.getLastInputType().isKeyboard();
        if (this.previewButton != null
                && (this.previewButton.isMouseOver(mouseX, mouseY) || keyboard && getFocused() == this.previewButton)) {
            return Component.translatable("rpghealthbar.config.preview.tooltip." + (ClientConfig.SHOW_PREVIEW.get() ? "on" : "off"));
        }
        if (this.editModeButton != null
                && (this.editModeButton.isMouseOver(mouseX, mouseY) || keyboard && getFocused() == this.editModeButton)) {
            return description("editMode", "simple", "advanced");
        }
        return null;
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
        boolean changed = this.opened.entrySet().stream().anyMatch(entry -> !ClientConfig.read(entry.getKey()).equals(entry.getValue()));
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
                for (ForgeConfigSpec.ConfigValue<?> value : EDITED) {
                    // Not read back: a reload may briefly undo a reset value before it's written again.
                    this.opened.put(value, value.getDefault());
                }
            }
            // Coming back rebuilds the options, showing their new values.
            this.minecraft.setScreen(this);
        }, Component.translatable("rpghealthbar.config.reset.title"), Component.translatable("rpghealthbar.config.reset.message")));
    }

    @SuppressWarnings("unchecked")
    private static <T> void restore(ForgeConfigSpec.ConfigValue<T> value, Object opened) {
        if (!ClientConfig.read(value).equals(opened)) {
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

        private final Tooltip tooltip;

        PreviewButton(OnPress onPress) {
            super(0, 0, BUTTON_HEIGHT, BUTTON_HEIGHT, Component.empty(), onPress, DEFAULT_NARRATION);
            // The screen is rebuilt on each click, so this button always starts with the current setting.
            String state = ClientConfig.SHOW_PREVIEW.get() ? "on" : "off";
            setMessage(Component.translatable("rpghealthbar.config.preview." + state));
            this.tooltip = Tooltip.create(Component.translatable("rpghealthbar.config.preview.tooltip." + state));
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

        /**
         * The description of the widget under the mouse, or else of the one reached with the
         * keyboard; null for neither.
         */
        @Nullable
        Component description(int mouseX, int mouseY) {
            if (isMouseOver(mouseX, mouseY) && getEntryAtPosition(mouseX, mouseY) instanceof WidgetRow row) {
                for (AbstractWidget widget : row.widgets) {
                    if (row.widgets.size() == 1 || widget.isMouseOver(mouseX, mouseY)) {
                        return row.description(widget);
                    }
                }
            }
            if (this.minecraft.getLastInputType().isKeyboard() && this.minecraft.screen != null
                    && this.minecraft.screen.getFocused() == this && getFocused() instanceof WidgetRow row
                    && row.getFocused() instanceof AbstractWidget widget) {
                return row.description(widget);
            }
            return null;
        }

        /** Gives every widget its description as a tooltip, or takes the tooltips away. */
        void showTooltips(boolean shown) {
            for (Row row : children()) {
                if (row instanceof WidgetRow widgetRow) {
                    widgetRow.showTooltips(shown);
                }
            }
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
     * The compatibility notes' row: a + button (− while they're open) and the label beside it, both
     * of which open and close them. How many notes there are follows the label, in white.
     */
    private static class CompatRow extends Row {
        private final Button button;
        private final Component label;
        private final Runnable toggle;
        private int labelLeft;
        private int labelTop;

        CompatRow(int count, boolean open, Runnable toggle) {
            this.toggle = toggle;
            this.button = Button.builder(Component.literal(open ? "-" : "+"), b -> toggle.run()).size(BUTTON_HEIGHT, BUTTON_HEIGHT).build();
            MutableComponent label = Component.translatable("rpghealthbar.config.compat").copy();
            if (count > 0) {
                label.append(" (" + count + ")");
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
        /** What each widget does, in their order, and the same as tooltips. */
        private List<Component> descriptions = List.of();
        private List<Tooltip> tooltips = List.of();


        WidgetRow(@Nullable Component label, AbstractWidget... widgets) {
            this.label = label;
            this.widgets = List.of(widgets);
        }

        WidgetRow described(Component... descriptions) {
            this.descriptions = List.of(descriptions);
            this.tooltips = this.descriptions.stream().map(Tooltip::create).toList();
            return this;
        }

        /** Greys the row's widgets out, so they can't be used, unless {@code active}. */
        WidgetRow active(boolean active) {
            this.widgets.forEach(widget -> widget.active = active);
            return this;
        }

        void showTooltips(boolean shown) {
            for (int i = 0; i < this.tooltips.size(); i++) {
                this.widgets.get(i).setTooltip(shown ? this.tooltips.get(i) : null);
            }
        }

        @Nullable
        Component description(AbstractWidget widget) {
            int index = this.widgets.indexOf(widget);
            return index >= 0 && index < this.descriptions.size() ? this.descriptions.get(index) : null;
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
