package com.joshuafonseca.rpghealthbar.compat;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Reads mana through the part of Iron's Spells that's the same in every 1.20.1 version (1.1.5 on);
 * only loaded through {@link IronsSpellsCompat}.
 */
final class IronsSpellsMana {
    /** Looked up by name: Iron's moved the class holding it between versions. */
    private static final ResourceLocation MAX_MANA_ID = new ResourceLocation(IronsSpellsCompat.MODID, "max_mana");
    @Nullable
    private static Attribute maxMana;

    private IronsSpellsMana() {
    }

    /** The local player's mana, as synced to the client by Iron's Spells. */
    static float mana() {
        return ClientMagicData.getPlayerMana();
    }

    static float maxMana(Player player) {
        if (maxMana == null) {
            maxMana = ForgeRegistries.ATTRIBUTES.getValue(MAX_MANA_ID);
            if (maxMana == null) {
                throw new IllegalStateException("Iron's Spells' max mana attribute " + MAX_MANA_ID + " isn't registered");
            }
        }
        return (float) player.getAttributeValue(maxMana);
    }
}
