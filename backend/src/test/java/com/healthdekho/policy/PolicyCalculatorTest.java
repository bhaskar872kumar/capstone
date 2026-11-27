package com.healthdekho.policy;

import com.healthdekho.product.PremiumFrequency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** REG_05: FR-3 calculations, checked against the spec's sample data. */
class PolicyCalculatorTest {

    @ParameterizedTest
    @CsvSource({
            "240, MONTHLY,     240",
            "120, QUARTERLY,   40",
            "24,  HALF_YEARLY, 4",
            "12,  YEARLY,      1"
    })
    void REG_05_installmentCountForEveryFrequency(int termMonths, PremiumFrequency frequency, int expected) {
        assertThat(PolicyCalculator.installmentCount(termMonths, frequency)).isEqualTo(expected);
    }

    @Test
    void REG_05_sampleTermPolicyMatchesSpec() {
        LocalDate start = LocalDate.of(2026, 11, 20);
        assertThat(PolicyCalculator.maturityDate(start, 240)).isEqualTo(LocalDate.of(2046, 11, 19));
        assertThat(PolicyCalculator.totalPremium(new BigDecimal("1250.00"), 240)).isEqualByComparingTo("300000.00");

        List<ScheduledInstallment> schedule =
                PolicyCalculator.schedule(start, 240, PremiumFrequency.MONTHLY, new BigDecimal("1250.00"));
        assertThat(schedule).hasSize(240);
        assertThat(schedule.get(0).dueDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(schedule.get(0).gracePeriodEndDate()).isEqualTo(LocalDate.of(2026, 12, 20));
        assertThat(schedule.get(1).dueDate()).isEqualTo(LocalDate.of(2026, 12, 20));
        assertThat(schedule.get(2).gracePeriodEndDate()).isEqualTo(LocalDate.of(2027, 2, 20));
    }

    @Test
    void REG_05_sampleHealthPolicyMatchesSpec() {
        LocalDate start = LocalDate.of(2026, 11, 20);
        assertThat(PolicyCalculator.maturityDate(start, 12)).isEqualTo(LocalDate.of(2027, 11, 19));
        assertThat(PolicyCalculator.totalPremium(new BigDecimal("18000.00"), 1)).isEqualByComparingTo("18000.00");
    }

    @Test
    void REG_05_quarterlyScheduleStepsThreeMonths() {
        List<ScheduledInstallment> schedule = PolicyCalculator.schedule(
                LocalDate.of(2026, 11, 25), 120, PremiumFrequency.QUARTERLY, new BigDecimal("7500"));
        assertThat(schedule.get(1).dueDate()).isEqualTo(LocalDate.of(2027, 2, 25));
        assertThat(schedule.getLast().installmentNumber()).isEqualTo(40);
        assertThat(schedule.getLast().scheduledAmount()).isEqualByComparingTo("7500.00");
    }

    @Test
    void REG_05_termThatDoesNotSplitIntoInstallmentsIsRejected() {
        assertThatThrownBy(() -> PolicyCalculator.installmentCount(13, PremiumFrequency.QUARTERLY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PolicyCalculator.installmentCount(0, PremiumFrequency.MONTHLY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void REG_05_ageAndEligibilityUseWholeYears() {
        LocalDate dob = LocalDate.of(1990, 4, 15);
        assertThat(PolicyCalculator.ageOn(dob, LocalDate.of(2026, 4, 14))).isEqualTo(35);
        assertThat(PolicyCalculator.ageOn(dob, LocalDate.of(2026, 4, 15))).isEqualTo(36);
        assertThat(PolicyCalculator.isEligible(18, 18, 60)).isTrue();
        assertThat(PolicyCalculator.isEligible(60, 18, 60)).isTrue();
        assertThat(PolicyCalculator.isEligible(17, 18, 60)).isFalse();
        assertThat(PolicyCalculator.isEligible(61, 18, 60)).isFalse();
    }
}
