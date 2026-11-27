package com.healthdekho.product;

/** Premium payment frequency (FR-1 B: Monthly, Quarterly, Half-Yearly or Yearly). */
public enum PremiumFrequency {
    MONTHLY,
    QUARTERLY,
    HALF_YEARLY,
    YEARLY;

    public int monthsPerInstallment() {
        return switch (this) {
            case MONTHLY -> 1;
            case QUARTERLY -> 3;
            case HALF_YEARLY -> 6;
            case YEARLY -> 12;
        };
    }

    public int installmentsPerYear() {
        return 12 / monthsPerInstallment();
    }
}
