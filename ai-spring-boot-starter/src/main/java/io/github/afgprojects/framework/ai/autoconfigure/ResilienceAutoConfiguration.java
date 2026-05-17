package io.github.afgprojects.framework.ai.autoconfigure;

import io.github.afgprojects.framework.ai.core.resilience.CircuitBreaker;
import io.github.afgprojects.framework.ai.core.resilience.RetryPolicy;
import io.github.afgprojects.framework.ai.core.resilience.ResilienceExecutor;
import io.github.afgprojects.framework.ai.resilience.DefaultCircuitBreaker;
import io.github.afgprojects.framework.ai.resilience.DefaultRetryPolicy;
import io.github.afgprojects.framework.ai.resilience.DefaultResilienceExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 韧性模块自动配置
 *
 * <p>配置重试、熔断器、韧性执行器。
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     resilience:
 *       enabled: true
 *       retry:
 *         max-retries: 3
 *         initial-interval-ms: 1000
 *       circuit-breaker:
 *         window-size: 100
 *         failure-rate-threshold: 0.5
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AiConfigurationProperties.class)
@ConditionalOnClass({RetryPolicy.class, CircuitBreaker.class, ResilienceExecutor.class})
@ConditionalOnProperty(prefix = "afg.ai.resilience", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ResilienceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ResilienceAutoConfiguration.class);

    /**
     * 配置重试策略
     */
    @Bean
    @ConditionalOnMissingBean(RetryPolicy.class)
    public RetryPolicy retryPolicy(AiConfigurationProperties properties) {
        AiConfigurationProperties.RetryConfig config = properties.getResilience().getRetry();

        log.info("Creating default retry policy: maxRetries={}, initialIntervalMs={}",
                config.getMaxRetries(), config.getInitialIntervalMs());

        return DefaultRetryPolicy.builder()
                .maxRetries(config.getMaxRetries())
                .initialIntervalMs(config.getInitialIntervalMs())
                .multiplier(config.getMultiplier())
                .maxIntervalMs(config.getMaxIntervalMs())
                .jitterFactor(config.getJitterFactor())
                .build();
    }

    /**
     * 配置熔断器
     */
    @Bean
    @ConditionalOnMissingBean(CircuitBreaker.class)
    public CircuitBreaker circuitBreaker(AiConfigurationProperties properties) {
        AiConfigurationProperties.CircuitBreakerConfig config = properties.getResilience().getCircuitBreaker();

        log.info("Creating default circuit breaker: windowSize={}, failureRateThreshold={}",
                config.getWindowSize(), config.getFailureRateThreshold());

        return DefaultCircuitBreaker.builder()
                .name("default")
                .windowSize(config.getWindowSize())
                .failureRateThreshold(config.getFailureRateThreshold())
                .halfOpenMaxCalls(config.getHalfOpenMaxCalls())
                .openStateTimeoutMs(config.getOpenStateTimeoutMs())
                .build();
    }

    /**
     * 配置韧性执行器
     */
    @Bean
    @ConditionalOnMissingBean(ResilienceExecutor.class)
    public ResilienceExecutor resilienceExecutor(RetryPolicy retryPolicy, CircuitBreaker circuitBreaker) {
        log.info("Creating default resilience executor");

        return new DefaultResilienceExecutor(retryPolicy, circuitBreaker);
    }
}