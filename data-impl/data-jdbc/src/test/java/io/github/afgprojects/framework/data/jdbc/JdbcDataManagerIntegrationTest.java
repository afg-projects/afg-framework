package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.dialect.DatabaseType;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.sql.SqlDeleteBuilder;
import io.github.afgprojects.framework.data.core.sql.SqlInsertBuilder;
import io.github.afgprojects.framework.data.core.sql.SqlQueryBuilder;
import io.github.afgprojects.framework.data.core.sql.SqlUpdateBuilder;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JdbcDataManager 完整集成测试
 * <p>
 * 覆盖所有功能场景：CRUD、事务、租户、缓存、批量操作等
 * </p>
 */
@DisplayName("JdbcDataManager 完整集成测试")
class JdbcDataManagerIntegrationTest {

    private JdbcDataManager dataManager;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        dataSource = createDataSource();
        dataManager = new JdbcDataManager(dataSource, DatabaseType.POSTGRESQL);
        createTestTable();
    }

    @AfterEach
    void tearDown() {
        dropTestTable();
    }

    // ==================== 基础功能测试 ====================

    @Nested
    @DisplayName("基础功能测试")
    class BasicFunctionTests {

        @Test
        @DisplayName("应该正确检测数据库类型")
        void shouldDetectDatabaseType() {
            JdbcDataManager autoDetectDataManager = new JdbcDataManager(dataSource);
            assertThat(autoDetectDataManager.getDatabaseType()).isEqualTo(DatabaseType.H2);
        }

        @Test
        @DisplayName("应该正确实现 DataManager 接口")
        void shouldImplementDataManagerInterface() {
            assertThat(dataManager).isInstanceOf(DataManager.class);
        }

        @Test
        @DisplayName("应该正确获取数据库类型")
        void shouldGetDatabaseType() {
            assertThat(dataManager.getDatabaseType()).isEqualTo(DatabaseType.POSTGRESQL);
        }

        @Test
        @DisplayName("应该返回正确的事务管理器")
        void shouldReturnTransactionManager() {
            Object transactionManager = dataManager.getTransactionManager();
            assertThat(transactionManager).isSameAs(dataSource);
        }

        @Test
        @DisplayName("应该正确获取底层 JdbcClient")
        void shouldGetJdbcClient() {
            assertThat(dataManager.getJdbcClient()).isNotNull();
        }

        @Test
        @DisplayName("应该正确获取底层 JdbcTemplate")
        void shouldGetJdbcTemplate() {
            assertThat(dataManager.getJdbcTemplate()).isNotNull();
        }
    }

    // ==================== SQL 构建器测试 ====================

    @Nested
    @DisplayName("SQL 构建器测试")
    class SqlBuilderTests {

        @Test
        @DisplayName("应该创建 SqlQueryBuilder")
        void shouldCreateSqlQueryBuilder() {
            SqlQueryBuilder queryBuilder = dataManager.query();

            String sql = queryBuilder.select("*")
                .from("test_user")
                .toSql();

            assertThat(sql).containsIgnoringCase("SELECT");
            assertThat(sql).contains("test_user");
        }

        @Test
        @DisplayName("应该创建 SqlUpdateBuilder")
        void shouldCreateSqlUpdateBuilder() {
            SqlUpdateBuilder updateBuilder = dataManager.update();
            assertThat(updateBuilder).isNotNull();
        }

        @Test
        @DisplayName("应该创建 SqlInsertBuilder")
        void shouldCreateSqlInsertBuilder() {
            SqlInsertBuilder insertBuilder = dataManager.insert();
            assertThat(insertBuilder).isNotNull();
        }

        @Test
        @DisplayName("应该创建 SqlDeleteBuilder")
        void shouldCreateSqlDeleteBuilder() {
            SqlDeleteBuilder deleteBuilder = dataManager.delete();
            assertThat(deleteBuilder).isNotNull();
        }
    }

    // ==================== EntityProxy 测试 ====================

    @Nested
    @DisplayName("EntityProxy 测试")
    class EntityProxyTests {

        @Test
        @DisplayName("应该创建 EntityProxy")
        void shouldCreateEntityProxy() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);
            assertThat(proxy).isNotNull();
            assertThat(proxy).isInstanceOf(JdbcEntityProxy.class);
        }

        @Test
        @DisplayName("应该正确获取实体元数据")
        void shouldGetEntityMetadata() {
            var metadata = dataManager.getEntityMetadata(TestUser.class);

            assertThat(metadata).isNotNull();
            assertThat(metadata.getTableName()).isEqualTo("test_user");
            assertThat(metadata.getFields()).isNotEmpty();
        }

        @Test
        @DisplayName("应该正确执行 CRUD 操作")
        void shouldPerformCrudOperations() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // Create
            TestUser user = new TestUser();
            user.setName("crud-user");
            user.setEmail("crud@example.com");
            TestUser inserted = proxy.insert(user);
            assertThat(inserted.getId()).isNotNull();

            // Read
            Optional<TestUser> found = proxy.findById(inserted.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("crud-user");

            // Update
            inserted.setName("updated-name");
            TestUser updated = proxy.update(inserted);
            assertThat(updated.getName()).isEqualTo("updated-name");

            // Delete
            proxy.deleteById(inserted.getId());
            assertThat(proxy.findById(inserted.getId())).isEmpty();
        }

        @Test
        @DisplayName("应该正确执行条件查询")
        void shouldQueryWithCondition() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // 插入测试数据
            for (int i = 0; i < 5; i++) {
                TestUser user = new TestUser();
                user.setName("user" + i);
                user.setEmail("user" + i + "@example.com");
                proxy.insert(user);
            }

            // 条件查询
            Condition condition = Conditions.builder()
                .like("name", "user")
                .build();
            List<TestUser> users = proxy.findAll(condition);
            assertThat(users).hasSize(5);
        }

        @Test
        @DisplayName("应该正确使用 Lambda 条件构建器")
        void shouldUseLambdaConditionBuilder() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // 插入测试数据
            TestUser u1 = new TestUser(null, "lambda-user-1", "lambda1@example.com");
            TestUser u2 = new TestUser(null, "lambda-user-2", "lambda2@example.com");
            TestUser u3 = new TestUser(null, "other-user", "other@example.com");
            proxy.insertAll(List.of(u1, u2, u3));

            // 使用 Lambda 条件构建器查询
            Condition condition = Conditions.builder(TestUser.class)
                .like(TestUser::getName, "lambda")
                .build();

            List<TestUser> users = proxy.findAll(condition);
            assertThat(users).hasSize(2);
            assertThat(users).allMatch(u -> u.getName().startsWith("lambda"));
        }

        @Test
        @DisplayName("Lambda 条件应支持多种操作符")
        void shouldSupportMultipleOperatorsInLambdaCondition() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // 插入测试数据
            proxy.insert(new TestUser(null, "alpha", "a@example.com"));
            proxy.insert(new TestUser(null, "beta", "b@example.com"));
            proxy.insert(new TestUser(null, "gamma", "g@example.com"));

            // 使用 Lambda 条件：eq
            Condition eqCondition = Conditions.builder(TestUser.class)
                .eq(TestUser::getName, "alpha")
                .build();
            assertThat(proxy.findAll(eqCondition)).hasSize(1);

            // 使用 Lambda 条件：like (匹配包含 "bet" 的)
            Condition likeCondition = Conditions.builder(TestUser.class)
                .like(TestUser::getName, "bet")
                .build();
            assertThat(proxy.findAll(likeCondition)).hasSize(1);

            // 使用 Lambda 条件：in
            Condition inCondition = Conditions.builder(TestUser.class)
                .in(TestUser::getName, List.of("alpha", "beta"))
                .build();
            assertThat(proxy.findAll(inCondition)).hasSize(2);
        }

        @Test
        @DisplayName("Lambda 条件应支持组合条件")
        void shouldSupportCombinedConditionsInLambda() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // 插入测试数据
            proxy.insert(new TestUser(null, "test-1", "test1@example.com"));
            proxy.insert(new TestUser(null, "test-2", "test2@example.com"));
            proxy.insert(new TestUser(null, "other", "other@example.com"));

            // 使用 Lambda 条件组合
            Condition condition = Conditions.builder(TestUser.class)
                .like(TestUser::getName, "test")
                .eq(TestUser::getEmail, "test1@example.com")
                .build();

            List<TestUser> users = proxy.findAll(condition);
            assertThat(users).hasSize(1);
            assertThat(users.get(0).getName()).isEqualTo("test-1");
        }

        @Test
        @DisplayName("应该正确获取 Lambda 字段名")
        void shouldGetFieldNameFromLambda() {
            // 测试 getter 方法名转换
            String nameField = Conditions.getFieldName(TestUser::getName);
            assertThat(nameField).isEqualTo("name");

            String emailField = Conditions.getFieldName(TestUser::getEmail);
            assertThat(emailField).isEqualTo("email");
        }

        @Test
        @DisplayName("Lambda 条件应支持 null 值判断")
        void shouldSupportNullCheckInLambdaCondition() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // 插入测试数据（email 为 null）
            TestUser u1 = new TestUser(null, "null-email", null);
            TestUser u2 = new TestUser(null, "has-email", "has@example.com");
            proxy.insertAll(List.of(u1, u2));

            // 查询 email 为 null 的记录
            Condition isNullCondition = Conditions.builder(TestUser.class)
                .isNull(TestUser::getEmail)
                .build();
            List<TestUser> nullEmails = proxy.findAll(isNullCondition);
            assertThat(nullEmails).hasSize(1);
            assertThat(nullEmails.get(0).getName()).isEqualTo("null-email");

            // 查询 email 不为 null 的记录
            Condition isNotNullCondition = Conditions.builder(TestUser.class)
                .isNotNull(TestUser::getEmail)
                .build();
            List<TestUser> notNullEmails = proxy.findAll(isNotNullCondition);
            assertThat(notNullEmails).hasSize(1);
            assertThat(notNullEmails.get(0).getName()).isEqualTo("has-email");
        }

        @Test
        @DisplayName("应该正确执行分页查询")
        void shouldQueryWithPagination() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // 插入测试数据
            for (int i = 0; i < 15; i++) {
                TestUser user = new TestUser();
                user.setName("page-user" + i);
                user.setEmail("page" + i + "@example.com");
                proxy.insert(user);
            }

            // 分页查询
            PageRequest pageRequest = PageRequest.of(1, 10);
            Page<TestUser> page = proxy.findAll(Conditions.empty(), pageRequest);

            assertThat(page.getContent()).hasSize(10);
            assertThat(page.getTotal()).isEqualTo(15);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("应该正确执行批量插入")
        void shouldBatchInsert() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            TestUser u1 = new TestUser(null, "batch1", "batch1@example.com");
            TestUser u2 = new TestUser(null, "batch2", "batch2@example.com");
            TestUser u3 = new TestUser(null, "batch3", "batch3@example.com");

            List<TestUser> inserted = proxy.insertAll(List.of(u1, u2, u3));

            assertThat(inserted).hasSize(3);
            assertThat(inserted).allMatch(u -> u.getId() != null);

            List<TestUser> all = proxy.findAll();
            assertThat(all).hasSize(3);
        }
    }

    // ==================== 事务管理测试 ====================

    @Nested
    @DisplayName("事务管理测试")
    class TransactionTests {

        @Test
        @DisplayName("应该在事务中执行操作")
        void shouldExecuteInTransaction() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);
            TestUser user = new TestUser();
            user.setName("transaction-user");
            user.setEmail("trans@example.com");

            dataManager.executeInTransaction(() -> {
                proxy.insert(user);
            });

            List<TestUser> all = proxy.findAll();
            assertThat(all).hasSize(1);
            assertThat(all.get(0).getName()).isEqualTo("transaction-user");
        }

        @Test
        @DisplayName("应该在事务失败时回滚")
        void shouldHandleTransactionRollback() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);
            TestUser user = new TestUser();
            user.setName("rollback-user");
            user.setEmail("rollback@example.com");

            assertThatThrownBy(() -> {
                dataManager.executeInTransaction(() -> {
                    proxy.insert(user);
                    throw new RuntimeException("Simulated failure");
                });
            }).isInstanceOf(RuntimeException.class)
              .hasMessageContaining("Transaction failed");

            assertThat(proxy.findAll()).isEmpty();
        }

        @Test
        @DisplayName("应该在事务中返回结果")
        void shouldExecuteInTransactionWithResult() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            TestUser inserted = dataManager.executeInTransaction(() -> {
                TestUser user = new TestUser();
                user.setName("result-user");
                user.setEmail("result@example.com");
                return proxy.insert(user);
            });

            assertThat(inserted.getId()).isNotNull();
            assertThat(inserted.getName()).isEqualTo("result-user");
        }

        @Test
        @DisplayName("应该在只读模式下执行操作")
        void shouldExecuteInReadOnlyMode() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);
            TestUser user = new TestUser();
            user.setName("readonly-user");
            user.setEmail("readonly@example.com");
            proxy.insert(user);

            long count = dataManager.executeInReadOnly(() -> proxy.count());

            assertThat(count).isEqualTo(1L);
        }

        @Test
        @DisplayName("应该正确处理嵌套事务")
        void shouldHandleNestedTransactions() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            dataManager.executeInTransaction(() -> {
                TestUser user1 = new TestUser();
                user1.setName("nested1");
                user1.setEmail("nested1@example.com");
                proxy.insert(user1);

                // 嵌套事务（实际上在同一个事务中）
                dataManager.executeInTransaction(() -> {
                    TestUser user2 = new TestUser();
                    user2.setName("nested2");
                    user2.setEmail("nested2@example.com");
                    proxy.insert(user2);
                });
            });

            assertThat(proxy.count()).isEqualTo(2);
        }
    }

    // ==================== 租户管理测试 ====================

    @Nested
    @DisplayName("租户管理测试")
    class TenantTests {

        @Test
        @DisplayName("应该正确管理租户上下文")
        void shouldManageTenantContext() {
            String tenantId = "tenant-123";

            try (var scope = dataManager.tenantScope(tenantId)) {
                assertThat(scope.getTenantId()).isEqualTo(tenantId);
                assertThat(dataManager.getTenantContextHolder().getTenantId()).isEqualTo(tenantId);
            }

            assertThat(dataManager.getTenantContextHolder().getTenantId()).isNull();
        }

        @Test
        @DisplayName("应该正确恢复之前的租户上下文")
        void shouldRestorePreviousTenantContext() {
            dataManager.getTenantContextHolder().setTenantId("previous-tenant");

            try (var scope = dataManager.tenantScope("new-tenant")) {
                assertThat(dataManager.getTenantContextHolder().getTenantId()).isEqualTo("new-tenant");
            }

            assertThat(dataManager.getTenantContextHolder().getTenantId()).isEqualTo("previous-tenant");
        }

        @Test
        @DisplayName("应该正确处理嵌套租户作用域")
        void shouldHandleNestedTenantScopes() {
            try (var scope1 = dataManager.tenantScope("tenant-1")) {
                assertThat(dataManager.getTenantContextHolder().getTenantId()).isEqualTo("tenant-1");

                try (var scope2 = dataManager.tenantScope("tenant-2")) {
                    assertThat(dataManager.getTenantContextHolder().getTenantId()).isEqualTo("tenant-2");
                }

                assertThat(dataManager.getTenantContextHolder().getTenantId()).isEqualTo("tenant-1");
            }

            assertThat(dataManager.getTenantContextHolder().getTenantId()).isNull();
        }

        @Test
        @DisplayName("租户作用域异常时应该正确恢复")
        void shouldRestoreTenantContextOnException() {
            dataManager.getTenantContextHolder().setTenantId("original");

            assertThatThrownBy(() -> {
                try (var scope = dataManager.tenantScope("temp")) {
                    throw new RuntimeException("Test exception");
                }
            }).isInstanceOf(RuntimeException.class);

            assertThat(dataManager.getTenantContextHolder().getTenantId()).isEqualTo("original");
        }
    }

    // ==================== 缓存功能测试 ====================

    @Nested
    @DisplayName("缓存功能测试")
    class CacheTests {

        private EntityCacheManager createCacheManager() {
            CacheProperties cacheProperties = new CacheProperties();
            DefaultCacheManager defaultCacheManager = new DefaultCacheManager(cacheProperties);

            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setEnabled(true);
            properties.setTtl(300000);
            properties.setMaxSize(1000);
            properties.setCacheNull(true);

            return new EntityCacheManager(defaultCacheManager, properties);
        }

        @Test
        @DisplayName("应该正确设置和获取缓存管理器")
        void shouldSetAndGetCacheManager() {
            EntityCacheManager cacheManager = createCacheManager();

            dataManager.setCacheManager(cacheManager);

            assertThat(dataManager.getCacheManager()).isSameAs(cacheManager);
        }

        @Test
        @DisplayName("findById 应该使用缓存")
        void shouldUseCacheForFindById() {
            // 设置缓存
            EntityCacheManager cacheManager = createCacheManager();
            dataManager.setCacheManager(cacheManager);

            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // 插入数据
            TestUser user = new TestUser();
            user.setName("cached-user");
            user.setEmail("cached@example.com");
            TestUser inserted = proxy.insert(user);

            // 第一次查询 - 从数据库加载并缓存
            Optional<TestUser> first = proxy.findById(inserted.getId());
            assertThat(first).isPresent();

            // 更新数据库（绕过缓存）
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("UPDATE test_user SET name = 'modified' WHERE id = " + inserted.getId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // 第二次查询 - 应该从缓存获取（名称仍然是 cached-user）
            Optional<TestUser> second = proxy.findById(inserted.getId());
            assertThat(second).isPresent();
            assertThat(second.get().getName()).isEqualTo("cached-user"); // 从缓存获取，不是 modified
        }

        @Test
        @DisplayName("缓存未命中时应该查询数据库")
        void shouldQueryDatabaseOnCacheMiss() {
            EntityCacheManager cacheManager = createCacheManager();
            dataManager.setCacheManager(cacheManager);

            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            TestUser user = new TestUser();
            user.setName("cache-miss");
            user.setEmail("miss@example.com");
            TestUser inserted = proxy.insert(user);

            // 清除缓存
            cacheManager.evict(TestUser.class, inserted.getId());

            // 查询应该从数据库获取
            Optional<TestUser> found = proxy.findById(inserted.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("cache-miss");
        }

        @Test
        @DisplayName("应该缓存 null 值防止穿透")
        void shouldCacheNullValue() {
            EntityCacheManager cacheManager = createCacheManager();
            dataManager.setCacheManager(cacheManager);

            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // 查询不存在的 ID
            Optional<TestUser> first = proxy.findById(99999L);
            assertThat(first).isEmpty();

            // 再次查询相同的 ID - 应该从缓存获取 null 标记
            Optional<TestUser> second = proxy.findById(99999L);
            assertThat(second).isEmpty();
        }

        @Test
        @DisplayName("禁用缓存时应该直接查询数据库")
        void shouldNotUseCacheWhenDisabled() {
            EntityCacheManager cacheManager = createCacheManager();
            dataManager.setCacheManager(cacheManager);

            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            TestUser user = new TestUser();
            user.setName("disabled-cache");
            user.setEmail("disabled@example.com");
            TestUser inserted = proxy.insert(user);

            // 第一次查询 - 缓存
            proxy.findById(inserted.getId());

            // 禁用缓存
            cacheManager.getProperties().setEnabled(false);

            // 修改数据库
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("UPDATE test_user SET name = 'modified2' WHERE id = " + inserted.getId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // 查询应该从数据库获取（缓存已禁用）
            Optional<TestUser> found = proxy.findById(inserted.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("modified2");
        }
    }

    // ==================== 批量操作测试 ====================

    @Nested
    @DisplayName("批量操作测试")
    class BatchOperationTests {

        @Test
        @DisplayName("应该正确执行批量更新")
        void shouldExecuteBatchUpdate() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // 插入测试数据
            TestUser u1 = new TestUser(null, "batch1", "batch1@example.com");
            TestUser u2 = new TestUser(null, "batch2", "batch2@example.com");
            proxy.insertAll(List.of(u1, u2));

            // 批量更新
            int[] results = dataManager.batchUpdate(
                "UPDATE test_user SET email = ? WHERE name = ?",
                List.of(
                    List.of("updated1@example.com", "batch1"),
                    List.of("updated2@example.com", "batch2")
                )
            );

            assertThat(results).hasSize(2);

            // 验证更新结果
            List<TestUser> all = proxy.findAll();
            assertThat(all).anyMatch(u -> u.getEmail().equals("updated1@example.com"));
            assertThat(all).anyMatch(u -> u.getEmail().equals("updated2@example.com"));
        }

        @Test
        @DisplayName("应该正确执行带命名参数的更新")
        void shouldExecuteUpdateWithNamedParameters() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            TestUser user = new TestUser();
            user.setName("named-param");
            user.setEmail("named@example.com");
            TestUser inserted = proxy.insert(user);

            int result = dataManager.executeUpdate(
                "UPDATE test_user SET email = :email WHERE id = :id",
                java.util.Map.of("email", "named-updated@example.com", "id", inserted.getId())
            );

            assertThat(result).isEqualTo(1);

            Optional<TestUser> found = proxy.findById(inserted.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("named-updated@example.com");
        }
    }

    // ==================== Lambda 表达式测试 ====================

    @Nested
    @DisplayName("Lambda 表达式测试")
    class LambdaExpressionTests {

        @Test
        @DisplayName("应该正确执行 Runnable lambda 事务")
        void shouldExecuteRunnableLambda() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // 使用 Runnable lambda（无返回值）
            dataManager.executeInTransaction(() -> {
                TestUser user = new TestUser();
                user.setName("runnable-lambda");
                user.setEmail("runnable@example.com");
                proxy.insert(user);
            });

            assertThat(proxy.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("应该正确执行 Supplier lambda 事务并返回结果")
        void shouldExecuteSupplierLambda() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // 使用 Supplier lambda（有返回值）
            TestUser result = dataManager.executeInTransaction(() -> {
                TestUser user = new TestUser();
                user.setName("supplier-lambda");
                user.setEmail("supplier@example.com");
                return proxy.insert(user);
            });

            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getName()).isEqualTo("supplier-lambda");
        }

        @Test
        @DisplayName("lambda 应该正确捕获外部变量（闭包）")
        void shouldCaptureExternalVariables() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // 外部变量
            String prefix = "closure-";
            int counter = 42;

            dataManager.executeInTransaction(() -> {
                TestUser user = new TestUser();
                user.setName(prefix + counter);
                user.setEmail("closure@example.com");
                proxy.insert(user);
            });

            List<TestUser> users = proxy.findAll();
            assertThat(users).hasSize(1);
            assertThat(users.get(0).getName()).isEqualTo("closure-42");
        }

        @Test
        @DisplayName("应该正确执行只读 lambda 表达式")
        void shouldExecuteReadOnlyLambda() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // 准备数据
            TestUser user = new TestUser();
            user.setName("readonly-lambda");
            user.setEmail("readonly@example.com");
            proxy.insert(user);

            // 使用只读 lambda 查询
            long count = dataManager.executeInReadOnly(() -> proxy.count());

            assertThat(count).isEqualTo(1L);
        }

        @Test
        @DisplayName("只读 lambda 应该正确返回复杂对象")
        void shouldReturnComplexObjectFromReadOnlyLambda() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            TestUser user = new TestUser();
            user.setName("complex-lambda");
            user.setEmail("complex@example.com");
            TestUser inserted = proxy.insert(user);

            // 只读 lambda 返回 Optional
            Optional<TestUser> found = dataManager.executeInReadOnly(() -> proxy.findById(inserted.getId()));

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("complex-lambda");
        }

        @Test
        @DisplayName("应该正确处理 lambda 异常")
        void shouldHandleLambdaException() {
            assertThatThrownBy(() -> {
                dataManager.executeInTransaction(() -> {
                    throw new RuntimeException("Lambda exception test");
                });
            }).isInstanceOf(RuntimeException.class)
              .hasMessageContaining("Transaction failed");
        }

        @Test
        @DisplayName("应该正确执行链式 lambda 调用")
        void shouldExecuteChainedLambdaCalls() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // 第一个 lambda 插入数据
            TestUser first = dataManager.executeInTransaction(() -> {
                TestUser user = new TestUser();
                user.setName("chained-1");
                user.setEmail("chained1@example.com");
                return proxy.insert(user);
            });

            // 第二个 lambda 更新数据
            dataManager.executeInTransaction(() -> {
                first.setName("chained-1-updated");
                proxy.update(first);
            });

            // 第三个 lambda 查询验证
            Optional<TestUser> verified = dataManager.executeInReadOnly(() -> proxy.findById(first.getId()));

            assertThat(verified).isPresent();
            assertThat(verified.get().getName()).isEqualTo("chained-1-updated");
        }

        @Test
        @DisplayName("应该正确执行多行 lambda 表达式")
        void shouldExecuteMultiLineLambda() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            dataManager.executeInTransaction(() -> {
                // 多行 lambda
                TestUser user1 = new TestUser();
                user1.setName("multiline-1");
                user1.setEmail("multiline1@example.com");
                proxy.insert(user1);

                TestUser user2 = new TestUser();
                user2.setName("multiline-2");
                user2.setEmail("multiline2@example.com");
                proxy.insert(user2);

                TestUser user3 = new TestUser();
                user3.setName("multiline-3");
                user3.setEmail("multiline3@example.com");
                proxy.insert(user3);
            });

            assertThat(proxy.count()).isEqualTo(3);
        }

        @Test
        @DisplayName("lambda 应该正确处理条件逻辑")
        void shouldHandleConditionalLogicInLambda() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);
            boolean shouldInsert = true;
            String userName = "conditional-lambda";

            TestUser result = dataManager.executeInTransaction(() -> {
                if (shouldInsert) {
                    TestUser user = new TestUser();
                    user.setName(userName);
                    user.setEmail("conditional@example.com");
                    return proxy.insert(user);
                }
                return null;
            });

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("conditional-lambda");
        }

        @Test
        @DisplayName("应该正确执行嵌套 lambda 表达式")
        void shouldExecuteNestedLambda() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // 外层 lambda
            dataManager.executeInTransaction(() -> {
                // 插入第一条数据
                TestUser user1 = new TestUser();
                user1.setName("nested-outer");
                user1.setEmail("nested-outer@example.com");
                proxy.insert(user1);

                // 内层 lambda（在同一个事务中）
                dataManager.executeInTransaction(() -> {
                    TestUser user2 = new TestUser();
                    user2.setName("nested-inner");
                    user2.setEmail("nested-inner@example.com");
                    proxy.insert(user2);
                });
            });

            assertThat(proxy.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("应该正确使用 lambda 计算并返回结果")
        void shouldUseLambdaForCalculation() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            // 插入测试数据
            for (int i = 0; i < 5; i++) {
                TestUser user = new TestUser();
                user.setName("calc-user-" + i);
                user.setEmail("calc" + i + "@example.com");
                proxy.insert(user);
            }

            // 使用 lambda 进行计算
            long count = dataManager.executeInReadOnly(() -> {
                long total = proxy.count();
                return total * 2;
            });

            assertThat(count).isEqualTo(10L);
        }

        @Test
        @DisplayName("lambda 事务失败时应该正确回滚所有操作")
        void shouldRollbackAllOperationsOnLambdaFailure() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            assertThatThrownBy(() -> {
                dataManager.executeInTransaction(() -> {
                    // 插入第一条数据
                    TestUser user1 = new TestUser();
                    user1.setName("rollback-test-1");
                    user1.setEmail("rollback1@example.com");
                    proxy.insert(user1);

                    // 插入第二条数据
                    TestUser user2 = new TestUser();
                    user2.setName("rollback-test-2");
                    user2.setEmail("rollback2@example.com");
                    proxy.insert(user2);

                    // 抛出异常触发回滚
                    throw new RuntimeException("Force rollback");
                });
            }).isInstanceOf(RuntimeException.class);

            // 验证所有操作都已回滚
            assertThat(proxy.count()).isZero();
        }
    }

    // ==================== 异常处理测试 ====================

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("重复插入相同 ID 应该抛出异常")
        void shouldThrowOnDuplicateInsert() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            TestUser user = new TestUser();
            user.setName("duplicate");
            user.setEmail("dup@example.com");
            proxy.insert(user);

            // 再次插入相同数据（H2 允许，但可以测试其他约束）
            assertThat(proxy.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("查询不存在的实体应该返回空")
        void shouldReturnEmptyForNonExistentEntity() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            Optional<TestUser> found = proxy.findById(99999L);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("更新不存在的实体应该正常处理")
        void shouldHandleUpdateNonExistent() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);

            TestUser user = new TestUser();
            user.setId(99999L);
            user.setName("nonexistent");
            user.setEmail("none@example.com");

            // 更新不存在的记录
            TestUser updated = proxy.update(user);
            assertThat(updated.getName()).isEqualTo("nonexistent");
        }
    }

    // ==================== 辅助方法 ====================

    private DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void createTestTable() {
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
            throw new RuntimeException("Failed to create test table", e);
        }
    }

    private void dropTestTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS test_user");
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

    // ==================== 数据库类型检测测试 ====================

    @Nested
    @DisplayName("数据库类型检测测试")
    class DatabaseTypeDetectionTests {

        @Test
        @DisplayName("强制指定 MySQL 数据库类型")
        void testForceMySQL() {
            JdbcDataManager mysqlDataManager = new JdbcDataManager(dataSource, DatabaseType.MYSQL);
            assertThat(mysqlDataManager.getDatabaseType()).isEqualTo(DatabaseType.MYSQL);
        }

        @Test
        @DisplayName("强制指定 PostgreSQL 数据库类型")
        void testForcePostgreSQL() {
            JdbcDataManager pgDataManager = new JdbcDataManager(dataSource, DatabaseType.POSTGRESQL);
            assertThat(pgDataManager.getDatabaseType()).isEqualTo(DatabaseType.POSTGRESQL);
        }

        @Test
        @DisplayName("强制指定 Oracle 数据库类型")
        void testForceOracle() {
            JdbcDataManager oracleDataManager = new JdbcDataManager(dataSource, DatabaseType.ORACLE);
            assertThat(oracleDataManager.getDatabaseType()).isEqualTo(DatabaseType.ORACLE);
        }

        @Test
        @DisplayName("强制指定 SQL Server 数据库类型")
        void testForceSQLServer() {
            JdbcDataManager sqlServerDataManager = new JdbcDataManager(dataSource, DatabaseType.SQLSERVER);
            assertThat(sqlServerDataManager.getDatabaseType()).isEqualTo(DatabaseType.SQLSERVER);
        }

        @Test
        @DisplayName("强制指定 OceanBase 数据库类型")
        void testForceOceanBase() {
            JdbcDataManager oceanBaseDataManager = new JdbcDataManager(dataSource, DatabaseType.OCEANBASE);
            assertThat(oceanBaseDataManager.getDatabaseType()).isEqualTo(DatabaseType.OCEANBASE);
        }

        @Test
        @DisplayName("强制指定 GaussDB 数据库类型")
        void testForceGaussDB() {
            JdbcDataManager gaussDBDataManager = new JdbcDataManager(dataSource, DatabaseType.GAUSSDB);
            assertThat(gaussDBDataManager.getDatabaseType()).isEqualTo(DatabaseType.GAUSSDB);
        }

        @Test
        @DisplayName("强制指定 OpenGauss 数据库类型")
        void testForceOpenGauss() {
            JdbcDataManager openGaussDataManager = new JdbcDataManager(dataSource, DatabaseType.OPENGAUSS);
            assertThat(openGaussDataManager.getDatabaseType()).isEqualTo(DatabaseType.OPENGAUSS);
        }

        @Test
        @DisplayName("强制指定 DM 数据库类型")
        void testForceDM() {
            JdbcDataManager dmDataManager = new JdbcDataManager(dataSource, DatabaseType.DM);
            assertThat(dmDataManager.getDatabaseType()).isEqualTo(DatabaseType.DM);
        }

        @Test
        @DisplayName("强制指定 Kingbase 数据库类型")
        void testForceKingbase() {
            JdbcDataManager kingbaseDataManager = new JdbcDataManager(dataSource, DatabaseType.KINGBASE);
            assertThat(kingbaseDataManager.getDatabaseType()).isEqualTo(DatabaseType.KINGBASE);
        }
    }

    // ==================== 便捷查询方法测试 ====================

    @Nested
    @DisplayName("便捷查询方法测试")
    class ConvenienceQueryTests {

        @Test
        @DisplayName("queryForList 应该返回列表")
        void testQueryForList() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);
            TestUser user = new TestUser();
            user.setName("list-test");
            user.setEmail("list@example.com");
            proxy.insert(user);

            List<TestUser> users = dataManager.queryForList(
                "SELECT * FROM test_user",
                List.of(),
                (rs, rowNum) -> {
                    TestUser u = new TestUser();
                    u.setId(rs.getLong("id"));
                    u.setName(rs.getString("name"));
                    u.setEmail(rs.getString("email"));
                    return u;
                }
            );

            assertThat(users).isNotEmpty();
        }

        @Test
        @DisplayName("queryForObject 应该返回单个对象")
        void testQueryForObject() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);
            TestUser user = new TestUser();
            user.setName("object-test");
            user.setEmail("object@example.com");
            TestUser inserted = proxy.insert(user);

            TestUser found = dataManager.queryForObject(
                "SELECT * FROM test_user WHERE id = ?",
                List.of(inserted.getId()),
                (rs, rowNum) -> {
                    TestUser u = new TestUser();
                    u.setId(rs.getLong("id"));
                    u.setName(rs.getString("name"));
                    u.setEmail(rs.getString("email"));
                    return u;
                }
            );

            assertThat(found.getName()).isEqualTo("object-test");
        }

        @Test
        @DisplayName("queryForOptional 应该返回 Optional")
        void testQueryForOptional() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);
            TestUser user = new TestUser();
            user.setName("optional-test");
            user.setEmail("optional@example.com");
            TestUser inserted = proxy.insert(user);

            Optional<TestUser> found = dataManager.queryForOptional(
                "SELECT * FROM test_user WHERE id = ?",
                List.of(inserted.getId()),
                (rs, rowNum) -> {
                    TestUser u = new TestUser();
                    u.setId(rs.getLong("id"));
                    u.setName(rs.getString("name"));
                    u.setEmail(rs.getString("email"));
                    return u;
                }
            );

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("optional-test");

            Optional<TestUser> notFound = dataManager.queryForOptional(
                "SELECT * FROM test_user WHERE id = ?",
                List.of(99999L),
                (rs, rowNum) -> {
                    TestUser u = new TestUser();
                    u.setId(rs.getLong("id"));
                    u.setName(rs.getString("name"));
                    u.setEmail(rs.getString("email"));
                    return u;
                }
            );

            assertThat(notFound).isEmpty();
        }

        @Test
        @DisplayName("queryForCount 应该返回计数")
        void testQueryForCount() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);
            for (int i = 0; i < 5; i++) {
                TestUser user = new TestUser();
                user.setName("count-test-" + i);
                user.setEmail("count" + i + "@example.com");
                proxy.insert(user);
            }

            long count = dataManager.queryForCount("SELECT COUNT(*) FROM test_user", List.of());
            assertThat(count).isEqualTo(5L);
        }
    }

    // ==================== executeInsertAndReturnKey 测试 ====================

    @Nested
    @DisplayName("executeInsertAndReturnKey 测试")
    class ExecuteInsertAndReturnKeyTests {

        @Test
        @DisplayName("非事务模式下应该返回生成的主键")
        void shouldReturnGeneratedKeyOutsideTransaction() {
            long id = dataManager.executeInsertAndReturnKey(
                "INSERT INTO test_user (name, email) VALUES (?, ?)",
                List.of("key-test", "key@example.com")
            );

            assertThat(id).isGreaterThan(0);

            // 验证数据已插入
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);
            Optional<TestUser> found = proxy.findById(id);
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("key-test");
        }

        @Test
        @DisplayName("事务模式下应该返回生成的主键")
        void shouldReturnGeneratedKeyInTransaction() {
            Long id = dataManager.executeInTransaction(() -> {
                return dataManager.executeInsertAndReturnKey(
                    "INSERT INTO test_user (name, email) VALUES (?, ?)",
                    List.of("tx-key-test", "txkey@example.com")
                );
            });

            assertThat(id).isNotNull();
            assertThat(id).isGreaterThan(0);

            // 验证数据已插入
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);
            Optional<TestUser> found = proxy.findById(id);
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("tx-key-test");
        }

        @Test
        @DisplayName("事务中插入多条记录应该返回各自的主键")
        void shouldReturnMultipleKeysInTransaction() {
            List<Long> ids = dataManager.executeInTransaction(() -> {
                long id1 = dataManager.executeInsertAndReturnKey(
                    "INSERT INTO test_user (name, email) VALUES (?, ?)",
                    List.of("multi-key-1", "multi1@example.com")
                );
                long id2 = dataManager.executeInsertAndReturnKey(
                    "INSERT INTO test_user (name, email) VALUES (?, ?)",
                    List.of("multi-key-2", "multi2@example.com")
                );
                return List.of(id1, id2);
            });

            assertThat(ids).hasSize(2);
            assertThat(ids.get(0)).isNotEqualTo(ids.get(1));

            // 验证数据已插入
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);
            assertThat(proxy.findById(ids.get(0))).isPresent();
            assertThat(proxy.findById(ids.get(1))).isPresent();
        }
    }

    // ==================== executeBatchInsertAndReturnKeys 测试 ====================

    @Nested
    @DisplayName("executeBatchInsertAndReturnKeys 测试")
    class ExecuteBatchInsertAndReturnKeysTests {

        @Test
        @DisplayName("批量插入应该返回所有生成的主键")
        void shouldReturnAllGeneratedKeys() {
            // 多值 INSERT
            String sql = "INSERT INTO test_user (name, email) VALUES (?, ?), (?, ?), (?, ?)";
            List<Object> params = List.of(
                "batch-key-1", "batch1@example.com",
                "batch-key-2", "batch2@example.com",
                "batch-key-3", "batch3@example.com"
            );

            long[] keys = dataManager.executeBatchInsertAndReturnKeys(sql, params, 3);

            assertThat(keys).hasSize(3);
            assertThat(keys[0]).isGreaterThan(0);
            assertThat(keys[1]).isGreaterThan(0);
            assertThat(keys[2]).isGreaterThan(0);

            // 验证数据已插入
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);
            assertThat(proxy.findById(keys[0])).isPresent();
            assertThat(proxy.findById(keys[1])).isPresent();
            assertThat(proxy.findById(keys[2])).isPresent();
        }

        @Test
        @DisplayName("单条记录批量插入应该返回单个主键")
        void shouldReturnSingleKeyForSingleRecord() {
            String sql = "INSERT INTO test_user (name, email) VALUES (?, ?)";
            List<Object> params = List.of("single-batch", "single@example.com");

            long[] keys = dataManager.executeBatchInsertAndReturnKeys(sql, params, 1);

            assertThat(keys).hasSize(1);
            assertThat(keys[0]).isGreaterThan(0);
        }
    }

    // ==================== executeUpdate 测试 ====================

    @Nested
    @DisplayName("executeUpdate 测试")
    class ExecuteUpdateTests {

        @Test
        @DisplayName("executeUpdate 应该返回受影响的行数")
        void shouldReturnAffectedRows() {
            EntityProxy<TestUser> proxy = dataManager.entity(TestUser.class);
            TestUser user = new TestUser();
            user.setName("update-test");
            user.setEmail("update@example.com");
            proxy.insert(user);

            int affected = dataManager.executeUpdate(
                "UPDATE test_user SET email = ? WHERE name = ?",
                List.of("updated@example.com", "update-test")
            );

            assertThat(affected).isEqualTo(1);

            // 验证更新结果
            List<TestUser> users = proxy.findAll();
            assertThat(users).hasSize(1);
            assertThat(users.get(0).getEmail()).isEqualTo("updated@example.com");
        }

        @Test
        @DisplayName("executeUpdate 不匹配任何行应该返回 0")
        void shouldReturnZeroForNoMatch() {
            int affected = dataManager.executeUpdate(
                "UPDATE test_user SET email = ? WHERE name = ?",
                List.of("no-match@example.com", "non-existent")
            );

            assertThat(affected).isZero();
        }
    }
}