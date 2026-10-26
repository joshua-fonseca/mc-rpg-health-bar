package com.joshuafonseca.rpghealthbar.compat;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.overlays.SpellWheelOverlay;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import java.lang.reflect.Field;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;

/** References Iron's Spells classes directly; only loaded through {@link IronsSpellsCompat}. */
final class IronsSpellsMana {
    /** Iron's keeps the hovered wheel slot private; it's updated each time the wheel is drawn. */
    @Nullable
    private static Field wheelSelection;

    private IronsSpellsMana() {
    }

    /** The local player's mana, as synced to the client by Iron's Spells. */
    static float mana() {
        return ClientMagicData.getPlayerMana();
    }

    static float maxMana(Player player) {
        return (float) player.getAttributeValue(AttributeRegistry.MAX_MANA);
    }

    /** Mana cost of the spell hovered in the spell wheel, the same number the wheel shows; 0 if none. */
    static float hoveredSpellCost(Player player) throws ReflectiveOperationException {
        SpellWheelOverlay wheel = SpellWheelOverlay.instance;
        if (!wheel.active) {
            return 0;
        }
        if (wheelSelection == null) {
            Field field = SpellWheelOverlay.class.getDeclaredField("wheelSelection");
            field.setAccessible(true);
            wheelSelection = field;
        }
        int index = wheelSelection.getInt(wheel);
        if (index < 0) {
            return 0;
        }
        SpellData data = ClientMagicData.getSpellSelectionManager().getSpellData(index);
        int level = data.getSpell().getLevelFor(data.getLevel(), player);
        return data.getSpell().getManaCost(level);
    }
}
