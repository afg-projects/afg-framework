package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.github.afgprojects.framework.core.api.ratelimit.RateLimitAlgorithm;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimitDimension;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimitResult;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimiter;
import io.github.afgprojects.framework.core.support.TestApplication;

/**
 * RateLimiter 集成测试
 */
@DisplayName("RateLimiter 集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.rate-limit.enabled=true",
                "afg.rate-limit.default-rate=100",
                "afg.rate-limit.default-burst=200",
                "afg.rate-limit.local.enabled=true",
                "afg.rate-limit.local.cache-size=1000"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RateLimiterIntegrationTest {

    @Autowired(required = false)
    private RateLimitProperties rateLimitProperties;

    @Autowired(required = false)
    private RateLimiter rateLimiter;

    @Nested
    @DisplayName("限流器配置测试")
    class RateLimiterConfigTests {

        @Test
        @DisplayName("应该自动配置限流属性")
        void shouldAutoConfigureRateLimitProperties() {
            assertThat(rateLimitProperties).isNotNull();
            assertThat(rateLimitProperties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("应该自动配置限流器")
        void shouldAutoConfigureRateLimiter() {
            assertThat(rateLimiter).isNotNull();
        }
    }

    @Nested
    @DisplayName("本地限流测试")
    class LocalRateLimitTests {

        @Test
        @DisplayName("应该能够执行本地限流")
        void shouldPerformLocalRateLimit() {
            if (rateLimiter == null) {
                return;
            }

            // 第一次应该通过
            RateLimitResult result1 = rateLimiter.builder()
                .key("local-test")
                .dimension(RateLimitDimension.API)
                .rate(10)
                .burst(20)
                .tryAcquire();
            assertThat(result1.allowed()).isTrue();
        }

        @Test
        @DisplayName("应该能够消耗所有限流令牌")
        void shouldConsumeAllTokens() {
            if (rateLimiter == null) {
                return;
            }

            // 消耗所有令牌
            for (int i = 0; i < 20; i++) {
                rateLimiter.builder()
                    .key("consume-test")
                    .dimension(RateLimitDimension.API)
                    .rate(10)
                    .burst(20)
                    .tryAcquire();
            }

            // 下一次应该被拒绝
            RateLimitResult result = rateLimiter.builder()
                .key("consume-test")
                .dimension(RateLimitDimension.API)
                .rate(10)
                .burst(20)
                .tryAcquire();

            assertThat(result.allowed()).isFalse();
        }

        @Test
        @DisplayName("应该支持滑动窗口算法")
        void shouldSupportSlidingWindow() {
            if (rateLimiter == null) {
                return;
            }

            RateLimitResult result = rateLimiter.builder()
                .key("sliding-test")
                .dimension(RateLimitDimension.API)
                .rate(10)
                .algorithm(RateLimitAlgorithm.SLIDING_WINDOW)
                .windowSize(60)
                .tryAcquire();

            assertThat(result.allowed()).isTrue();
        }
    }

    @Nested
    @DisplayName("限流属性测试")
    class RateLimitPropertiesTests {

        @Test
        @DisplayName("应该正确配置默认速率")
        void shouldConfigureDefaultRate() {
            assertThat(rateLimitProperties.getDefaultRate()).isEqualTo(100);
        }

        @Test
        @DisplayName("应该正确配置本地限流")
        void shouldConfigureLocalRateLimit() {
            assertThat(rateLimitProperties.getLocal().isEnabled()).isTrue();
            assertThat(rateLimitProperties.getLocal().getCacheSize()).isEqualTo(1000);
        }

        @Test
        @DisplayName("应该正确配置键前缀")
        void shouldConfigureKeyPrefix() {
            assertThat(rateLimitProperties.getKeyPrefix()).isNotNull();
        }
    }
}
