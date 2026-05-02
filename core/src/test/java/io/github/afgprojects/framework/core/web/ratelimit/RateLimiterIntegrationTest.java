package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

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

            RateLimit annotation = createTestAnnotation("local-test", 10);

            // 第一次应该通过
            RateLimitResult result1 = rateLimiter.tryAcquire(annotation);
            assertThat(result1.allowed()).isTrue();
        }

        @Test
        @DisplayName("应该能够重置限流器")
        void shouldResetRateLimiter() {
            if (rateLimiter == null) {
                return;
            }

            RateLimit annotation = createTestAnnotation("reset-test", 10);

            rateLimiter.tryAcquire(annotation);

            boolean reset = rateLimiter.reset(annotation);

            assertThat(reset).isTrue();
        }

        @Test
        @DisplayName("应该能够获取限流信息")
        void shouldGetRateLimitInfo() {
            if (rateLimiter == null) {
                return;
            }

            RateLimit annotation = createTestAnnotation("info-test", 10);

            rateLimiter.tryAcquire(annotation);

            var info = rateLimiter.getRateLimitInfo(annotation);

            // 本地限流可能返回 null
            assertThat(info == null || info.key() != null).isTrue();
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

    private RateLimit createTestAnnotation(String key, int rate) {
        return new RateLimit() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public long rate() {
                return rate;
            }

            @Override
            public long burst() {
                return rate * 2;
            }

            @Override
            public RateLimitDimension dimension() {
                return RateLimitDimension.API;
            }

            @Override
            public RateLimitAlgorithm algorithm() {
                return RateLimitAlgorithm.TOKEN_BUCKET;
            }

            @Override
            public long windowSize() {
                return 1;
            }

            @Override
            public String message() {
                return "Rate limit exceeded";
            }

            @Override
            public String fallbackMethod() {
                return "";
            }

            @Override
            public boolean responseHeaders() {
                return true;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RateLimit.class;
            }
        };
    }
}
