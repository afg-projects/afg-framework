package io.github.afgprojects.framework.core.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.afgprojects.framework.core.metrics.CustomMetrics;
import io.github.afgprojects.framework.core.metrics.DefaultMetricsTagProvider;
import io.github.afgprojects.framework.core.metrics.MetricsTagProvider;
import io.github.afgprojects.framework.core.web.metrics.MetricsAspect;
import io.github.afgprojects.framework.core.web.metrics.MetricsProperties;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;

/**
 * 指标监控自动配置
 * <p>
 * 自动配置条件:
 * <ul>
 *   <li>存在 MeterRegistry bean (Spring Boot Actuator 提供)</li>
 *   <li>afg.metrics.enabled=true (默认为 true)</li>
 * </ul>
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>自定义指标注册</li>
 *   <li>Histogram 百分位配置</li>
 *   <li>指标标签增强</li>
 *   <li>指标过滤与转换</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   metrics:
 *     enabled: true
 *     tags:
 *       application: ${spring.application.name}
 *       env: ${spring.profiles.active:default}
 *     histogram:
 *       enabled: true
 *       percentiles: 0.5,0.95,0.99
 * </pre>
 */
@AutoConfiguration
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(prefix = "afg.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({MetricsProperties.class, io.github.afgprojects.framework.core.autoconfigure.MetricsProperties.class})
public class MetricsAutoConfiguration {

    /**
     * 配置指标切面
     *
     * @param meterRegistry Micrometer 注册表
     * @param properties    指标配置属性
     * @return 指标切面实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "afg.metrics.annotations",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public MetricsAspect metricsAspect(MeterRegistry meterRegistry, MetricsProperties properties) {
        return new MetricsAspect(meterRegistry, properties);
    }

    /**
     * 通用标签配置
     *
     * <p>为所有指标添加通用标签
     */
    @Bean
    public MeterFilter commonTagsMeterFilter(io.github.afgprojects.framework.core.autoconfigure.MetricsProperties properties) {
        Map<String, String> tags = new HashMap<>();
        if (properties.getTags() != null) {
            tags.putAll(properties.getTags());
        }
        if (properties.getCommonTags() != null) {
            tags.putAll(properties.getCommonTags());
        }

        // 将 Map 转换为 Iterable<Tag>
        Iterable<Tag> tagIterable = Tags.of(tags.entrySet().stream()
                .map(entry -> Tag.of(entry.getKey(), entry.getValue()))
                .toList());

        return MeterFilter.commonTags(tagIterable);
    }

    /**
     * Histogram 配置
     *
     * <p>为 Timer 指标配置百分位直方图
     */
    @Bean
    public MeterFilter histogramMeterFilter(io.github.afgprojects.framework.core.autoconfigure.MetricsProperties properties) {
        if (!properties.getHistogram().isEnabled()) {
            return MeterFilter.accept();
        }

        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                // 为所有 Timer 指标应用 histogram 配置
                if (id.getType() == Meter.Type.TIMER) {
                    // 将 Duration 转换为纳秒，用于 Timer 指标
                    double minNanos = properties.getHistogram().getMinimumExpectedValue().toNanos();
                    double maxNanos = properties.getHistogram().getMaximumExpectedValue().toNanos();

                    return DistributionStatisticConfig.builder()
                            .percentiles(properties.getHistogram().getPercentiles())
                            .percentilesHistogram(properties.getHistogram().isPercentileHistogram())
                            .minimumExpectedValue(minNanos)
                            .maximumExpectedValue(maxNanos)
                            .build()
                            .merge(config);
                }
                return config;
            }
        };
    }

    /**
     * 指标标签提供者
     *
     * <p>动态提供标签
     */
    @Bean
    @ConditionalOnMissingBean
    public MetricsTagProvider metricsTagProvider() {
        return new DefaultMetricsTagProvider();
    }

    /**
     * 自定义指标注册
     *
     * <p>注册应用级自定义指标
     */
    @Bean
    @ConditionalOnMissingBean
    public CustomMetrics customMetrics(MeterRegistry meterRegistry, io.github.afgprojects.framework.core.autoconfigure.MetricsProperties properties) {
        return new CustomMetrics(meterRegistry, properties);
    }
}
