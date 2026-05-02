package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * RateLimitResult 测试
 */
@DisplayName("RateLimitResult 测试")
class RateLimitResultTest {

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryTests {

        @Test
        @DisplayName("应该创建允许结果")
        void shouldCreateAllowedResult() {
            RateLimitResult result = RateLimitResult.allowed(10, 100, 60000);

            assertThat(result.allowed()).isTrue();
            assertThat(result.remaining()).isEqualTo(10);
            assertThat(result.limit()).isEqualTo(100);
            assertThat(result.resetTimeMs()).isEqualTo(60000);
            assertThat(result.retryAfterMs()).isEqualTo(0);
        }

        @Test
        @DisplayName("应该创建拒绝结果")
        void shouldCreateRejectedResult() {
            RateLimitResult result = RateLimitResult.rejected(100, 60000, 5000);

            assertThat(result.allowed()).isFalse();
            assertThat(result.remaining()).isEqualTo(0);
            assertThat(result.limit()).isEqualTo(100);
            assertThat(result.resetTimeMs()).isEqualTo(60000);
            assertThat(result.retryAfterMs()).isEqualTo(5000);
        }
    }

    @Nested
    @DisplayName("响应头测试")
    class HeaderTests {

        @Test
        @DisplayName("应该返回正确的 Limit 头")
        void shouldReturnLimitHeader() {
            RateLimitResult result = RateLimitResult.allowed(10, 100, 60000);

            assertThat(result.getLimitHeader()).isEqualTo("100");
        }

        @Test
        @DisplayName("应该返回正确的 Remaining 头")
        void shouldReturnRemainingHeader() {
            RateLimitResult result = RateLimitResult.allowed(10, 100, 60000);

            assertThat(result.getRemainingHeader()).isEqualTo("10");
        }

        @Test
        @DisplayName("应该返回正确的 Reset 头（秒）")
        void shouldReturnResetHeader() {
            RateLimitResult result = RateLimitResult.allowed(10, 100, 60000);

            assertThat(result.getResetHeader()).isEqualTo("60");
        }

        @Test
        @DisplayName("应该返回正确的 Retry-After 头（秒）")
        void shouldReturnRetryAfterHeader() {
            RateLimitResult result = RateLimitResult.rejected(100, 60000, 5000);

            assertThat(result.getRetryAfterHeader()).isEqualTo("5");
        }

        @Test
        @DisplayName("应该向上取整 Retry-After")
        void shouldCeilRetryAfter() {
            RateLimitResult result = RateLimitResult.rejected(100, 60000, 1001);

            assertThat(result.getRetryAfterHeader()).isEqualTo("2");
        }
    }
}
