package com.joshuafonseca.rpghealthbar.client;

import java.util.Locale;

public final class NumberText {
    private NumberText() {
    }

    /** Whole numbers without a decimal ("20"), anything else with one decimal ("17.3"). */
    public static String format(float value) {
        return formatRounded(Math.round(value * 10.0F) / 10.0F);
    }

    /** Rounded to the nearest 0.5, so always a whole number ("20") or a half ("17.5"). */
    public static String formatHalves(float value) {
        return formatRounded(Math.round(value * 2.0F) / 2.0F);
    }

    private static String formatRounded(float rounded) {
        if (rounded == (int) rounded) {
            return Integer.toString((int) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", rounded);
    }
}
