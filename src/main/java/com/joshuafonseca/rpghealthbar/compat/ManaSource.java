package com.joshuafonseca.rpghealthbar.compat;

import com.joshuafonseca.rpghealthbar.config.BarSize;
import com.joshuafonseca.rpghealthbar.config.BarText;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * A mod whose mana gets its own bar, in the order the bars stack from the top. Each one is its own
 * HUD piece with its own colors and text and width settings; the layout, renderer, HUD Editor and
 * config screen go through all of them, so adding a mod means adding one here (with its config
 * values, piece and lang lines).
 * <p>
 * Only {@link #available}, {@link #shown}, {@link #read} and {@link #hidesOwnDisplay} touch the mod,
 * through its compat class, which is safe to call whether or not the mod is installed.
 */
public enum ManaSource {
    /** Iron's Spells 'n Spellbooks. Its settings keep their original names (manaText...), from before Confluence. */
    IRONS(HudPiece.MANA, "irons", 0xFF2C88D0, 0xFF143C64, 0xFF143C64,
            "manaText", ClientConfig.MANA_TEXT, ClientConfig.SHOW_MANA_TEXT, "manaSize", ClientConfig.MANA_SIZE,
            ClientConfig.MANA_WIDTH, ClientConfig.MANA_LEFT) {
        @Override
        public boolean available() {
            return IronsSpellsCompat.isAvailable();
        }

        /** Hidden when Confluence's convertIronsSpellMana makes Iron's spells use Confluence's mana. */
        @Override
        public boolean shown() {
            return available() && !ironsConverted();
        }

        @Nullable
        @Override
        protected Mana readShown(Player player) {
            return IronsSpellsCompat.read(player);
        }

        @Override
        public Mana placeholder(float percent) {
            return new Mana(percent, 100, 0);
        }

        @Override
        public BarText text() {
            return ClientConfig.manaText();
        }

        @Override
        public boolean hidesOwnDisplay(ResourceLocation layer) {
            return layer.equals(IronsSpellsCompat.MANA_OVERLAY);
        }
    },
    /**
     * Confluence: Otherworld, below Iron's: a darker, purplish blue like its mana stars. With
     * convertIronsSpellMana on, Iron's spell cost preview goes on this bar.
     */
    CONFLUENCE(HudPiece.CONFLUENCE_MANA, "confluence", 0xFF3A50B8, 0xFF1A2456, 0xFF1A2456,
            "confluenceManaText", ClientConfig.CONFLUENCE_MANA_TEXT, null, "confluenceManaSize", ClientConfig.CONFLUENCE_MANA_SIZE,
            ClientConfig.CONFLUENCE_MANA_WIDTH, ClientConfig.CONFLUENCE_MANA_LEFT) {
        @Override
        public boolean available() {
            return ConfluenceCompat.isAvailable();
        }

        @Nullable
        @Override
        protected Mana readShown(Player player) {
            return ConfluenceCompat.read(ironsConverted() ? IronsSpellsCompat.hoveredCost(player) : 0);
        }

        /** Its convertIronsSpellMana and convertArsNouveauMana (1.1.4+). */
        @Override
        protected boolean hasUnifiedManaOption(ManaSource other) {
            return other == IRONS || other == ARS;
        }

        /** Confluence starts players at 20 mana. */
        @Override
        public Mana placeholder(float percent) {
            return new Mana(percent / 5, 20, 0);
        }

        /** In soul mode its display shows soul instead of mana, so it's kept. */
        @Override
        public boolean hidesOwnDisplay(ResourceLocation layer) {
            return layer.equals(ConfluenceCompat.MANA_LAYER) && available()
                    && !ConfluenceCompat.isSoulMode(Minecraft.getInstance().player);
        }
    },
    /** Ars Nouveau, below the others: purple like its own bar. Its mana is separate from Iron's, even with both installed. */
    ARS(HudPiece.ARS_MANA, "ars", 0xFFA048E0, 0xFF40205C, 0xFF40205C,
            "arsManaText", ClientConfig.ARS_MANA_TEXT, null, "arsManaSize", ClientConfig.ARS_MANA_SIZE,
            ClientConfig.ARS_MANA_WIDTH, ClientConfig.ARS_MANA_LEFT) {
        @Override
        public boolean available() {
            return ArsNouveauCompat.isAvailable();
        }

        /** Hidden when Confluence's convertArsNouveauMana makes Ars's spells use Confluence's mana. */
        @Override
        public boolean shown() {
            return available() && !(ConfluenceCompat.isAvailable() && ConfluenceCompat.convertsArsMana());
        }

        @Nullable
        @Override
        protected Mana readShown(Player player) {
            return ArsNouveauCompat.read(player);
        }

        /** Ars Nouveau starts players at 100 mana. */
        @Override
        public Mana placeholder(float percent) {
            return new Mana(percent, 100, 0);
        }

        @Override
        public boolean hidesOwnDisplay(ResourceLocation layer) {
            return layer.equals(ArsNouveauCompat.MANA_LAYER);
        }

        /** Ars's own bar is animated with rising bubbles. */
        @Override
        public ModConfigSpec.BooleanValue bubblesValue() {
            return ClientConfig.ARS_MANA_BUBBLES;
        }
    };

    /** How far a name's color is lightened from its bar color toward white. */
    private static final float NAME_LIGHTEN = 0.35F;

    private final HudPiece piece;
    private final String nameKey;
    private final int color;
    private final int lostColor;
    private final int costColor;
    private final String textKey;
    private final ModConfigSpec.EnumValue<BarText> textValue;
    @Nullable
    private final ModConfigSpec.BooleanValue legacyTextShown;
    private final String sizeKey;
    private final ModConfigSpec.EnumValue<BarSize> size;
    private final ModConfigSpec.IntValue width;
    private final ModConfigSpec.IntValue left;

    /**
     * @param nameKey         the mod's name in lang, under rpghealthbar.config.manaMod
     * @param lostColor       the section just lost, shown briefly before it shrinks away
     * @param costColor       a spell's cost blinks between the mana color and this
     * @param legacyTextShown the setting from before {@code textValue}, whose false still turns the numbers off
     */
    ManaSource(HudPiece piece, String nameKey, int color, int lostColor, int costColor,
               String textKey, ModConfigSpec.EnumValue<BarText> textValue, @Nullable ModConfigSpec.BooleanValue legacyTextShown,
               String sizeKey, ModConfigSpec.EnumValue<BarSize> size, ModConfigSpec.IntValue width, ModConfigSpec.IntValue left) {
        this.piece = piece;
        this.nameKey = nameKey;
        this.color = color;
        this.lostColor = lostColor;
        this.costColor = costColor;
        this.textKey = textKey;
        this.textValue = textValue;
        this.legacyTextShown = legacyTextShown;
        this.sizeKey = sizeKey;
        this.size = size;
        this.width = width;
        this.left = left;
    }

    /** Whether the mod is installed and its mana can be read. */
    public abstract boolean available();

    /** Whether its bar is part of the layout. */
    public boolean shown() {
        return available();
    }

    /** The player's mana, or null when the bar isn't shown or the mana couldn't be read. */
    @Nullable
    public final Mana read(Player player) {
        return shown() ? readShown(player) : null;
    }

    @Nullable
    protected abstract Mana readShown(Player player);

    /** A bar {@code percent} full, for previews when there's no player to read from. */
    public abstract Mana placeholder(float percent);

    /**
     * Whether this mod has its own option to share mana with {@code other}. Compatibility mods
     * that do it aren't counted: this mod doesn't account for them.
     */
    protected boolean hasUnifiedManaOption(ManaSource other) {
        return false;
    }

    /** Whether either mod has its own option to share mana with the other. */
    public static boolean canUnify(ManaSource a, ManaSource b) {
        return a.hasUnifiedManaOption(b) || b.hasUnifiedManaOption(a);
    }

    /** Whether to hide the GUI layer {@code layer}, the mod's own mana display, while this mod's on. */
    public abstract boolean hidesOwnDisplay(ResourceLocation layer);

    /** Its setting for bubbles rising through the fill, like on the mod's own bar, or null if it has none. */
    @Nullable
    public ModConfigSpec.BooleanValue bubblesValue() {
        return null;
    }

    /** Whether bubbles rise through its fill. */
    public boolean bubbles() {
        ModConfigSpec.BooleanValue value = bubblesValue();
        return value != null && value.get();
    }

    /** Where its numbers go. */
    public BarText text() {
        return textValue.get();
    }

    public HudPiece piece() {
        return piece;
    }

    public int color() {
        return color;
    }

    public int lostColor() {
        return lostColor;
    }

    public int costColor() {
        return costColor;
    }

    /** Its name in the config screen's text: its bar color, lightened to read well on the dark background. */
    public int textColor() {
        int r = (this.color >> 16) & 0xFF;
        int g = (this.color >> 8) & 0xFF;
        int b = this.color & 0xFF;
        return (lighten(r) << 16) | (lighten(g) << 8) | lighten(b);
    }

    private static int lighten(int channel) {
        return Math.round(channel + (255 - channel) * NAME_LIGHTEN);
    }

    /**
     * {@code text} with each mana mod's name in its {@link #textColor}, wherever it appears
     * (labels, tooltips, notes), keeping the rest of the text's styles.
     */
    public static Component colorNames(Component text) {
        Map<String, Integer> colors = new HashMap<>();
        StringBuilder names = new StringBuilder();
        for (ManaSource source : values()) {
            String name = source.displayName().getString();
            colors.put(name, source.textColor());
            names.append(names.isEmpty() ? "" : "|").append(Pattern.quote(name));
        }
        Pattern pattern = Pattern.compile("\\b(" + names + ")\\b");
        MutableComponent out = Component.empty();
        text.visit((style, part) -> {
            Matcher matcher = pattern.matcher(part);
            int last = 0;
            while (matcher.find()) {
                out.append(Component.literal(part.substring(last, matcher.start())).withStyle(style));
                out.append(Component.literal(matcher.group()).withStyle(style.withColor(colors.get(matcher.group()))));
                last = matcher.end();
            }
            out.append(Component.literal(part.substring(last)).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    /** The mod's name, as in "(Iron's + Confluence detected)". */
    public Component displayName() {
        return Component.translatable("rpghealthbar.config.manaMod." + nameKey);
    }

    /** The text setting's key in lang and the config screen. */
    public String textKey() {
        return textKey;
    }

    public ModConfigSpec.EnumValue<BarText> textValue() {
        return textValue;
    }

    @Nullable
    public ModConfigSpec.BooleanValue legacyTextShown() {
        return legacyTextShown;
    }

    /** The Simple mode width setting's key in lang and the config screen. */
    public String sizeKey() {
        return sizeKey;
    }

    public ModConfigSpec.EnumValue<BarSize> size() {
        return size;
    }

    /** Advanced mode's width, or 0 for {@link #size}. */
    public ModConfigSpec.IntValue width() {
        return width;
    }

    /** Advanced mode: how far left of its usual spot the bar starts. */
    public ModConfigSpec.IntValue left() {
        return left;
    }

    /** The bars in the layout, top first. */
    public static List<ManaSource> shownBars() {
        return Arrays.stream(values()).filter(ManaSource::shown).toList();
    }

    /** Each shown bar's mana, top first; a bar whose mana couldn't be read is left out. */
    public static Map<ManaSource, Mana> readAll(Player player) {
        Map<ManaSource, Mana> mana = new EnumMap<>(ManaSource.class);
        for (ManaSource source : values()) {
            Mana value = source.read(player);
            if (value != null) {
                mana.put(source, value);
            }
        }
        return mana;
    }

    /** Each shown bar {@code percent} full, for previews. */
    public static Map<ManaSource, Mana> placeholders(float percent) {
        Map<ManaSource, Mana> mana = new EnumMap<>(ManaSource.class);
        for (ManaSource source : values()) {
            if (source.shown()) {
                mana.put(source, source.placeholder(percent));
            }
        }
        return mana;
    }

    /** Whether any mana mod hides {@code layer}, its own mana display. */
    public static boolean hidesAnyOwnDisplay(ResourceLocation layer) {
        for (ManaSource source : values()) {
            if (source.hidesOwnDisplay(layer)) {
                return true;
            }
        }
        return false;
    }

    /** With Confluence's convertIronsSpellMana on, Iron's spells use Confluence's mana. */
    private static boolean ironsConverted() {
        return IronsSpellsCompat.isAvailable() && ConfluenceCompat.convertsIronsMana();
    }
}
