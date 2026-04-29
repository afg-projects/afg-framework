package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

class RateLimitResultTest extends BaseUnitTest {

    @Test
    void should_createAllowedResult() {
        long resetTime = System.currentTimeMillis() + 1000;
        RateLimitResult result = RateLimitResult.allowed(5, 10, resetTime);

        assertThat(result.allowed()).isTrue();
        assertThat(result.remaining()).isEqualTo(5);
        assertThat(result.limit()).isEqualTo(10);
        assertThat(result.resetTimeMs()).isEqualTo(resetTime);
        assertThat(result.retryAfterMs()).isEqualTo(0);
    }

    @Test
    void should_createRejectedResult() {
        long resetTime = System.currentTimeMillis() + 1000;
        RateLimitResult result = RateLimitResult.rejected(10, resetTime, 500);

        assertThat(result.allowed()).isFalse();
        assertThat(result.remaining()).isEqualTo(0);
        assertThat(result.limit()).isEqualTo(10);
        assertThat(result.resetTimeMs()).isEqualTo(resetTime);
        assertThat(result.retryAfterMs()).isEqualTo(500);
    }

    @Test
    void should_returnCorrectHeaders() {
        long resetTime = 1700000000000L; // Fixed timestamp
        RateLimitResult result = RateLimitResult.allowed(5, 10, resetTime);

        assertThat(result.getLimitHeader()).isEqualTo("10");
        assertThat(result.getRemainingHeader()).isEqualTo("5");
        assertThat(result.getResetHeader()).isEqualTo("1700000000");
    }

    @Test
    void should_roundUpRetryAfter() {
        // 100ms should round up to 1 second
        RateLimitResult result1 = RateLimitResult.rejected(10, System.currentTimeMillis(), 100);
        assertThat(result1.getRetryAfterHeader()).isEqualTo("1");

        // 1000ms should be 1 second
        RateLimitResult result2 = RateLimitResult.rejected(10, System.currentTimeMillis(), 1000);
        assertThat(result2.getRetryAfterHeader()).isEqualTo("1");

        // 1001ms should round up to 2 seconds
        RateLimitResult result3 = RateLimitResult.rejected(10, System.currentTimeMillis(), 1001);
        assertThat(result3.getRetryAfterHeader()).isEqualTo("2");
    }
}
