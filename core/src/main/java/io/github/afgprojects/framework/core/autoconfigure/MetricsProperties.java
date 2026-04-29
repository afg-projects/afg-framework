package io.github.afgprojects.framework.core.autoconfigure;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * 监控指标配置属性
 *
 * <p>配置 Micrometer 指标相关属性
 */
@Data
public class MetricsProperties {

    /**
     * 是否启用指标功能
     */
    private boolean enabled = true;

    /**
     * 是否启用注解驱动的指标
     */
    private boolean annotationsEnabled = true;

    /**
     * 通用标签
     * <p>
     * 会添加到所有指标
     */
    private @Nullable Map<String, String> tags;

    /**
     * 额外的通用标签
     */
    private @Nullable Map<String, String> commonTags;

    /**
     * Histogram 配置
     */
    private HistogramConfigProperties histogram = new HistogramConfigProperties();

    /**
     * 自定义指标配置
     */
    private CustomMetricsConfig custom = new CustomMetricsConfig();

    /**
     * Histogram 配置属性
     */
    @Data
    public static class HistogramConfigProperties {

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
         * <p>
         * 如 0.5, 0.95, 0.99
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
    }

    /**
     * 自定义指标配置
     */
    @Data
    public static class CustomMetricsConfig {

        /**
         * 是否启用自定义指标
         */
        private boolean enabled = true;

        /**
         * 计数器指标配置
         */
        private List<CounterConfig> counters = List.of();

        /**
         * 仪表盘指标配置
         */
        private List<GaugeConfig> gauges = List.of();
    }

    /**
     * 计数器配置
     */
    @Data
    public static class CounterConfig {

        /**
         * 指标名称
         */
        private String name;

        /**
         * 指标描述
         */
        private @Nullable String description;

        /**
         * 标签
         */
        private Map<String, String> tags = new HashMap<>();
    }

    /**
     * 仪表盘配置
     */
    @Data
    public static class GaugeConfig {

        /**
         * 指标名称
         */
        private String name;

        /**
         * 指标描述
         */
        private @Nullable String description;

        /**
         * 标签
         */
        private Map<String, String> tags = new HashMap<>();
    }
}