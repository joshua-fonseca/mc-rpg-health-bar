package com.joshuafonseca.rpghealthbar;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

// Client-only mod: on a dedicated server it loads but does nothing.
@Mod(RpgHealthBar.MODID)
public class RpgHealthBar {
    public static final String MODID = "rpghealthbar";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RpgHealthBar() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            // In its own class, so a server never loads client classes.
            ClientSetup.init();
        }
    }
}
