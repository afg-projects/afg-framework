package io.github.afgprojects.framework.core.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * RetryPolicy 测试
 */
@DisplayName("RetryPolicy 测试")
class RetryPolicyTest {

    @Nested
    @DisplayName("defaultPolicy 测试")
    class DefaultPolicyTests {

        @Test
        @DisplayName("应该创建默认重试策略")
        void shouldCreateDefaultPolicy() {
            // when
            RetryPolicy policy = RetryPolicy.defaultPolicy();

            // then
            assertThat(policy.getMaxAttempts()).isEqualTo(3);
        }

        @Test
        @DisplayName("默认策略应该在配置的状态码上重试")
        void shouldRetryOnDefaultStatusCodes() {
            // given
            RetryPolicy policy = RetryPolicy.defaultPolicy();

            // then
            assertThat(policy.shouldRetry(502, null)).isTrue();
            assertThat(policy.shouldRetry(503, null)).isTrue();
            assertThat(policy.shouldRetry(504, null)).isTrue();
            assertThat(policy.shouldRetry(500, null)).isFalse();
        }

        @Test
        @DisplayName("默认策略应该对网络异常重试")
        void shouldRetryOnNetworkException() {
            // given
            RetryPolicy policy = RetryPolicy.defaultPolicy();

            // when & then - 网络异常应该重试
            assertThat(policy.shouldRetry(200, new IOException())).isTrue();
            assertThat(policy.shouldRetry(200, new SocketTimeoutException())).isTrue();
            assertThat(policy.shouldRetry(200, new ConnectException())).isTrue();
            // 非网络异常不应该重试
            assertThat(policy.shouldRetry(200, new RuntimeException())).isFalse();
            assertThat(policy.shouldRetry(200, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Builder 测试")
    class BuilderTests {

        @Test
        @DisplayName("应该构建自定义重试策略")
        void shouldBuildCustomPolicy() {
            // when
            RetryPolicy policy = RetryPolicy.builder()
                    .maxAttempts(5)
                    .initialInterval(500)
                    .multiplier(1.5)
                    .maxInterval(5000)
                    .retryOnStatus(Set.of(500, 502))
                    .build();

            // then
            assertThat(policy.getMaxAttempts()).isEqualTo(5);
            assertThat(policy.shouldRetry(500, null)).isTrue();
            assertThat(policy.shouldRetry(502, null)).isTrue();
            assertThat(policy.shouldRetry(503, null)).isFalse();
        }

        @Test
        @DisplayName("应该计算正确的等待时间（指数退避 + 抖动）")
        void shouldCalculateCorrectWaitDuration() {
            // given
            RetryPolicy policy = RetryPolicy.builder()
                    .initialInterval(1000)
                    .multiplier(2.0)
                    .maxInterval(10000)
                    .build();

            // when & then - 抖动范围 10-25%，所以实际值在 baseInterval * 1.1 到 baseInterval * 1.25 之间
            Duration d1 = policy.getWaitDuration(1);
            assertThat(d1.toMillis()).isBetween(1100L, 1250L); // 1000 + 10-25% 抖动

            Duration d2 = policy.getWaitDuration(2);
            assertThat(d2.toMillis()).isBetween(2200L, 2500L); // 2000 + 10-25% 抖动

            Duration d3 = policy.getWaitDuration(3);
            assertThat(d3.toMillis()).isBetween(4400L, 5000L); // 4000 + 10-25% 抖动

            Duration d4 = policy.getWaitDuration(4);
            assertThat(d4.toMillis()).isBetween(8800L, 10000L); // 8000 + 10-25% 抖动
        }

        @Test
        @DisplayName("等待时间不应该超过最大间隔")
        void shouldNotExceedMaxInterval() {
            // given
            RetryPolicy policy = RetryPolicy.builder()
                    .initialInterval(1000)
                    .multiplier(10.0)
                    .maxInterval(5000)
                    .build();

            // when & then - 抖动范围 10-25%
            Duration d1 = policy.getWaitDuration(1);
            assertThat(d1.toMillis()).isBetween(1100L, 1250L);

            // 10000 > 5000，所以使用 5000 作为基数
            Duration d2 = policy.getWaitDuration(2);
            assertThat(d2.toMillis()).isBetween(5500L, 6250L);

            Duration d3 = policy.getWaitDuration(3);
            assertThat(d3.toMillis()).isBetween(5500L, 6250L);
        }

        @Test
        @DisplayName("应该支持自定义异常重试谓词")
        void shouldSupportCustomExceptionPredicate() {
            // when
            RetryPolicy policy = RetryPolicy.builder()
                    .retryOnException(e -> e instanceof IllegalStateException)
                    .build();

            // then
            assertThat(policy.shouldRetry(200, new IllegalStateException())).isTrue();
            assertThat(policy.shouldRetry(200, new RuntimeException())).isFalse();
        }

        @Test
        @DisplayName("应该使用默认值构建策略")
        void shouldBuildWithDefaultValues() {
            // when
            RetryPolicy policy = RetryPolicy.builder().build();

            // then
            assertThat(policy.getMaxAttempts()).isEqualTo(3);
            // 默认初始间隔 1000，加上抖动后范围 1100-1250
            assertThat(policy.getWaitDuration(1).toMillis()).isBetween(1100L, 1250L);
        }
    }

    @Nested
    @DisplayName("shouldRetry 测试")
    class ShouldRetryTests {

        @Test
        @DisplayName("配置的状态码应该触发重试")
        void shouldRetryOnConfiguredStatusCodes() {
            // given
            RetryPolicy policy =
                    RetryPolicy.builder().retryOnStatus(Set.of(429, 500)).build();

            // then
            assertThat(policy.shouldRetry(429, null)).isTrue();
            assertThat(policy.shouldRetry(500, null)).isTrue();
            assertThat(policy.shouldRetry(200, null)).isFalse();
            assertThat(policy.shouldRetry(404, null)).isFalse();
        }

        @Test
        @DisplayName("网络异常应该触发重试")
        void shouldRetryOnNetworkException() {
            // given
            RetryPolicy policy = RetryPolicy.builder().build();

            // then - 默认只对网络异常重试
            assertThat(policy.shouldRetry(200, new IOException())).isTrue();
            assertThat(policy.shouldRetry(200, new SocketTimeoutException())).isTrue();
            assertThat(policy.shouldRetry(200, new ConnectException())).isTrue();
        }

        @Test
        @DisplayName("状态码和异常同时满足时应该重试")
        void shouldRetryWhenEitherConditionMet() {
            // given
            RetryPolicy policy =
                    RetryPolicy.builder().retryOnStatus(Set.of(503)).build();

            // then
            assertThat(policy.shouldRetry(503, new IOException())).isTrue();
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("空状态码集合应该只依赖异常重试")
        void shouldOnlyRetryOnExceptionWhenEmptyStatusCodes() {
            // given
            RetryPolicy policy = RetryPolicy.builder().retryOnStatus(Set.of()).build();

            // then - 默认只对网络异常重试
            assertThat(policy.shouldRetry(500, null)).isFalse();
            assertThat(policy.shouldRetry(500, new IOException())).isTrue();
            assertThat(policy.shouldRetry(500, new RuntimeException())).isFalse();
        }

        @Test
        @DisplayName("初始间隔等于最大间隔时应该返回固定值加抖动")
        void shouldReturnFixedIntervalWithJitterWhenInitialEqualsMax() {
            // given
            RetryPolicy policy = RetryPolicy.builder()
                    .initialInterval(2000)
                    .maxInterval(2000)
                    .multiplier(2.0)
                    .build();

            // then - 所有值都是 2000 + 抖动 (2200-2500)
            assertThat(policy.getWaitDuration(1).toMillis()).isBetween(2200L, 2500L);
            assertThat(policy.getWaitDuration(2).toMillis()).isBetween(2200L, 2500L);
            assertThat(policy.getWaitDuration(10).toMillis()).isBetween(2200L, 2500L);
        }
    }
}
