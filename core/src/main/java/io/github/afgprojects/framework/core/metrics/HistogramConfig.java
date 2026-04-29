package io.github.afgprojects.framework.core.metrics;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * Histogram 配置
 *
 * <p>用于配置 Timer 指标的百分位直方图
 */
@Data
public class HistogramConfig {

    /**
     * 是否启用 Histogram
     */
    private boolean enabled = true;

    /**
     * 是否启用百分位直方图
     */
    private boolean percentileHistogram = true;

    /**
     * 百分位数列表
     */
    private double[] percentiles = {0.5, 0.95, 0.99};

    /**
     * 最小预期值
     */
    private Duration minimumExpectedValue = Duration.ofMillis(1);

    /**
     * 最大预期值
     */
    private Duration maximumExpectedValue = Duration.ofSeconds(10);

    /**
     * 时间精度（SLA 边界）
     */
    private @Nullable Duration[] sla;
}