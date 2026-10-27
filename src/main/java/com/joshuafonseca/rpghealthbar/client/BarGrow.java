package com.joshuafonseca.rpghealthbar.client;

import net.minecraft.Util;

/**
 * A fill that grows in from where it was when the value rises, over {@link #GROW_MILLIS}, easing
 * out. When the value drops, it follows at once.
 */
public final class BarGrow {
    private static final float GROW_MILLIS = 500.0F;

    private float shown;
    private float from;
    private float target;
    private long start;

    /** Updates the fill for this frame and returns the value its right end is at. */
    public float update(float value) {
        long now = Util.getMillis();
        if (value > target) {
            from = shown;
            start = now;
        }
        target = value;
        if (value <= shown) {
            shown = value;
        } else {
            float t = Math.min(1.0F, (now - start) / GROW_MILLIS);
            float eased = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
            shown = from + (value - from) * eased;
        }
        return shown;
    }
}
