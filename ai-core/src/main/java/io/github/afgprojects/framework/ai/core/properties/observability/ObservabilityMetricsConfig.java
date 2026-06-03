package io.github.afgprojects.framework.ai.core.properties.observability;

import lombok.Data;

/**
 * Observability 指标采集配置。
 */
@Data
public class ObservabilityMetricsConfig {

    /**
     * Metrics prefix for all AI-related metrics.
     */
    private String prefix = "afg.ai";

    /**
     * Whether to include token usage metrics.
     */
    private boolean includeTokenUsage = true;
}
