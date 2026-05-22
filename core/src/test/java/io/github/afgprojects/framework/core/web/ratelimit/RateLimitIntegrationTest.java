package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimitDimension;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimitResult;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimiter;
import io.github.afgprojects.framework.core.support.TestApplication;
import io.github.afgprojects.framework.core.support.BaseIntegrationTest;

/**
 * RateLimit 集成测试
 * 继承 BaseIntegrationTest 以使用 Testcontainers 提供的 Redis 容器
 */
@DisplayName("RateLimit 集成测试")
class RateLimitIntegrationTest extends BaseIntegrationTest {

    @Autowired(required = false)
    private AfgCoreProperties afgCoreProperties;

    @Nested
    @DisplayName("RateLimit 配置测试")
    class RateLimitConfigTests {

        @Test
        @DisplayName("应该正确配置 RateLimitProperties")
        void shouldConfigureRateLimitProperties() {
            assertThat(afgCoreProperties).isNotNull();
            assertThat(afgCoreProperties.getRateLimit().isEnabled()).isTrue();
            assertThat(afgCoreProperties.getRateLimit().getDefaultRate()).isEqualTo(10);
        }
    }
}
