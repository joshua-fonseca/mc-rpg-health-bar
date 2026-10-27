package com.joshuafonseca.rpghealthbar.client;

import net.minecraft.Util;

/**
 * The darker "lost" section of a bar: when the value drops, the trail stays at the old value for a
 * moment, then shrinks smoothly down to the new value. When the value rises, it follows at once.
 */
public final class BarTrail {
    /** How long the lost section stays before it starts shrinking, unless given. */
    private static final long DEFAULT_HOLD_MILLIS = 750;
    /** Time for the remaining lost section to shrink by about two thirds; it's gone in well under a second. */
    private static final float SHRINK_MILLIS = 120.0F;

    private final long holdMillis;
    private float trail = Float.NaN;
    private float lastValue;
    private long lastDrop;
    private long lastUpdate;

    public BarTrail() {
        this(DEFAULT_HOLD_MILLIS);
    }

    public BarTrail(long holdMillis) {
        this.holdMillis = holdMillis;
    }

    /** Updates the trail for this frame and returns the value its right end is at. */
    public float update(float value) {
        long now = Util.getMillis();
        if (Float.isNaN(trail) || value >= trail) {
            trail = value;
        } else if (value < lastValue) {
            lastDrop = now;
        } else if (now - lastDrop > holdMillis) {
            float elapsed = now - lastUpdate;
            trail = value + (trail - value) * (float) Math.exp(-elapsed / SHRINK_MILLIS);
            if (trail - value < 0.01F) {
                trail = value;
            }
        }
        lastValue = value;
        lastUpdate = now;
        return trail;
    }
}
