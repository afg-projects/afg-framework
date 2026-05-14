package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.api.ratelimit.RateLimitAlgorithm;
import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * RateLimitAlgorithm 枚举单元测试。
 * <p>
 * 测试限流算法枚举的功能，验证令牌桶和滑动窗口算法的存在。
 *
 * @see io.github.afgprojects.framework.core.api.ratelimit.RateLimitAlgorithm
 */
class RateLimitAlgorithmTest extends BaseUnitTest {

    /**
     * 测试存在令牌桶算法。
     */
    @Test
    void should_haveTokenBucketAlgorithm() {
        assertThat(RateLimitAlgorithm.TOKEN_BUCKET).isNotNull();
        assertThat(RateLimitAlgorithm.TOKEN_BUCKET.name()).isEqualTo("TOKEN_BUCKET");
    }

    @Test
    void should_haveSlidingWindowAlgorithm() {
        assertThat(RateLimitAlgorithm.SLIDING_WINDOW).isNotNull();
        assertThat(RateLimitAlgorithm.SLIDING_WINDOW.name()).isEqualTo("SLIDING_WINDOW");
    }

    @Test
    void should_haveTwoAlgorithms() {
        assertThat(RateLimitAlgorithm.values()).hasSize(2);
    }
}
