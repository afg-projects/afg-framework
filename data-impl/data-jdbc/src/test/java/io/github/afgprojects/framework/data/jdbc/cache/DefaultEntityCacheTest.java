package io.github.afgprojects.framework.data.jdbc.cache;

import io.github.afgprojects.framework.core.cache.CacheProperties;
import io.github.afgprojects.framework.core.cache.CacheConfig;
import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * DefaultEntityCache 单元测试
 */
@DisplayName("DefaultEntityCache 测试")
class DefaultEntityCacheTest {

    private DefaultCacheManager createCacheManager() {
        CacheProperties cacheProperties = new CacheProperties();
        return new DefaultCacheManager(cacheProperties);
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该使用默认配置创建缓存")
        void shouldCreateCacheWithDefaultConfig() {
            DefaultCacheManager cacheManager = createCacheManager();
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager);

            assertThat(cache.getEntityClass()).isEqualTo(TestEntity.class);
            assertThat(cache.getCacheName()).startsWith("entity:");
        }

        @Test
        @DisplayName("应该使用自定义配置创建缓存")
        void shouldCreateCacheWithCustomConfig() {
            DefaultCacheManager cacheManager = createCacheManager();
            CacheConfig config = new CacheConfig();
            config.defaultTtl(60000);
            config.maximumSize(500);

            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager, config);

            assertThat(cache.getEntityClass()).isEqualTo(TestEntity.class);
        }

        @Test
        @DisplayName("应该使用 cacheConfigName 创建缓存")
        void shouldCreateCacheWithConfigName() {
            DefaultCacheManager cacheManager = createCacheManager();

            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager, "customConfig");

            assertThat(cache.getEntityClass()).isEqualTo(TestEntity.class);
            assertThat(cache.getCacheName()).startsWith("entity:");
        }
    }

    @Nested
    @DisplayName("get/put 测试")
    class GetPutTests {

        @Test
        @DisplayName("获取不存在的实体应该返回空")
        void shouldReturnEmptyWhenNotExists() {
            DefaultCacheManager cacheManager = createCacheManager();
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager);

            Optional<TestEntity> result = cache.get("non-existent");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("放入缓存后应该能获取")
        void shouldPutAndGetEntity() {
            DefaultCacheManager cacheManager = createCacheManager();
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager);
            TestEntity entity = new TestEntity("1", "Test");

            cache.put("1", entity);
            Optional<TestEntity> result = cache.get("1");

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo("1");
            assertThat(result.get().getName()).isEqualTo("Test");
        }

        @Test
        @DisplayName("应该使用 TTL 放入缓存")
        void shouldPutEntityWithTtl() {
            DefaultCacheManager cacheManager = createCacheManager();
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager);
            TestEntity entity = new TestEntity("1", "Test");

            assertThatCode(() -> cache.put("1", entity, 30000))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("evict 测试")
    class EvictTests {

        @Test
        @DisplayName("应该失效指定 ID 的缓存")
        void shouldEvictCacheById() {
            DefaultCacheManager cacheManager = createCacheManager();
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager);
            TestEntity entity = new TestEntity("1", "Test");
            cache.put("1", entity);

            cache.evict("1");

            Optional<TestEntity> result = cache.get("1");
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("evictAll/clear 测试")
    class EvictAllTests {

        @Test
        @DisplayName("应该清空缓存")
        void shouldClearCache() {
            DefaultCacheManager cacheManager = createCacheManager();
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager);
            cache.put("1", new TestEntity("1", "Test"));
            cache.put("2", new TestEntity("2", "Test2"));

            cache.evictAll();

            assertThat(cache.get("1")).isEmpty();
            assertThat(cache.get("2")).isEmpty();
        }

        @Test
        @DisplayName("clear 应该清空缓存")
        void shouldClearCacheWithClear() {
            DefaultCacheManager cacheManager = createCacheManager();
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager);
            cache.put("1", new TestEntity("1", "Test"));

            cache.clear();

            assertThat(cache.get("1")).isEmpty();
        }
    }

    @Nested
    @DisplayName("containsKey 测试")
    class ContainsKeyTests {

        @Test
        @DisplayName("存在时应该返回 true")
        void shouldReturnTrueWhenKeyExists() {
            DefaultCacheManager cacheManager = createCacheManager();
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager);
            cache.put("1", new TestEntity("1", "Test"));

            boolean result = cache.containsKey("1");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("不存在时应该返回 false")
        void shouldReturnFalseWhenKeyNotExists() {
            DefaultCacheManager cacheManager = createCacheManager();
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager);

            boolean result = cache.containsKey("non-existent");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("size 测试")
    class SizeTests {

        @Test
        @DisplayName("应该返回缓存大小")
        void shouldReturnCacheSize() {
            DefaultCacheManager cacheManager = createCacheManager();
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager);
            cache.put("1", new TestEntity("1", "Test"));
            cache.put("2", new TestEntity("2", "Test2"));

            long size = cache.size();

            assertThat(size).isGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getEntityClass/getCacheName 测试")
    class GetterTests {

        @Test
        @DisplayName("应该返回实体类")
        void shouldReturnEntityClass() {
            DefaultCacheManager cacheManager = createCacheManager();
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager);

            assertThat(cache.getEntityClass()).isEqualTo(TestEntity.class);
        }

        @Test
        @DisplayName("应该返回缓存名称")
        void shouldReturnCacheName() {
            DefaultCacheManager cacheManager = createCacheManager();
            DefaultEntityCache<TestEntity> cache = new DefaultEntityCache<>(TestEntity.class, cacheManager);

            assertThat(cache.getCacheName()).startsWith("entity:");
            assertThat(cache.getCacheName()).contains("TestEntity");
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