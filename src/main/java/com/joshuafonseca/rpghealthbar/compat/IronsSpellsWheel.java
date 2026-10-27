package com.joshuafonseca.rpghealthbar.compat;

import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.overlays.SpellWheelOverlay;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import java.lang.reflect.Field;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Player;

/**
 * Reads the spell wheel through Iron's spell selection API, which Iron's 3.2.0 introduced; older
 * versions fail here and only lose the spell cost preview. Only loaded through {@link IronsSpellsCompat}.
 */
final class IronsSpellsWheel {
    /** Iron's keeps the hovered wheel slot private; it's updated each time the wheel is drawn. */
    @Nullable
    private static Field wheelSelection;

    private IronsSpellsWheel() {
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
