package io.github.afgprojects.framework.ai.core.properties.observability;

import lombok.Data;

/**
 * 指标配置。
 */
@Data
public class MetricsConfig {

    /**
     * 是否启用指标采集。
     */
    private boolean enabled = true;
}
