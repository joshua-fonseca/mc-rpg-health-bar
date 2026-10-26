package com.joshuafonseca.rpghealthbar.compat;

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

    private static final boolean LOADED = ModList.get().isLoaded(MODID);

    private IronsSpellsCompat() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    /** Only call when {@link #isLoaded()} is true. */
    public static float mana() {
        return IronsSpellsMana.mana();
    }

    /** Only call when {@link #isLoaded()} is true. */
    public static float maxMana(Player player) {
        return IronsSpellsMana.maxMana(player);
    }
}
