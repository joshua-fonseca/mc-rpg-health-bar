package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.compat.AppleSkinCompat;
import com.joshuafonseca.rpghealthbar.compat.Mana;
import com.joshuafonseca.rpghealthbar.compat.ManaSource;
import com.joshuafonseca.rpghealthbar.compat.ThirstCompat;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * The numbers shown on the bars, and the health and hunger bar colors, which change with status
 * effects (see {@link StatusColors}). {@code regenerating} drives the sweep that stands in for the
 * vanilla hearts' wave. {@code saturation} is outlined on the hunger bar and {@code heldFood}
 * previewed on it, only with AppleSkin. {@code mana} has each shown mana bar's numbers (see
 * {@link ManaSource}). {@code thirst} is Thirst Was Taken 2's, or null when it isn't shown.
 */
public record HudValues(float health, float maxHealth, float absorption, float armor, float food, float saturation,
                        @Nullable AppleSkinCompat.HeldFood heldFood, Map<ManaSource, Mana> mana,
                        @Nullable ThirstCompat.Thirst thirst, int healthColor, int hungerColor, int thirstColor, boolean regenerating) {
    public static HudValues of(Player player) {
        return new HudValues(player.getHealth(), player.getMaxHealth(), player.getAbsorptionAmount(),
                player.getArmorValue(), player.getFoodData().getFoodLevel(), AppleSkinCompat.saturation(player), AppleSkinCompat.heldFood(player),
                ManaSource.readAll(player),
                ThirstCompat.read(player),
                StatusColors.health(player), StatusColors.hunger(player), StatusColors.thirst(player),
                player.hasEffect(MobEffects.REGENERATION));
    }

    /** These values with another thirst. */
    public HudValues withThirst(@Nullable ThirstCompat.Thirst other) {
        return new HudValues(health, maxHealth, absorption, armor, food, saturation, heldFood, mana, other,
                healthColor, hungerColor, thirstColor, regenerating);
    }

    /** Shown in the position editor when there's no player to read from (e.g. opened from the title screen). */
    public static HudValues placeholder() {
        return new HudValues(20, 20, 0, 10, 20, 0, null, ManaSource.placeholders(100),
                new ThirstCompat.Thirst(20, 0, 20), StatusColors.HEALTH, StatusColors.HUNGER, StatusColors.THIRST, false);
    }
}
