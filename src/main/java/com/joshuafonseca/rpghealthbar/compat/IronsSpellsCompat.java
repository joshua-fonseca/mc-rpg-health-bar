package com.joshuafonseca.rpghealthbar.compat;

import com.joshuafonseca.rpghealthbar.RpgHealthBar;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

/**
 * Safe to call whether or not Iron's Spells 'n Spellbooks is installed. Only touches
 * {@link IronsSpellsMana} and {@link IronsSpellsWheel} (which reference Iron's classes) when the mod is loaded.
 */
public final class IronsSpellsCompat {
    public static final String MODID = "irons_spellbooks";
    /** Iron's own mana bar, which our mana bar replaces. */
    public static final ResourceLocation MANA_OVERLAY = new ResourceLocation(MODID, "mana_overlay");

    /** Cleared if reading mana ever fails, e.g. a future Iron's version renamed something we use. */
    private static boolean available = ModList.get().isLoaded(MODID);
    /** Cleared separately if reading the spell wheel fails, so the mana bar itself keeps working. */
    private static boolean wheelAvailable = true;

    private IronsSpellsCompat() {
    }

    /**
     * @param hoveredCost mana cost of the spell hovered in Iron's spell wheel, or 0 when the wheel
     *                    is closed
     */
    public record Mana(float current, float max, float hoveredCost) {
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
        float current;
        float max;
        try {
            current = IronsSpellsMana.mana();
            max = IronsSpellsMana.maxMana(player);
        } catch (LinkageError | RuntimeException e) {
            available = false;
            RpgHealthBar.LOGGER.warn("Couldn't read mana from Iron's Spells; this version may not be supported. The mana bar is turned off.", e);
            return null;
        }
        return new Mana(current, max, hoveredCost(player));
    }

    /** 0 if the wheel is closed, nothing is hovered yet, or the wheel can't be read. */
    private static float hoveredCost(Player player) {
        if (!wheelAvailable) {
            return 0;
        }
        try {
            return IronsSpellsWheel.hoveredSpellCost(player);
        } catch (LinkageError | ReflectiveOperationException | RuntimeException e) {
            wheelAvailable = false;
            RpgHealthBar.LOGGER.warn("Couldn't read Iron's Spells' spell wheel; this version may not be supported. Spell costs won't be previewed on the mana bar.", e);
            return 0;
        }
    }
}
