package com.healthdekho.policy;

import com.healthdekho.common.Money;
import com.healthdekho.product.PremiumFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.IntStream;

/**
 * FR-3 calculations. Deterministic code only: AI never decides any of these numbers.
 * Checked against the spec's sample data: HD-TERM-001 from 2026-11-20 gives 240 installments,
 * maturity 2046-11-19 and a total of 300000.00.
 */
public final class PolicyCalculator {

    /** One-month grace period after each due date (FR-4 C). */
    public static final int GRACE_PERIOD_MONTHS = 1;

    private PolicyCalculator() {
    }

    public static int installmentCount(int termMonths, PremiumFrequency frequency) {
        if (termMonths <= 0) {
            throw new IllegalArgumentException("Policy term must be positive.");
        }
        int step = frequency.monthsPerInstallment();
        if (termMonths % step != 0) {
            throw new IllegalArgumentException(
                    "A %d-month term can't be split into %s installments.".formatted(termMonths, frequency));
        }
        return termMonths / step;
    }

    /** Last day of cover: start + term - 1 day (2026-11-20 + 12 months gives 2027-11-19). */
    public static LocalDate maturityDate(LocalDate startDate, int termMonths) {
        return startDate.plusMonths(termMonths).minusDays(1);
    }

    public static BigDecimal totalPremium(BigDecimal premiumAmount, int installmentCount) {
        return Money.of(premiumAmount.multiply(BigDecimal.valueOf(installmentCount)));
    }

    public static List<ScheduledInstallment> schedule(LocalDate startDate, int termMonths,
                                                      PremiumFrequency frequency, BigDecimal premiumAmount) {
        int count = installmentCount(termMonths, frequency);
        int step = frequency.monthsPerInstallment();
        BigDecimal amount = Money.of(premiumAmount);
        return IntStream.rangeClosed(1, count)
                .mapToObj(number -> {
                    LocalDate dueDate = startDate.plusMonths((long) (number - 1) * step);
                    return new ScheduledInstallment(number, dueDate, dueDate.plusMonths(GRACE_PERIOD_MONTHS), amount);
                })
                .toList();
    }

    public static int ageOn(LocalDate dateOfBirth, LocalDate onDate) {
        return Period.between(dateOfBirth, onDate).getYears();
    }

    public static boolean isEligible(int age, int minEntryAge, int maxEntryAge) {
        return age >= minEntryAge && age <= maxEntryAge;
    }
}
