package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimitDimension;

/**
 * RateLimit 枚举测试
 */
@DisplayName("RateLimit 枚举测试")
class RateLimitEnumsTest {

    @Nested
    @DisplayName("RateLimitAlgorithm 测试")
    class RateLimitAlgorithmTest {

        @Test
        @DisplayName("应该包含所有算法类型")
        void shouldContainAllAlgorithms() {
            AfgCoreProperties.RateLimitConfig.RateLimitAlgorithm[] algorithms = AfgCoreProperties.RateLimitConfig.RateLimitAlgorithm.values();

            assertThat(algorithms).hasSize(4);
            assertThat(algorithms).contains(
                    AfgCoreProperties.RateLimitConfig.RateLimitAlgorithm.TOKEN_BUCKET,
                    AfgCoreProperties.RateLimitConfig.RateLimitAlgorithm.SLIDING_WINDOW,
                    AfgCoreProperties.RateLimitConfig.RateLimitAlgorithm.FIXED_WINDOW,
                    AfgCoreProperties.RateLimitConfig.RateLimitAlgorithm.LEAKY_BUCKET
            );
        }
    }

    @Nested
    @DisplayName("RateLimitDimension 测试")
    class RateLimitDimensionTest {

        @Test
        @DisplayName("应该包含所有限流维度")
        void shouldContainAllDimensions() {
            RateLimitDimension[] dimensions = RateLimitDimension.values();

            assertThat(dimensions).hasSize(4);
            assertThat(dimensions).contains(
                    RateLimitDimension.IP,
                    RateLimitDimension.USER,
                    RateLimitDimension.TENANT,
                    RateLimitDimension.API
            );
        }
    }
}
