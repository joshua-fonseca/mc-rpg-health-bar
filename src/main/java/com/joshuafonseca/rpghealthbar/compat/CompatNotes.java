package com.joshuafonseca.rpghealthbar.compat;

import com.joshuafonseca.rpghealthbar.client.NumberText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * What the config screen's Compatibility notes list: supported mods installed at a version too old
 * for what we use, and support turned off because a mod couldn't be read. Empty when all is well.
 * Each mod gets one note at most.
 */
public final class CompatNotes {
    /**
     * One note. A problem (a mod too old, or support turned off) shows in red; anything else is
     * just good to know, and shows in white.
     */
    public record Note(Component text, boolean problem) {
    }

    /** The oldest version of {@code modId} that works, as the README's Compatibility table gives it. */
    private record Requirement(String modId, String name, String minVersion) {
    }

    private static final List<Requirement> REQUIREMENTS = List.of(
            new Requirement(IronsSpellsCompat.MODID, "Iron's", "3.4.0"),
            new Requirement(AppleSkinCompat.MODID, "AppleSkin", "3.0.4"),
            // The first version whose mana is a decimal, as read now.
            new Requirement(ConfluenceCompat.MODID, "Confluence", "1.1.4"));

    private CompatNotes() {
    }

    public static List<Note> notes() {
        List<Note> notes = new ArrayList<>();
        Set<String> noted = new HashSet<>();
        for (Requirement requirement : REQUIREMENTS) {
            Optional<String> installed = installedVersion(requirement.modId());
            if (installed.isPresent() && isOlder(installed.get(), requirement.minVersion())) {
                notes.add(new Note(Component.translatable("rpghealthbar.config.compat.tooOld", requirement.name(), requirement.minVersion(), installed.get()), true));
                noted.add(requirement.modId());
            }
        }
        if (!noted.contains(IronsSpellsCompat.MODID) && installedVersion(IronsSpellsCompat.MODID).isPresent()) {
            if (!IronsSpellsCompat.isAvailable()) {
                notes.add(new Note(Component.translatable("rpghealthbar.config.compat.failed", "Iron's"), true));
            } else if (!IronsSpellsCompat.isWheelAvailable()) {
                notes.add(new Note(Component.translatable("rpghealthbar.config.compat.wheelFailed"), true));
            }
        }
        if (!noted.contains(AppleSkinCompat.MODID) && installedVersion(AppleSkinCompat.MODID).isPresent() && !AppleSkinCompat.isWorking()) {
            notes.add(new Note(Component.translatable("rpghealthbar.config.compat.failed", "AppleSkin"), true));
        }
        if (!noted.contains(ConfluenceCompat.MODID) && installedVersion(ConfluenceCompat.MODID).isPresent()) {
            if (!ConfluenceCompat.isWorking()) {
                notes.add(new Note(Component.translatable("rpghealthbar.config.compat.failed", "Confluence"), true));
            }
        }
        if (!noted.contains(ArsNouveauCompat.MODID) && installedVersion(ArsNouveauCompat.MODID).isPresent() && !ArsNouveauCompat.isWorking()) {
            notes.add(new Note(Component.translatable("rpghealthbar.config.compat.failed", "Ars"), true));
        }
        // Not a problem: with both mods' bars shown, say what two mana bars mean and how to get one.
        // With convertIronsSpellMana on, Iron's bar is already gone.
        if (ConfluenceCompat.isAvailable() && ManaSource.IRONS.shown()) {
            notes.add(new Note(Component.translatable("rpghealthbar.config.compat.unifiedMana"), false));
        }
        // Confluence adds Ars Nouveau's max mana (above Ars's starting max) to its own even with
        // convertArsNouveauMana off; counted before Ars sets it, that's -50, and Confluence's max goes below 0.
        Mana confluence = ArsNouveauCompat.isAvailable() ? ConfluenceCompat.read(0) : null;
        if (confluence != null && confluence.max() < 0) {
            notes.add(new Note(Component.translatable("rpghealthbar.config.compat.arsNegativeMana", NumberText.format(confluence.max())), true));
        }
        // Not a problem: Confluence (1.1.4+, NeoForge only) can take over Ars Nouveau's mana; until
        // its convertArsNouveauMana is on, say how to get one bar.
        if (ConfluenceCompat.isAvailable() && ManaSource.ARS.shown()) {
            notes.add(new Note(Component.translatable("rpghealthbar.config.compat.arsUnifiedMana"), false));
        }
        // Not a problem either: two mana mods with no option of their own to share mana each keep
        // their own bar. Compatibility mods that merge them aren't accounted for, so say so.
        List<ManaSource> manaMods = Arrays.stream(ManaSource.values()).filter(ManaSource::available).toList();
        for (int i = 0; i < manaMods.size(); i++) {
            for (int j = i + 1; j < manaMods.size(); j++) {
                if (!ManaSource.canUnify(manaMods.get(i), manaMods.get(j))) {
                    notes.add(new Note(Component.translatable("rpghealthbar.config.compat.noUnifiedMana",
                            manaMods.get(i).displayName(), manaMods.get(j).displayName()), false));
                }
            }
        }
        return notes;
    }

    /** Whether any note is a problem, rather than just good to know. */
    public static boolean anyProblem(List<Note> notes) {
        return notes.stream().anyMatch(Note::problem);
    }

    /**
     * The mod's own version, without the Minecraft version mods add before or after it: Iron's
     * Spells reports "1.21.1-3.16.3", AppleSkin "3.0.4+mc1.21".
     */
    private static Optional<String> installedVersion(String modId) {
        return ModList.get().getModContainerById(modId).map(container -> {
            String version = container.getModInfo().getVersion().toString();
            int plus = version.indexOf('+');
            if (plus >= 0) {
                version = version.substring(0, plus);
            }
            return version.substring(version.lastIndexOf('-') + 1);
        });
    }

    private static boolean isOlder(String version, String minVersion) {
        return new DefaultArtifactVersion(version).compareTo(new DefaultArtifactVersion(minVersion)) < 0;
    }
}
