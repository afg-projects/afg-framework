package io.github.afgprojects.framework.ai.observability.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.observability.AuditLogger;
import io.github.afgprojects.framework.ai.core.api.observability.MetricsCollector;
import io.github.afgprojects.framework.ai.core.api.observability.Tracer;
import io.github.afgprojects.framework.ai.core.observability.AiAuditedAspect;
import io.github.afgprojects.framework.ai.observability.DefaultAuditLogger;
import io.github.afgprojects.framework.ai.observability.DefaultMetricsCollector;
import io.github.afgprojects.framework.ai.observability.DefaultTracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for AI observability support.
 *
 * <p>Configures metrics collection, tracing, and audit logging
 * using the default in-memory implementations.
 *
 * <p>The outer configuration is activated when {@link DefaultMetricsCollector} is on the classpath.
 * When it is not available (e.g. only core interfaces are present), the
 * {@link FallbackObservationConfiguration} nested class provides equivalent beans
 * without the classpath requirement.
 *
 * @see ObservabilityProperties
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProperties.class)
@ConditionalOnClass({DefaultMetricsCollector.class, AiAuditedAspect.class})
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

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "afg.ai.observability.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AiAuditedAspect aiAuditedAspect(AuditLogger auditLogger) {
        log.info("Creating AiAuditedAspect");
        return new AiAuditedAspect(auditLogger);
    }

    /**
     * Fallback configuration that provides observation beans when
     * {@link DefaultMetricsCollector} is not on the classpath.
     *
     * <p>This nested class mirrors the bean definitions from the outer configuration
     * but without the {@link ConditionalOnClass} requirement, allowing the
     * observability support to work with only the core interfaces available.
     *
     * <p>Beans are guarded by {@link ConditionalOnMissingBean} so they will not
     * override those created by the outer configuration when both are active.
     */
    @Configuration
    @EnableConfigurationProperties(ObservabilityProperties.class)
    @ConditionalOnProperty(prefix = "afg.ai.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class FallbackObservationConfiguration {

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
            int maxEntries = properties.getAudit().getMaxEntries();
            log.info("Creating DefaultAuditLogger with maxEntries={}", maxEntries);
            return new DefaultAuditLogger(maxEntries);
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "afg.ai.observability.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
        public AiAuditedAspect aiAuditedAspect(AuditLogger auditLogger) {
            log.info("Creating AiAuditedAspect");
            return new AiAuditedAspect(auditLogger);
        }
    }
}
