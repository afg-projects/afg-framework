package io.github.afgprojects.framework.data.jdbc.cache;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.core.cache.CacheProperties;
import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import io.github.afgprojects.framework.data.jdbc.JdbcEntityProxy;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 实体二级缓存测试
 */
@DisplayName("实体二级缓存测试")
class EntityCacheTest {

    private DataSource dataSource;
    private JdbcDataManager dataManager;
    private EntityCacheManager cacheManager;
    private JdbcEntityProxy<CachedUser> userProxy;

    @BeforeEach
    void setUp() {
        dataSource = createDataSource();

        // 创建缓存管理器
        CacheProperties cacheProperties = new CacheProperties();
        cacheProperties.setEnabled(true);
        DefaultCacheManager defaultCacheManager = new DefaultCacheManager(cacheProperties);

        // 创建实体缓存管理器
        EntityCacheProperties entityCacheProperties = new EntityCacheProperties();
        entityCacheProperties.setEnabled(true);
        entityCacheProperties.setTtl(60000); // 60秒过期
        entityCacheProperties.setMaxSize(100);
        entityCacheProperties.setCacheNull(true);
        cacheManager = new EntityCacheManager(defaultCacheManager, entityCacheProperties);

        // 创建数据管理器并设置缓存
        dataManager = new JdbcDataManager(dataSource);
        dataManager.setCacheManager(cacheManager);

        createTestTable();
        userProxy = (JdbcEntityProxy<CachedUser>) dataManager.entity(CachedUser.class);
    }

    @AfterEach
    void tearDown() {
        dropTestTable();
        if (cacheManager != null) {
            cacheManager.clearAll();
        }
    }

    @Nested
    @DisplayName("findById 缓存测试")
    class FindByIdCacheTests {

        @Test
        @DisplayName("首次查询应从数据库获取并缓存")
        void shouldCacheOnFirstFindById() {
            // Given - 插入测试数据
            CachedUser user = new CachedUser();
            user.setName("张三");
            user.setEmail("zhangsan@example.com");
            user = userProxy.insert(user);

            // When - 首次查询
            Optional<CachedUser> firstResult = userProxy.findById(user.getId());

            // Then - 结果正确且缓存存在
            assertThat(firstResult).isPresent();
            assertThat(firstResult.get().getName()).isEqualTo("张三");

            // 验证缓存中已存在
            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            assertThat(cache.containsKey(user.getId())).isTrue();
            assertThat(cache.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("第二次查询应从缓存获取")
        void shouldGetFromCacheOnSecondFindById() {
            // Given - 插入测试数据
            CachedUser user = new CachedUser();
            user.setName("李四");
            user.setEmail("lisi@example.com");
            user = userProxy.insert(user);

            // 首次查询（从数据库获取并缓存）
            userProxy.findById(user.getId());

            // 直接更新数据库（绕过缓存失效）
            updateDatabaseDirectly(user.getId(), "数据库更新");

            // When - 第二次查询（应从缓存获取）
            Optional<CachedUser> cachedResult = userProxy.findById(user.getId());

            // Then - 返回的是缓存中的旧值，而非数据库更新后的值
            assertThat(cachedResult).isPresent();
            assertThat(cachedResult.get().getName()).isEqualTo("李四"); // 缓存中的旧值

            // 验证数据库中的值已更新
            Optional<CachedUser> dbResult = findByIdDirectly(user.getId());
            assertThat(dbResult).isPresent();
            assertThat(dbResult.get().getName()).isEqualTo("数据库更新");
        }

        @Test
        @DisplayName("更新后应失效缓存")
        void shouldEvictCacheAfterUpdate() {
            // Given - 插入并缓存
            CachedUser user = new CachedUser();
            user.setName("王五");
            user.setEmail("wangwu@example.com");
            user = userProxy.insert(user);
            userProxy.findById(user.getId()); // 缓存

            // When - 更新实体
            user.setName("王五更新");
            userProxy.update(user);

            // Then - 缓存已被失效
            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            assertThat(cache.containsKey(user.getId())).isFalse();

            // 再次查询应从数据库获取新值
            Optional<CachedUser> result = userProxy.findById(user.getId());
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("王五更新");
        }

        @Test
        @DisplayName("删除后应失效缓存")
        void shouldEvictCacheAfterDelete() {
            // Given - 插入并缓存
            CachedUser user = new CachedUser();
            user.setName("赵六");
            user.setEmail("zhaoliu@example.com");
            user = userProxy.insert(user);
            userProxy.findById(user.getId()); // 缓存

            // 验证缓存存在
            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            assertThat(cache.containsKey(user.getId())).isTrue();

            // When - 删除实体
            userProxy.deleteById(user.getId());

            // Then - 缓存已被失效
            assertThat(cache.containsKey(user.getId())).isFalse();

            // 再次查询应返回空
            Optional<CachedUser> result = userProxy.findById(user.getId());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("查询不存在的实体应缓存 null 标记（防穿透）")
        void shouldCacheNullMarkerForNonExistentEntity() {
            // Given
            Long nonExistentId = 99999L;

            // When - 查询不存在的实体
            Optional<CachedUser> result = userProxy.findById(nonExistentId);

            // Then - 返回空，但缓存中存在 null 标记
            assertThat(result).isEmpty();

            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            assertThat(cache.containsKey(nonExistentId)).isTrue(); // null 标记存在
            assertThat(cache.get(nonExistentId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("缓存配置测试")
    class CacheConfigTests {

        @Test
        @DisplayName("应正确应用 TTL 配置")
        void shouldApplyTtlConfig() {
            // Given
            EntityCacheProperties props = new EntityCacheProperties();
            props.setEnabled(true);
            props.setTtl(5000); // 5秒

            // 验证配置
            assertThat(props.getTtl()).isEqualTo(5000);
        }

        @Test
        @DisplayName("应正确应用最大容量配置")
        void shouldApplyMaxSizeConfig() {
            // Given
            EntityCacheProperties props = new EntityCacheProperties();
            props.setEnabled(true);
            props.setMaxSize(500);

            // 验证配置
            assertThat(props.getMaxSize()).isEqualTo(500);
        }

        @Test
        @DisplayName("禁用缓存时应直接查询数据库")
        void shouldQueryDatabaseWhenCacheDisabled() {
            // Given - 禁用缓存
            EntityCacheProperties disabledProps = new EntityCacheProperties();
            disabledProps.setEnabled(false);

            CacheProperties cacheProps = new CacheProperties();
            DefaultCacheManager defaultManager = new DefaultCacheManager(cacheProps);
            EntityCacheManager disabledCacheManager = new EntityCacheManager(defaultManager, disabledProps);

            JdbcDataManager dmWithoutCache = new JdbcDataManager(dataSource);
            dmWithoutCache.setCacheManager(disabledCacheManager);

            JdbcEntityProxy<CachedUser> proxyWithoutCache =
                (JdbcEntityProxy<CachedUser>) dmWithoutCache.entity(CachedUser.class);

            CachedUser user = new CachedUser();
            user.setName("测试用户");
            user.setEmail("test@example.com");
            user = proxyWithoutCache.insert(user);

            // When - 查询（缓存禁用）
            proxyWithoutCache.findById(user.getId());

            // Then - 缓存管理器显示禁用
            assertThat(disabledCacheManager.isEnabled()).isFalse();

            // 直接更新数据库
            updateDatabaseDirectly(user.getId(), "直接更新");

            // 再次查询应获取数据库新值（因为没有缓存）
            Optional<CachedUser> result = proxyWithoutCache.findById(user.getId());
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("直接更新");
        }
    }

    @Nested
    @DisplayName("EntityCacheManager 测试")
    class EntityCacheManagerTests {

        @Test
        @DisplayName("应正确管理多个实体类型的缓存")
        void shouldManageMultipleEntityCaches() {
            // When - 为两种实体类型创建缓存
            EntityCache<CachedUser> userCache = cacheManager.getCache(CachedUser.class);
            EntityCache<AnotherEntity> anotherCache = cacheManager.getCache(AnotherEntity.class);

            // Then - 两个缓存都存在且独立
            assertThat(userCache).isNotNull();
            assertThat(anotherCache).isNotNull();
            assertThat(userCache.getCacheName()).contains("CachedUser");
            assertThat(anotherCache.getCacheName()).contains("AnotherEntity");
        }

        @Test
        @DisplayName("evictAll 应失效所有缓存")
        void shouldEvictAllCaches() {
            // Given - 插入并缓存多个实体
            CachedUser u1 = new CachedUser();
            u1.setName("用户1");
            u1.setEmail("u1@example.com");
            u1 = userProxy.insert(u1);

            CachedUser u2 = new CachedUser();
            u2.setName("用户2");
            u2.setEmail("u2@example.com");
            u2 = userProxy.insert(u2);

            userProxy.findById(u1.getId());
            userProxy.findById(u2.getId());

            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            assertThat(cache.size()).isEqualTo(2);

            // When - 失效所有缓存
            cacheManager.evictAllCaches();

            // Then - 所有缓存已清空
            assertThat(cache.size()).isEqualTo(0);
        }
    }

    // ==================== CRUD 操作缓存测试 ====================

    @Nested
    @DisplayName("CRUD 操作缓存测试")
    class CrudCacheTests {

        @Test
        @DisplayName("insert 后 findById 应缓存实体")
        void shouldCacheAfterInsertAndFindById() {
            // Given
            CachedUser user = new CachedUser();
            user.setName("新用户");
            user.setEmail("new@example.com");

            // When - 插入实体
            CachedUser inserted = userProxy.insert(user);

            // Then - insert 不会自动缓存，需要 findById 才会缓存
            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            assertThat(cache.containsKey(inserted.getId())).isFalse();

            // When - findById 后才会缓存
            userProxy.findById(inserted.getId());

            // Then - 实体应被缓存
            assertThat(cache.containsKey(inserted.getId())).isTrue();
        }

        @Test
        @DisplayName("save 新实体后 findById 应缓存")
        void shouldCacheAfterSaveNewAndFindById() {
            // Given - 无 ID 的新实体
            CachedUser user = new CachedUser();
            user.setName("save新用户");
            user.setEmail("save@example.com");

            // When
            CachedUser saved = userProxy.save(user);

            // Then - save 不会自动缓存
            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            assertThat(cache.containsKey(saved.getId())).isFalse();

            // When - findById 后才会缓存
            userProxy.findById(saved.getId());

            // Then
            assertThat(cache.containsKey(saved.getId())).isTrue();
        }

        @Test
        @DisplayName("save 已有实体应失效旧缓存")
        void shouldEvictCacheAfterSaveExisting() {
            // Given - 插入并缓存
            CachedUser user = new CachedUser();
            user.setName("原始名称");
            user.setEmail("original@example.com");
            user = userProxy.insert(user);

            // 首次查询缓存
            userProxy.findById(user.getId());

            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            assertThat(cache.containsKey(user.getId())).isTrue();

            // When - 更新实体
            user.setName("更新名称");
            userProxy.save(user);

            // Then - 缓存应被失效
            assertThat(cache.containsKey(user.getId())).isFalse();
        }

        @Test
        @DisplayName("updateAll 批量更新应失效所有缓存")
        void shouldEvictAfterUpdateAll() {
            // Given - 插入并缓存多个实体
            CachedUser u1 = new CachedUser();
            u1.setName("更新前1");
            u1.setEmail("before1@example.com");
            u1 = userProxy.insert(u1);

            CachedUser u2 = new CachedUser();
            u2.setName("更新前2");
            u2.setEmail("before2@example.com");
            u2 = userProxy.insert(u2);

            // 缓存
            userProxy.findById(u1.getId());
            userProxy.findById(u2.getId());

            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            assertThat(cache.size()).isEqualTo(2);

            // When - 批量更新
            u1.setName("更新后1");
            u2.setName("更新后2");
            userProxy.updateAll(java.util.List.of(u1, u2));

            // Then - 缓存应被失效
            assertThat(cache.containsKey(u1.getId())).isFalse();
            assertThat(cache.containsKey(u2.getId())).isFalse();
        }

        @Test
        @DisplayName("deleteAll 批量删除应失效所有缓存")
        void shouldEvictAfterDeleteAll() {
            // Given - 插入并缓存多个实体
            CachedUser u1 = new CachedUser();
            u1.setName("删除1");
            u1.setEmail("delete1@example.com");
            u1 = userProxy.insert(u1);

            CachedUser u2 = new CachedUser();
            u2.setName("删除2");
            u2.setEmail("delete2@example.com");
            u2 = userProxy.insert(u2);

            // 缓存
            userProxy.findById(u1.getId());
            userProxy.findById(u2.getId());

            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            assertThat(cache.size()).isEqualTo(2);

            // When - 批量删除
            userProxy.deleteAll(java.util.List.of(u1, u2));

            // Then - 缓存应被失效
            assertThat(cache.containsKey(u1.getId())).isFalse();
            assertThat(cache.containsKey(u2.getId())).isFalse();
        }

        @Test
        @DisplayName("deleteAllById 批量删除应失效所有缓存")
        void shouldEvictAfterDeleteAllById() {
            // Given - 插入并缓存多个实体
            CachedUser u1 = new CachedUser();
            u1.setName("按ID删除1");
            u1.setEmail("delete-by-id1@example.com");
            u1 = userProxy.insert(u1);

            CachedUser u2 = new CachedUser();
            u2.setName("按ID删除2");
            u2.setEmail("delete-by-id2@example.com");
            u2 = userProxy.insert(u2);

            // 缓存
            userProxy.findById(u1.getId());
            userProxy.findById(u2.getId());

            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            assertThat(cache.size()).isEqualTo(2);

            // When - 批量按ID删除
            userProxy.deleteAllById(java.util.List.of(u1.getId(), u2.getId()));

            // Then - 缓存应被失效
            assertThat(cache.containsKey(u1.getId())).isFalse();
            assertThat(cache.containsKey(u2.getId())).isFalse();
        }
    }

    // ==================== 条件操作缓存测试 ====================

    @Nested
    @DisplayName("条件操作缓存测试")
    class ConditionCacheTests {

        @Test
        @DisplayName("条件更新应失效缓存")
        void shouldEvictAfterConditionUpdate() {
            // Given - 插入并缓存
            CachedUser user = new CachedUser();
            user.setName("条件更新前");
            user.setEmail("condition@example.com");
            user = userProxy.insert(user);
            userProxy.findById(user.getId());

            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            assertThat(cache.containsKey(user.getId())).isTrue();

            // When - 条件更新（注意：条件更新可能不会自动失效缓存，取决于实现）
            long updated = userProxy.updateAll(
                Conditions.eq("id", user.getId()),
                java.util.Map.of("name", "条件更新后")
            );

            // Then - 如果更新成功，缓存应被失效
            if (updated > 0) {
                assertThat(cache.containsKey(user.getId())).isFalse();
            }
        }

        @Test
        @DisplayName("条件删除应失效缓存")
        void shouldEvictAfterConditionDelete() {
            // Given - 插入并缓存
            CachedUser user = new CachedUser();
            user.setName("条件删除");
            user.setEmail("condition-delete@example.com");
            user = userProxy.insert(user);
            userProxy.findById(user.getId());

            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            assertThat(cache.containsKey(user.getId())).isTrue();

            // When - 条件删除（注意：条件删除可能不会自动失效缓存，取决于实现）
            long deleted = userProxy.deleteAll(Conditions.eq("id", user.getId()));

            // Then - 如果删除成功，缓存应被失效
            if (deleted > 0) {
                assertThat(cache.containsKey(user.getId())).isFalse();
            }
        }
    }

    // ==================== EntityCache 接口完整测试 ====================

    @Nested
    @DisplayName("EntityCache 接口完整测试")
    class EntityCacheInterfaceTests {

        @Test
        @DisplayName("put 应正确缓存实体")
        void shouldPutEntity() {
            // Given
            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            CachedUser user = new CachedUser();
            user.setId(100L);
            user.setName("直接缓存");
            user.setEmail("direct@example.com");

            // When
            cache.put(100L, user);

            // Then
            assertThat(cache.containsKey(100L)).isTrue();
            assertThat(cache.get(100L)).isPresent();
            assertThat(cache.get(100L).get().getName()).isEqualTo("直接缓存");
        }

        @Test
        @DisplayName("put 带 TTL 应正确缓存实体")
        void shouldPutEntityWithTtl() {
            // Given
            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            CachedUser user = new CachedUser();
            user.setId(200L);
            user.setName("TTL缓存");
            user.setEmail("ttl@example.com");

            // When
            cache.put(200L, user, 5000); // 5秒 TTL

            // Then
            assertThat(cache.containsKey(200L)).isTrue();
            assertThat(cache.get(200L)).isPresent();
        }

        @Test
        @DisplayName("evict 应正确失效单个缓存")
        void shouldEvictSingleCache() {
            // Given
            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            CachedUser user = new CachedUser();
            user.setId(300L);
            user.setName("失效测试");
            cache.put(300L, user);
            assertThat(cache.containsKey(300L)).isTrue();

            // When
            cache.evict(300L);

            // Then
            assertThat(cache.containsKey(300L)).isFalse();
        }

        @Test
        @DisplayName("evictAll 应正确失效所有缓存")
        void shouldEvictAllCache() {
            // Given
            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            cache.put(1L, new CachedUser(1L, "用户1", "u1@example.com"));
            cache.put(2L, new CachedUser(2L, "用户2", "u2@example.com"));
            assertThat(cache.size()).isEqualTo(2);

            // When
            cache.evictAll();

            // Then
            assertThat(cache.size()).isEqualTo(0);
        }

        @Test
        @DisplayName("clear 应正确清空缓存")
        void shouldClearCache() {
            // Given
            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            cache.put(1L, new CachedUser(1L, "清空测试", "clear@example.com"));
            assertThat(cache.size()).isEqualTo(1);

            // When
            cache.clear();

            // Then
            assertThat(cache.size()).isEqualTo(0);
        }

        @Test
        @DisplayName("getEntityClass 应返回正确的实体类型")
        void shouldReturnCorrectEntityClass() {
            // Given
            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);

            // When & Then
            assertThat(cache.getEntityClass()).isEqualTo(CachedUser.class);
        }

        @Test
        @DisplayName("getCacheName 应返回正确的缓存名称")
        void shouldReturnCorrectCacheName() {
            // Given
            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);

            // When & Then
            assertThat(cache.getCacheName()).contains("CachedUser");
        }

        @Test
        @DisplayName("size 应返回正确的缓存大小")
        void shouldReturnCorrectSize() {
            // Given
            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            cache.put(1L, new CachedUser(1L, "大小1", "size1@example.com"));
            cache.put(2L, new CachedUser(2L, "大小2", "size2@example.com"));

            // When & Then
            assertThat(cache.size()).isEqualTo(2);
        }
    }

    // ==================== EntityCacheManager 完整测试 ====================

    @Nested
    @DisplayName("EntityCacheManager 完整测试")
    class EntityCacheManagerCompleteTests {

        @Test
        @DisplayName("getCacheIfPresent 缓存不存在时应返回 null")
        void shouldReturnNullWhenCacheNotPresent() {
            // Given - 新的实体类型，未创建缓存
            // When
            EntityCache<AnotherEntity> cache = cacheManager.getCacheIfPresent(AnotherEntity.class);

            // Then
            assertThat(cache).isNull();
        }

        @Test
        @DisplayName("getCacheIfPresent 缓存存在时应返回缓存")
        void shouldReturnCacheWhenPresent() {
            // Given - 先获取缓存创建它
            cacheManager.getCache(CachedUser.class);

            // When
            EntityCache<CachedUser> cache = cacheManager.getCacheIfPresent(CachedUser.class);

            // Then
            assertThat(cache).isNotNull();
        }

        @Test
        @DisplayName("evict 应正确失效指定实体的缓存")
        void shouldEvictSpecificEntity() {
            // Given
            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            cache.put(1L, new CachedUser(1L, "失效指定", "evict-specific@example.com"));
            assertThat(cache.containsKey(1L)).isTrue();

            // When
            cacheManager.evict(CachedUser.class, 1L);

            // Then
            assertThat(cache.containsKey(1L)).isFalse();
        }

        @Test
        @DisplayName("evictAll 应正确失效指定实体类型的所有缓存")
        void shouldEvictAllForEntityType() {
            // Given
            EntityCache<CachedUser> cache = cacheManager.getCache(CachedUser.class);
            cache.put(1L, new CachedUser(1L, "失效全部1", "evict-all1@example.com"));
            cache.put(2L, new CachedUser(2L, "失效全部2", "evict-all2@example.com"));
            assertThat(cache.size()).isEqualTo(2);

            // When
            cacheManager.evictAll(CachedUser.class);

            // Then
            assertThat(cache.size()).isEqualTo(0);
        }

        @Test
        @DisplayName("getProperties 应返回正确的配置")
        void shouldReturnCorrectProperties() {
            // When
            EntityCacheProperties props = cacheManager.getProperties();

            // Then
            assertThat(props).isNotNull();
            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getTtl()).isEqualTo(60000);
            assertThat(props.getMaxSize()).isEqualTo(100);
        }
    }

    // ==================== 辅助方法 ====================

    private DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:cachetest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void createTestTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE cached_user (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(200)
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test table", e);
        }
    }

    private void dropTestTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS cached_user");
        } catch (Exception ignored) {
        }
    }

    private void createAnotherTestTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS another_entity (
                    id SERIAL PRIMARY KEY,
                    value VARCHAR(100)
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create another test table", e);
        }
    }

    private void dropAnotherTestTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS another_entity");
        } catch (Exception ignored) {
        }
    }

    private void updateDatabaseDirectly(Long id, String newName) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE cached_user SET name = '" + newName + "' WHERE id = " + id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update database directly", e);
        }
    }

    private Optional<CachedUser> findByIdDirectly(Long id) {
        try (Connection conn = dataSource.getConnection();
             var pstmt = conn.prepareStatement("SELECT * FROM cached_user WHERE id = ?")) {
            pstmt.setLong(1, id);
            var rs = pstmt.executeQuery();
            if (rs.next()) {
                CachedUser user = new CachedUser();
                user.setId(rs.getLong("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                return Optional.of(user);
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Failed to query database directly", e);
        }
    }

    /**
     * 测试用户实体
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CachedUser {
        private Long id;
        private String name;
        private String email;
    }

    /**
     * 另一个测试实体
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnotherEntity {
        private Long id;
        private String value;
    }
}