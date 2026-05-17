package io.github.afgprojects.framework.ai.autoconfigure;

import io.github.afgprojects.framework.ai.core.observability.AuditLogger;
import io.github.afgprojects.framework.ai.core.observability.MetricsCollector;
import io.github.afgprojects.framework.ai.core.observability.Tracer;
import io.github.afgprojects.framework.ai.observability.DefaultAuditLogger;
import io.github.afgprojects.framework.ai.observability.DefaultMetricsCollector;
import io.github.afgprojects.framework.ai.observability.DefaultTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 可观测性模块自动配置
 *
 * <p>配置指标收集器、追踪器、审计日志记录器。
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     observability:
 *       enabled: true
 *       metrics:
 *         enabled: true
 *       tracing:
 *         enabled: true
 *       audit:
 *         enabled: true
 *         max-entries: 10000
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AiConfigurationProperties.class)
@ConditionalOnClass({MetricsCollector.class, Tracer.class, AuditLogger.class})
@ConditionalOnProperty(prefix = "afg.ai.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityAutoConfiguration.class);

    /**
     * 配置指标收集器
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.ai.observability.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(MetricsCollector.class)
    public MetricsCollector metricsCollector() {
        log.info("Creating default metrics collector");

        return new DefaultMetricsCollector();
    }

    /**
     * 配置追踪器
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.ai.observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(Tracer.class)
    public Tracer tracer() {
        log.info("Creating default tracer");

        return new DefaultTracer();
    }

    /**
     * 配置审计日志记录器
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