package io.github.afgprojects.framework.ai.autoconfigure;

import io.github.afgprojects.framework.ai.core.observability.AuditLogger;
import io.github.afgprojects.framework.ai.core.observability.MetricsCollector;
import io.github.afgprojects.framework.ai.core.observability.Tracer;
import io.github.afgprojects.framework.ai.observability.DefaultAuditLogger;
import io.github.afgprojects.framework.ai.observability.DefaultMetricsCollector;
import io.github.afgprojects.framework.ai.observability.DefaultTracer;
import io.github.afgprojects.framework.ai.observability.SpringAiObservationAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring AI Observation 自动配置
 *
 * <p>集成 Spring AI 内置的 Micrometer Observation 支持，提供：
 * <ul>
 *   <li>Span 追踪：通过 ObservationRegistry 创建和管理 Span</li>
 *   <li>指标收集：通过 MeterRegistry 记录 AI 操作指标</li>
 *   <li>上下文传播：支持跨服务的追踪上下文传递</li>
 *   <li>Spring AI 集成：与 Spring AI 的 Observation 自动配置协同工作</li>
 * </ul>
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     observability:
 *       enabled: true
 *       spring-ai-observation:
 *         enabled: true  # 启用 Spring AI Observation 集成
 *       metrics:
 *         enabled: true
 *       tracing:
 *         enabled: true
 *       audit:
 *         enabled: true
 *         max-entries: 10000
 * }</pre>
 *
 * <p>Spring AI 自动收集以下指标：
 * <ul>
 *   <li>afg.ai.{operation}.requests - 请求计数</li>
 *   <li>afg.ai.{operation}.duration - 响应时间</li>
 *   <li>afg.ai.tokens.input - 输入 Token 数</li>
 *   <li>afg.ai.tokens.output - 输出 Token 数</li>
 *   <li>afg.ai.cost - 成本统计</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration(after = AiAutoConfiguration.class)
@EnableConfigurationProperties(AiConfigurationProperties.class)
@ConditionalOnClass(name = {
    "io.github.afgprojects.framework.ai.core.observability.MetricsCollector",
    "io.github.afgprojects.framework.ai.core.observability.Tracer",
    "io.github.afgprojects.framework.ai.core.observability.AuditLogger",
    "io.micrometer.observation.ObservationRegistry"
})
@ConditionalOnProperty(prefix = "afg.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "afg.ai.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ObservationAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ObservationAutoConfiguration.class);

    /**
     * 配置 Spring AI Observation 适配器
     *
     * <p>当 ObservationRegistry 可用时，使用 Spring AI 内置的 Observation 支持。
     * 同时实现 Tracer 和 MetricsCollector 接口，提供统一的可观测性能力。
     */
    @Bean
    @ConditionalOnBean(ObservationRegistry.class)
    @ConditionalOnProperty(prefix = "afg.ai.observability.spring-ai-observation", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean({Tracer.class, MetricsCollector.class})
    public SpringAiObservationAdapter springAiObservationAdapter(
            ObservationRegistry observationRegistry,
            @Autowired(required = false) MeterRegistry meterRegistry) {

        log.info("Creating Spring AI Observation adapter with ObservationRegistry");

        return new SpringAiObservationAdapter(observationRegistry, meterRegistry);
    }

    /**
     * 配置 Tracer（当 Spring AI Observation 未启用时使用默认实现）
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.ai.observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(Tracer.class)
    public Tracer tracer() {
        log.info("Creating default tracer");

        return new DefaultTracer();
    }

    /**
     * 配置指标收集器（当 Spring AI Observation 未启用时使用默认实现）
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.ai.observability.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(MetricsCollector.class)
    public MetricsCollector metricsCollector() {
        log.info("Creating default metrics collector");

        return new DefaultMetricsCollector();
    }

    /**
     * 配置审计日志记录器
     *
     * <p>审计日志是框架特有功能，始终保留。
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.ai.observability.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(AuditLogger.class)
    public AuditLogger auditLogger(AiConfigurationProperties properties) {
        int maxEntries = properties.getObservability().getAudit().getMaxEntries();

        log.info("Creating default audit logger with maxEntries={}", maxEntries);

        return new DefaultAuditLogger(maxEntries);
    }
}