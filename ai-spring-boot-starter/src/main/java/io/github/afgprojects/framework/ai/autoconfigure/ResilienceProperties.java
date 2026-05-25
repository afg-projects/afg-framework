package io.github.afgprojects.framework.ai.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI Resilience configuration properties.
 *
 * <p>Prefix: {@code afg.ai.resilience}
 */
@Data
@ConfigurationProperties(prefix = "afg.ai.resilience")
public class ResilienceProperties {

    /**
     * Whether resilience support is enabled.
     */
    private boolean enabled = true;

    /**
     * Retry policy configuration.
     */
    private RetryConfig retry = new RetryConfig();

    /**
     * Circuit breaker configuration.
     */
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

    @Data
    public static class RetryConfig {

        /**
         * Maximum number of retry attempts.
         */
        private int maxRetries = 3;

        /**
         * Initial interval in milliseconds before the first retry.
         */
        private long initialIntervalMs = 1000;

        /**
         * Multiplier for exponential backoff between retries.
         */
        private double multiplier = 2.0;

        /**
         * Maximum retry interval in milliseconds.
         */
        private long maxIntervalMs = 30000;

        /**
         * Jitter factor (0-1) to avoid thundering herd.
         */
        private double jitterFactor = 0.5;
    }

    @Data
    public static class CircuitBreakerConfig {

        /**
         * Circuit breaker name.
         */
        private String name = "default";

        /**
         * Sliding window size for failure rate calculation.
         */
        private int windowSize = 100;

        /**
         * Failure rate threshold (0-1) to open the circuit.
         */
        private double failureRateThreshold = 0.5;

        /**
         * Maximum number of calls permitted in half-open state.
         */
        private int halfOpenMaxCalls = 10;

        /**
         * Wait duration in milliseconds before transitioning from open to half-open.
         */
        private long openStateTimeoutMs = 30000;
    }
}