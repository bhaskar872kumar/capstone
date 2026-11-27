package com.healthdekho.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money helpers: every amount is a BigDecimal with scale 2, rounded HALF_UP. */
public final class Money {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private Money() {
    }

    public static BigDecimal of(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }
}
