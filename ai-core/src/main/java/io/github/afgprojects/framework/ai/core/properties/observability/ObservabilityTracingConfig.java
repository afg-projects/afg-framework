package io.github.afgprojects.framework.ai.core.properties.observability;

import lombok.Data;

/**
 * Observability 链路追踪配置。
 */
@Data
public class ObservabilityTracingConfig {

    /**
     * Whether tracing is enabled.
     */
    private boolean enabled = true;

    /**
     * Whether to include prompt content in traces.
     */
    private boolean includePrompts = false;
}
