package com.joshuafonseca.rpghealthbar.client;

import java.util.Locale;

public final class NumberText {
    private NumberText() {
    }

    /** Whole numbers without a decimal ("20"), anything else with one decimal ("17.5"). */
    public static String format(float value) {
        float rounded = Math.round(value * 10.0F) / 10.0F;
        if (rounded == (int) rounded) {
            return Integer.toString((int) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", rounded);
    }
}
