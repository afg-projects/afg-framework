package io.github.afgprojects.framework.core.properties.metrics;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * 指标配置。
 */
@Data
public class AfgCoreMetricsProperties {

    /**
     * 是否启用指标功能。
     */
    private boolean enabled = true;

    /**
     * 是否启用注解驱动的指标。
     */
    private boolean annotationsEnabled = true;

    /**
     * 通用标签。
     */
    private @Nullable Map<String, String> tags;

    /**
     * Histogram 配置。
     */
    private AfgCoreMetricsHistogramProperties histogram = new AfgCoreMetricsHistogramProperties();
}
