package com.healthdekho.recommendation;

import java.util.Locale;

/** How well a recommended product fits the customer's goals (FR-2 D). */
public enum Suitability {
    HIGH,
    MEDIUM,
    LOW;

    /** Reads the AI's value; anything unexpected becomes MEDIUM. */
    public static Suitability parse(String value) {
        if (value == null) {
            return MEDIUM;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "HIGH" -> HIGH;
            case "LOW" -> LOW;
            default -> MEDIUM;
        };
    }
}
