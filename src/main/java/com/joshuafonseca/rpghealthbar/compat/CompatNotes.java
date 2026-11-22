package com.joshuafonseca.rpghealthbar.compat;

import java.util.ArrayList;
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
    /** The oldest version of {@code modId} that works, as the README's Compatibility table gives it. */
    private record Requirement(String modId, String name, String minVersion) {
    }

    private static final List<Requirement> REQUIREMENTS = List.of(
            new Requirement(IronsSpellsCompat.MODID, "Iron's Spells", "3.4.0"),
            new Requirement(AppleSkinCompat.MODID, "AppleSkin", "3.0.4"));

    private CompatNotes() {
    }

    public static List<Component> notes() {
        List<Component> notes = new ArrayList<>();
        Set<String> noted = new HashSet<>();
        for (Requirement requirement : REQUIREMENTS) {
            Optional<String> installed = installedVersion(requirement.modId());
            if (installed.isPresent() && isOlder(installed.get(), requirement.minVersion())) {
                notes.add(Component.translatable("rpghealthbar.config.compat.tooOld", requirement.name(), requirement.minVersion(), installed.get()));
                noted.add(requirement.modId());
            }
        }
        if (!noted.contains(IronsSpellsCompat.MODID) && installedVersion(IronsSpellsCompat.MODID).isPresent()) {
            if (!IronsSpellsCompat.isAvailable()) {
                notes.add(Component.translatable("rpghealthbar.config.compat.failed", "Iron's Spells"));
            } else if (!IronsSpellsCompat.isWheelAvailable()) {
                notes.add(Component.translatable("rpghealthbar.config.compat.wheelFailed"));
            }
        }
        if (!noted.contains(AppleSkinCompat.MODID) && installedVersion(AppleSkinCompat.MODID).isPresent() && !AppleSkinCompat.isWorking()) {
            notes.add(Component.translatable("rpghealthbar.config.compat.failed", "AppleSkin"));
        }
        return notes;
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
