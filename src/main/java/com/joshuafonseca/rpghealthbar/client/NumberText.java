package com.joshuafonseca.rpghealthbar.client;

import java.util.Locale;

public final class NumberText {
    private NumberText() {
    }

    /** Whole numbers without a decimal ("20"), anything else with one decimal ("17.3"). */
    public static String format(float value) {
        return formatRounded(Math.round(value * 10.0F) / 10.0F);
    }

    /** Rounded to the nearest 0.25: "20", "17.25", "17.5" or "17.75". */
    public static String formatQuarters(float value) {
        return formatRounded(Math.round(value * 4.0F) / 4.0F);
    }

    /** No trailing zeros: "20" rather than "20.00", "17.5" rather than "17.50". */
    private static String formatRounded(float rounded) {
        if (rounded == (int) rounded) {
            return Integer.toString((int) rounded);
        }
        String text = String.format(Locale.ROOT, "%.2f", rounded);
        return text.endsWith("0") ? text.substring(0, text.length() - 1) : text;
    }
}
