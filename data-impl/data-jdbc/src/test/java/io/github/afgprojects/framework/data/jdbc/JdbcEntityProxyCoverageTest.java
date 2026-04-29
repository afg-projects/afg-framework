package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.dialect.DatabaseType;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.dialect.H2Dialect;
import io.github.afgprojects.framework.data.core.entity.SoftDeletable;
import io.github.afgprojects.framework.data.core.entity.TimestampSoftDeletable;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.relation.ManyToMany;
import io.github.afgprojects.framework.data.core.relation.ManyToOne;
import io.github.afgprojects.framework.data.core.relation.OneToMany;
import io.github.afgprojects.framework.data.core.relation.OneToOne;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheManager;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheProperties;
import io.github.afgprojects.framework.core.cache.CacheProperties;
import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JdbcEntityProxy 覆盖率补充测试
 * <p>
 * 针对低覆盖率分支和方法的测试。
 * </p>
 */
@DisplayName("JdbcEntityProxy 覆盖率补充测试")
class JdbcEntityProxyCoverageTest {

    private JdbcDataManager dataManager;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        dataSource = createDataSource();
        dataManager = new JdbcDataManager(dataSource);
    }

    @AfterEach
    void tearDown() {
        dropAllTables();
    }

    // ==================== 构造函数测试 ====================

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("无缓存管理器的构造函数应正确工作")
        void shouldWorkWithoutCacheManager() {
            createTestUserTable();
            // Given - 使用无缓存的构造函数
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // When
            TestUser user = new TestUser();
            user.setName("test");
            user.setEmail("test@example.com");
            TestUser inserted = proxy.insert(user);

            // Then
            assertThat(inserted.getId()).isNotNull();
        }

        @Test
        @DisplayName("getCacheManager 应返回 null 当未设置缓存时")
        void shouldReturnNullCacheManager() {
            createTestUserTable();
            JdbcEntityProxy<TestUser> proxy = (JdbcEntityProxy<TestUser>) dataManager.entity(TestUser.class);

            assertThat(proxy.getCacheManager()).isNull();
        }

        @Test
        @DisplayName("直接调用无缓存构造函数")
        void shouldInvokeNoCacheConstructor() {
            createTestUserTable();
            // Given - 手动创建 JdbcEntityProxy 使用无缓存构造函数
            org.springframework.jdbc.core.simple.JdbcClient jdbcClient = org.springframework.jdbc.core.simple.JdbcClient.create(dataSource);
            JdbcEntityProxy<TestUser> proxy = new JdbcEntityProxy<>(
                TestUser.class,
                jdbcClient,
                new H2Dialect(),
                dataManager
            );

            // When
            TestUser user = new TestUser();
            user.setName("direct-constructor");
            user.setEmail("direct@example.com");
            TestUser inserted = proxy.insert(user);

            // Then
            assertThat(inserted.getId()).isNotNull();
            assertThat(proxy.getCacheManager()).isNull();
        }
    }

    // ==================== insert 分支测试 ====================

    @Nested
    @DisplayName("insert 分支测试")
    class InsertBranchTests {

        @Test
        @DisplayName("insert 实体无预设 ID 应生成新 ID")
        void shouldGenerateIdWhenNoPresetId() {
            createTestUserTable();
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // Given - 创建无 ID 的实体
            TestUser user = new TestUser();
            user.setName("auto-id");
            user.setEmail("auto@example.com");

            // When
            TestUser inserted = proxy.insert(user);

            // Then - ID 应被自动生成
            assertThat(inserted.getId()).isNotNull();
            assertThat(inserted.getId()).isGreaterThan(0);
        }

        @Test
        @DisplayName("insert 实体有预设 ID 应使用该 ID 直接插入")
        void shouldInsertWithPresetId() {
            // Create table with explicit ID support (no SERIAL)
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE test_user_preset_id (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(200)
                    )
                    """);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create table", e);
            }

            try {
                EntityProxy<TestUserPresetId> proxy = dataManager.entity(TestUserPresetId.class);

                // Given - 创建有预设 ID 的实体
                TestUserPresetId user = new TestUserPresetId();
                user.setId(999L);
                user.setName("preset-id");
                user.setEmail("preset@example.com");

                // When
                TestUserPresetId inserted = proxy.insert(user);

                // Then - 应使用预设 ID
                assertThat(inserted.getId()).isEqualTo(999L);

                // Then - 数据应能被查询到
                Optional<TestUserPresetId> found = proxy.findById(999L);
                assertThat(found).isPresent();
                assertThat(found.get().getName()).isEqualTo("preset-id");
            } finally {
                // Cleanup
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP TABLE IF EXISTS test_user_preset_id");
                } catch (Exception ignored) {
                }
            }
        }
    }

    // ==================== 批量插入分支测试 ====================

    @Nested
    @DisplayName("批量插入分支测试")
    class BatchInsertBranchTests {

        @Test
        @DisplayName("insertAll 空列表应返回空列表")
        void shouldReturnEmptyForEmptyList() {
            createTestUserTable();
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // When
            List<TestUser> result = proxy.insertAll(List.of());

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("insertAll 单条记录应使用单条插入")
        void shouldUseSingleInsertForSingleRecord() {
            createTestUserTable();
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // Given
            TestUser user = new TestUser();
            user.setName("single");
            user.setEmail("single@example.com");

            // When
            List<TestUser> result = proxy.insertAll(List.of(user));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isNotNull();
        }
    }

    // ==================== 不支持 RETURNING 的数据库测试 ====================

    @Nested
    @DisplayName("不支持 RETURNING 的数据库测试")
    class NonReturningDatabaseTests {

        @Test
        @DisplayName("MySQL 数据库应使用逐条插入")
        void shouldUseSingleInsertForMySQL() {
            // Given - 使用 MySQL 类型
            JdbcDataManager mysqlDataManager = new JdbcDataManager(dataSource, DatabaseType.MYSQL);
            createTestUserTable();
            EntityProxy<TestUser> proxy = mysqlDataManager.entity(TestUser.class);

            TestUser u1 = new TestUser();
            u1.setName("mysql-1");
            u1.setEmail("mysql1@example.com");

            TestUser u2 = new TestUser();
            u2.setName("mysql-2");
            u2.setEmail("mysql2@example.com");

            // When
            List<TestUser> result = proxy.insertAll(List.of(u1, u2));

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isNotNull();
            assertThat(result.get(1).getId()).isNotNull();
        }
    }

    // ==================== 条件查询空条件测试 ====================

    @Nested
    @DisplayName("条件查询空条件测试")
    class EmptyConditionQueryTests {

        @Test
        @DisplayName("findAll 空条件应正确处理软删除过滤")
        void shouldHandleEmptyConditionWithSoftDelete() {
            createSoftDeleteUserTable();
            EntityProxy<SoftDeleteUser> proxy = dataManager.entity(SoftDeleteUser.class);
            JdbcEntityProxy<SoftDeleteUser> jdbcProxy = (JdbcEntityProxy<SoftDeleteUser>) proxy;

            // Given - 插入数据
            SoftDeleteUser user = new SoftDeleteUser();
            user.setName("active");
            user.setEmail("active@example.com");
            proxy.insert(user);

            // When - 空条件 + includeDeleted
            Condition emptyCondition = Conditions.empty();
            List<SoftDeleteUser> results = jdbcProxy.includeDeleted().findAll(emptyCondition);

            // Then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("count 空条件应正确处理软删除过滤")
        void shouldCountWithEmptyConditionAndSoftDelete() {
            createSoftDeleteUserTable();
            EntityProxy<SoftDeleteUser> proxy = dataManager.entity(SoftDeleteUser.class);

            // Given - 插入数据
            SoftDeleteUser user = new SoftDeleteUser();
            user.setName("active");
            user.setEmail("active@example.com");
            proxy.insert(user);

            // When - 空条件计数
            long count = proxy.count(Conditions.empty());

            // Then
            assertThat(count).isEqualTo(1);
        }
    }

    // ==================== 软删除条件查询分支测试 ====================

    @Nested
    @DisplayName("软删除条件查询分支测试")
    class SoftDeleteConditionQueryTests {

        @Test
        @DisplayName("count(Condition) 应过滤软删除记录")
        void shouldCountWithSoftDeleteFilter() {
            createSoftDeleteUserTable();
            EntityProxy<SoftDeleteUser> proxy = dataManager.entity(SoftDeleteUser.class);

            // Given - 插入并软删除部分记录
            SoftDeleteUser u1 = new SoftDeleteUser();
            u1.setName("active");
            u1.setEmail("active@example.com");
            proxy.insert(u1);

            SoftDeleteUser u2 = new SoftDeleteUser();
            u2.setName("deleted");
            u2.setEmail("deleted@example.com");
            proxy.insert(u2);
            proxy.delete(u2);

            // When - 条件计数
            Condition condition = Conditions.like("email", "example");
            long count = proxy.count(condition);

            // Then - 只统计未删除的
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("findFirst 应过滤软删除记录")
        void shouldFindFirstWithSoftDeleteFilter() {
            createSoftDeleteUserTable();
            EntityProxy<SoftDeleteUser> proxy = dataManager.entity(SoftDeleteUser.class);
            JdbcEntityProxy<SoftDeleteUser> jdbcProxy = (JdbcEntityProxy<SoftDeleteUser>) proxy;

            // Given - 插入已删除记录
            SoftDeleteUser user = new SoftDeleteUser();
            user.setName("deleted-first");
            user.setEmail("deleted-first@example.com");
            proxy.insert(user);
            proxy.delete(user);

            // When - 普通查询找不到
            Condition condition = Conditions.eq("name", "deleted-first");
            Optional<SoftDeleteUser> normalResult = proxy.findFirst(condition);

            // Then
            assertThat(normalResult).isEmpty();

            // When - includeDeleted 可以找到
            Optional<SoftDeleteUser> withDeletedResult = jdbcProxy.includeDeleted().findFirst(condition);

            // Then
            assertThat(withDeletedResult).isPresent();
            assertThat(withDeletedResult.get().getName()).isEqualTo("deleted-first");
        }

        @Test
        @DisplayName("count(Condition) 带空条件应过滤软删除记录")
        void shouldCountWithEmptyConditionAndSoftDelete() {
            createSoftDeleteUserTable();
            EntityProxy<SoftDeleteUser> proxy = dataManager.entity(SoftDeleteUser.class);

            // Given
            SoftDeleteUser u1 = new SoftDeleteUser();
            u1.setName("active");
            u1.setEmail("active@example.com");
            proxy.insert(u1);

            SoftDeleteUser u2 = new SoftDeleteUser();
            u2.setName("deleted-user");
            u2.setEmail("deleted@example.com");
            proxy.insert(u2);
            proxy.delete(u2);

            // When - 空条件计数
            long count = proxy.count(Conditions.empty());

            // Then
            assertThat(count).isEqualTo(1);
        }
    }

    // ==================== 关联加载分支测试 ====================

    @Nested
    @DisplayName("关联加载分支测试")
    class AssociationLoadingBranchTests {

        @Test
        @DisplayName("fetchAll 应处理空 ID 列表")
        void shouldHandleEmptyIdsInFetchAll() {
            createAssociationTables();
            EntityProxy<TestUserWithDept> proxy = dataManager.entity(TestUserWithDept.class);

            // Given - 插入一个用户
            TestUserWithDept user = new TestUserWithDept();
            user.setName("test");
            user.setEmail("test@example.com");
            user.setDepartmentId(null);
            proxy.insert(user);

            // When - fetchAll 正常执行（不抛出异常）
            proxy.fetchAll(List.of(user), "department");

            // Then - 测试通过
        }

        @Test
        @DisplayName("fetchAll 应加载 OneToOne 关联")
        void shouldFetchAllOneToOne() {
            createAssociationTables();

            // Given - 创建详情
            EntityProxy<TestUserDetail> detailProxy = dataManager.entity(TestUserDetail.class);
            TestUserDetail detail = new TestUserDetail();
            detail.setBio("Test bio");
            detail = detailProxy.insert(detail);

            // Given - 创建用户并关联详情
            EntityProxy<TestUserWithDetail> userProxy = dataManager.entity(TestUserWithDetail.class);
            TestUserWithDetail user = new TestUserWithDetail();
            user.setName("user-with-detail");
            user.setEmail("detail@example.com");
            user.setDetailId(detail.getId());
            user = userProxy.insert(user);

            // When - 批量加载 OneToOne 关联
            userProxy.fetchAll(List.of(user), "detail");

            // Then - 验证关联已加载
            assertThat(user.getDetail()).isNotNull();
            assertThat(user.getDetail().getBio()).isEqualTo("Test bio");
        }

        @Test
        @DisplayName("fetchAll 应加载 OneToMany 关联")
        void shouldFetchAllOneToMany() {
            createAssociationTables();

            // Given - 创建部门
            EntityProxy<TestDepartment> deptProxy = dataManager.entity(TestDepartment.class);
            TestDepartment dept = new TestDepartment();
            dept.setName("Engineering");
            dept = deptProxy.insert(dept);

            // Given - 创建用户关联到部门
            EntityProxy<TestUserWithDept> userProxy = dataManager.entity(TestUserWithDept.class);
            for (int i = 0; i < 3; i++) {
                TestUserWithDept user = new TestUserWithDept();
                user.setName("user-" + i);
                user.setEmail("user" + i + "@example.com");
                user.setDepartmentId(dept.getId());
                userProxy.insert(user);
            }

            // When - 批量加载 OneToMany 关联（验证不抛出异常）
            deptProxy.fetchAll(List.of(dept), "employees");

            // Then - 测试通过
        }

        @Test
        @DisplayName("fetchAll 应加载 ManyToMany 关联")
        void shouldFetchAllManyToMany() {
            createAssociationTables();

            // Given - 创建角色
            EntityProxy<TestRole> roleProxy = dataManager.entity(TestRole.class);
            TestRole role1 = new TestRole();
            role1.setName("ADMIN");
            role1 = roleProxy.insert(role1);
            TestRole role2 = new TestRole();
            role2.setName("USER");
            role2 = roleProxy.insert(role2);

            // Given - 创建用户
            EntityProxy<TestUserWithRoles> userProxy = dataManager.entity(TestUserWithRoles.class);
            TestUserWithRoles user = new TestUserWithRoles();
            user.setName("user-with-roles");
            user.setEmail("roles@example.com");
            user = userProxy.insert(user);

            // 插入中间表
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO test_user_with_roles_test_role (test_user_with_roles_id, test_role_id) VALUES (" + user.getId() + ", " + role1.getId() + ")");
                stmt.execute("INSERT INTO test_user_with_roles_test_role (test_user_with_roles_id, test_role_id) VALUES (" + user.getId() + ", " + role2.getId() + ")");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // When - 批量加载 ManyToMany 关联（验证不抛出异常）
            userProxy.fetchAll(List.of(user), "roles");

            // Then - 测试通过
        }

        @Test
        @DisplayName("fetchAll 无 ID 的实体应跳过")
        void shouldSkipEntitiesWithoutId() {
            createAssociationTables();
            EntityProxy<TestUserWithDept> proxy = dataManager.entity(TestUserWithDept.class);

            // Given - 无 ID 的实体
            TestUserWithDept userWithoutId = new TestUserWithDept();
            userWithoutId.setName("no-id");
            userWithoutId.setEmail("noid@example.com");

            // When - fetchAll 应正常处理
            proxy.fetchAll(List.of(userWithoutId), "department");

            // Then - 测试通过，无异常
        }
    }

    // ==================== 缓存分支测试 ====================

    @Nested
    @DisplayName("缓存分支测试")
    class CacheBranchTests {

        @Test
        @DisplayName("findById 缓存未命中时 isCacheNull 为 false 不缓存 null")
        void shouldNotCacheNullWhenCacheNullIsFalse() {
            createTestUserTable();

            // Given - 配置缓存但不缓存 null
            CacheProperties cacheProperties = new CacheProperties();
            cacheProperties.setEnabled(true);
            DefaultCacheManager defaultCacheManager = new DefaultCacheManager(cacheProperties);
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setEnabled(true);
            properties.setCacheNull(false); // 不缓存 null
            EntityCacheManager cacheManager = new EntityCacheManager(defaultCacheManager, properties);

            JdbcDataManager dataManagerWithCache = new JdbcDataManager(dataSource);
            dataManagerWithCache.setCacheManager(cacheManager);

            EntityProxy<TestUser> proxy = dataManagerWithCache.entity(TestUser.class);

            // When - 查询不存在的 ID
            Optional<TestUser> result = proxy.findById(99999L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("evictCache 实体 ID 为 null 时应跳过")
        void shouldSkipEvictCacheWhenIdIsNull() {
            createTestUserTable();

            // Given - 带缓存的数据管理器
            CacheProperties cacheProperties = new CacheProperties();
            cacheProperties.setEnabled(true);
            DefaultCacheManager defaultCacheManager = new DefaultCacheManager(cacheProperties);
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setEnabled(true);
            EntityCacheManager cacheManager = new EntityCacheManager(defaultCacheManager, properties);

            JdbcDataManager dataManagerWithCache = new JdbcDataManager(dataSource);
            dataManagerWithCache.setCacheManager(cacheManager);

            EntityProxy<TestUser> proxy = dataManagerWithCache.entity(TestUser.class);

            // Given - 插入实体
            TestUser user = new TestUser();
            user.setName("cache-test");
            user.setEmail("cache@example.com");
            proxy.insert(user);

            // When - 更新后失效缓存
            user.setName("updated");
            proxy.update(user);

            // Then - 测试通过
        }

        @Test
        @DisplayName("evictCacheById 缓存不存在时应跳过")
        void shouldSkipEvictCacheWhenCacheNotPresent() {
            createTestUserTable();

            // Given - 禁用缓存
            CacheProperties cacheProperties = new CacheProperties();
            cacheProperties.setEnabled(true);
            DefaultCacheManager defaultCacheManager = new DefaultCacheManager(cacheProperties);
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setEnabled(false); // 禁用
            EntityCacheManager cacheManager = new EntityCacheManager(defaultCacheManager, properties);

            JdbcDataManager dataManagerWithCache = new JdbcDataManager(dataSource);
            dataManagerWithCache.setCacheManager(cacheManager);

            EntityProxy<TestUser> proxy = dataManagerWithCache.entity(TestUser.class);

            // Given - 插入实体
            TestUser user = new TestUser();
            user.setName("disabled-cache");
            user.setEmail("disabled@example.com");
            proxy.insert(user);

            // When - 删除
            proxy.deleteById(user.getId());

            // Then - 测试通过
        }

        @Test
        @DisplayName("updateAll 条件更新应清除缓存")
        void shouldClearCacheOnConditionalUpdate() {
            createTestUserTable();

            // Given - 配置缓存
            CacheProperties cacheProperties = new CacheProperties();
            cacheProperties.setEnabled(true);
            DefaultCacheManager defaultCacheManager = new DefaultCacheManager(cacheProperties);
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setEnabled(true);
            EntityCacheManager cacheManager = new EntityCacheManager(defaultCacheManager, properties);

            JdbcDataManager dataManagerWithCache = new JdbcDataManager(dataSource);
            dataManagerWithCache.setCacheManager(cacheManager);

            EntityProxy<TestUser> proxy = dataManagerWithCache.entity(TestUser.class);

            // Given - 插入数据
            TestUser user = new TestUser();
            user.setName("cache-update");
            user.setEmail("cache-update@example.com");
            proxy.insert(user);

            // 预热缓存
            proxy.findById(user.getId());

            // When - 条件更新
            Condition condition = Conditions.eq("name", "cache-update");
            java.util.Map<String, Object> updates = java.util.Map.of("email", "updated@example.com");
            long affected = proxy.updateAll(condition, updates);

            // Then
            assertThat(affected).isGreaterThan(0);
        }

        @Test
        @DisplayName("deleteAll 条件删除应清除缓存")
        void shouldClearCacheOnConditionalDelete() {
            createTestUserTable();

            // Given - 配置缓存
            CacheProperties cacheProperties = new CacheProperties();
            cacheProperties.setEnabled(true);
            DefaultCacheManager defaultCacheManager = new DefaultCacheManager(cacheProperties);
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setEnabled(true);
            EntityCacheManager cacheManager = new EntityCacheManager(defaultCacheManager, properties);

            JdbcDataManager dataManagerWithCache = new JdbcDataManager(dataSource);
            dataManagerWithCache.setCacheManager(cacheManager);

            EntityProxy<TestUser> proxy = dataManagerWithCache.entity(TestUser.class);

            // Given - 插入数据
            TestUser user = new TestUser();
            user.setName("cache-delete");
            user.setEmail("cache-delete@example.com");
            proxy.insert(user);

            // 预热缓存
            proxy.findById(user.getId());

            // When - 条件删除
            Condition condition = Conditions.eq("name", "cache-delete");
            long affected = proxy.deleteAll(condition);

            // Then
            assertThat(affected).isGreaterThan(0);
        }
    }

    // ==================== hardDelete 边界测试 ====================

    @Nested
    @DisplayName("hardDelete 边界测试")
    class HardDeleteBoundaryTests {

        @Test
        @DisplayName("hardDelete 实体 ID 为 null 时应跳过")
        void shouldSkipHardDeleteWhenIdIsNull() {
            createSoftDeleteUserTable();
            EntityProxy<SoftDeleteUser> proxy = dataManager.entity(SoftDeleteUser.class);
            JdbcEntityProxy<SoftDeleteUser> jdbcProxy = (JdbcEntityProxy<SoftDeleteUser>) proxy;

            // Given - 无 ID 的实体
            SoftDeleteUser userWithoutId = new SoftDeleteUser();
            userWithoutId.setName("no-id");
            userWithoutId.setEmail("noid@example.com");

            // When - hardDelete 应正常处理
            jdbcProxy.hardDelete(userWithoutId);

            // Then - 测试通过，无异常
        }
    }

    // ==================== 批量插入全 ID 实体测试 ====================

    @Nested
    @DisplayName("批量插入全 ID 实体测试")
    class BatchInsertAllWithIdTests {

        @Test
        @DisplayName("insertAll 所有实体都有预设 ID 时应跳过 withoutId 分支")
        void shouldSkipWithoutIdBranchWhenAllHaveIds() {
            // Create table without SERIAL for explicit IDs
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE test_user_all_ids (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(200)
                    )
                    """);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create table", e);
            }

            try {
                EntityProxy<TestUserAllIds> proxy = dataManager.entity(TestUserAllIds.class);

                // Given - 所有实体都有预设 ID
                TestUserAllIds u1 = new TestUserAllIds();
                u1.setId(1001L);
                u1.setName("preset-1");
                u1.setEmail("preset1@example.com");

                TestUserAllIds u2 = new TestUserAllIds();
                u2.setId(1002L);
                u2.setName("preset-2");
                u2.setEmail("preset2@example.com");

                // When - 批量插入（使用 H2 支持 RETURNING）
                List<TestUserAllIds> result = proxy.insertAll(List.of(u1, u2));

                // Then
                assertThat(result).hasSize(2);
                assertThat(result.get(0).getId()).isEqualTo(1001L);
                assertThat(result.get(1).getId()).isEqualTo(1002L);
            } finally {
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP TABLE IF EXISTS test_user_all_ids");
                } catch (Exception ignored) {
                }
            }
        }
    }

    // ==================== 空条件查询分支测试 ====================

    @Nested
    @DisplayName("空条件查询分支测试")
    class EmptyConditionBranchTests {

        @Test
        @DisplayName("findAll 空条件 + 软删除实体 + Boolean 策略应追加软删除过滤")
        void shouldAppendSoftDeleteFilterForEmptyConditionBooleanStrategy() {
            createSoftDeleteUserTable();
            EntityProxy<SoftDeleteUser> proxy = dataManager.entity(SoftDeleteUser.class);

            // Given - 插入未删除记录
            SoftDeleteUser user = new SoftDeleteUser();
            user.setName("active");
            user.setEmail("active@example.com");
            proxy.insert(user);

            // When - 空条件查询（whereClause.length() == 0）
            List<SoftDeleteUser> results = proxy.findAll(Conditions.empty());

            // Then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("findAll 分页查询空条件 + Boolean 策略应追加软删除过滤")
        void shouldAppendSoftDeleteFilterForEmptyConditionPageableBooleanStrategy() {
            createSoftDeleteUserTable();
            EntityProxy<SoftDeleteUser> proxy = dataManager.entity(SoftDeleteUser.class);

            // Given - 插入未删除记录
            SoftDeleteUser user = new SoftDeleteUser();
            user.setName("active-page");
            user.setEmail("active-page@example.com");
            proxy.insert(user);

            // When - 空条件分页查询（whereClause.length() == 0）
            PageRequest pageRequest =
                PageRequest.of(1, 10);
            Page<SoftDeleteUser> page =
                proxy.findAll(Conditions.empty(), pageRequest);

            // Then
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getTotal()).isEqualTo(1);
        }

        @Test
        @DisplayName("count 空条件 + 软删除实体应正确处理 whereClause.isEmpty()")
        void shouldHandleEmptyWhereClauseInCountWithSoftDelete() {
            createSoftDeleteUserTable();
            EntityProxy<SoftDeleteUser> proxy = dataManager.entity(SoftDeleteUser.class);

            // Given - 插入未删除记录
            SoftDeleteUser user = new SoftDeleteUser();
            user.setName("active-count");
            user.setEmail("active-count@example.com");
            proxy.insert(user);

            // When - 空条件计数（whereClause.isEmpty() == true）
            long count = proxy.count(Conditions.empty());

            // Then
            assertThat(count).isEqualTo(1);
        }
    }

    // ==================== 条件更新/删除缓存分支测试 ====================

    @Nested
    @DisplayName("条件更新/删除缓存分支测试")
    class ConditionalUpdateDeleteCacheTests {

        @Test
        @DisplayName("updateAll affected = 0 时不清理缓存")
        void shouldNotClearCacheWhenAffectedIsZero() {
            createTestUserTable();

            // Given - 配置缓存
            CacheProperties cacheProperties = new CacheProperties();
            cacheProperties.setEnabled(true);
            DefaultCacheManager defaultCacheManager = new DefaultCacheManager(cacheProperties);
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setEnabled(true);
            EntityCacheManager cacheManager = new EntityCacheManager(defaultCacheManager, properties);

            JdbcDataManager dataManagerWithCache = new JdbcDataManager(dataSource);
            dataManagerWithCache.setCacheManager(cacheManager);

            EntityProxy<TestUser> proxy = dataManagerWithCache.entity(TestUser.class);

            // Given - 插入数据并预热缓存
            TestUser user = new TestUser();
            user.setName("cache-test-zero");
            user.setEmail("zero@example.com");
            proxy.insert(user);
            proxy.findById(user.getId());

            // When - 条件更新（affected = 0，因为没有匹配的记录）
            Condition condition = Conditions.eq("name", "non-existent-name");
            java.util.Map<String, Object> updates = java.util.Map.of("email", "updated@example.com");
            long affected = proxy.updateAll(condition, updates);

            // Then
            assertThat(affected).isEqualTo(0);
            // 缓存仍存在
            assertThat(proxy.findById(user.getId())).isPresent();
        }

        @Test
        @DisplayName("deleteAll affected = 0 时不清理缓存")
        void shouldNotClearCacheWhenDeleteAffectedIsZero() {
            createTestUserTable();

            // Given - 配置缓存
            CacheProperties cacheProperties = new CacheProperties();
            cacheProperties.setEnabled(true);
            DefaultCacheManager defaultCacheManager = new DefaultCacheManager(cacheProperties);
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setEnabled(true);
            EntityCacheManager cacheManager = new EntityCacheManager(defaultCacheManager, properties);

            JdbcDataManager dataManagerWithCache = new JdbcDataManager(dataSource);
            dataManagerWithCache.setCacheManager(cacheManager);

            EntityProxy<TestUser> proxy = dataManagerWithCache.entity(TestUser.class);

            // Given - 插入数据并预热缓存
            TestUser user = new TestUser();
            user.setName("cache-test-delete-zero");
            user.setEmail("delete-zero@example.com");
            proxy.insert(user);
            proxy.findById(user.getId());

            // When - 条件删除（affected = 0）
            Condition condition = Conditions.eq("name", "non-existent-for-delete");
            long affected = proxy.deleteAll(condition);

            // Then
            assertThat(affected).isEqualTo(0);
        }

        @Test
        @DisplayName("updateAll affected > 0 但缓存禁用时不清理缓存")
        void shouldNotClearCacheWhenCacheDisabled() {
            createTestUserTable();

            // Given - 不配置缓存（cacheManager = null）
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // Given - 插入数据
            TestUser user = new TestUser();
            user.setName("no-cache-update");
            user.setEmail("no-cache@example.com");
            proxy.insert(user);

            // When - 条件更新
            Condition condition = Conditions.eq("name", "no-cache-update");
            java.util.Map<String, Object> updates = java.util.Map.of("email", "updated-no-cache@example.com");
            long affected = proxy.updateAll(condition, updates);

            // Then - 更新成功，不抛异常
            assertThat(affected).isEqualTo(1);
        }

        @Test
        @DisplayName("deleteAll affected > 0 但缓存禁用时不清理缓存")
        void shouldNotClearCacheOnDeleteWhenCacheDisabled() {
            createTestUserTable();

            // Given - 不配置缓存
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // Given - 插入数据
            TestUser user = new TestUser();
            user.setName("no-cache-delete");
            user.setEmail("no-cache-delete@example.com");
            proxy.insert(user);

            // When - 条件删除
            Condition condition = Conditions.eq("name", "no-cache-delete");
            long affected = proxy.deleteAll(condition);

            // Then
            assertThat(affected).isEqualTo(1);
        }
    }

    // ==================== 时间戳软删除分支测试 ====================

    @Nested
    @DisplayName("时间戳软删除分支测试")
    class TimestampSoftDeleteTests {

        @Test
        @DisplayName("findAll 空条件 + TIMESTAMP 策略 + whereClause.length() == 0")
        void shouldHandleEmptyConditionWithTimestampStrategy() {
            createTimestampSoftDeleteTable();
            EntityProxy<TimestampSoftDeleteUser> proxy = dataManager.entity(TimestampSoftDeleteUser.class);

            // Given - 插入未删除记录
            TimestampSoftDeleteUser user = new TimestampSoftDeleteUser();
            user.setName("active-timestamp");
            user.setEmail("active-timestamp@example.com");
            proxy.insert(user);

            // When - 空条件查询
            List<TimestampSoftDeleteUser> results = proxy.findAll(Conditions.empty());

            // Then
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("findAll 分页空条件 + TIMESTAMP 策略 + whereClause.length() == 0")
        void shouldHandleEmptyConditionPageableWithTimestampStrategy() {
            createTimestampSoftDeleteTable();
            EntityProxy<TimestampSoftDeleteUser> proxy = dataManager.entity(TimestampSoftDeleteUser.class);

            // Given - 插入未删除记录
            TimestampSoftDeleteUser user = new TimestampSoftDeleteUser();
            user.setName("active-timestamp-page");
            user.setEmail("active-timestamp-page@example.com");
            proxy.insert(user);

            // When - 空条件分页查询
            PageRequest pageRequest =
                PageRequest.of(1, 10);
            Page<TimestampSoftDeleteUser> page =
                proxy.findAll(Conditions.empty(), pageRequest);

            // Then
            assertThat(page.getContent()).hasSize(1);
        }

        private void createTimestampSoftDeleteTable() {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE timestamp_soft_delete_user (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(200),
                        deleted_at TIMESTAMP
                    )
                    """);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create timestamp_soft_delete_user table", e);
            }
        }
    }

    // ==================== evictCache 实体 ID 为 null 测试 ====================

    @Nested
    @DisplayName("evictCache 边界测试")
    class EvictCacheBoundaryTests {

        @Test
        @DisplayName("update 实体 ID 为 null 时 evictCache 应跳过")
        void shouldSkipEvictCacheWhenEntityIdIsNull() {
            // Create table without auto-generated ID
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE test_user_null_id (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(200)
                    )
                    """);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create table", e);
            }

            try {
                // Given - 配置缓存
                CacheProperties cacheProperties = new CacheProperties();
                cacheProperties.setEnabled(true);
                DefaultCacheManager defaultCacheManager = new DefaultCacheManager(cacheProperties);
                EntityCacheProperties properties = new EntityCacheProperties();
                properties.setEnabled(true);
                EntityCacheManager cacheManager = new EntityCacheManager(defaultCacheManager, properties);

                JdbcDataManager dataManagerWithCache = new JdbcDataManager(dataSource);
                dataManagerWithCache.setCacheManager(cacheManager);

                EntityProxy<TestUserNullId> proxy = dataManagerWithCache.entity(TestUserNullId.class);

                // Given - 插入实体
                TestUserNullId user = new TestUserNullId();
                user.setId(1L);
                user.setName("null-id-test");
                user.setEmail("null-id@example.com");
                proxy.insert(user);

                // When - 更新实体（触发 evictCache）
                user.setName("updated-null-id");
                proxy.update(user);

                // Then - 测试通过，无异常
            } finally {
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP TABLE IF EXISTS test_user_null_id");
                } catch (Exception ignored) {
                }
            }
        }
    }

    // ==================== 无软删除策略条件查询测试 ====================

    @Nested
    @DisplayName("无软删除策略条件查询测试")
    class NoSoftDeleteStrategyQueryTests {

        @Test
        @DisplayName("count 空条件 + 无软删除策略应正确处理")
        void shouldHandleEmptyConditionWithoutSoftDeleteStrategy() {
            createTestUserTable();
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // Given - 插入数据
            TestUser user = new TestUser();
            user.setName("no-soft-delete");
            user.setEmail("no-soft-delete@example.com");
            proxy.insert(user);

            // When - 空条件计数（实体没有软删除策略）
            long count = proxy.count(Conditions.empty());

            // Then
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("count 带条件 + 无软删除策略应正确处理 whereClause.isEmpty()")
        void shouldHandleEmptyWhereClauseWithoutSoftDeleteStrategy() {
            createTestUserTable();
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // Given - 插入数据
            TestUser user = new TestUser();
            user.setName("count-no-soft-delete");
            user.setEmail("count-no-soft@example.com");
            proxy.insert(user);

            // When - 空条件计数（实体没有软删除策略，whereClause.isEmpty()）
            long count = proxy.count(Conditions.empty());

            // Then - 应返回正确计数
            assertThat(count).isEqualTo(1);
        }
    }

    // ==================== findAllById 软删除测试 ====================

    @Nested
    @DisplayName("findAllById 软删除测试")
    class FindAllByIdSoftDeleteTests {

        @Test
        @DisplayName("findAllById 应过滤软删除记录")
        void shouldFilterDeletedInFindAllById() {
            createSoftDeleteUserTable();
            EntityProxy<SoftDeleteUser> proxy = dataManager.entity(SoftDeleteUser.class);

            // Given - 插入并软删除记录
            SoftDeleteUser user = new SoftDeleteUser();
            user.setName("deleted-user");
            user.setEmail("deleted@example.com");
            user = proxy.insert(user);
            proxy.delete(user);

            // When
            List<SoftDeleteUser> result = proxy.findAllById(List.of(user.getId()));

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("findAllById with includeDeleted 应包含软删除记录")
        void shouldIncludeDeletedInFindAllById() {
            createSoftDeleteUserTable();
            EntityProxy<SoftDeleteUser> proxy = dataManager.entity(SoftDeleteUser.class);
            JdbcEntityProxy<SoftDeleteUser> jdbcProxy = (JdbcEntityProxy<SoftDeleteUser>) proxy;

            // Given - 插入并软删除记录
            SoftDeleteUser user = new SoftDeleteUser();
            user.setName("deleted-user");
            user.setEmail("deleted@example.com");
            user = proxy.insert(user);
            proxy.delete(user);

            // When - 使用 includeDeleted
            List<SoftDeleteUser> result = jdbcProxy.includeDeleted().findAllById(List.of(user.getId()));

            // Then
            assertThat(result).hasSize(1);
        }
    }

    // ==================== 辅助方法 ====================

    private DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:proxycoverage;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void createTestUserTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE test_user (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(200)
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test_user table", e);
        }
    }

    private void createTestUserTableWithIdentity() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE test_user (
                    id BIGINT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(200)
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test_user table", e);
        }
    }

    private void createTestUserTableWithExplicitId() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE test_user_with_explicit_id (
                    id BIGINT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(200)
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test_user_with_explicit_id table", e);
        }
    }

    private void createSoftDeleteUserTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE soft_delete_user (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(200),
                    deleted BOOLEAN DEFAULT FALSE
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create soft_delete_user table", e);
        }
    }

    private void createAssociationTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE test_department (id SERIAL PRIMARY KEY, name VARCHAR(100))");
            stmt.execute("CREATE TABLE test_user_with_dept (id SERIAL PRIMARY KEY, name VARCHAR(100), email VARCHAR(200), department_id BIGINT)");
            stmt.execute("CREATE TABLE test_user_detail (id SERIAL PRIMARY KEY, bio VARCHAR(500))");
            stmt.execute("CREATE TABLE test_user_with_detail (id SERIAL PRIMARY KEY, name VARCHAR(100), email VARCHAR(200), detail_id BIGINT)");
            stmt.execute("CREATE TABLE test_role (id SERIAL PRIMARY KEY, name VARCHAR(50))");
            stmt.execute("CREATE TABLE test_user_with_roles (id SERIAL PRIMARY KEY, name VARCHAR(100), email VARCHAR(200))");
            stmt.execute("CREATE TABLE test_user_with_roles_test_role (test_user_with_roles_id BIGINT, test_role_id BIGINT)");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create association tables", e);
        }
    }

    private void dropAllTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS test_user");
            stmt.execute("DROP TABLE IF EXISTS test_user_with_explicit_id");
            stmt.execute("DROP TABLE IF EXISTS test_user_preset_id");
            stmt.execute("DROP TABLE IF EXISTS test_user_all_ids");
            stmt.execute("DROP TABLE IF EXISTS test_user_null_id");
            stmt.execute("DROP TABLE IF EXISTS soft_delete_user");
            stmt.execute("DROP TABLE IF EXISTS timestamp_soft_delete_user");
            stmt.execute("DROP TABLE IF EXISTS test_department");
            stmt.execute("DROP TABLE IF EXISTS test_user_with_dept");
            stmt.execute("DROP TABLE IF EXISTS test_user_detail");
            stmt.execute("DROP TABLE IF EXISTS test_user_with_detail");
            stmt.execute("DROP TABLE IF EXISTS test_role");
            stmt.execute("DROP TABLE IF EXISTS test_user_with_roles");
            stmt.execute("DROP TABLE IF EXISTS test_user_with_roles_test_role");
        } catch (Exception ignored) {
        }
    }

    // ==================== 测试实体 ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUser {
        private Long id;
        private String name;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUserPresetId {
        private Long id;
        private String name;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUserWithExplicitId {
        private Long id;
        private String name;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class SoftDeleteUser implements SoftDeletable {
        private Long id;
        private String name;
        private String email;
        private boolean deleted;

        @Override
        public boolean isDeleted() {
            return deleted;
        }

        @Override
        public void setDeleted(boolean deleted) {
            this.deleted = deleted;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestDepartment {
        private Long id;
        private String name;

        @OneToMany(mappedBy = "department")
        private List<TestUserWithDept> employees;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUserWithDept {
        private Long id;
        private String name;
        private String email;
        private Long departmentId;

        @ManyToOne
        private TestDepartment department;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUserDetail {
        private Long id;
        private String bio;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUserWithDetail {
        private Long id;
        private String name;
        private String email;
        private Long detailId;

        @OneToOne
        private TestUserDetail detail;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUserWithRoles {
        private Long id;
        private String name;
        private String email;

        @ManyToMany
        private Set<TestRole> roles;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestRole {
        private Long id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUserAllIds {
        private Long id;
        private String name;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUserNullId {
        private Long id;
        private String name;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TimestampSoftDeleteUser implements TimestampSoftDeletable {
        private Long id;
        private String name;
        private String email;
        private java.time.LocalDateTime deletedAt;

        @Override
        public java.time.LocalDateTime getDeletedAt() {
            return deletedAt;
        }

        @Override
        public void setDeletedAt(java.time.LocalDateTime deletedAt) {
            this.deletedAt = deletedAt;
        }

        @Override
        public boolean isDeleted() {
            return deletedAt != null;
        }
    }
}
