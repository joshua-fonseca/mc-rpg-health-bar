package com.joshuafonseca.rpghealthbar.compat;

import com.thirstwastaken2.api.ThirstApi;
import net.minecraft.world.entity.player.Player;

/**
 * The only class naming Thirst Was Taken 2's classes, so it's only loaded when the mod is
 * installed. Uses its public API.
 */
final class ThirstApiBridge {
    private ThirstApiBridge() {
    }

    static int max() {
        return ThirstApi.maxThirst();
    }

    static boolean isEnabled(Player player) {
        return ThirstApi.isEnabled(player);
    }

    static int thirst(Player player) {
        return ThirstApi.thirst(player);
    }

    static int quenched(Player player) {
        return ThirstApi.quenched(player);
    }
}
