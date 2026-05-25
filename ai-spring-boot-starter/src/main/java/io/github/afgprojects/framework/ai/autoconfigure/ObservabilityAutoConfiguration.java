package io.github.afgprojects.framework.ai.autoconfigure;

import io.github.afgprojects.framework.ai.core.observability.AuditLogger;
import io.github.afgprojects.framework.ai.core.observability.MetricsCollector;
import io.github.afgprojects.framework.ai.core.observability.Tracer;
import io.github.afgprojects.framework.ai.observability.DefaultAuditLogger;
import io.github.afgprojects.framework.ai.observability.DefaultMetricsCollector;
import io.github.afgprojects.framework.ai.observability.DefaultTracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for AI observability support.
 *
 * <p>Configures metrics collection, tracing, and audit logging
 * using the default in-memory implementations.
 *
 * @see ObservabilityProperties
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "afg.ai.observability.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MetricsCollector metricsCollector() {
        log.info("Creating DefaultMetricsCollector");
        return new DefaultMetricsCollector();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "afg.ai.observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Tracer tracer() {
        log.info("Creating DefaultTracer");
        return new DefaultTracer();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "afg.ai.observability.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AuditLogger auditLogger(ObservabilityProperties properties) {
        log.info("Creating DefaultAuditLogger with maxEntries={}", properties.getAudit().getMaxEntries());
        return new DefaultAuditLogger(properties.getAudit().getMaxEntries());
    }
}
