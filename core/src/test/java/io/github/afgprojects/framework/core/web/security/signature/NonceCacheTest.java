package io.github.afgprojects.framework.core.web.security.signature;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * NonceCache 测试
 */
@DisplayName("NonceCache 测试")
class NonceCacheTest {

    private NonceCache cache;

    @BeforeEach
    void setUp() {
        cache = new NonceCache(100);
    }

    @Nested
    @DisplayName("Nonce 检查测试")
    class CheckAndAddTests {

        @Test
        @DisplayName("新 nonce 应该添加成功")
        void shouldAddNewNonce() {
            // given
            String nonce = "nonce-1";
            long timestamp = System.currentTimeMillis();

            // when
            boolean result = cache.checkAndAdd(nonce, timestamp);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("重复 nonce 应该添加失败")
        void shouldRejectDuplicateNonce() {
            // given
            String nonce = "nonce-1";
            long timestamp = System.currentTimeMillis();
            cache.checkAndAdd(nonce, timestamp);

            // when
            boolean result = cache.checkAndAdd(nonce, timestamp);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("不同 nonce 应该都能添加")
        void shouldAddDifferentNonces() {
            // given
            long timestamp = System.currentTimeMillis();

            // when
            boolean result1 = cache.checkAndAdd("nonce-1", timestamp);
            boolean result2 = cache.checkAndAdd("nonce-2", timestamp);
            boolean result3 = cache.checkAndAdd("nonce-3", timestamp);

            // then
            assertThat(result1).isTrue();
            assertThat(result2).isTrue();
            assertThat(result3).isTrue();
        }
    }

    @Nested
    @DisplayName("缓存查询测试")
    class QueryTests {

        @Test
        @DisplayName("应该正确检查 nonce 是否存在")
        void shouldCheckNonceExists() {
            // given
            String nonce = "nonce-1";
            long timestamp = System.currentTimeMillis();
            cache.checkAndAdd(nonce, timestamp);

            // when
            boolean exists = cache.contains(nonce);
            boolean notExists = cache.contains("nonce-2");

            // then
            assertThat(exists).isTrue();
            assertThat(notExists).isFalse();
        }

        @Test
        @DisplayName("应该正确获取 nonce 的时间戳")
        void shouldGetNonceTimestamp() {
            // given
            String nonce = "nonce-1";
            long timestamp = System.currentTimeMillis();
            cache.checkAndAdd(nonce, timestamp);

            // when
            Long retrievedTimestamp = cache.getTimestamp(nonce);

            // then
            assertThat(retrievedTimestamp).isEqualTo(timestamp);
        }

        @Test
        @DisplayName("不存在的 nonce 应该返回 null")
        void shouldReturnNullForMissingNonce() {
            // when
            Long timestamp = cache.getTimestamp("nonexistent");

            // then
            assertThat(timestamp).isNull();
        }
    }

    @Nested
    @DisplayName("缓存清理测试")
    class CleanTests {

        @Test
        @DisplayName("应该清理过期的 nonce")
        void shouldCleanExpiredNonces() {
            // given
            long oldTime = System.currentTimeMillis() - 10000;
            long newTime = System.currentTimeMillis();
            cache.checkAndAdd("old-nonce", oldTime);
            cache.checkAndAdd("new-nonce", newTime);

            // when
            cache.cleanExpired(System.currentTimeMillis() - 5000);

            // then
            assertThat(cache.contains("old-nonce")).isFalse();
            assertThat(cache.contains("new-nonce")).isTrue();
        }

        @Test
        @DisplayName("清空缓存应该移除所有 nonce")
        void shouldClearAllNonces() {
            // given
            cache.checkAndAdd("nonce-1", System.currentTimeMillis());
            cache.checkAndAdd("nonce-2", System.currentTimeMillis());
            cache.checkAndAdd("nonce-3", System.currentTimeMillis());

            // when
            cache.clear();

            // then
            assertThat(cache.size()).isZero();
        }
    }

    @Nested
    @DisplayName("LRU 淘汰测试")
    class LruTests {

        @Test
        @DisplayName("超过容量应该淘汰旧的 nonce")
        void shouldEvictOldestWhenExceedCapacity() {
            // given
            NonceCache smallCache = new NonceCache(3);

            // when
            smallCache.checkAndAdd("nonce-1", System.currentTimeMillis());
            smallCache.checkAndAdd("nonce-2", System.currentTimeMillis());
            smallCache.checkAndAdd("nonce-3", System.currentTimeMillis());
            smallCache.checkAndAdd("nonce-4", System.currentTimeMillis());

            // then
            assertThat(smallCache.size()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("容量测试")
    class SizeTests {

        @Test
        @DisplayName("应该正确返回缓存大小")
        void shouldReturnCorrectSize() {
            // given
            assertThat(cache.size()).isZero();

            // when
            cache.checkAndAdd("nonce-1", System.currentTimeMillis());
            cache.checkAndAdd("nonce-2", System.currentTimeMillis());

            // then
            assertThat(cache.size()).isEqualTo(2);
        }
    }
}
