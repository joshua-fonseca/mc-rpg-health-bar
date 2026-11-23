package com.joshuafonseca.rpghealthbar.compat;

/**
 * A mana bar's numbers, from Iron's Spells or Confluence.
 *
 * @param hoveredCost mana cost of the spell hovered in Iron's spell wheel, or 0 when the wheel is
 *                    closed or the cost goes on the other bar
 */
public record Mana(float current, float max, float hoveredCost) {
}
