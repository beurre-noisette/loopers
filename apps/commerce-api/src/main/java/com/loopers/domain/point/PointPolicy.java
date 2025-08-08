package com.loopers.domain.point;

import java.math.BigDecimal;

public class PointPolicy {

    public static class UserRegistration {
        private static final BigDecimal DEFAULT_WELCOME_POINT = BigDecimal.valueOf(1000);
        private static final String WELCOME_EVENT_REASON = "WELCOME_EVENT";
        private static final String DEFAULT_REASON = "USER_REGISTRATION";

        public static PointCreationPolicy getCreationPolicy () {
            if (isWelcomeEventActive()) {
                return new PointCreationPolicy(DEFAULT_WELCOME_POINT, WELCOME_EVENT_REASON);
            }

            return new PointCreationPolicy(BigDecimal.ZERO, DEFAULT_REASON);
        }

        private static boolean isWelcomeEventActive() {
            return false;
        }

    }

    public record PointCreationPolicy(
            BigDecimal initialAmount,
            String reason
    ) {}
}
