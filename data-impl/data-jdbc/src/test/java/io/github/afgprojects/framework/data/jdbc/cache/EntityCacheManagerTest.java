package io.github.afgprojects.framework.data.jdbc.cache;

import io.github.afgprojects.framework.core.cache.CacheProperties;
import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * EntityCacheManager 单元测试
 * <p>
 * 测试缓存管理器的基本功能。
 */
@DisplayName("EntityCacheManager 测试")
class EntityCacheManagerTest {

    private DefaultCacheManager createCacheManager() {
        CacheProperties cacheProperties = new CacheProperties();
        return new DefaultCacheManager(cacheProperties);
    }

    private EntityCacheProperties createEntityCacheProperties() {
        EntityCacheProperties properties = new EntityCacheProperties();
        properties.setEnabled(true);
        properties.setTtl(300000);
        properties.setMaxSize(1000);
        properties.setCacheNull(true);
        properties.setNullValueTtl(60000);
        return properties;
    }

    @Nested
    @DisplayName("getCache 测试")
    class GetCacheTests {

        @Test
        @DisplayName("应该创建并返回实体缓存")
        void shouldCreateAndReturnCache() {
            DefaultCacheManager cacheManager = createCacheManager();
            EntityCacheProperties properties = createEntityCacheProperties();
            EntityCacheManager manager = new EntityCacheManager(cacheManager, properties);

            EntityCache<TestEntity> cache = manager.getCache(TestEntity.class);

            assertThat(cache).isNotNull();
            assertThat(cache.getEntityClass()).isEqualTo(TestEntity.class);
        }

        @Test
        @DisplayName("相同实体类型应该返回相同缓存")
        void shouldReturnSameCacheForSameEntityClass() {
            DefaultCacheManager cacheManager = createCacheManager();
            EntityCacheProperties properties = createEntityCacheProperties();
            EntityCacheManager manager = new EntityCacheManager(cacheManager, properties);

            EntityCache<TestEntity> cache1 = manager.getCache(TestEntity.class);
            EntityCache<TestEntity> cache2 = manager.getCache(TestEntity.class);

            assertThat(cache1).isSameAs(cache2);
        }
    }

    @Nested
    @DisplayName("getCacheIfPresent 测试")
    class GetCacheIfPresentTests {

        @Test
        @DisplayName("未创建缓存应该返回 null")
        void shouldReturnNullWhenCacheNotCreated() {
            DefaultCacheManager cacheManager = createCacheManager();
            EntityCacheProperties properties = createEntityCacheProperties();
            EntityCacheManager manager = new EntityCacheManager(cacheManager, properties);

            EntityCache<TestEntity> cache = manager.getCacheIfPresent(TestEntity.class);

            assertThat(cache).isNull();
        }

        @Test
        @DisplayName("已创建缓存应该返回缓存实例")
        void shouldReturnCacheWhenCreated() {
            DefaultCacheManager cacheManager = createCacheManager();
            EntityCacheProperties properties = createEntityCacheProperties();
            EntityCacheManager manager = new EntityCacheManager(cacheManager, properties);

            manager.getCache(TestEntity.class);
            EntityCache<TestEntity> cache = manager.getCacheIfPresent(TestEntity.class);

            assertThat(cache).isNotNull();
        }
    }

    @Nested
    @DisplayName("isEnabled 测试")
    class IsEnabledTests {

        @Test
        @DisplayName("启用时应该返回 true")
        void shouldReturnTrueWhenEnabled() {
            DefaultCacheManager cacheManager = createCacheManager();
            EntityCacheProperties properties = createEntityCacheProperties();
            properties.setEnabled(true);
            EntityCacheManager manager = new EntityCacheManager(cacheManager, properties);

            assertThat(manager.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("禁用时应该返回 false")
        void shouldReturnFalseWhenDisabled() {
            DefaultCacheManager cacheManager = createCacheManager();
            EntityCacheProperties properties = createEntityCacheProperties();
            properties.setEnabled(false);
            EntityCacheManager manager = new EntityCacheManager(cacheManager, properties);

            assertThat(manager.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("getProperties 测试")
    class GetPropertiesTests {

        @Test
        @DisplayName("应该返回配置属性")
        void shouldReturnProperties() {
            DefaultCacheManager cacheManager = createCacheManager();
            EntityCacheProperties properties = createEntityCacheProperties();
            EntityCacheManager manager = new EntityCacheManager(cacheManager, properties);

            assertThat(manager.getProperties()).isEqualTo(properties);
        }
    }

    @Nested
    @DisplayName("evict 测试")
    class EvictTests {

        @Test
        @DisplayName("失效不存在的缓存不应该抛出异常")
        void shouldNotThrowWhenEvictingNonExistentCache() {
            DefaultCacheManager cacheManager = createCacheManager();
            EntityCacheProperties properties = createEntityCacheProperties();
            EntityCacheManager manager = new EntityCacheManager(cacheManager, properties);

            assertThatCode(() -> manager.evict(TestEntity.class, "test-id"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("失效存在的缓存应该成功")
        void shouldEvictExistingCache() {
            DefaultCacheManager cacheManager = createCacheManager();
            EntityCacheProperties properties = createEntityCacheProperties();
            EntityCacheManager manager = new EntityCacheManager(cacheManager, properties);

            EntityCache<TestEntity> cache = manager.getCache(TestEntity.class);
            TestEntity entity = new TestEntity();
            cache.put("test-id", entity);

            assertThatCode(() -> manager.evict(TestEntity.class, "test-id"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("evictAll 测试")
    class EvictAllTests {

        @Test
        @DisplayName("失效所有缓存不应该抛出异常")
        void shouldNotThrowWhenEvictingAll() {
            DefaultCacheManager cacheManager = createCacheManager();
            EntityCacheProperties properties = createEntityCacheProperties();
            EntityCacheManager manager = new EntityCacheManager(cacheManager, properties);

            assertThatCode(() -> manager.evictAll(TestEntity.class))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("evictAllCaches 测试")
    class EvictAllCachesTests {

        @Test
        @DisplayName("应该失效所有实体的缓存")
        void shouldEvictAllEntityCaches() {
            DefaultCacheManager cacheManager = createCacheManager();
            EntityCacheProperties properties = createEntityCacheProperties();
            EntityCacheManager manager = new EntityCacheManager(cacheManager, properties);

            manager.getCache(TestEntity.class);
            manager.getCache(AnotherEntity.class);

            assertThatCode(() -> manager.evictAllCaches())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("clearAll 测试")
    class ClearAllTests {

        @Test
        @DisplayName("应该清空所有缓存")
        void shouldClearAllCaches() {
            DefaultCacheManager cacheManager = createCacheManager();
            EntityCacheProperties properties = createEntityCacheProperties();
            EntityCacheManager manager = new EntityCacheManager(cacheManager, properties);

            manager.getCache(TestEntity.class);

            assertThatCode(() -> manager.clearAll())
                    .doesNotThrowAnyException();
        }
    }

    // Test entities
    static class TestEntity {
        private String id;
        private String name;

        public TestEntity() {}
    }

    static class AnotherEntity {
        private String id;

        public AnotherEntity() {}
    }
}