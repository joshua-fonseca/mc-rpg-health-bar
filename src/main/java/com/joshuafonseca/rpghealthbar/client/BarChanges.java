package com.joshuafonseca.rpghealthbar.client;

import net.minecraft.Util;

/** Remembers when a bar's value last dropped and rose, for the flash and highlight animations. */
public final class BarChanges {
    private float lastValue = Float.NaN;
    private long lastDrop = Long.MIN_VALUE / 2;
    private long lastGain = Long.MIN_VALUE / 2;
    /** Where the latest run of gains started; gains close together are shown as one section. */
    private float gainFrom;

    /**
     * Call once per frame with the bar's current value.
     *
     * @param gainWindow gains within this many milliseconds of the last one extend the same section
     */
    public void update(float value, long gainWindow) {
        long now = Util.getMillis();
        if (!Float.isNaN(lastValue)) {
            if (value < lastValue) {
                lastDrop = now;
                gainFrom = Math.min(gainFrom, value);
            } else if (value > lastValue) {
                if (now - lastGain > gainWindow) {
                    gainFrom = lastValue;
                }
                lastGain = now;
            }
        }
        lastValue = value;
    }

    /** Milliseconds since the value last rose (very large if it never has). */
    public long sinceGain() {
        return Util.getMillis() - lastGain;
    }

    /** The value the latest gain started from. */
    public float gainFrom() {
        return gainFrom;
    }

    /** Milliseconds since the value last dropped (very large if it never has). */
    public long sinceDrop() {
        return Util.getMillis() - lastDrop;
    }
}
