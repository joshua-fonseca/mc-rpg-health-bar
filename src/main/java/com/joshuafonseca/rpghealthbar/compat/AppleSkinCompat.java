package com.joshuafonseca.rpghealthbar.compat;

import com.joshuafonseca.rpghealthbar.RpgHealthBar;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/**
 * Safe to call whether or not AppleSkin is installed. Only touches {@link AppleSkinConfig} (which
 * references AppleSkin's classes) when the mod is loaded.
 */
public final class AppleSkinCompat {
    public static final String MODID = "appleskin";
    /** AppleSkin's overlays on the vanilla hunger icons: saturation, held food's value, exhaustion. */
    public static final ResourceLocation SATURATION_LAYER = layer("saturation_level");
    public static final ResourceLocation HUNGER_RESTORED_LAYER = layer("hunger_restored");
    public static final ResourceLocation EXHAUSTION_LAYER = layer("exhaustion_level");
    /** AppleSkin's preview of the health held food would restore, drawn as vanilla hearts. */
    public static final ResourceLocation HEALTH_RESTORED_LAYER = layer("health_restored");

    /** Cleared if reading AppleSkin's config ever fails, e.g. a future version renamed it. */
    private static boolean available = ModList.get().isLoaded(MODID);

    private AppleSkinCompat() {
    }

    /** Whether a layer is one of AppleSkin's overlays on the vanilla hunger icons. */
    public static boolean isHungerOverlay(ResourceLocation layer) {
        return layer.equals(SATURATION_LAYER) || layer.equals(HUNGER_RESTORED_LAYER) || layer.equals(EXHAUSTION_LAYER);
    }

    /**
     * The player's saturation, to outline on the hunger bar, or 0 if AppleSkin isn't installed or
     * its saturation overlay is turned off in its config. Vanilla only sends saturation to the client
     * now and then; AppleSkin keeps it up to date, so it's only shown with AppleSkin.
     */
    public static float saturation(Player player) {
        if (!available) {
            return 0;
        }
        try {
            if (!AppleSkinConfig.showSaturation()) {
                return 0;
            }
        } catch (LinkageError | RuntimeException e) {
            available = false;
            RpgHealthBar.LOGGER.warn("Couldn't read AppleSkin's config; this version may not be supported. Saturation on the hunger bar is turned off.", e);
            return 0;
        }
        return player.getFoodData().getSaturationLevel();
    }

    private static ResourceLocation layer(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
