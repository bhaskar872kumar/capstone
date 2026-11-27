package com.healthdekho.policy;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One line of a calculated premium schedule, before it is saved. */
public record ScheduledInstallment(
        int installmentNumber,
        LocalDate dueDate,
        LocalDate gracePeriodEndDate,
        BigDecimal scheduledAmount) {
}
