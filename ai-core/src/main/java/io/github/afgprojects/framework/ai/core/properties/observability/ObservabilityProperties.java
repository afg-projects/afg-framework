package io.github.afgprojects.framework.ai.core.properties.observability;

import io.github.afgprojects.framework.ai.core.properties.observability.ObservabilityAuditConfig;
import io.github.afgprojects.framework.ai.core.properties.observability.ObservabilityMetricsConfig;
import io.github.afgprojects.framework.ai.core.properties.observability.ObservabilityTracingConfig;

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
    private ObservabilityMetricsConfig metrics = new ObservabilityMetricsConfig();

    /**
     * Tracing configuration.
     */
    private ObservabilityTracingConfig tracing = new ObservabilityTracingConfig();

    /**
     * Audit logging configuration.
     */
    private ObservabilityAuditConfig audit = new ObservabilityAuditConfig();
}
