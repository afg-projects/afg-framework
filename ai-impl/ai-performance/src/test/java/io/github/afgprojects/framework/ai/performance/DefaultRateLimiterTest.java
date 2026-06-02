package io.github.afgprojects.framework.ai.performance;

import io.github.afgprojects.framework.ai.core.api.performance.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultRateLimiter 单元测试
 */
class DefaultRateLimiterTest {

    private DefaultRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new DefaultRateLimiter(10, Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("尝试获取许可")
    void tryAcquire() {
        boolean acquired = limiter.tryAcquire("user-001");

        assertThat(acquired).isTrue();
    }

    @Test
    @DisplayName("获取多个许可")
    void tryAcquire_multiplePermits() {
        boolean acquired = limiter.tryAcquire("user-001", 5);

        assertThat(acquired).isTrue();
    }

    @Test
    @DisplayName("超出限制")
    void tryAcquire_exceedsLimit() {
        // 消耗所有许可
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire("user-001");
        }

        // 应该被限制
        boolean acquired = limiter.tryAcquire("user-001");

        assertThat(acquired).isFalse();
    }

    @Test
    @DisplayName("获取状态")
    void getStatus() {
        limiter.tryAcquire("user-001", 5);

        RateLimiter.RateLimitStatus status = limiter.getStatus("user-001");

        assertThat(status.isLimited()).isFalse();
        assertThat(status.getAvailablePermits()).isGreaterThanOrEqualTo(5);
        assertThat(status.getUsedPermits()).isEqualTo(5);
    }

    @Test
    @DisplayName("重置限制")
    void reset() {
        // 消耗所有许可
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire("user-001");
        }

        limiter.reset("user-001");

        // 重置后应该可以获取
        boolean acquired = limiter.tryAcquire("user-001");

        assertThat(acquired).isTrue();
    }

    @Test
    @DisplayName("不同键独立限制")
    void differentKeys() {
        // 消耗 user-001 的所有许可
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire("user-001");
        }

        // user-002 应该不受影响
        boolean acquired = limiter.tryAcquire("user-002");

        assertThat(acquired).isTrue();
    }

    @Test
    @DisplayName("设置配置")
    void setConfig() {
        RateLimiter.RateLimitConfig config = new RateLimiter.RateLimitConfig() {
            @Override
            public long getPermits() { return 5; }
            @Override
            public Duration getWindow() { return Duration.ofSeconds(1); }
            @Override
            public RateLimiter.RateLimitConfig.Algorithm getAlgorithm() {
                return RateLimiter.RateLimitConfig.Algorithm.TOKEN_BUCKET;
            }
            @Override
            public Duration getMaxWaitTime() { return null; }
        };

        limiter.setConfig("user-001", config);

        // 消耗 5 个许可
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire("user-001");
        }

        // 应该被限制
        boolean acquired = limiter.tryAcquire("user-001");

        assertThat(acquired).isFalse();
    }

    @Test
    @DisplayName("获取所有限制键")
    void getKeys() {
        limiter.tryAcquire("user-001");
        limiter.tryAcquire("user-002");

        java.util.List<String> keys = limiter.getKeys();

        assertThat(keys).contains("user-001", "user-002");
    }

    @Test
    @DisplayName("令牌补充")
    void tokenRefill() throws InterruptedException {
        // 消耗所有许可
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire("user-001");
        }

        // 应该被限制
        assertThat(limiter.tryAcquire("user-001")).isFalse();

        // 等待部分补充
        Thread.sleep(200);

        // 应该有新许可可用
        boolean acquired = limiter.tryAcquire("user-001");

        assertThat(acquired).isTrue();
    }
}