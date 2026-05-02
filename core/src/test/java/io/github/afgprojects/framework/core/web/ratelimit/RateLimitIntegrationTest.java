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
 * RateLimit 集成测试
 */
@DisplayName("RateLimit 集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.ratelimit.enabled=true",
                "afg.ratelimit.default-limit=100"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RateLimitIntegrationTest {

    @Autowired(required = false)
    private RateLimitProperties rateLimitProperties;

    @Nested
    @DisplayName("RateLimit 配置测试")
    class RateLimitConfigTests {

        @Test
        @DisplayName("应该正确配置 RateLimitProperties")
        void shouldConfigureRateLimitProperties() {
            assertThat(rateLimitProperties).isNotNull();
            assertThat(rateLimitProperties.isEnabled()).isTrue();
            assertThat(rateLimitProperties.getDefaultRate()).isEqualTo(10);
        }
    }
}
