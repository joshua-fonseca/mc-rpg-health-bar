package com.joshuafonseca.rpghealthbar.compat;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.world.entity.player.Player;

/** References Iron's Spells classes directly; only loaded through {@link IronsSpellsCompat}. */
final class IronsSpellsMana {
    private IronsSpellsMana() {
    }

    /** The local player's mana, as synced to the client by Iron's Spells. */
    static float mana() {
        return ClientMagicData.getPlayerMana();
    }

    static float maxMana(Player player) {
        return (float) player.getAttributeValue(AttributeRegistry.MAX_MANA);
    }
}
