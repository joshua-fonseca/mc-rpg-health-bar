package com.joshuafonseca.rpghealthbar.client;

import net.minecraft.Util;

/** Remembers when a bar's value last dropped and rose, for the flash and highlight animations. */
public final class BarChanges {
    private float lastValue = Float.NaN;
    private long lastDrop = Long.MIN_VALUE / 2;

    /** Call once per frame with the bar's current value. */
    public void update(float value) {
        if (!Float.isNaN(lastValue) && value < lastValue) {
            lastDrop = Util.getMillis();
        }
        lastValue = value;
    }

    /** Milliseconds since the value last dropped (very large if it never has). */
    public long sinceDrop() {
        return Util.getMillis() - lastDrop;
    }
}
