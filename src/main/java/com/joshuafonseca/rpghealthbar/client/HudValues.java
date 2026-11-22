package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.compat.AppleSkinCompat;
import com.joshuafonseca.rpghealthbar.compat.Mana;
import com.joshuafonseca.rpghealthbar.compat.ManaBars;
import javax.annotation.Nullable;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * The numbers shown on the bars, and the health and hunger bar colors, which change with status
 * effects (see {@link StatusColors}). {@code regenerating} drives the sweep that stands in for the
 * vanilla hearts' wave. {@code saturation} is outlined on the hunger bar and {@code heldFood}
 * previewed on it, only with AppleSkin. {@code ironsMana} and {@code confluenceMana} are null when
 * that bar isn't shown (see {@link ManaBars}).
 */
public record HudValues(float health, float maxHealth, float absorption, float armor, float food, float saturation,
                        @Nullable AppleSkinCompat.HeldFood heldFood, @Nullable Mana ironsMana, @Nullable Mana confluenceMana,
                        int healthColor, int hungerColor, boolean regenerating) {
    public static HudValues of(Player player) {
        return new HudValues(player.getHealth(), player.getMaxHealth(), player.getAbsorptionAmount(),
                player.getArmorValue(), player.getFoodData().getFoodLevel(), AppleSkinCompat.saturation(player), AppleSkinCompat.heldFood(player),
                ManaBars.irons(player), ManaBars.confluence(player),
                StatusColors.health(player), StatusColors.hunger(player),
                player.hasEffect(MobEffects.REGENERATION));
    }

    /** Shown in the position editor when there's no player to read from (e.g. opened from the title screen). */
    public static HudValues placeholder() {
        return new HudValues(20, 20, 0, 10, 20, 0, null, ManaBars.placeholderIrons(100), ManaBars.placeholderConfluence(100),
                StatusColors.HEALTH, StatusColors.HUNGER, false);
    }
}
