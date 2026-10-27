package com.joshuafonseca.rpghealthbar.compat;

import squeek.appleskin.ModConfig;

/** References AppleSkin's classes directly; only loaded through {@link AppleSkinCompat}. */
final class AppleSkinConfig {
    private AppleSkinConfig() {
    }

    /** AppleSkin's "show saturation overlay" setting, which also controls the outline on our bar. */
    static boolean showSaturation() {
        return ModConfig.SHOW_SATURATION_OVERLAY.get();
    }
}
