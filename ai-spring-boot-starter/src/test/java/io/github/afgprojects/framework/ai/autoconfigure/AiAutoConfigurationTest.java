package io.github.afgprojects.framework.ai.autoconfigure;

import io.github.afgprojects.framework.ai.core.performance.Cache;
import io.github.afgprojects.framework.ai.core.performance.RateLimiter;
import io.github.afgprojects.framework.ai.core.persistence.MessageHistoryStore;
import io.github.afgprojects.framework.ai.core.persistence.SessionStore;
import io.github.afgprojects.framework.ai.core.resilience.CircuitBreaker;
import io.github.afgprojects.framework.ai.core.resilience.ResilienceExecutor;
import io.github.afgprojects.framework.ai.core.resilience.RetryPolicy;
import io.github.afgprojects.framework.ai.core.observability.MetricsCollector;
import io.github.afgprojects.framework.ai.core.observability.Tracer;
import io.github.afgprojects.framework.ai.core.observability.AuditLogger;
import io.github.afgprojects.framework.ai.core.security.ApiKeyManager;
import io.github.afgprojects.framework.ai.core.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.security.PiiDetector;
import io.github.afgprojects.framework.ai.resilience.DefaultRetryPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI 自动配置测试
 *
 * @author afg-projects
 * @since 1.0.0
 */
class AiAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AiAutoConfiguration.class));

    @Test
    void shouldNotCreateBeansWhenDisabled() {
        contextRunner
                .withPropertyValues("afg.ai.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ResilienceExecutor.class);
                    assertThat(context).doesNotHaveBean(MetricsCollector.class);
                    assertThat(context).doesNotHaveBean(ApiKeyManager.class);
                    assertThat(context).doesNotHaveBean(SessionStore.class);
                    assertThat(context).doesNotHaveBean(Cache.class);
                });
    }

    @Test
    void shouldCreateResilienceBeansWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "afg.ai.enabled=true",
                        "afg.ai.llm.provider=none",
                        "afg.ai.resilience.enabled=true",
                        "afg.ai.resilience.retry.max-retries=3",
                        "afg.ai.resilience.circuit-breaker.failure-rate-threshold=0.5"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(RetryPolicy.class);
                    assertThat(context).hasSingleBean(CircuitBreaker.class);
                    assertThat(context).hasSingleBean(ResilienceExecutor.class);

                    RetryPolicy retryPolicy = context.getBean(RetryPolicy.class);
                    assertThat(retryPolicy.getMaxRetries()).isEqualTo(3);

                    CircuitBreaker circuitBreaker = context.getBean(CircuitBreaker.class);
                    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
                });
    }

    @Test
    void shouldCreateObservabilityBeansWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "afg.ai.enabled=true",
                        "afg.ai.llm.provider=none",
                        "afg.ai.observability.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(MetricsCollector.class);
                    assertThat(context).hasSingleBean(Tracer.class);
                    assertThat(context).hasSingleBean(AuditLogger.class);
                });
    }

    @Test
    void shouldCreateSecurityBeansWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "afg.ai.enabled=true",
                        "afg.ai.llm.provider=none",
                        "afg.ai.security.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ApiKeyManager.class);
                    assertThat(context).hasSingleBean(ContentSafetyChecker.class);
                    assertThat(context).hasSingleBean(PiiDetector.class);
                });
    }

    @Test
    void shouldCreatePersistenceBeansWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "afg.ai.enabled=true",
                        "afg.ai.llm.provider=none",
                        "afg.ai.persistence.enabled=true",
                        "afg.ai.persistence.session.max-sessions-per-user=50",
                        "afg.ai.persistence.message-history.max-messages-per-session=500"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(SessionStore.class);
                    assertThat(context).hasSingleBean(MessageHistoryStore.class);
                });
    }

    @Test
    void shouldCreatePerformanceBeansWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "afg.ai.enabled=true",
                        "afg.ai.llm.provider=none",
                        "afg.ai.performance.enabled=true",
                        "afg.ai.performance.cache.max-size=500",
                        "afg.ai.performance.cache.ttl-seconds=300",
                        "afg.ai.performance.rate-limit.default-permits=50"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(Cache.class);
                    assertThat(context).hasSingleBean(RateLimiter.class);
                });
    }

    @Test
    void shouldCreateAllBeansByDefault() {
        contextRunner
                .withPropertyValues(
                        "afg.ai.enabled=true",
                        "afg.ai.llm.provider=none"
                )
                .run(context -> {
                    // Resilience
                    assertThat(context).hasSingleBean(ResilienceExecutor.class);
                    assertThat(context).hasSingleBean(RetryPolicy.class);
                    assertThat(context).hasSingleBean(CircuitBreaker.class);

                    // Observability
                    assertThat(context).hasSingleBean(MetricsCollector.class);
                    assertThat(context).hasSingleBean(Tracer.class);
                    assertThat(context).hasSingleBean(AuditLogger.class);

                    // Security
                    assertThat(context).hasSingleBean(ApiKeyManager.class);
                    assertThat(context).hasSingleBean(ContentSafetyChecker.class);
                    assertThat(context).hasSingleBean(PiiDetector.class);

                    // Persistence
                    assertThat(context).hasSingleBean(SessionStore.class);
                    assertThat(context).hasSingleBean(MessageHistoryStore.class);

                    // Performance
                    assertThat(context).hasSingleBean(Cache.class);
                    assertThat(context).hasSingleBean(RateLimiter.class);
                });
    }

    @Test
    void shouldAllowCustomBeanOverride() {
        contextRunner
                .withPropertyValues(
                        "afg.ai.enabled=true",
                        "afg.ai.llm.provider=none"
                )
                .withBean(RetryPolicy.class, () -> DefaultRetryPolicy.builder().maxRetries(99).build())
                .run(context -> {
                    RetryPolicy retryPolicy = context.getBean(RetryPolicy.class);
                    assertThat(retryPolicy.getMaxRetries()).isEqualTo(99);
                });
    }

    @Test
    void shouldConfigureCacheWithCustomSettings() {
        contextRunner
                .withPropertyValues(
                        "afg.ai.enabled=true",
                        "afg.ai.llm.provider=none",
                        "afg.ai.performance.cache.max-size=2000",
                        "afg.ai.performance.cache.ttl-seconds=1800"
                )
                .run(context -> {
                    @SuppressWarnings("unchecked")
                    Cache<String, Object> cache = context.getBean(Cache.class);
                    assertThat(cache).isNotNull();
                });
    }

    @Test
    void shouldConfigureRateLimiterWithCustomSettings() {
        contextRunner
                .withPropertyValues(
                        "afg.ai.enabled=true",
                        "afg.ai.llm.provider=none",
                        "afg.ai.performance.rate-limit.default-permits=200",
                        "afg.ai.performance.rate-limit.window-seconds=2"
                )
                .run(context -> {
                    RateLimiter rateLimiter = context.getBean(RateLimiter.class);
                    assertThat(rateLimiter).isNotNull();
                });
    }
}
