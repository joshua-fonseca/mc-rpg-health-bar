package com.joshuafonseca.rpghealthbar;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

// Client-only mod: this entry point is never loaded on a dedicated server.
@Mod(value = RpgHealthBar.MODID, dist = Dist.CLIENT)
public class RpgHealthBar {
    public static final String MODID = "rpghealthbar";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RpgHealthBar(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("RPG Health Bar loaded");
    }
}
