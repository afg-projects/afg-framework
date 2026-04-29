package io.github.afgprojects.framework.data.jdbc.cache;

import io.github.afgprojects.framework.core.cache.CacheProperties;
import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultEntityCache 覆盖率补充测试
 */
@DisplayName("DefaultEntityCache 覆盖率补充测试")
class DefaultEntityCacheCoverageTest {

    private DefaultCacheManager createCacheManager() {
        CacheProperties cacheProperties = new CacheProperties();
        return new DefaultCacheManager(cacheProperties);
    }

    @Nested
    @DisplayName("cacheConfigName 构造函数测试")
    class CacheConfigNameConstructorTests {

        @Test
        @DisplayName("使用 cacheConfigName 创建缓存")
        void shouldCreateCacheWithConfigName() {
            // Given
            DefaultCacheManager cacheManager = createCacheManager();

            // When
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(
                TestEntity.class, cacheManager, "customConfig"
            );

            // Then
            assertThat(cache.getEntityClass()).isEqualTo(TestEntity.class);
            assertThat(cache.getCacheName()).startsWith("entity:");
            assertThat(cache.getCacheName()).contains("TestEntity");
        }

        @Test
        @DisplayName("使用 null cacheConfigName 创建缓存")
        void shouldCreateCacheWithNullConfigName() {
            // Given
            DefaultCacheManager cacheManager = createCacheManager();

            // When
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(
                TestEntity.class, cacheManager, (String) null
            );

            // Then
            assertThat(cache.getEntityClass()).isEqualTo(TestEntity.class);
        }
    }

    @Nested
    @DisplayName("缓存操作完整测试")
    class CacheOperationTests {

        @Test
        @DisplayName("完整 CRUD 操作流程")
        void shouldPerformFullCrudOperations() {
            // Given
            DefaultCacheManager cacheManager = createCacheManager();
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager);
            TestEntity entity = new TestEntity("1", "Test");

            // Create - put
            cache.put("1", entity);

            // Read - get
            Optional<TestEntity> found = cache.get("1");
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Test");

            // Update - put with same key
            TestEntity updated = new TestEntity("1", "Updated");
            cache.put("1", updated);
            Optional<TestEntity> updatedFound = cache.get("1");
            assertThat(updatedFound).isPresent();
            assertThat(updatedFound.get().getName()).isEqualTo("Updated");

            // Delete - evict
            cache.evict("1");
            assertThat(cache.get("1")).isEmpty();
        }

        @Test
        @DisplayName("缓存 null 值")
        void shouldCacheNullValue() {
            // Given
            DefaultCacheManager cacheManager = createCacheManager();
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager);

            // When - 缓存 null
            cache.put("null-key", null);

            // Then
            Optional<TestEntity> found = cache.get("null-key");
            assertThat(found).isEmpty(); // Optional.ofNullable(null) = empty
        }
    }

    @Nested
    @DisplayName("并发安全测试")
    class ConcurrencySafetyTests {

        @Test
        @DisplayName("多线程并发读写应安全")
        void shouldBeThreadSafe() throws Exception {
            // Given
            DefaultCacheManager cacheManager = createCacheManager();
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager);

            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];

            // When - 并发写入
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    cache.put(String.valueOf(index), new TestEntity(String.valueOf(index), "Thread-" + index));
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // Then - 验证所有数据都已写入
            for (int i = 0; i < threadCount; i++) {
                Optional<TestEntity> found = cache.get(String.valueOf(i));
                assertThat(found).isPresent();
            }
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryTests {

        @Test
        @DisplayName("containsKey 对 null 值应返回 true")
        void shouldReturnTrueForNullValue() {
            // Given
            DefaultCacheManager cacheManager = createCacheManager();
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager);
            cache.put("null-key", null);

            // When
            boolean contains = cache.containsKey("null-key");

            // Then - null 值也应该被缓存
            assertThat(contains).isTrue();
        }

        @Test
        @DisplayName("不同类型的 ID 应分别缓存")
        void shouldCacheDifferentIdTypes() {
            // Given
            DefaultCacheManager cacheManager = createCacheManager();
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager);

            // When
            cache.put(1L, new TestEntity("1", "Long"));
            cache.put("1", new TestEntity("1", "String"));

            // Then
            assertThat(cache.get(1L)).isPresent();
            assertThat(cache.get("1")).isPresent();
        }
    }

    // Test entity
    static class TestEntity {
        private String id;
        private String name;

        public TestEntity() {}

        public TestEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
