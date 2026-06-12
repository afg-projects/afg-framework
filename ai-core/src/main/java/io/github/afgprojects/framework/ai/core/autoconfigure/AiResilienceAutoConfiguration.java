package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.resilience.CircuitBreaker;
import io.github.afgprojects.framework.ai.core.api.resilience.FallbackStrategy;
import io.github.afgprojects.framework.ai.core.api.resilience.ResilienceExecutor;
import io.github.afgprojects.framework.ai.core.api.resilience.RetryPolicy;
import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import io.github.afgprojects.framework.ai.core.resilience.DefaultCircuitBreaker;
import io.github.afgprojects.framework.ai.core.resilience.DefaultResilienceExecutor;
import io.github.afgprojects.framework.ai.core.resilience.DefaultRetryPolicy;
import io.github.afgprojects.framework.ai.core.resilience.DefaultValueFallbackStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AFG AI 韧性自动配置。
 *
 * <p>配置前缀：{@code afg.ai.resilience}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(after = AiCoreAutoConfiguration.class,
        afterName = "io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration")
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.resilience", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiResilienceAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.resilience", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class ResilienceConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public RetryPolicy retryPolicy(AfgAiProperties properties) {
            AfgAiProperties.ResilienceConfig.RetryConfig config = properties.getResilience().getRetry();
            log.info("Creating DefaultRetryPolicy with maxRetries={}, initialIntervalMs={}, multiplier={}, maxIntervalMs={}, jitterFactor={}",
                    config.getMaxRetries(), config.getInitialIntervalMs(), config.getMultiplier(),
                    config.getMaxIntervalMs(), config.getJitterFactor());
            return DefaultRetryPolicy.builder()
                    .maxRetries(config.getMaxRetries())
                    .initialIntervalMs(config.getInitialIntervalMs())
                    .multiplier(config.getMultiplier())
                    .maxIntervalMs(config.getMaxIntervalMs())
                    .jitterFactor(config.getJitterFactor())
                    .build();
        }

        @Bean
        @ConditionalOnMissingBean
        public CircuitBreaker circuitBreaker(AfgAiProperties properties) {
            AfgAiProperties.ResilienceConfig.CircuitBreakerConfig config = properties.getResilience().getCircuitBreaker();
            log.info("Creating DefaultCircuitBreaker with name={}, windowSize={}, failureRateThreshold={}, halfOpenMaxCalls={}, openStateTimeoutMs={}",
                    config.getName(), config.getWindowSize(), config.getFailureRateThreshold(),
                    config.getHalfOpenMaxCalls(), config.getOpenStateTimeoutMs());
            return DefaultCircuitBreaker.builder()
                    .name(config.getName())
                    .windowSize(config.getWindowSize())
                    .failureRateThreshold(config.getFailureRateThreshold())
                    .halfOpenMaxCalls(config.getHalfOpenMaxCalls())
                    .openStateTimeoutMs(config.getOpenStateTimeoutMs())
                    .build();
        }

        @Bean
        @ConditionalOnMissingBean
        public ResilienceExecutor resilienceExecutor(RetryPolicy retryPolicy, CircuitBreaker circuitBreaker) {
            log.info("Creating DefaultResilienceExecutor");
            return new DefaultResilienceExecutor(retryPolicy, circuitBreaker);
        }

        @Bean
        @ConditionalOnMissingBean(FallbackStrategy.class)
        public DefaultValueFallbackStrategy<?> defaultValueFallbackStrategy() {
            log.info("Creating DefaultValueFallbackStrategy");
            return new DefaultValueFallbackStrategy<>();
        }

        // TODO: 阶段4添加AOP切面Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public AiResilientAspect aiResilientAspect() {
        //     return new AiResilientAspect();
        // }
    }
}
