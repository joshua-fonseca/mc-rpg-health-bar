package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.compat.IronsSpellsCompat;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;

/**
 * The numbers shown on the bars, and the health and hunger bar colors, which change with status
 * effects (see {@link StatusColors}).
 */
public record HudValues(float health, float maxHealth, float absorption, float armor, float food, @Nullable IronsSpellsCompat.Mana mana,
                        int healthColor, int hungerColor) {
    public static HudValues of(Player player) {
        return new HudValues(player.getHealth(), player.getMaxHealth(), player.getAbsorptionAmount(),
                player.getArmorValue(), player.getFoodData().getFoodLevel(), IronsSpellsCompat.read(player),
                StatusColors.health(player), StatusColors.hunger(player));
    }

    /** Shown in the position editor when there's no player to read from (e.g. opened from the title screen). */
    public static HudValues placeholder() {
        IronsSpellsCompat.Mana mana = IronsSpellsCompat.isAvailable() ? new IronsSpellsCompat.Mana(100, 100, 0) : null;
        return new HudValues(20, 20, 0, 10, 20, mana, StatusColors.HEALTH, StatusColors.HUNGER);
    }
}
