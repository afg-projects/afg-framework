package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.api.resilience.RetryPolicy;
import io.github.afgprojects.framework.ai.core.resilience.DefaultRetryPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * DefaultRetryPolicy 纯单元测试
 */
@DisplayName("DefaultRetryPolicy")
class DefaultRetryPolicyTest {

    @Nested
    @DisplayName("shouldRetry")
    class ShouldRetry {

        @Test
        @DisplayName("超过最大重试次数应不再重试")
        void shouldNotRetry_whenMaxRetriesExceeded() {
            RetryPolicy policy = new DefaultRetryPolicy(3, 1000, 2.0, 30000, 0.5);

            assertThat(policy.shouldRetry(new java.net.SocketTimeoutException(), 3)).isFalse();
        }

        @Test
        @DisplayName("可重试异常类型应重试")
        void shouldRetry_whenRetryableException() {
            RetryPolicy policy = new DefaultRetryPolicy(3, 1000, 2.0, 30000, 0.5);

            assertThat(policy.shouldRetry(new java.net.SocketTimeoutException(), 0)).isTrue();
            assertThat(policy.shouldRetry(new java.net.ConnectException(), 0)).isTrue();
            assertThat(policy.shouldRetry(new java.io.IOException(), 0)).isTrue();
        }

        @Test
        @DisplayName("消息含 timeout 应重试")
        void shouldRetry_whenMessageContainsTimeout() {
            RetryPolicy policy = new DefaultRetryPolicy(3, 1000, 2.0, 30000, 0.5);

            assertThat(policy.shouldRetry(new RuntimeException("connection timeout"), 0)).isTrue();
        }

        @Test
        @DisplayName("消息含 rate limit 应重试")
        void shouldRetry_whenMessageContainsRateLimit() {
            RetryPolicy policy = new DefaultRetryPolicy(3, 1000, 2.0, 30000, 0.5);

            assertThat(policy.shouldRetry(new RuntimeException("rate limit exceeded"), 0)).isTrue();
        }

        @Test
        @DisplayName("消息含 429 应重试")
        void shouldRetry_whenMessageContains429() {
            RetryPolicy policy = new DefaultRetryPolicy(3, 1000, 2.0, 30000, 0.5);

            assertThat(policy.shouldRetry(new RuntimeException("HTTP 429 Too Many Requests"), 0)).isTrue();
        }

        @Test
        @DisplayName("不可重试异常应不重试")
        void shouldNotRetry_whenNonRetryableException() {
            RetryPolicy policy = new DefaultRetryPolicy(3, 1000, 2.0, 30000, 0.5);

            assertThat(policy.shouldRetry(new IllegalArgumentException("bad arg"), 0)).isFalse();
        }
    }

    @Nested
    @DisplayName("getWaitTime")
    class GetWaitTime {

        @Test
        @DisplayName("等待时间应随重试次数指数增长")
        void shouldIncreaseExponentially() {
            RetryPolicy policy = new DefaultRetryPolicy(3, 1000, 2.0, 30000, 0);

            long wait0 = policy.getWaitTime(0);
            long wait1 = policy.getWaitTime(1);
            long wait2 = policy.getWaitTime(2);

            assertThat(wait0).isGreaterThan(0);
            assertThat(wait1).isGreaterThan(wait0);
            assertThat(wait2).isGreaterThan(wait1);
        }

        @Test
        @DisplayName("等待时间不应超过最大间隔")
        void shouldNotExceedMaxInterval() {
            RetryPolicy policy = new DefaultRetryPolicy(10, 1000, 2.0, 5000, 0);

            for (int i = 0; i < 10; i++) {
                assertThat(policy.getWaitTime(i)).isLessThanOrEqualTo(5000);
            }
        }
    }

    @Nested
    @DisplayName("getMaxRetries")
    class GetMaxRetries {

        @Test
        @DisplayName("应返回配置的最大重试次数")
        void shouldReturnConfiguredMaxRetries() {
            RetryPolicy policy = new DefaultRetryPolicy(5, 1000, 2.0, 30000, 0.5);

            assertThat(policy.getMaxRetries()).isEqualTo(5);
        }

        @Test
        @DisplayName("默认构造方法应返回 3")
        void shouldReturnDefaultMaxRetries() {
            RetryPolicy policy = new DefaultRetryPolicy();

            assertThat(policy.getMaxRetries()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("成功操作应直接返回结果")
        void shouldReturnResult_whenSuccess() throws Exception {
            RetryPolicy policy = new DefaultRetryPolicy(3, 1, 2.0, 10, 0);

            String result = policy.execute(() -> "success");

            assertThat(result).isEqualTo("success");
        }

        @Test
        @DisplayName("不可重试异常应直接抛出")
        void shouldThrowDirectly_whenNonRetryable() {
            RetryPolicy policy = new DefaultRetryPolicy(3, 1, 2.0, 10, 0);

            assertThatThrownBy(() -> policy.execute(() -> {
                throw new IllegalArgumentException("bad");
            })).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("bad");
        }
    }

    @Nested
    @DisplayName("addRetryableException")
    class AddRetryableException {

        @Test
        @DisplayName("应添加自定义可重试异常类型")
        void shouldAddCustomRetryableException() {
            var policy = new DefaultRetryPolicy(3, 1000, 2.0, 30000, 0.5);
            policy.addRetryableException(IllegalStateException.class);

            assertThat(policy.shouldRetry(new IllegalStateException("retryable"), 0)).isTrue();
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("Builder 应创建自定义重试策略")
        void shouldCreateCustomRetryPolicy() {
            var policy = DefaultRetryPolicy.builder()
                    .maxRetries(5)
                    .initialIntervalMs(500)
                    .multiplier(3.0)
                    .maxIntervalMs(60000)
                    .jitterFactor(0.2)
                    .build();

            assertThat(policy.getMaxRetries()).isEqualTo(5);
        }
    }
}
