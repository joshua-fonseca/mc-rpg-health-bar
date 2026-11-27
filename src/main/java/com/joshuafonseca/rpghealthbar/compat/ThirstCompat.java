package com.joshuafonseca.rpghealthbar.compat;

import com.joshuafonseca.rpghealthbar.RpgHealthBar;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

/**
 * Safe to call whether or not Thirst Was Taken 2 is installed. Only touches {@link ThirstApiBridge}
 * (which references its classes) when the mod is loaded. The original Thirst Was Taken is a
 * different mod and isn't read.
 */
public final class ThirstCompat {
    public static final String MODID = "thirstwastaken2";
    /** Its thirst bar's layer: moved into our layout on Default, hidden otherwise. */
    public static final ResourceLocation LAYER = new ResourceLocation(MODID, "thirst_bar");
    /** Its droplet icons, for the config screen's preview. */
    public static final ResourceLocation ICONS = new ResourceLocation(MODID, "textures/gui/thirst_icons.png");

    private static final String CONFIG = "com.thirstwastaken2.config.ThirstConfig";

    private static final boolean INSTALLED = ModList.get().isLoaded(MODID);
    /** Cleared if reading the mod ever fails, e.g. a version older than its player API. */
    private static boolean available = INSTALLED;
    private static boolean checked;

    private ThirstCompat() {
    }

    /**
     * @param thirst   0 to {@code max}
     * @param quenched the reserve to outline, or 0 for no outline
     */
    public record Thirst(float thirst, float quenched, float max) {
    }

    /** Whether thirst should be part of the layout. */
    public static boolean isAvailable() {
        if (available && !checked) {
            checked = true;
            try {
                ThirstApiBridge.max();
            } catch (LinkageError | RuntimeException e) {
                disable(e);
            }
        }
        return available;
    }

    /**
     * Whether the mod's own bar would show the player's thirst: not while riding a living entity,
     * with thirst turned off for the player, or if it can't be read.
     */
    public static boolean isShown(Player player) {
        if (!isAvailable() || player.getVehicle() instanceof LivingEntity) {
            return false;
        }
        try {
            return ThirstApiBridge.isEnabled(player);
        } catch (LinkageError | RuntimeException e) {
            disable(e);
            return false;
        }
    }

    /** The player's thirst, or null when it isn't shown ({@link #isShown}) or can't be read. */
    @Nullable
    public static Thirst read(Player player) {
        if (!isShown(player)) {
            return null;
        }
        try {
            // Like the mod's own bar, quenched is only outlined with AppleSkin.
            float quenched = AppleSkinCompat.isInstalled() && quenchedOverlayOn() ? ThirstApiBridge.quenched(player) : 0;
            return new Thirst(ThirstApiBridge.thirst(player), quenched, ThirstApiBridge.max());
        } catch (LinkageError | RuntimeException e) {
            disable(e);
            return null;
        }
    }

    /**
     * Whether the mod's quenched overlay setting isn't OFF. It's not part of its API, so it's read by
     * name, and counts as on if that fails.
     */
    private static boolean quenchedOverlayOn() {
        try {
            Class<?> config = Class.forName(CONFIG);
            Object current = config.getMethod("get").invoke(null);
            return !"OFF".equals(String.valueOf(config.getField("appleskinQuenchedOverlay").get(current)));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return true;
        }
    }

    private static void disable(Throwable e) {
        available = false;
        RpgHealthBar.LOGGER.warn("Couldn't read Thirst Was Taken 2; this version may not be supported. Its own thirst bar is left as it is.", e);
    }
}
