package io.github.afgprojects.framework.ai.spring.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.observability.MetricsCollector;
import io.github.afgprojects.framework.ai.core.api.observability.Tracer;
import io.github.afgprojects.framework.ai.spring.config.SpringAiProperties;
import io.github.afgprojects.framework.ai.spring.observation.SpringAiObservationAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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
 * <p>当 classpath 上存在 Micrometer Observation 且 {@code afg.ai.spring.observation-enabled=true} 时自动激活。
 * 注册 {@link SpringAiObservationAdapter} 作为 {@link Tracer} 和 {@link MetricsCollector} 的实现。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SpringAiProperties.class)
@ConditionalOnClass(ObservationRegistry.class)
@ConditionalOnProperty(prefix = "afg.ai.spring", name = "observation-enabled", havingValue = "true", matchIfMissing = true)
public class SpringAiObservationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Tracer.class)
    @ConditionalOnBean(ObservationRegistry.class)
    public SpringAiObservationAdapter springAiObservationAdapter(
            ObservationRegistry observationRegistry,
            ObjectProvider<MeterRegistry> meterRegistryProvider,
            SpringAiProperties properties) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        log.info("Creating SpringAiObservationAdapter with MeterRegistry available: {}", meterRegistry != null);
        return new SpringAiObservationAdapter(observationRegistry, meterRegistry);
    }
}