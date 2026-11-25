package com.joshuafonseca.rpghealthbar.compat;

/**
 * A mana bar's numbers, and what the mod's own bar shows on top of them.
 *
 * @param hoveredCost mana cost of the spell hovered in Iron's spell wheel, or 0 when the wheel is
 *                    closed or the cost goes on the other bar
 * @param reserved    Ars Nouveau: mana set aside and not spendable, as a fraction of max mana (0 for none);
 *                    like Ars's bar, it's a dark section at the right end and the bar is scaled to fit it
 * @param failedCost  Ars Nouveau: the cost of a spell that just failed for lack of mana, flashed in red
 *                    from the bar's left end like Ars's bar does (0 for none)
 * @param failedAlpha how strong that red still is, fading from 1 to 0
 */
public record Mana(float current, float max, float hoveredCost, float reserved, float failedCost, float failedAlpha) {
    public Mana(float current, float max, float hoveredCost) {
        this(current, max, hoveredCost, 0, 0, 0);
    }
}
