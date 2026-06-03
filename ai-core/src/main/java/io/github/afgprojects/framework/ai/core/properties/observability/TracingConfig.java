package io.github.afgprojects.framework.ai.core.properties.observability;

import lombok.Data;

/**
 * 链路追踪配置。
 */
@Data
public class TracingConfig {

    /**
     * 是否启用链路追踪。
     */
    private boolean enabled = true;
}
