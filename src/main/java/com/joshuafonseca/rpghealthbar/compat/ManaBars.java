package com.joshuafonseca.rpghealthbar.compat;

import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;

/**
 * Which mana bars are shown: Iron's Spells' (top) and Confluence's (below it). With both mods and
 * Confluence's convertIronsSpellMana on, Iron's spells use Confluence's mana, so only Confluence's
 * bar is shown, with Iron's spell cost preview on it.
 */
public final class ManaBars {
    private ManaBars() {
    }

    public static boolean showIrons() {
        return IronsSpellsCompat.isAvailable() && !ironsConverted();
    }

    public static boolean showConfluence() {
        return ConfluenceCompat.isAvailable();
    }

    public static int count() {
        return (showIrons() ? 1 : 0) + (showConfluence() ? 1 : 0);
    }

    @Nullable
    public static Mana irons(Player player) {
        return showIrons() ? IronsSpellsCompat.read(player) : null;
    }

    @Nullable
    public static Mana confluence(Player player) {
        return showConfluence() ? ConfluenceCompat.read(ironsConverted() ? IronsSpellsCompat.hoveredCost(player) : 0) : null;
    }

    /** A full or partly full Iron's bar for previews, or null when it isn't shown. */
    @Nullable
    public static Mana placeholderIrons(float percent) {
        return showIrons() ? new Mana(percent, 100, 0) : null;
    }

    /** A Confluence bar for previews, or null when it isn't shown. Confluence starts players at 20 mana. */
    @Nullable
    public static Mana placeholderConfluence(float percent) {
        return showConfluence() ? new Mana(percent / 5, 20, 0) : null;
    }

    private static boolean ironsConverted() {
        return IronsSpellsCompat.isAvailable() && ConfluenceCompat.convertsIronsMana();
    }
}
