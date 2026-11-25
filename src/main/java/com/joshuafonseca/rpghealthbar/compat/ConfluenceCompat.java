package com.joshuafonseca.rpghealthbar.compat;

import com.joshuafonseca.rpghealthbar.RpgHealthBar;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/**
 * Safe to call whether or not Confluence: Otherworld is installed. Confluence has no API for other
 * mods, so its mana is read by name; if a future version renames something, support is turned
 * off (and the config screen's Compatibility notes say so) instead of crashing the game.
 */
public final class ConfluenceCompat {
    public static final String MODID = "confluence";
    /** Its mana display (soul in soul mode), which our mana bar replaces outside soul mode. */
    public static final ResourceLocation MANA_LAYER = ResourceLocation.fromNamespaceAndPath(MODID, "mana_hud");
    /** Its own health, armor and hunger displays, drawn when they're on in its client config (they are by default). */
    public static final ResourceLocation HEALTH_LAYER = ResourceLocation.fromNamespaceAndPath(MODID, "health_hud");
    public static final ResourceLocation ARMOR_LAYER = ResourceLocation.fromNamespaceAndPath(MODID, "armor_hud");
    public static final ResourceLocation FOOD_LAYER = ResourceLocation.fromNamespaceAndPath(MODID, "food_hud");

    private static final String CLIENT_PACKETS = "org.confluence.mod.client.handler.ClientPacketHandler";
    private static final String COMPATIBILITY = "org.confluence.mod.client.handler.CompatibilityHandler";
    private static final String PLAYER_UTILS = "org.confluence.mod.util.PlayerUtils";
    private static final String CLIENT_CONFIGS = "org.confluence.mod.client.ClientConfigs";

    private static final boolean INSTALLED = ModList.get().isLoaded(MODID);
    private static boolean available = INSTALLED;
    private static boolean looked;
    @Nullable
    private static Method currentMana;
    @Nullable
    private static Method maxMana;
    /** Not in every version, so each is optional. */
    @Nullable
    private static Method convertsIronsMana;
    @Nullable
    private static Method convertsArsMana;
    @Nullable
    private static Method soulMode;
    private static final Map<String, Optional<Field>> SWITCHES = new HashMap<>();

    private ConfluenceCompat() {
    }

    /** Whether the Confluence mana bar should be part of the layout. */
    public static boolean isAvailable() {
        lookUp();
        return available;
    }

    /** The local player's Confluence mana, or null if it can't be read. */
    @Nullable
    public static Mana read(float hoveredCost) {
        if (!isAvailable()) {
            return null;
        }
        try {
            return new Mana(((Number) currentMana.invoke(null)).floatValue(), ((Number) maxMana.invoke(null)).floatValue(), hoveredCost);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            disable("read mana", e);
            return null;
        }
    }

    /**
     * Whether the server has Confluence's convertIronsSpellMana on: Iron's spells then use
     * Confluence's mana (Iron's max mana adds to Confluence's), and Iron's spell costs are given in
     * Confluence's units.
     */
    public static boolean convertsIronsMana() {
        if (!isAvailable() || convertsIronsMana == null) {
            return false;
        }
        try {
            return (Boolean) convertsIronsMana.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            convertsIronsMana = null;
            RpgHealthBar.LOGGER.warn("Couldn't read whether Confluence converts Iron's Spells mana; treating it as off.", e);
            return false;
        }
    }

    /**
     * Whether convertIronsSpellMana can be read. If not, it's treated as off, so the notes don't
     * suggest turning it on: it may already be.
     */
    public static boolean readsConvertsIronsMana() {
        convertsIronsMana();
        return isAvailable() && convertsIronsMana != null;
    }

    /** Whether convertArsNouveauMana can be read; see {@link #readsConvertsIronsMana}. */
    public static boolean readsConvertsArsMana() {
        convertsArsMana();
        return isAvailable() && convertsArsMana != null;
    }

    /**
     * Whether the server has Confluence's convertArsNouveauMana on: Ars Nouveau's spells then use
     * Confluence's mana (Ars's max mana adds to Confluence's), and Confluence hides Ars's mana bar.
     */
    public static boolean convertsArsMana() {
        if (!isAvailable() || convertsArsMana == null) {
            return false;
        }
        try {
            return (Boolean) convertsArsMana.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            convertsArsMana = null;
            RpgHealthBar.LOGGER.warn("Couldn't read whether Confluence converts Ars Nouveau mana; treating it as off.", e);
            return false;
        }
    }

    /** Whether the player is in Confluence's soul mode, where its display shows soul instead of mana. */
    public static boolean isSoulMode(@Nullable Player player) {
        if (player == null || !isAvailable() || soulMode == null) {
            return false;
        }
        try {
            return (Boolean) soulMode.invoke(null, player);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            soulMode = null;
            RpgHealthBar.LOGGER.warn("Couldn't read Confluence's soul mode; treating it as off.", e);
            return false;
        }
    }

    /** Whether Confluence's own health display is on in its client config (terraStyleHealth, on by default). */
    public static boolean showsOwnHealth() {
        return clientSwitch("terraStyleHealth");
    }

    /** Whether Confluence's own armor display is on in its client config (terraStyleArmor, on by default). */
    public static boolean showsOwnArmor() {
        return clientSwitch("terraStyleArmor");
    }

    /** Whether Confluence's own hunger display is on in its client config (terraStyleFood, on by default). */
    public static boolean showsOwnFood() {
        return clientSwitch("terraStyleFood");
    }

    /** One of Confluence's client switches, looked up once; if it can't be read, assumed on, its default. */
    private static boolean clientSwitch(String name) {
        if (!INSTALLED) {
            return false;
        }
        Field field = SWITCHES.computeIfAbsent(name, key -> {
            try {
                return Optional.of(Class.forName(CLIENT_CONFIGS).getField(key));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return Optional.empty();
            }
        }).orElse(null);
        try {
            return field == null || field.getBoolean(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return true;
        }
    }

    /** Whether reading Confluence still works, for the Compatibility notes. */
    public static boolean isWorking() {
        return isAvailable();
    }

    private static void lookUp() {
        if (looked || !INSTALLED) {
            return;
        }
        looked = true;
        try {
            Class<?> packets = Class.forName(CLIENT_PACKETS);
            currentMana = packets.getMethod("getCurrentMana");
            maxMana = packets.getMethod("getMaxMana");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            disable("find its mana", e);
            return;
        }
        convertsIronsMana = optionalMethod(COMPATIBILITY, "isConvertIronsSpellMana");
        convertsArsMana = optionalMethod(COMPATIBILITY, "isConvertArsNouveauMana");
        soulMode = optionalMethod(PLAYER_UTILS, "isSoulMode", Player.class);
    }

    @Nullable
    private static Method optionalMethod(String className, String name, Class<?>... parameters) {
        try {
            return Class.forName(className).getMethod(name, parameters);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static void disable(String what, Throwable e) {
        available = false;
        RpgHealthBar.LOGGER.warn("Couldn't " + what + " from Confluence; this version may not be supported. Its mana bar is turned off.", e);
    }
}
