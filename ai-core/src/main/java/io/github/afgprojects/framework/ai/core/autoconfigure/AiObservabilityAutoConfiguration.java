package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
// import io.github.afgprojects.framework.ai.core.api.observability.AuditLogger;
// import io.github.afgprojects.framework.ai.core.api.observability.MetricsCollector;
// import io.github.afgprojects.framework.ai.core.api.observability.Tracer;
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
@AutoConfiguration
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiObservabilityAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class ObservabilityConfiguration {

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultAuditLogger defaultAuditLogger(AfgAiProperties properties) {
        //     return new DefaultAuditLogger(properties.getObservability().getAudit());
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultMetricsCollector defaultMetricsCollector(AfgAiProperties properties) {
        //     return new DefaultMetricsCollector(properties.getObservability().getMetrics());
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultTracer defaultTracer() {
        //     return new DefaultTracer();
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public AiAuditedAspect aiAuditedAspect(AuditLogger auditLogger) {
        //     return new AiAuditedAspect(auditLogger);
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public AiHealthEndpoint aiHealthEndpoint(MetricsCollector metricsCollector) {
        //     return new AiHealthEndpoint(metricsCollector);
        // }
    }
}
