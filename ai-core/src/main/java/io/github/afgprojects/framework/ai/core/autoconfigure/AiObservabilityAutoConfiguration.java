package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import io.github.afgprojects.framework.ai.core.api.observability.AuditLogger;
import io.github.afgprojects.framework.ai.core.api.observability.MetricsCollector;
import io.github.afgprojects.framework.ai.core.api.observability.Tracer;
import io.github.afgprojects.framework.ai.core.observability.DefaultAuditLogger;
import io.github.afgprojects.framework.ai.core.observability.DefaultMetricsCollector;
import io.github.afgprojects.framework.ai.core.observability.DefaultTracer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AFG AI 可观测性自动配置。
 *
 * <p>配置前缀：{@code afg.ai.observability}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration(after = AiCoreAutoConfiguration.class,
        afterName = "io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration")
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiObservabilityAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class ObservabilityConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "afg.ai.observability.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
        public DefaultAuditLogger defaultAuditLogger(AfgAiProperties properties) {
            return new DefaultAuditLogger(properties.getObservability().getAudit().getMaxEntries());
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "afg.ai.observability.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
        public DefaultMetricsCollector defaultMetricsCollector() {
            return new DefaultMetricsCollector();
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "afg.ai.observability.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
        public DefaultTracer defaultTracer() {
            return new DefaultTracer();
        }
    }
}
