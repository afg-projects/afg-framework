package io.github.afgprojects.framework.ai.core.observability.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI Observability configuration properties.
 *
 * <p>Prefix: {@code afg.ai.observability}
 */
@Data
@ConfigurationProperties(prefix = "afg.ai.observability")
public class ObservabilityProperties {

    /**
     * Whether observability support is enabled.
     */
    private boolean enabled = true;

    /**
     * Metrics collection configuration.
     */
    private MetricsConfig metrics = new MetricsConfig();

    /**
     * Tracing configuration.
     */
    private TracingConfig tracing = new TracingConfig();

    /**
     * Audit logging configuration.
     */
    private AuditConfig audit = new AuditConfig();

    @Data
    public static class MetricsConfig {

        /**
         * Metrics prefix for all AI-related metrics.
         */
        private String prefix = "afg.ai";

        /**
         * Whether to include token usage metrics.
         */
        private boolean includeTokenUsage = true;
    }

    @Data
    public static class TracingConfig {

        /**
         * Whether tracing is enabled.
         */
        private boolean enabled = true;

        /**
         * Whether to include prompt content in traces.
         */
        private boolean includePrompts = false;
    }

    @Data
    public static class AuditConfig {

        /**
         * Whether audit logging is enabled.
         */
        private boolean enabled = true;

        /**
         * Whether to include response bodies in audit logs.
         */
        private boolean includeResponses = false;

        /**
         * Maximum body length to log (in characters).
         */
        private int maxBodyLength = 10000;

        /**
         * Maximum number of audit log entries to retain (in-memory store).
         */
        private int maxEntries = 10000;
    }
}
