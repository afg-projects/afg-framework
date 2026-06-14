package io.github.afgprojects.framework.integration.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.api.ratelimit.RateLimitStorage;
import io.github.afgprojects.framework.integration.redis.ratelimit.RedisRateLimitProperties;
import io.github.afgprojects.framework.integration.redis.ratelimit.RedisRateLimitStorage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisRateLimitStorage 集成测试
 *
 * <p>基于真实 Redis 容器测试限流存储操作
 */
@DisplayName("RedisRateLimitStorage 限流存储测试")
class RedisRateLimitStorageTest extends BaseRedisTest {

    private RateLimitStorage rateLimitStorage;

    @BeforeEach
    void setUp() {
        RedisRateLimitProperties properties = new RedisRateLimitProperties();
        rateLimitStorage = new RedisRateLimitStorage(getRedissonClient(), properties);
    }

    @Nested
    @DisplayName("getStorageType")
    class StorageType {

        @Test
        @DisplayName("getStorageType 应返回 redis")
        void shouldReturnRedis() {
            assertThat(rateLimitStorage.getStorageType()).isEqualTo("redis");
        }
    }

    @Nested
    @DisplayName("increment 操作")
    class Increment {

        @Test
        @DisplayName("increment 应原子递增并返回新值")
        void shouldIncrementAndReturnNewValue() {
            long result = rateLimitStorage.increment("test:ratelimit:counter", 1, 60);

            assertThat(result).isEqualTo(1);

            long result2 = rateLimitStorage.increment("test:ratelimit:counter", 1, 60);
            assertThat(result2).isEqualTo(2);
        }

        @Test
        @DisplayName("increment 支持自定义增量")
        void shouldSupportCustomDelta() {
            long result = rateLimitStorage.increment("test:ratelimit:delta", 5, 60);
            assertThat(result).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("compareAndSet 操作")
    class CompareAndSet {

        @Test
        @DisplayName("compareAndSet 当期望值匹配时应成功")
        void shouldSucceed_whenExpectMatches() {
            rateLimitStorage.increment("test:ratelimit:cas", 1, 60);
            // 当前值应为 1

            boolean result = rateLimitStorage.compareAndSet("test:ratelimit:cas", 1, 10);
            assertThat(result).isTrue();
            assertThat(rateLimitStorage.get("test:ratelimit:cas")).isEqualTo(10);
        }

        @Test
        @DisplayName("compareAndSet 当期望值不匹配时应失败")
        void shouldFail_whenExpectDoesNotMatch() {
            rateLimitStorage.increment("test:ratelimit:cas2", 1, 60);

            boolean result = rateLimitStorage.compareAndSet("test:ratelimit:cas2", 999, 10);
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("get / exists / delete 操作")
    class BasicOperations {

        @Test
        @DisplayName("get 不存在的 key 应返回 0")
        void shouldReturnZero_whenKeyNotExists() {
            long value = rateLimitStorage.get("test:ratelimit:nonexistent");
            assertThat(value).isEqualTo(0);
        }

        @Test
        @DisplayName("exists 应正确判断 key 是否存在")
        void shouldDetectKeyExistence() {
            assertThat(rateLimitStorage.exists("test:ratelimit:exist")).isFalse();

            rateLimitStorage.increment("test:ratelimit:exist", 1, 60);
            assertThat(rateLimitStorage.exists("test:ratelimit:exist")).isTrue();
        }

        @Test
        @DisplayName("delete 应删除 key")
        void shouldDeleteKey() {
            rateLimitStorage.increment("test:ratelimit:del", 1, 60);
            assertThat(rateLimitStorage.exists("test:ratelimit:del")).isTrue();

            rateLimitStorage.delete("test:ratelimit:del");
            assertThat(rateLimitStorage.exists("test:ratelimit:del")).isFalse();
        }
    }

    @Nested
    @DisplayName("tryAcquireTokenBucket 操作")
    class TokenBucket {

        @Test
        @DisplayName("令牌桶限流应在限流范围内允许请求")
        void shouldAllowWithinRateLimit() {
            var result = rateLimitStorage.tryAcquireTokenBucket("test:ratelimit:token", 10, 10);

            assertThat(result.allowed()).isTrue();
        }

        @Test
        @DisplayName("令牌桶限流应在超过限流后拒绝请求")
        void shouldRejectWhenExceedRateLimit() {
            String key = "test:ratelimit:token-reject";
            // 先消耗所有令牌
            for (int i = 0; i < 10; i++) {
                rateLimitStorage.tryAcquireTokenBucket(key, 10, 10);
            }

            // 下一次应该被拒绝
            var result = rateLimitStorage.tryAcquireTokenBucket(key, 10, 10);
            assertThat(result.allowed()).isFalse();
        }
    }

    @Nested
    @DisplayName("tryAcquireSlidingWindow 操作")
    class SlidingWindow {

        @Test
        @DisplayName("滑动窗口限流应在限流范围内允许请求")
        void shouldAllowWithinRateLimit() {
            var result = rateLimitStorage.tryAcquireSlidingWindow("test:ratelimit:window", 10, 1);

            assertThat(result.allowed()).isTrue();
        }
    }

    @AfterEach
    void cleanup() {
        getRedissonClient().getKeys().getKeysByPattern("test:ratelimit:*").forEach(key -> {
            getRedissonClient().getBucket(key).delete();
        });
    }
}
