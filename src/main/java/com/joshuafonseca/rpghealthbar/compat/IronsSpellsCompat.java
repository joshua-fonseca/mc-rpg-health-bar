package com.joshuafonseca.rpghealthbar.compat;

import com.joshuafonseca.rpghealthbar.RpgHealthBar;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/**
 * Safe to call whether or not Iron's Spells 'n Spellbooks is installed. Only touches
 * {@link IronsSpellsMana} (which references Iron's classes) when the mod is loaded.
 */
public final class IronsSpellsCompat {
    public static final String MODID = "irons_spellbooks";
    /** Iron's own mana bar, which our mana bar replaces. */
    public static final ResourceLocation MANA_OVERLAY = ResourceLocation.fromNamespaceAndPath(MODID, "mana_overlay");

    /** Cleared if reading mana ever fails, e.g. a future Iron's version renamed something we use. */
    private static boolean available = ModList.get().isLoaded(MODID);

    private IronsSpellsCompat() {
    }

    public record Mana(float current, float max) {
    }

    /** Whether the mana bar should be part of the layout. */
    public static boolean isAvailable() {
        return available;
    }

    /**
     * The local player's mana, or null if Iron's Spells isn't installed or its API has changed in a
     * way we can't read. In the latter case a warning is logged once and the mana bar is turned off
     * for the rest of the session instead of crashing the game.
     */
    @Nullable
    public static Mana read(Player player) {
        if (!available) {
            return null;
        }
        try {
            return new Mana(IronsSpellsMana.mana(), IronsSpellsMana.maxMana(player));
        } catch (LinkageError | RuntimeException e) {
            available = false;
            RpgHealthBar.LOGGER.warn("Couldn't read mana from Iron's Spells; this version may not be supported. The mana bar is turned off.", e);
            return null;
        }
    }
}
