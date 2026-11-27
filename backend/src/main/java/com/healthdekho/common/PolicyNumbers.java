package com.healthdekho.common;

/** Policy identifier format: HDP-{year}-{number}, for example HDP-2026-0001 (spec sample data). */
public final class PolicyNumbers {

    private PolicyNumbers() {
    }

    public static String format(int year, long sequenceValue) {
        return "HDP-%d-%04d".formatted(year, sequenceValue);
    }
}
