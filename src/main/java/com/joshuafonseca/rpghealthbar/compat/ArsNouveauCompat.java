package com.joshuafonseca.rpghealthbar.compat;

import com.joshuafonseca.rpghealthbar.RpgHealthBar;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/**
 * Safe to call whether or not Ars Nouveau is installed. Its mana is read by name, the way its own
 * mana bar reads it: from the player's mana capability, which the server keeps in sync. If a future
 * version renames something, support is turned off (and the config screen's Compatibility notes
 * say so) instead of crashing the game.
 */
public final class ArsNouveauCompat {
    public static final String MODID = "ars_nouveau";
    /** Its mana bar, which ours replaces. */
    public static final ResourceLocation MANA_LAYER = ResourceLocation.fromNamespaceAndPath(MODID, "mana_hud");

    private static final String CAPABILITIES = "com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry";
    private static final String MANA_CAP = "com.hollingsworth.arsnouveau.api.mana.IManaCap";
    private static final String CLIENT_INFO = "com.hollingsworth.arsnouveau.client.ClientInfo";
    /** How long Ars's red flash lasts, in ticks, from the moment a spell fails: ClientInfo.redOverlayTicks starts here. */
    private static final float RED_TICKS = 35.0F;
    /** Ours fades out a little faster than Ars's, over this many ticks from the moment the spell fails. */
    private static final float RED_FADE_TICKS = 20.0F;

    private static final boolean INSTALLED = ModList.get().isLoaded(MODID);
    private static boolean available = INSTALLED;
    private static boolean looked;
    @Nullable
    private static Method getMana;
    @Nullable
    private static Method currentMana;
    @Nullable
    private static Method maxMana;
    /** What Ars's own bar draws besides the mana, from its client state; optional, so missing ones are just skipped. */
    @Nullable
    private static Field reservedMana;
    @Nullable
    private static Field redTicks;
    @Nullable
    private static Field redMana;

    private ArsNouveauCompat() {
    }

    /** Whether the Ars Nouveau mana bar should be part of the layout. */
    public static boolean isAvailable() {
        lookUp();
        return available;
    }

    /**
     * The player's Ars Nouveau mana, or null if it can't be read (or isn't synced yet). It regenerates
     * in fractions; like Ars's own bar, the whole mana is shown.
     */
    @Nullable
    public static Mana read(Player player) {
        if (!isAvailable()) {
            return null;
        }
        try {
            Object cap = getMana.invoke(null, player);
            if (cap == null) {
                return null;
            }
            float reserved = reservedMana == null ? 0 : Math.max(0, reservedMana.getFloat(null));
            int ticks = redTicks == null || redMana == null ? 0 : redTicks.getInt(null);
            float failedCost = ticks > 0 ? redMana.getFloat(null) : 0;
            return new Mana((float) Math.floor(((Number) currentMana.invoke(cap)).doubleValue()), ((Number) maxMana.invoke(cap)).floatValue(), 0,
                    reserved, failedCost, Math.max(0, Math.min(1, 1 - (RED_TICKS - ticks) / RED_FADE_TICKS)));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            disable("read mana", e);
            return null;
        }
    }

    /** Whether reading Ars Nouveau still works, for the Compatibility notes. */
    public static boolean isWorking() {
        return isAvailable();
    }

    private static void lookUp() {
        if (looked || !INSTALLED) {
            return;
        }
        looked = true;
        try {
            getMana = Class.forName(CAPABILITIES).getMethod("getMana", LivingEntity.class);
            Class<?> cap = Class.forName(MANA_CAP);
            currentMana = cap.getMethod("getCurrentMana");
            maxMana = cap.getMethod("getMaxMana");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            disable("find its mana", e);
            return;
        }
        reservedMana = optionalField("reservedOverlayMana");
        redTicks = optionalField("redOverlayTicks");
        redMana = optionalField("redOverlayMana");
    }

    @Nullable
    private static Field optionalField(String name) {
        try {
            return Class.forName(CLIENT_INFO).getField(name);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static void disable(String what, Throwable e) {
        available = false;
        RpgHealthBar.LOGGER.warn("Couldn't " + what + " from Ars Nouveau; this version may not be supported. Its mana bar is turned off.", e);
    }
}
