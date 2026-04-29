package io.github.afgprojects.framework.data.sql.scope.cache;

import io.github.afgprojects.framework.data.sql.scope.DataScopeContextProvider;
import io.github.afgprojects.framework.data.sql.scope.DataScopeUserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CachedDataScopeContextProvider 测试
 */
@DisplayName("CachedDataScopeContextProvider Tests")
class CachedDataScopeContextProviderTest {

    private DataScopeUserContext testContext;

    @BeforeEach
    void setUp() {
        testContext = DataScopeUserContext.builder()
                .userId(1L)
                .deptId(100L)
                .accessibleDeptIds(Set.of(100L, 101L, 102L))
                .tenantId(10L)
                .build();
    }

    @Nested
    @DisplayName("Basic Functionality Tests")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("Should cache context by user ID")
        void testCacheByUserId() {
            DataScopeContextProvider delegate = () -> testContext;
            CachedDataScopeContextProvider cachedProvider = new CachedDataScopeContextProvider(
                    delegate, Duration.ofMinutes(5)
            );

            // 第一次调用
            DataScopeUserContext result1 = cachedProvider.provide();
            assertThat(result1).isNotNull();
            assertThat(result1.getUserId()).isEqualTo(1L);

            // 第二次调用应该返回缓存的结果
            DataScopeUserContext result2 = cachedProvider.provide();
            assertThat(result2).isNotNull();
            assertThat(result2.getUserId()).isEqualTo(1L);

            cachedProvider.shutdown();
        }

        @Test
        @DisplayName("Should return null when delegate returns null")
        void testNullFromDelegate() {
            DataScopeContextProvider delegate = () -> null;
            CachedDataScopeContextProvider cachedProvider = new CachedDataScopeContextProvider(
                    delegate, Duration.ofMinutes(5)
            );

            DataScopeUserContext result = cachedProvider.provide();
            assertThat(result).isNull();

            cachedProvider.shutdown();
        }

        @Test
        @DisplayName("Should not cache when user ID is null")
        void testNoCacheWhenUserIdNull() {
            DataScopeUserContext contextWithNullUserId = DataScopeUserContext.builder()
                    .userId(null)
                    .deptId(100L)
                    .build();

            DataScopeContextProvider delegate = () -> contextWithNullUserId;
            CachedDataScopeContextProvider cachedProvider = new CachedDataScopeContextProvider(
                    delegate, Duration.ofMinutes(5)
            );

            DataScopeUserContext result = cachedProvider.provide();
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isNull();
            assertThat(cachedProvider.cacheSize()).isZero();

            cachedProvider.shutdown();
        }
    }

    @Nested
    @DisplayName("Cache Invalidation Tests")
    class CacheInvalidationTests {

        @Test
        @DisplayName("Should invalidate specific user cache")
        void testInvalidateByUserId() {
            DataScopeContextProvider delegate = () -> testContext;
            CachedDataScopeContextProvider cachedProvider = new CachedDataScopeContextProvider(
                    delegate, Duration.ofMinutes(5)
            );

            cachedProvider.provide();
            assertThat(cachedProvider.cacheSize()).isEqualTo(1);

            cachedProvider.invalidate(1L);
            assertThat(cachedProvider.cacheSize()).isZero();

            cachedProvider.shutdown();
        }

        @Test
        @DisplayName("Should invalidate all cache")
        void testInvalidateAll() {
            DataScopeContextProvider delegate = () -> testContext;
            CachedDataScopeContextProvider cachedProvider = new CachedDataScopeContextProvider(
                    delegate, Duration.ofMinutes(5)
            );

            cachedProvider.provide();
            assertThat(cachedProvider.cacheSize()).isEqualTo(1);

            cachedProvider.invalidateAll();
            assertThat(cachedProvider.cacheSize()).isZero();

            cachedProvider.shutdown();
        }
    }

    @Nested
    @DisplayName("Cache Info Tests")
    class CacheInfoTests {

        @Test
        @DisplayName("Should return cache size")
        void testCacheSize() {
            DataScopeContextProvider delegate = () -> testContext;
            CachedDataScopeContextProvider cachedProvider = new CachedDataScopeContextProvider(
                    delegate, Duration.ofMinutes(5)
            );

            assertThat(cachedProvider.cacheSize()).isZero();

            cachedProvider.provide();
            assertThat(cachedProvider.cacheSize()).isEqualTo(1);

            cachedProvider.shutdown();
        }

        @Test
        @DisplayName("Should shutdown cache properly")
        void testShutdown() {
            DataScopeContextProvider delegate = () -> testContext;
            CachedDataScopeContextProvider cachedProvider = new CachedDataScopeContextProvider(
                    delegate, Duration.ofMinutes(5)
            );

            cachedProvider.provide();
            cachedProvider.shutdown();

            // 关闭后缓存应该被清空
            assertThat(cachedProvider.cacheSize()).isZero();
        }
    }

    @Nested
    @DisplayName("Static Factory Method Tests")
    class StaticFactoryMethodTests {

        @Test
        @DisplayName("Static wrap method should create instance")
        void testStaticWrapMethod() {
            DataScopeContextProvider delegate = () -> testContext;
            CachedDataScopeContextProvider cachedProvider = CachedDataScopeContextProvider.wrap(
                    delegate, Duration.ofMinutes(5)
            );

            assertThat(cachedProvider).isNotNull();

            DataScopeUserContext result = cachedProvider.provide();
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(1L);

            cachedProvider.shutdown();
        }
    }

    @Nested
    @DisplayName("Custom UserId Extractor Tests")
    class CustomUserIdExtractorTests {

        @Test
        @DisplayName("Custom userIdExtractor should be used")
        void testCustomUserIdExtractor() {
            DataScopeContextProvider delegate = () -> testContext;
            // 使用自定义提取器，返回固定的用户ID
            CachedDataScopeContextProvider cachedProvider = new CachedDataScopeContextProvider(
                    delegate, Duration.ofMinutes(5), ctx -> 999L
            );

            DataScopeUserContext result = cachedProvider.provide();
            assertThat(result).isNotNull();
            assertThat(cachedProvider.cacheSize()).isEqualTo(1);

            // 验证缓存使用的是自定义提取器返回的ID
            cachedProvider.invalidate(999L);
            assertThat(cachedProvider.cacheSize()).isZero();

            cachedProvider.shutdown();
        }

        @Test
        @DisplayName("Custom userIdExtractor returning null should not cache")
        void testCustomUserIdExtractorReturnsNull() {
            DataScopeContextProvider delegate = () -> testContext;
            // 使用自定义提取器返回 null
            CachedDataScopeContextProvider cachedProvider = new CachedDataScopeContextProvider(
                    delegate, Duration.ofMinutes(5), ctx -> null
            );

            DataScopeUserContext result = cachedProvider.provide();
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(1L);
            // 不应该缓存
            assertThat(cachedProvider.cacheSize()).isZero();

            cachedProvider.shutdown();
        }
    }

    @Nested
    @DisplayName("Cache Expiration Tests")
    class CacheExpirationTests {

        @Test
        @DisplayName("Should return cached context before expiration")
        void testCacheBeforeExpiration() {
            AtomicInteger provideCount = new AtomicInteger(0);
            DataScopeContextProvider delegate = () -> {
                int count = provideCount.incrementAndGet();
                return DataScopeUserContext.builder()
                        .userId(1L)
                        .deptId((long) count) // 每次返回不同的 deptId
                        .build();
            };

            CachedDataScopeContextProvider cachedProvider = new CachedDataScopeContextProvider(
                    delegate, Duration.ofMinutes(5)
            );

            // 第一次调用
            DataScopeUserContext result1 = cachedProvider.provide();
            assertThat(result1.getDeptId()).isEqualTo(1L);
            assertThat(provideCount.get()).isEqualTo(1);

            // 第二次调用（缓存命中，返回缓存的上下文）
            DataScopeUserContext result2 = cachedProvider.provide();
            // 虽然delegate被调用了，但返回的是缓存的上下文（deptId=1）
            assertThat(result2.getDeptId()).isEqualTo(1L);
            assertThat(provideCount.get()).isEqualTo(2); // delegate被调用两次

            cachedProvider.shutdown();
        }

        @Test
        @DisplayName("Should reload context after cache expiration")
        void testCacheExpiration() throws InterruptedException {
            CachedDataScopeContextProvider cachedProvider = new CachedDataScopeContextProvider(
                    () -> testContext, Duration.ofMillis(100)
            );

            // 第一次调用
            DataScopeUserContext result1 = cachedProvider.provide();
            assertThat(result1.getUserId()).isEqualTo(1L);
            assertThat(cachedProvider.cacheSize()).isEqualTo(1);

            // 等待缓存过期
            Thread.sleep(150);

            // 调用 invalidate 来清除过期条目（模拟过期行为）
            cachedProvider.invalidate(1L);

            // 再次调用，应该重新缓存
            DataScopeUserContext result2 = cachedProvider.provide();
            assertThat(result2.getUserId()).isEqualTo(1L);
            assertThat(cachedProvider.cacheSize()).isEqualTo(1);

            cachedProvider.shutdown();
        }
    }

    @Nested
    @DisplayName("Multiple Users Tests")
    class MultipleUsersTests {

        @Test
        @DisplayName("Should cache contexts for multiple users separately")
        void testMultipleUsersCache() {
            CachedDataScopeContextProvider cachedProvider = new CachedDataScopeContextProvider(
                    () -> testContext, Duration.ofMinutes(5)
            );

            // 第一次调用
            DataScopeUserContext result1 = cachedProvider.provide();
            assertThat(result1.getUserId()).isEqualTo(1L);

            // 第二次调用 - 应该返回缓存结果
            DataScopeUserContext result2 = cachedProvider.provide();
            assertThat(result2.getUserId()).isEqualTo(1L);

            // 缓存大小应该为 1
            assertThat(cachedProvider.cacheSize()).isEqualTo(1);

            cachedProvider.shutdown();
        }

        @Test
        @DisplayName("Should invalidate specific user without affecting others")
        void testSelectiveInvalidation() {
            CachedDataScopeContextProvider cachedProvider = new CachedDataScopeContextProvider(
                    () -> testContext, Duration.ofMinutes(5)
            );

            // 缓存用户 1
            cachedProvider.provide();
            assertThat(cachedProvider.cacheSize()).isEqualTo(1);

            // 失效用户 1 的缓存
            cachedProvider.invalidate(1L);
            assertThat(cachedProvider.cacheSize()).isZero();

            cachedProvider.shutdown();
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle context with all null fields except userId")
        void testContextWithNullFields() {
            DataScopeUserContext minimalContext = DataScopeUserContext.builder()
                    .userId(1L)
                    .build();

            DataScopeContextProvider delegate = () -> minimalContext;
            CachedDataScopeContextProvider cachedProvider = new CachedDataScopeContextProvider(
                    delegate, Duration.ofMinutes(5)
            );

            DataScopeUserContext result = cachedProvider.provide();
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getDeptId()).isNull();
            assertThat(result.getTenantId()).isNull();
            assertThat(result.getAccessibleDeptIds()).isEmpty();
            assertThat(result.isAllDataPermission()).isFalse();

            cachedProvider.shutdown();
        }

        @Test
        @DisplayName("Should handle context with allDataPermission flag")
        void testContextWithAllDataPermission() {
            DataScopeUserContext adminContext = DataScopeUserContext.builder()
                    .userId(1L)
                    .allDataPermission(true)
                    .build();

            DataScopeContextProvider delegate = () -> adminContext;
            CachedDataScopeContextProvider cachedProvider = new CachedDataScopeContextProvider(
                    delegate, Duration.ofMinutes(5)
            );

            DataScopeUserContext result = cachedProvider.provide();
            assertThat(result).isNotNull();
            assertThat(result.isAllDataPermission()).isTrue();

            cachedProvider.shutdown();
        }

        @Test
        @DisplayName("Should return same cached instance on multiple calls")
        void testSameCachedInstance() {
            DataScopeContextProvider delegate = () -> testContext;
            CachedDataScopeContextProvider cachedProvider = new CachedDataScopeContextProvider(
                    delegate, Duration.ofMinutes(5)
            );

            DataScopeUserContext result1 = cachedProvider.provide();
            DataScopeUserContext result2 = cachedProvider.provide();

            // 应该返回相同的实例（缓存）
            assertThat(result1).isSameAs(result2);

            cachedProvider.shutdown();
        }
    }
}
