package io.github.afgprojects.framework.ai.resilience.autoconfigure;

import io.github.afgprojects.framework.ai.core.resilience.CircuitBreaker;
import io.github.afgprojects.framework.ai.core.resilience.ResilienceExecutor;
import io.github.afgprojects.framework.ai.core.resilience.RetryPolicy;
import io.github.afgprojects.framework.ai.resilience.DefaultCircuitBreaker;
import io.github.afgprojects.framework.ai.resilience.DefaultResilienceExecutor;
import io.github.afgprojects.framework.ai.resilience.DefaultRetryPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for AI resilience support.
 *
 * <p>Configures retry policies, circuit breakers, and resilience executors.
 *
 * @see ResilienceProperties
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(DefaultRetryPolicy.class)
@EnableConfigurationProperties(ResilienceProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.resilience", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ResilienceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RetryPolicy retryPolicy(ResilienceProperties properties) {
        ResilienceProperties.RetryConfig config = properties.getRetry();
        log.info("Creating DefaultRetryPolicy with maxRetries={}, initialIntervalMs={}, multiplier={}, maxIntervalMs={}, jitterFactor={}",
                config.getMaxRetries(), config.getInitialIntervalMs(), config.getMultiplier(),
                config.getMaxIntervalMs(), config.getJitterFactor());
        return new DefaultRetryPolicy(
                config.getMaxRetries(),
                config.getInitialIntervalMs(),
                config.getMultiplier(),
                config.getMaxIntervalMs(),
                config.getJitterFactor());
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreaker circuitBreaker(ResilienceProperties properties) {
        ResilienceProperties.CircuitBreakerConfig config = properties.getCircuitBreaker();
        log.info("Creating DefaultCircuitBreaker with name={}, windowSize={}, failureRateThreshold={}, halfOpenMaxCalls={}, openStateTimeoutMs={}",
                config.getName(), config.getWindowSize(), config.getFailureRateThreshold(),
                config.getHalfOpenMaxCalls(), config.getOpenStateTimeoutMs());
        return new DefaultCircuitBreaker(
                config.getName(),
                config.getWindowSize(),
                config.getFailureRateThreshold(),
                config.getHalfOpenMaxCalls(),
                config.getOpenStateTimeoutMs());
    }

    @Bean
    @ConditionalOnMissingBean
    public ResilienceExecutor resilienceExecutor(RetryPolicy retryPolicy, CircuitBreaker circuitBreaker) {
        log.info("Creating DefaultResilienceExecutor");
        return new DefaultResilienceExecutor(retryPolicy, circuitBreaker);
    }
}
