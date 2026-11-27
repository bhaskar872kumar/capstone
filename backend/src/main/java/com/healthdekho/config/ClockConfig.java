package com.healthdekho.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Business "today" for all date rules (age, start date, grace period).
 * Services never call LocalDate.now() directly, so tests can fix the date.
 */
@Configuration
public class ClockConfig {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    @Bean
    public Clock clock(@Value("${app.clock.fixed-date:}") String fixedDate) {
        if (fixedDate != null && !fixedDate.isBlank()) {
            LocalDate date = LocalDate.parse(fixedDate.trim());
            return Clock.fixed(date.atTime(10, 0).atZone(BUSINESS_ZONE).toInstant(), BUSINESS_ZONE);
        }
        return Clock.system(BUSINESS_ZONE);
    }
}
