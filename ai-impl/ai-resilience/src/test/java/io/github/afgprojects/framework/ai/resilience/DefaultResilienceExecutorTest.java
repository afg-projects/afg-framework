package io.github.afgprojects.framework.ai.resilience;

import io.github.afgprojects.framework.ai.core.api.resilience.CircuitBreaker;
import io.github.afgprojects.framework.ai.core.api.resilience.FallbackStrategy;
import io.github.afgprojects.framework.ai.core.api.resilience.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DefaultResilienceExecutor 单元测试
 */
class DefaultResilienceExecutorTest {

    private DefaultResilienceExecutor executor;

    @BeforeEach
    void setUp() {
        RetryPolicy retryPolicy = DefaultRetryPolicy.builder()
                .maxRetries(2)
                .initialIntervalMs(10)
                .build();

        CircuitBreaker circuitBreaker = DefaultCircuitBreaker.builder()
                .windowSize(10)
                .failureRateThreshold(0.5)
                .build();

        executor = new DefaultResilienceExecutor(retryPolicy, circuitBreaker);
    }

    @Test
    @DisplayName("执行成功操作")
    void execute_success() throws Exception {
        String result = executor.execute(() -> "success");

        assertThat(result).isEqualTo("success");
    }

    @Test
    @DisplayName("失败后重试成功")
    void execute_retrySuccess() throws Exception {
        int[] counter = {0};

        String result = executor.execute(() -> {
            counter[0]++;
            if (counter[0] < 2) {
                throw new java.net.SocketTimeoutException("timeout");
            }
            return "success";
        });

        assertThat(result).isEqualTo("success");
        assertThat(counter[0]).isEqualTo(2);
    }

    @Test
    @DisplayName("执行带降级的操作")
    void executeWithFallback_fallbackUsed() {
        FallbackStrategy<String> fallback = new FallbackStrategy<>() {
            @Override
            public String fallback(Exception exception, FallbackContext context) {
                return "fallback result";
            }

            @Override
            public boolean shouldFallback(Exception exception) {
                return true;
            }
        };

        String result = executor.executeWithFallback(
                () -> {
                    throw new RuntimeException("always fails");
                },
                fallback
        );

        assertThat(result).isEqualTo("fallback result");
    }

    @Test
    @DisplayName("降级不应执行时抛出异常")
    void executeWithFallback_fallbackNotAllowed() {
        FallbackStrategy<String> fallback = new FallbackStrategy<>() {
            @Override
            public String fallback(Exception exception, FallbackContext context) {
                return "fallback result";
            }

            @Override
            public boolean shouldFallback(Exception exception) {
                return false;
            }
        };

        assertThatThrownBy(() -> executor.executeWithFallback(
                () -> {
                    throw new RuntimeException("always fails");
                },
                fallback
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("always fails");
    }

    @Test
    @DisplayName("获取重试策略")
    void getRetryPolicy() {
        assertThat(executor.getRetryPolicy()).isNotNull();
    }

    @Test
    @DisplayName("获取熔断器")
    void getCircuitBreaker() {
        assertThat(executor.getCircuitBreaker()).isNotNull();
    }

    @Test
    @DisplayName("熔断器打开时执行降级")
    void execute_circuitBreakerOpen_fallbackUsed() {
        // 强制打开熔断器
        executor.getCircuitBreaker().forceOpen();

        FallbackStrategy<String> fallback = new FallbackStrategy<>() {
            @Override
            public String fallback(Exception exception, FallbackContext context) {
                return "circuit breaker fallback";
            }

            @Override
            public boolean shouldFallback(Exception exception) {
                return true;
            }
        };

        String result = executor.executeWithFallback(() -> "should not reach here", fallback);

        assertThat(result).isEqualTo("circuit breaker fallback");
    }
}