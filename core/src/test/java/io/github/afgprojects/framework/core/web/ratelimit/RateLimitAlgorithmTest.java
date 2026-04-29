package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

class RateLimitAlgorithmTest extends BaseUnitTest {

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
