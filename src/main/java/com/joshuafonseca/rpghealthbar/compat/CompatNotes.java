package com.joshuafonseca.rpghealthbar.compat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.fml.ModList;
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

    /**
     * The oldest version of {@code modId} that works, as the README's Compatibility table gives it;
     * with a {@code feature}, the oldest for just that feature. Every Forge build of Confluence
     * (1.2.9 and later) works, so it has none.
     */
    private record Requirement(String modId, String name, String minVersion, @Nullable String feature) {
    }

    private static final List<Requirement> REQUIREMENTS = List.of(
            new Requirement(IronsSpellsCompat.MODID, "Iron's", "1.1.5", null),
            new Requirement(IronsSpellsCompat.MODID, "Iron's", "3.1.4", "rpghealthbar.config.compat.feature.spellCost"),
            new Requirement(AppleSkinCompat.MODID, "AppleSkin", "2.5.0", null));

    private CompatNotes() {
    }

    public static List<Note> notes() {
        List<Note> notes = new ArrayList<>();
        Set<String> noted = new HashSet<>();
        for (Requirement requirement : REQUIREMENTS) {
            Optional<String> installed = installedVersion(requirement.modId());
            if (installed.isEmpty() || noted.contains(requirement.modId()) || !isOlder(installed.get(), requirement.minVersion())) {
                continue;
            }
            Component text = requirement.feature() == null
                    ? Component.translatable("rpghealthbar.config.compat.tooOld", requirement.name(), requirement.minVersion(), installed.get())
                    : Component.translatable("rpghealthbar.config.compat.tooOldFeature", requirement.name(), requirement.minVersion(),
                            Component.translatable(requirement.feature()), installed.get());
            notes.add(new Note(text, true));
            noted.add(requirement.modId());
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
        if (installedVersion(ConfluenceCompat.MODID).isPresent() && !ConfluenceCompat.isWorking()) {
            notes.add(new Note(Component.translatable("rpghealthbar.config.compat.failed", "Confluence"), true));
        }
        if (installedVersion(ArsNouveauCompat.MODID).isPresent() && !ArsNouveauCompat.isWorking()) {
            notes.add(new Note(Component.translatable("rpghealthbar.config.compat.failed", "Ars"), true));
        }
        // Not a problem: two mana mods with no option of their own to share mana each keep their
        // own bar. Compatibility mods that merge them aren't accounted for, so say so.
        // One note lists every such pair, a line each, then says it once.
        List<ManaSource> manaMods = Arrays.stream(ManaSource.values()).filter(ManaSource::available).toList();
        MutableComponent pairs = Component.empty();
        for (int i = 0; i < manaMods.size(); i++) {
            for (int j = i + 1; j < manaMods.size(); j++) {
                if (!ManaSource.canUnify(manaMods.get(i), manaMods.get(j))) {
                    pairs.append(Component.translatable("rpghealthbar.config.compat.noUnifiedMana",
                            manaMods.get(i).displayName(), manaMods.get(j).displayName())).append("\n");
                }
            }
        }
        if (!pairs.getSiblings().isEmpty()) {
            notes.add(new Note(pairs.append(Component.translatable("rpghealthbar.config.compat.noUnifiedManaMods")), false));
        }
        return notes;
    }

    /** Whether any note is a problem, rather than just good to know. */
    public static boolean anyProblem(List<Note> notes) {
        return notes.stream().anyMatch(Note::problem);
    }

    /**
     * The mod's own version, without the Minecraft version mods add before or after it: Iron's
     * Spells reports "1.20.1-3.16.3", AppleSkin "2.5.1+mc1.20.1".
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
