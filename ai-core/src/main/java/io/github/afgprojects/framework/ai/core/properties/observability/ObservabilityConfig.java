package io.github.afgprojects.framework.ai.core.properties.observability;

import lombok.Data;

/**
 * 可观测性配置。
 */
@Data
public class ObservabilityConfig {

    /**
     * 是否启用可观测性。
     */
    private boolean enabled = true;

    /**
     * 审计日志配置。
     */
    private AuditConfig audit = new AuditConfig();

    /**
     * 指标配置。
     */
    private MetricsConfig metrics = new MetricsConfig();

    /**
     * 链路追踪配置。
     */
    private TracingConfig tracing = new TracingConfig();
}
