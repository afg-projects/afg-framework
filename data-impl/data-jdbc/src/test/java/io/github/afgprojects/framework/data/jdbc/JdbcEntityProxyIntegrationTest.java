package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.entity.SoftDeleteStrategy;
import io.github.afgprojects.framework.data.core.entity.VersionedEntity;
import io.github.afgprojects.framework.data.core.entity.SoftDeletable;
import io.github.afgprojects.framework.data.core.entity.TimestampSoftDeletable;
import io.github.afgprojects.framework.data.core.exception.OptimisticLockException;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Page;
import io.github.afgprojects.framework.data.core.relation.ManyToMany;
import io.github.afgprojects.framework.data.core.relation.ManyToOne;
import io.github.afgprojects.framework.data.core.scope.DataScope;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JdbcEntityProxy 集成测试
 * <p>
 * 测试 CRUD 操作、条件查询、分页查询等功能。
 * 使用 H2 内存数据库（PostgreSQL 兼容模式）进行集成测试。
 * </p>
 */
@DisplayName("JdbcEntityProxy 集成测试")
class JdbcEntityProxyIntegrationTest {

    private JdbcDataManager dataManager;
    private EntityProxy<TestUser> userProxy;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        dataSource = createDataSource();
        dataManager = new JdbcDataManager(dataSource);
        createTestTable();
        userProxy = dataManager.entity(TestUser.class);
    }

    @AfterEach
    void tearDown() {
        dropTestTable();
    }

    // ==================== 基础 CRUD 测试 ====================

    @Nested
    @DisplayName("插入操作测试")
    class InsertTests {

        @Test
        @DisplayName("应该成功插入实体")
        void shouldInsertEntity() {
            // Given
            TestUser user = new TestUser();
            user.setName("张三");
            user.setEmail("zhangsan@example.com");

            // When
            TestUser inserted = userProxy.insert(user);

            // Then
            assertThat(inserted.getId()).isNotNull();
            assertThat(inserted.getId()).isGreaterThan(0);
            assertThat(inserted.getName()).isEqualTo("张三");
            assertThat(inserted.getEmail()).isEqualTo("zhangsan@example.com");

            // 验证已保存到数据库
            Optional<TestUser> found = userProxy.findById(inserted.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("张三");
        }

        @Test
        @DisplayName("应该成功批量插入实体")
        void shouldInsertAllEntities() {
            // Given
            TestUser u1 = new TestUser(); u1.setName("user1"); u1.setEmail("user1@example.com");
            TestUser u2 = new TestUser(); u2.setName("user2"); u2.setEmail("user2@example.com");
            TestUser u3 = new TestUser(); u3.setName("user3"); u3.setEmail("user3@example.com");
            List<TestUser> users = List.of(u1, u2, u3);

            // When
            List<TestUser> inserted = userProxy.insertAll(users);

            // Then
            assertThat(inserted).hasSize(3);
            assertThat(inserted).allMatch(u -> u.getId() != null);
        }

        @Test
        @DisplayName("应该支持大批量插入并分批处理")
        void shouldInsertLargeBatchWithBatching() {
            // Given - 2500 条记录，默认 batchSize=1000，应该分成 3 批
            List<TestUser> users = new ArrayList<>();
            for (int i = 0; i < 2500; i++) {
                TestUser user = new TestUser();
                user.setName("batch-user-" + i);
                user.setEmail("batch" + i + "@example.com");
                users.add(user);
            }

            // When
            List<TestUser> inserted = userProxy.insertAll(users);

            // Then
            assertThat(inserted).hasSize(2500);
            assertThat(inserted).allMatch(u -> u.getId() != null);

            // 验证数据已插入
            assertThat(userProxy.count()).isEqualTo(2500);
        }

        @Test
        @DisplayName("应该支持自定义批次大小")
        void shouldSupportCustomBatchSize() {
            // Given - 100 条记录，自定义 batchSize=10，应该分成 10 批
            List<TestUser> users = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                TestUser user = new TestUser();
                user.setName("custom-batch-" + i);
                user.setEmail("custom" + i + "@example.com");
                users.add(user);
            }

            // When
            @SuppressWarnings("unchecked")
            EntityProxy<TestUser> customProxy = ((JdbcEntityProxy<TestUser>) userProxy).withBatchSize(10);
            List<TestUser> inserted = customProxy.insertAll(users);

            // Then
            assertThat(inserted).hasSize(100);
            assertThat(inserted).allMatch(u -> u.getId() != null);

            // 验证批次大小配置
            assertThat(((JdbcEntityProxy<TestUser>) customProxy).getBatchSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("空列表批量插入应返回空列表")
        void shouldReturnEmptyForEmptyList() {
            // Given
            List<TestUser> users = List.of();

            // When
            List<TestUser> inserted = userProxy.insertAll(users);

            // Then
            assertThat(inserted).isEmpty();
        }

        @Test
        @DisplayName("单条记录批量插入应正常工作")
        void shouldHandleSingleRecordBatchInsert() {
            // Given
            TestUser user = new TestUser();
            user.setName("single-batch");
            user.setEmail("single@example.com");

            // When
            List<TestUser> inserted = userProxy.insertAll(List.of(user));

            // Then
            assertThat(inserted).hasSize(1);
            assertThat(inserted.get(0).getId()).isNotNull();
        }

        @Test
        @DisplayName("应该正确处理 null 值参数")
        void shouldHandleNullParameters() {
            // Given - email 为 null
            TestUser user = new TestUser();
            user.setName("null-email-user");
            user.setEmail(null);

            // When
            TestUser inserted = userProxy.insert(user);

            // Then
            assertThat(inserted.getId()).isNotNull();
            Optional<TestUser> found = userProxy.findById(inserted.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isNull();
        }
    }

    @Nested
    @DisplayName("更新操作测试")
    class UpdateTests {

        @Test
        @DisplayName("应该成功更新实体")
        void shouldUpdateEntity() {
            // Given
            TestUser user = new TestUser();
            user.setName("原名称");
            user.setEmail("original@example.com");
            user = userProxy.insert(user);

            user.setName("新名称");
            user.setEmail("updated@example.com");

            // When
            TestUser updated = userProxy.update(user);

            // Then
            assertThat(updated.getId()).isEqualTo(user.getId());
            assertThat(updated.getName()).isEqualTo("新名称");

            // 验证数据库中的数据
            Optional<TestUser> found = userProxy.findById(user.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("新名称");
            assertThat(found.get().getEmail()).isEqualTo("updated@example.com");
        }

        @Test
        @DisplayName("应该成功批量更新实体")
        void shouldUpdateAllEntities() {
            // Given
            TestUser u1 = new TestUser(); u1.setName("user1"); u1.setEmail("u1@example.com");
            u1 = userProxy.insert(u1);
            TestUser u2 = new TestUser(); u2.setName("user2"); u2.setEmail("u2@example.com");
            u2 = userProxy.insert(u2);

            u1.setName("updated1"); u1.setEmail("u1new@example.com");
            u2.setName("updated2"); u2.setEmail("u2new@example.com");
            List<TestUser> updates = List.of(u1, u2);

            // When
            List<TestUser> updated = userProxy.updateAll(updates);

            // Then
            assertThat(updated).hasSize(2);
            assertThat(userProxy.findById(u1.getId()).orElseThrow().getName()).isEqualTo("updated1");
            assertThat(userProxy.findById(u2.getId()).orElseThrow().getName()).isEqualTo("updated2");
        }
    }

    @Nested
    @DisplayName("删除操作测试")
    class DeleteTests {

        @Test
        @DisplayName("应该成功删除实体")
        void shouldDeleteEntity() {
            // Given
            TestUser user = new TestUser();
            user.setName("delete-user");
            user.setEmail("delete@example.com");
            user = userProxy.insert(user);

            // When
            userProxy.delete(user);

            // Then
            assertThat(userProxy.findById(user.getId())).isEmpty();
        }

        @Test
        @DisplayName("应该成功按 ID 删除实体")
        void shouldDeleteById() {
            // Given
            TestUser user = new TestUser();
            user.setName("delete-by-id");
            user.setEmail("delete@example.com");
            user = userProxy.insert(user);

            // When
            userProxy.deleteById(user.getId());

            // Then
            assertThat(userProxy.findById(user.getId())).isEmpty();
        }

        @Test
        @DisplayName("应该成功批量删除实体")
        void shouldDeleteAllEntities() {
            // Given
            TestUser u1 = new TestUser(); u1.setName("bulk1"); u1.setEmail("bulk1@example.com"); u1 = userProxy.insert(u1);
            TestUser u2 = new TestUser(); u2.setName("bulk2"); u2.setEmail("bulk2@example.com"); u2 = userProxy.insert(u2);

            // When
            userProxy.deleteAll(List.of(u1, u2));

            // Then
            assertThat(userProxy.findById(u1.getId())).isEmpty();
            assertThat(userProxy.findById(u2.getId())).isEmpty();
        }

        @Test
        @DisplayName("应该成功批量按 ID 删除实体")
        void shouldDeleteAllById() {
            // Given
            TestUser u1 = new TestUser(); u1.setName("bulk-id1"); u1.setEmail("bulk1@example.com"); u1 = userProxy.insert(u1);
            TestUser u2 = new TestUser(); u2.setName("bulk-id2"); u2.setEmail("bulk2@example.com"); u2 = userProxy.insert(u2);

            // When
            userProxy.deleteAllById(List.of(u1.getId(), u2.getId()));

            // Then
            assertThat(userProxy.findById(u1.getId())).isEmpty();
            assertThat(userProxy.findById(u2.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("查询操作测试")
    class QueryTests {

        @Test
        @DisplayName("应该成功按 ID 查找实体")
        void shouldFindById() {
            // Given
            TestUser user = new TestUser();
            user.setName("find-user");
            user.setEmail("find@example.com");
            user = userProxy.insert(user);

            // When
            Optional<TestUser> found = userProxy.findById(user.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("find-user");
        }

        @Test
        @DisplayName("查找不存在 ID 应返回空")
        void shouldReturnEmptyForNonExistentId() {
            // When
            Optional<TestUser> found = userProxy.findById(99999L);

            // Then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("应该成功查找所有实体")
        void shouldFindAll() {
            // Given
            TestUser u1 = new TestUser(); u1.setName("user1"); u1.setEmail("u1@example.com"); userProxy.insert(u1);
            TestUser u2 = new TestUser(); u2.setName("user2"); u2.setEmail("u2@example.com"); userProxy.insert(u2);

            // When
            List<TestUser> all = userProxy.findAll();

            // Then
            assertThat(all).hasSize(2);
        }

        @Test
        @DisplayName("应该成功按多个 ID 查找实体")
        void shouldFindAllById() {
            // Given
            TestUser u1 = new TestUser(); u1.setName("multi1"); u1.setEmail("m1@example.com"); u1 = userProxy.insert(u1);
            TestUser u2 = new TestUser(); u2.setName("multi2"); u2.setEmail("m2@example.com"); u2 = userProxy.insert(u2);
            TestUser u3 = new TestUser(); u3.setName("multi3"); u3.setEmail("m3@example.com"); u3 = userProxy.insert(u3);

            // When
            List<TestUser> found = userProxy.findAllById(List.of(u1.getId(), u3.getId()));

            // Then
            assertThat(found).hasSize(2);
            assertThat(found.stream().map(TestUser::getName).toList())
                .containsExactlyInAnyOrder("multi1", "multi3");
        }

        @Test
        @DisplayName("应该正确统计实体数量")
        void shouldCountEntities() {
            // Given
            TestUser u1 = new TestUser(); u1.setName("count1"); u1.setEmail("c1@example.com"); userProxy.insert(u1);
            TestUser u2 = new TestUser(); u2.setName("count2"); u2.setEmail("c2@example.com"); userProxy.insert(u2);

            // When
            long count = userProxy.count();

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("应该正确判断实体是否存在")
        void shouldCheckEntityExists() {
            // Given
            TestUser user = new TestUser();
            user.setName("exists-user");
            user.setEmail("exists@example.com");
            user = userProxy.insert(user);

            // When & Then
            assertThat(userProxy.existsById(user.getId())).isTrue();
            assertThat(userProxy.existsById(99999L)).isFalse();
        }
    }

    // ==================== 条件查询测试 ====================

    @Nested
    @DisplayName("条件查询测试")
    class ConditionQueryTests {

        @BeforeEach
        void setUpData() {
            TestUser u1 = new TestUser(); u1.setName("Alice"); u1.setEmail("alice@example.com"); userProxy.insert(u1);
            TestUser u2 = new TestUser(); u2.setName("Bob"); u2.setEmail("bob@example.com"); userProxy.insert(u2);
            TestUser u3 = new TestUser(); u3.setName("Charlie"); u3.setEmail("charlie@example.com"); userProxy.insert(u3);
        }

        @Test
        @DisplayName("应该正确使用等于条件查询")
        void shouldQueryWithEqCondition() {
            // Given
            Condition condition = Conditions.eq("name", "Alice");

            // When
            List<TestUser> results = userProxy.findAll(condition);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("应该正确使用 LIKE 条件查询")
        void shouldQueryWithLikeCondition() {
            // Given
            Condition condition = Conditions.like("email", "example");

            // When
            List<TestUser> results = userProxy.findAll(condition);

            // Then
            assertThat(results).hasSize(3);
        }

        @Test
        @DisplayName("应该正确使用 IN 条件查询")
        void shouldQueryWithInCondition() {
            // Given
            Condition condition = Conditions.in("name", List.of("Alice", "Bob"));

            // When
            List<TestUser> results = userProxy.findAll(condition);

            // Then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("应该正确查找单个实体")
        void shouldFindOne() {
            // Given
            Condition condition = Conditions.eq("name", "Alice");

            // When
            Optional<TestUser> result = userProxy.findOne(condition);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("查找多个实体时应抛出异常")
        void shouldThrowWhenFindOneReturnsMultiple() {
            // Given
            Condition condition = Conditions.like("email", "example");

            // When & Then
            assertThatThrownBy(() -> userProxy.findOne(condition))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Expected one result");
        }

        @Test
        @DisplayName("应该正确查找第一个实体")
        void shouldFindFirst() {
            // Given
            Condition condition = Conditions.like("email", "example");

            // When
            Optional<TestUser> result = userProxy.findFirst(condition);

            // Then
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("应该正确判断条件是否存在记录")
        void shouldCheckExistsWithCondition() {
            // Given
            Condition existingCondition = Conditions.eq("name", "Alice");
            Condition nonExistingCondition = Conditions.eq("name", "NonExistent");

            // When & Then
            assertThat(userProxy.exists(existingCondition)).isTrue();
            assertThat(userProxy.exists(nonExistingCondition)).isFalse();
        }

        @Test
        @DisplayName("应该正确统计条件匹配数量")
        void shouldCountWithCondition() {
            // Given
            Condition condition = Conditions.like("email", "example");

            // When
            long count = userProxy.count(condition);

            // Then
            assertThat(count).isEqualTo(3);
        }
    }

    // ==================== 分页查询测试 ====================

    @Nested
    @DisplayName("分页查询测试")
    class PaginationTests {

        @BeforeEach
        void setUpData() {
            for (int i = 1; i <= 15; i++) {
                TestUser user = new TestUser();
                user.setName("User" + i);
                user.setEmail("user" + i + "@example.com");
                userProxy.insert(user);
            }
        }

        @Test
        @DisplayName("应该正确分页查询")
        void shouldPaginateResults() {
            // Given
            Condition condition = Conditions.like("email", "example");
            PageRequest pageRequest = PageRequest.of(1, 5);

            // When
            Page<TestUser> page = userProxy.findAll(condition, pageRequest);

            // Then
            assertThat(page.getContent()).hasSize(5);
            assertThat(page.getTotal()).isEqualTo(15);
            assertThat(page.getPage()).isEqualTo(1);
            assertThat(page.getSize()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(3);
            assertThat(page.hasNext()).isTrue();
            assertThat(page.isFirst()).isTrue();
        }

        @Test
        @DisplayName("应该正确获取第二页数据")
        void shouldGetSecondPage() {
            // Given
            Condition condition = Conditions.like("email", "example");
            PageRequest pageRequest = PageRequest.of(2, 5);

            // When
            Page<TestUser> page = userProxy.findAll(condition, pageRequest);

            // Then
            assertThat(page.getContent()).hasSize(5);
            assertThat(page.getPage()).isEqualTo(2);
            assertThat(page.hasNext()).isTrue();
            assertThat(page.hasPrevious()).isTrue();
        }

        @Test
        @DisplayName("应该正确获取最后一页")
        void shouldGetLastPage() {
            // Given
            Condition condition = Conditions.like("email", "example");
            PageRequest pageRequest = PageRequest.of(3, 5);

            // When
            Page<TestUser> page = userProxy.findAll(condition, pageRequest);

            // Then
            assertThat(page.getContent()).hasSize(5);
            assertThat(page.isLast()).isTrue();
            assertThat(page.hasNext()).isFalse();
        }
    }

    // ==================== 条件更新/删除测试 ====================

    @Nested
    @DisplayName("条件更新/删除测试")
    class ConditionalUpdateDeleteTests {

        @BeforeEach
        void setUpData() {
            TestUser u1 = new TestUser(); u1.setName("update-user"); u1.setEmail("old@example.com"); userProxy.insert(u1);
            TestUser u2 = new TestUser(); u2.setName("delete-user"); u2.setEmail("delete@example.com"); userProxy.insert(u2);
        }

        @Test
        @DisplayName("应该正确条件更新")
        void shouldUpdateWithCondition() {
            // Given
            Condition condition = Conditions.eq("name", "update-user");
            java.util.Map<String, Object> updates = java.util.Map.of("email", "new@example.com");

            // When
            long affected = userProxy.updateAll(condition, updates);

            // Then
            assertThat(affected).isEqualTo(1);
            List<TestUser> updated = userProxy.findAll(Conditions.eq("name", "update-user"));
            assertThat(updated.get(0).getEmail()).isEqualTo("new@example.com");
        }

        @Test
        @DisplayName("应该正确条件删除")
        void shouldDeleteWithCondition() {
            // Given
            Condition condition = Conditions.eq("name", "delete-user");

            // When
            long affected = userProxy.deleteAll(condition);

            // Then
            assertThat(affected).isEqualTo(1);
            assertThat(userProxy.findAll(Conditions.eq("name", "delete-user"))).isEmpty();
        }
    }

    // ==================== 保存操作测试 ====================

    @Nested
    @DisplayName("save 操作测试")
    class SaveTests {

        @Test
        @DisplayName("save 应该自动判断插入新实体")
        void shouldSaveInsertNewEntity() {
            // Given - ID 为 null 表示新实体
            TestUser newUser = new TestUser();
            newUser.setName("new-save-user");
            newUser.setEmail("new@example.com");

            // When
            TestUser saved = userProxy.save(newUser);

            // Then
            assertThat(saved.getId()).isNotNull();
        }

        @Test
        @DisplayName("save 应该自动判断更新已有实体")
        void shouldSaveUpdateExistingEntity() {
            // Given
            TestUser inserted = new TestUser();
            inserted.setName("original");
            inserted.setEmail("original@example.com");
            inserted = userProxy.insert(inserted);

            inserted.setName("updated");
            inserted.setEmail("updated@example.com");

            // When
            TestUser saved = userProxy.save(inserted);

            // Then
            assertThat(saved.getId()).isEqualTo(inserted.getId());
            assertThat(userProxy.findById(inserted.getId()).orElseThrow().getName()).isEqualTo("updated");
        }
    }

    // ==================== 结果映射测试 ====================

    @Nested
    @DisplayName("结果映射测试")
    class ResultMappingTests {

        @Test
        @DisplayName("应该正确映射结果集到实体")
        void shouldMapResultSetToEntity() {
            // Given
            TestUser user = new TestUser();
            user.setName("MappingTest");
            user.setEmail("mapping@example.com");
            user = userProxy.insert(user);

            // When
            TestUser found = userProxy.findById(user.getId()).orElseThrow();

            // Then
            assertThat(found.getId()).isEqualTo(user.getId());
            assertThat(found.getName()).isEqualTo("MappingTest");
            assertThat(found.getEmail()).isEqualTo("mapping@example.com");
        }

        @Test
        @DisplayName("应该正确映射下划线列名到驼峰属性")
        void shouldMapSnakeCaseToCamelCase() {
            // Given - 使用带下划线的列名表
            createSnakeCaseTable();
            EntityProxy<SnakeCaseEntity> proxy = dataManager.entity(SnakeCaseEntity.class);
            SnakeCaseEntity entity = new SnakeCaseEntity();
            entity.setUserName("测试用户");
            entity.setUserDesc("测试描述");
            proxy.insert(entity);

            // When
            SnakeCaseEntity found = proxy.findById(1L).orElseThrow();

            // Then
            assertThat(found.getUserName()).isEqualTo("测试用户");
            assertThat(found.getUserDesc()).isEqualTo("测试描述");

            // Cleanup
            dropSnakeCaseTable();
        }
    }

    // ==================== 企业级特性测试 ====================

    @Nested
    @DisplayName("企业级特性测试")
    class EnterpriseFeatureTests {

        @Test
        @DisplayName("应该正确设置数据作用域")
        void shouldSetDataScope() {
            // Given
            DataScope scope = DataScope.of(
                "test_user",
                "dept_id",
                DataScopeType.DEPT
            );

            // When
            EntityProxy<TestUser> scopedProxy = userProxy.withDataScope(scope);

            // Then
            assertThat(scopedProxy).isNotNull();
        }

        @Test
        @DisplayName("应该正确设置多个数据作用域")
        void shouldSetMultipleDataScopes() {
            // Given
            DataScope scope1 = DataScope.of(
                "test_user",
                "dept_id",
                DataScopeType.DEPT
            );
            DataScope scope2 = DataScope.of(
                "test_user",
                "org_id",
                DataScopeType.DEPT_AND_CHILD
            );

            // When
            EntityProxy<TestUser> scopedProxy = userProxy.withDataScopes(scope1, scope2);

            // Then
            assertThat(scopedProxy).isNotNull();
        }

        @Test
        @DisplayName("应该正确设置租户")
        void shouldSetTenant() {
            // When
            EntityProxy<TestUser> tenantProxy = userProxy.withTenant("tenant-123");

            // Then
            assertThat(tenantProxy).isNotNull();
        }

        @Test
        @DisplayName("应该正确设置数据源")
        void shouldSetDataSource() {
            // When
            EntityProxy<TestUser> dataSourceProxy = userProxy.withDataSource("secondary-ds");

            // Then
            assertThat(dataSourceProxy).isNotNull();
        }

        @Test
        @DisplayName("应该正确设置只读模式")
        void shouldSetReadOnly() {
            // When
            EntityProxy<TestUser> readOnlyProxy = userProxy.withReadOnly();

            // Then
            assertThat(readOnlyProxy).isNotNull();
        }

        @Test
        @DisplayName("应该正确设置包含已删除")
        void shouldSetIncludeDeleted() {
            // When
            EntityProxy<TestUser> includeDeletedProxy = userProxy.includeDeleted();

            // Then
            assertThat(includeDeletedProxy).isNotNull();
        }
    }

    // ==================== 乐观锁测试 ====================

    @Nested
    @DisplayName("乐观锁测试")
    class OptimisticLockTests {

        private EntityProxy<VersionedTestUser> versionedUserProxy;

        @BeforeEach
        void setUpVersionedTable() {
            createVersionedTestTable();
            versionedUserProxy = dataManager.entity(VersionedTestUser.class);
        }

        @AfterEach
        void tearDownVersionedTable() {
            dropVersionedTestTable();
        }

        @Test
        @DisplayName("应该成功插入版本化实体，初始版本号为0")
        void shouldInsertVersionedEntityWithInitialVersion() {
            // Given
            VersionedTestUser user = new VersionedTestUser();
            user.setName("version-user");
            user.setEmail("version@example.com");

            // When
            VersionedTestUser inserted = versionedUserProxy.insert(user);

            // Then
            assertThat(inserted.getId()).isNotNull();
            assertThat(inserted.getVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("应该成功更新版本化实体，版本号递增")
        void shouldUpdateVersionedEntityAndIncrementVersion() {
            // Given
            VersionedTestUser user = new VersionedTestUser();
            user.setName("original-name");
            user.setEmail("original@example.com");
            user = versionedUserProxy.insert(user);
            long originalVersion = user.getVersion();

            // When
            user.setName("updated-name");
            VersionedTestUser updated = versionedUserProxy.update(user);

            // Then
            assertThat(updated.getVersion()).isEqualTo(originalVersion + 1);

            // 验证数据库中的版本号也已更新
            VersionedTestUser found = versionedUserProxy.findById(user.getId()).orElseThrow();
            assertThat(found.getVersion()).isEqualTo(originalVersion + 1);
        }

        @Test
        @DisplayName("版本冲突时应抛出 OptimisticLockException")
        void shouldThrowOptimisticLockExceptionOnVersionConflict() {
            // Given
            VersionedTestUser user = new VersionedTestUser();
            user.setName("conflict-user");
            user.setEmail("conflict@example.com");
            VersionedTestUser insertedUser = versionedUserProxy.insert(user);

            // 模拟另一个事务已更新该记录（版本号已变化）
            VersionedTestUser userInDb = versionedUserProxy.findById(insertedUser.getId()).orElseThrow();
            versionedUserProxy.update(userInDb); // 版本号变为 1

            // 实体仍持有旧版本号 0，尝试更新时应抛出异常
            insertedUser.setName("stale-update");

            // When & Then
            assertThatThrownBy(() -> versionedUserProxy.update(insertedUser))
                .isInstanceOf(OptimisticLockException.class)
                .hasMessageContaining("Optimistic lock conflict")
                .hasMessageContaining("VersionedTestUser")
                .hasMessageContaining("expectedVersion=0");

            // 验证异常信息
            OptimisticLockException ex = assertThrows(
                OptimisticLockException.class,
                () -> versionedUserProxy.update(insertedUser)
            );
            assertThat(ex.getEntityClassName()).isEqualTo("VersionedTestUser");
            assertThat(ex.getEntityId()).isEqualTo(insertedUser.getId());
            assertThat(ex.getExpectedVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("多次更新应正确递增版本号")
        void shouldIncrementVersionOnMultipleUpdates() {
            // Given
            VersionedTestUser user = new VersionedTestUser();
            user.setName("multi-update");
            user.setEmail("multi@example.com");
            user = versionedUserProxy.insert(user);

            // When - 执行 3 次更新
            for (int i = 0; i < 3; i++) {
                user.setName("update-" + i);
                user = versionedUserProxy.update(user);
            }

            // Then
            assertThat(user.getVersion()).isEqualTo(3L);
            VersionedTestUser found = versionedUserProxy.findById(user.getId()).orElseThrow();
            assertThat(found.getVersion()).isEqualTo(3L);
        }

        @Test
        @DisplayName("非版本化实体应正常更新不受影响")
        void shouldUpdateNonVersionedEntityNormally() {
            // Given
            TestUser user = new TestUser();
            user.setName("non-versioned");
            user.setEmail("non-versioned@example.com");
            user = userProxy.insert(user);

            // When
            user.setName("updated-non-versioned");
            TestUser updated = userProxy.update(user);

            // Then
            assertThat(updated.getName()).isEqualTo("updated-non-versioned");
            TestUser found = userProxy.findById(user.getId()).orElseThrow();
            assertThat(found.getName()).isEqualTo("updated-non-versioned");
        }

        @Test
        @DisplayName("批量更新版本化实体应逐个处理版本")
        void shouldUpdateAllVersionedEntities() {
            // Given
            VersionedTestUser u1 = new VersionedTestUser();
            u1.setName("batch1"); u1.setEmail("b1@example.com");
            u1 = versionedUserProxy.insert(u1);

            VersionedTestUser u2 = new VersionedTestUser();
            u2.setName("batch2"); u2.setEmail("b2@example.com");
            u2 = versionedUserProxy.insert(u2);

            // When
            u1.setName("updated-batch1");
            u2.setName("updated-batch2");
            List<VersionedTestUser> updated = versionedUserProxy.updateAll(List.of(u1, u2));

            // Then
            assertThat(updated).hasSize(2);
            assertThat(updated.get(0).getVersion()).isEqualTo(1L);
            assertThat(updated.get(1).getVersion()).isEqualTo(1L);
        }

        private void createVersionedTestTable() {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE versioned_test_user (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(200),
                        version BIGINT DEFAULT 0,
                        create_time TIMESTAMP,
                        update_time TIMESTAMP
                    )
                    """);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create versioned test table", e);
            }
        }

        private void dropVersionedTestTable() {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS versioned_test_user");
            } catch (Exception ignored) {
            }
        }
    }

    // ==================== 辅助方法 ====================

    private DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        // 使用 PostgreSQL 兼容模式
        ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private DataSource createCacheDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        // 使用独立的内存数据库用于缓存测试
        ds.setURL("jdbc:h2:mem:cachedb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
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

    private void createSnakeCaseTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE snake_case_entity (
                    id SERIAL PRIMARY KEY,
                    user_name VARCHAR(100),
                    user_desc VARCHAR(200)
                )
                """);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create snake_case table", e);
        }
    }

    private void dropSnakeCaseTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS snake_case_entity");
        } catch (Exception ignored) {
        }
    }

    /**
     * 测试用户实体
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestUser {
        private Long id;
        private String name;
        private String email;
    }

    /**
     * 下划线列名测试实体
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class SnakeCaseEntity {
        private Long id;
        private String userName;
        private String userDesc;
    }

    /**
     * 版本化测试用户实体（用于乐观锁测试）
     */
    @Data
    @NoArgsConstructor
    @lombok.EqualsAndHashCode(callSuper = true)
    static class VersionedTestUser extends VersionedEntity<Long> {
        private String name;
        private String email;
    }

    // ==================== 软删除实体测试 ====================

    @Nested
    @DisplayName("软删除实体测试")
    class SoftDeleteEntityTests {

        private EntityProxy<SoftDeleteUser> softDeleteProxy;
        private JdbcEntityProxy<SoftDeleteUser> jdbcSoftDeleteProxy;

        @BeforeEach
        void setUpSoftDeleteTable() {
            createSoftDeleteTable();
            softDeleteProxy = dataManager.entity(SoftDeleteUser.class);
            jdbcSoftDeleteProxy = (JdbcEntityProxy<SoftDeleteUser>) softDeleteProxy;
        }

        @AfterEach
        void tearDownSoftDeleteTable() {
            dropSoftDeleteTable();
        }

        @Test
        @DisplayName("软删除实体应正确识别删除策略")
        void shouldRecognizeSoftDeleteStrategy() {
            assertThat(jdbcSoftDeleteProxy.isSoftDeletable()).isTrue();
            assertThat(jdbcSoftDeleteProxy.getSoftDeleteStrategy())
                    .isEqualTo(SoftDeleteStrategy.BOOLEAN);
        }

        @Test
        @DisplayName("删除操作应执行软删除而非物理删除")
        void shouldSoftDeleteInsteadOfPhysicalDelete() {
            // Given
            SoftDeleteUser user = new SoftDeleteUser();
            user.setName("soft-delete-user");
            user.setEmail("soft@example.com");
            user = softDeleteProxy.insert(user);

            // When
            softDeleteProxy.delete(user);

            // Then - 记录仍在数据库中，但标记为已删除
            assertThat(softDeleteProxy.findById(user.getId())).isEmpty();
            // 使用 includeDeleted 可以查到
            List<SoftDeleteUser> allWithDeleted = jdbcSoftDeleteProxy.includeDeleted().findAll();
            assertThat(allWithDeleted).hasSize(1);
        }

        @Test
        @DisplayName("deleteById 应执行软删除")
        void shouldSoftDeleteById() {
            // Given
            SoftDeleteUser user = new SoftDeleteUser();
            user.setName("soft-delete-by-id");
            user.setEmail("softbyid@example.com");
            user = softDeleteProxy.insert(user);

            // When
            softDeleteProxy.deleteById(user.getId());

            // Then - 记录仍在数据库中，但标记为已删除
            assertThat(softDeleteProxy.findById(user.getId())).isEmpty();
        }

        @Test
        @DisplayName("deleteAll 应执行软删除")
        void shouldSoftDeleteAll() {
            // Given
            SoftDeleteUser u1 = new SoftDeleteUser();
            u1.setName("soft-delete-all-1");
            u1.setEmail("sda1@example.com");
            softDeleteProxy.insert(u1);

            SoftDeleteUser u2 = new SoftDeleteUser();
            u2.setName("soft-delete-all-2");
            u2.setEmail("sda2@example.com");
            softDeleteProxy.insert(u2);

            // When
            softDeleteProxy.deleteAll(List.of(u1, u2));

            // Then
            assertThat(softDeleteProxy.findById(u1.getId())).isEmpty();
            assertThat(softDeleteProxy.findById(u2.getId())).isEmpty();
        }

        @Test
        @DisplayName("hardDelete 应物理删除记录")
        void shouldHardDelete() {
            // Given
            SoftDeleteUser user = new SoftDeleteUser();
            user.setName("hard-delete-user");
            user.setEmail("hard@example.com");
            user = softDeleteProxy.insert(user);
            Long userId = user.getId();

            // When
            jdbcSoftDeleteProxy.hardDelete(user);

            // Then - 记录完全被删除
            List<SoftDeleteUser> allWithDeleted = jdbcSoftDeleteProxy.includeDeleted().findAll();
            assertThat(allWithDeleted.stream().noneMatch(u -> u.getId().equals(userId))).isTrue();
        }

        @Test
        @DisplayName("hardDeleteById 应物理删除记录")
        void shouldHardDeleteById() {
            // Given
            SoftDeleteUser user = new SoftDeleteUser();
            user.setName("hard-delete-by-id");
            user.setEmail("hardbyid@example.com");
            user = softDeleteProxy.insert(user);
            Long userId = user.getId();

            // When
            jdbcSoftDeleteProxy.hardDeleteById(userId);

            // Then - 记录完全被删除
            List<SoftDeleteUser> allWithDeleted = jdbcSoftDeleteProxy.includeDeleted().findAll();
            assertThat(allWithDeleted.stream().noneMatch(u -> u.getId().equals(userId))).isTrue();
        }

        @Test
        @DisplayName("hardDeleteAllById 应物理删除多条记录")
        void shouldHardDeleteAllById() {
            // Given
            SoftDeleteUser u1 = new SoftDeleteUser();
            u1.setName("hard-delete-all-1");
            u1.setEmail("hda1@example.com");
            softDeleteProxy.insert(u1);

            SoftDeleteUser u2 = new SoftDeleteUser();
            u2.setName("hard-delete-all-2");
            u2.setEmail("hda2@example.com");
            softDeleteProxy.insert(u2);

            // When
            jdbcSoftDeleteProxy.hardDeleteAllById(List.of(u1.getId(), u2.getId()));

            // Then
            List<SoftDeleteUser> allWithDeleted = jdbcSoftDeleteProxy.includeDeleted().findAll();
            assertThat(allWithDeleted).isEmpty();
        }

        @Test
        @DisplayName("restoreById 应恢复已软删除的记录")
        void shouldRestoreById() {
            // Given
            SoftDeleteUser user = new SoftDeleteUser();
            user.setName("restore-user");
            user.setEmail("restore@example.com");
            user = softDeleteProxy.insert(user);
            softDeleteProxy.delete(user); // 软删除

            // When
            softDeleteProxy.restoreById(user.getId());

            // Then
            assertThat(softDeleteProxy.findById(user.getId())).isPresent();
        }

        @Test
        @DisplayName("restoreAllById 应恢复多条已软删除的记录")
        void shouldRestoreAllById() {
            // Given
            SoftDeleteUser u1 = new SoftDeleteUser();
            u1.setName("restore-all-1");
            u1.setEmail("ra1@example.com");
            softDeleteProxy.insert(u1);

            SoftDeleteUser u2 = new SoftDeleteUser();
            u2.setName("restore-all-2");
            u2.setEmail("ra2@example.com");
            softDeleteProxy.insert(u2);

            softDeleteProxy.deleteAll(List.of(u1, u2)); // 软删除

            // When
            softDeleteProxy.restoreAllById(List.of(u1.getId(), u2.getId()));

            // Then
            assertThat(softDeleteProxy.findById(u1.getId())).isPresent();
            assertThat(softDeleteProxy.findById(u2.getId())).isPresent();
        }

        @Test
        @DisplayName("findAll 应自动过滤已删除记录")
        void shouldFilterDeletedRecords() {
            // Given
            SoftDeleteUser u1 = new SoftDeleteUser();
            u1.setName("active-user");
            u1.setEmail("active@example.com");
            softDeleteProxy.insert(u1);

            SoftDeleteUser u2 = new SoftDeleteUser();
            u2.setName("deleted-user");
            u2.setEmail("deleted@example.com");
            softDeleteProxy.insert(u2);
            softDeleteProxy.delete(u2); // 软删除

            // When
            List<SoftDeleteUser> activeUsers = softDeleteProxy.findAll();

            // Then
            assertThat(activeUsers).hasSize(1);
            assertThat(activeUsers.get(0).getName()).isEqualTo("active-user");
        }

        @Test
        @DisplayName("count 应自动过滤已删除记录")
        void shouldCountOnlyActiveRecords() {
            // Given
            SoftDeleteUser u1 = new SoftDeleteUser();
            u1.setName("count-active");
            u1.setEmail("active@example.com");
            softDeleteProxy.insert(u1);

            SoftDeleteUser u2 = new SoftDeleteUser();
            u2.setName("count-deleted");
            u2.setEmail("deleted@example.com");
            softDeleteProxy.insert(u2);
            softDeleteProxy.delete(u2); // 软删除

            // When
            long count = softDeleteProxy.count();

            // Then
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("includeDeleted 应包含已删除记录")
        void shouldIncludeDeletedRecords() {
            // Given
            SoftDeleteUser u1 = new SoftDeleteUser();
            u1.setName("include-active");
            u1.setEmail("active@example.com");
            softDeleteProxy.insert(u1);

            SoftDeleteUser u2 = new SoftDeleteUser();
            u2.setName("include-deleted");
            u2.setEmail("deleted@example.com");
            softDeleteProxy.insert(u2);
            softDeleteProxy.delete(u2); // 软删除

            // When
            List<SoftDeleteUser> allUsers = jdbcSoftDeleteProxy.includeDeleted().findAll();

            // Then
            assertThat(allUsers).hasSize(2);
        }

        private void createSoftDeleteTable() {
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
                throw new RuntimeException("Failed to create soft delete table", e);
            }
        }

        private void dropSoftDeleteTable() {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS soft_delete_user");
            } catch (Exception ignored) {
            }
        }
    }

    // ==================== 时间戳软删除实体测试 ====================

    @Nested
    @DisplayName("时间戳软删除实体测试")
    class TimestampSoftDeleteEntityTests {

        private EntityProxy<TimestampSoftDeleteUser> timestampProxy;
        private JdbcEntityProxy<TimestampSoftDeleteUser> jdbcTimestampProxy;

        @BeforeEach
        void setUpTimestampSoftDeleteTable() {
            createTimestampSoftDeleteTable();
            timestampProxy = dataManager.entity(TimestampSoftDeleteUser.class);
            jdbcTimestampProxy = (JdbcEntityProxy<TimestampSoftDeleteUser>) timestampProxy;
        }

        @AfterEach
        void tearDownTimestampSoftDeleteTable() {
            dropTimestampSoftDeleteTable();
        }

        @Test
        @DisplayName("时间戳软删除实体应识别TIMESTAMP策略")
        void shouldRecognizeTimestampStrategy() {
            assertThat(jdbcTimestampProxy.isSoftDeletable()).isTrue();
            assertThat(jdbcTimestampProxy.getSoftDeleteStrategy())
                    .isEqualTo(SoftDeleteStrategy.TIMESTAMP);
        }

        @Test
        @DisplayName("时间戳软删除应设置deletedAt字段")
        void shouldSoftDeleteWithTimestamp() {
            // Given
            TimestampSoftDeleteUser user = new TimestampSoftDeleteUser();
            user.setName("timestamp-delete");
            user.setEmail("timestamp@example.com");
            user = timestampProxy.insert(user);

            // When
            timestampProxy.delete(user);

            // Then
            assertThat(timestampProxy.findById(user.getId())).isEmpty();
            // 验证记录仍在数据库中（使用includeDeleted）
            List<TimestampSoftDeleteUser> allWithDeleted = jdbcTimestampProxy.includeDeleted().findAll();
            assertThat(allWithDeleted).hasSize(1);
        }

        @Test
        @DisplayName("时间戳软删除恢复应清除deletedAt字段")
        void shouldRestoreTimestampSoftDelete() {
            // Given
            TimestampSoftDeleteUser user = new TimestampSoftDeleteUser();
            user.setName("timestamp-restore");
            user.setEmail("restore@example.com");
            user = timestampProxy.insert(user);
            timestampProxy.delete(user);

            // When
            timestampProxy.restoreById(user.getId());

            // Then
            assertThat(timestampProxy.findById(user.getId())).isPresent();
        }

        @Test
        @DisplayName("时间戳软删除 findAll 应自动过滤已删除记录")
        void shouldFilterDeletedRecordsInFindAll() {
            // Given
            TimestampSoftDeleteUser u1 = new TimestampSoftDeleteUser();
            u1.setName("ts-active");
            u1.setEmail("ts-active@example.com");
            timestampProxy.insert(u1);

            TimestampSoftDeleteUser u2 = new TimestampSoftDeleteUser();
            u2.setName("ts-deleted");
            u2.setEmail("ts-deleted@example.com");
            timestampProxy.insert(u2);
            timestampProxy.delete(u2);

            // When
            List<TimestampSoftDeleteUser> activeUsers = timestampProxy.findAll();

            // Then
            assertThat(activeUsers).hasSize(1);
            assertThat(activeUsers.get(0).getName()).isEqualTo("ts-active");
        }

        @Test
        @DisplayName("时间戳软删除 count 应自动过滤已删除记录")
        void shouldCountOnlyActiveRecords() {
            // Given
            TimestampSoftDeleteUser u1 = new TimestampSoftDeleteUser();
            u1.setName("ts-count-active");
            u1.setEmail("ts-count-active@example.com");
            timestampProxy.insert(u1);

            TimestampSoftDeleteUser u2 = new TimestampSoftDeleteUser();
            u2.setName("ts-count-deleted");
            u2.setEmail("ts-count-deleted@example.com");
            timestampProxy.insert(u2);
            timestampProxy.delete(u2);

            // When
            long count = timestampProxy.count();

            // Then
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("时间戳软删除条件查询应自动过滤已删除记录")
        void shouldFilterDeletedInConditionQuery() {
            // Given
            TimestampSoftDeleteUser user = new TimestampSoftDeleteUser();
            user.setName("ts-condition");
            user.setEmail("ts-condition@example.com");
            user = timestampProxy.insert(user);
            timestampProxy.delete(user);

            // When
            Condition condition = Conditions.eq("name", "ts-condition");
            List<TimestampSoftDeleteUser> results = timestampProxy.findAll(condition);

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("时间戳软删除 includeDeleted 应包含已删除记录")
        void shouldIncludeDeletedRecords() {
            // Given
            TimestampSoftDeleteUser u1 = new TimestampSoftDeleteUser();
            u1.setName("ts-include-active");
            u1.setEmail("ts-include-active@example.com");
            timestampProxy.insert(u1);

            TimestampSoftDeleteUser u2 = new TimestampSoftDeleteUser();
            u2.setName("ts-include-deleted");
            u2.setEmail("ts-include-deleted@example.com");
            timestampProxy.insert(u2);
            timestampProxy.delete(u2);

            // When
            List<TimestampSoftDeleteUser> allUsers = jdbcTimestampProxy.includeDeleted().findAll();

            // Then
            assertThat(allUsers).hasSize(2);
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
                throw new RuntimeException("Failed to create timestamp soft delete table", e);
            }
        }

        private void dropTimestampSoftDeleteTable() {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS timestamp_soft_delete_user");
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 软删除测试用户实体
     */
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

    /**
     * 时间戳软删除测试用户实体
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TimestampSoftDeleteUser implements TimestampSoftDeletable {
        private Long id;
        private String name;
        private String email;
        private java.time.LocalDateTime deletedAt;

        @Override
        public boolean isDeleted() {
            return deletedAt != null;
        }

        @Override
        public java.time.LocalDateTime getDeletedAt() {
            return deletedAt;
        }

        @Override
        public void setDeletedAt(java.time.LocalDateTime deletedAt) {
            this.deletedAt = deletedAt;
        }
    }

    // ==================== 缓存测试 ====================

    @Nested
    @DisplayName("缓存功能测试")
    class CacheTests {

        private JdbcDataManager dataManagerWithCache;
        private EntityCacheManager cacheManager;
        private DataSource cacheDataSource;
        private EntityProxy<TestUser> cachedUserProxy;

        @BeforeEach
        void setUpCache() {
            // 创建独立的H2数据库用于缓存测试
            cacheDataSource = createCacheDataSource();

            // 创建带缓存的 DataManager
            CacheProperties cacheProperties = new CacheProperties();
            cacheProperties.setEnabled(true);
            cacheProperties.setCacheNull(true);
            DefaultCacheManager defaultCacheManager = new DefaultCacheManager(cacheProperties);
            EntityCacheProperties properties = new EntityCacheProperties();
            properties.setEnabled(true);
            properties.setCacheNull(true);
            cacheManager = new EntityCacheManager(defaultCacheManager, properties);

            dataManagerWithCache = new JdbcDataManager(cacheDataSource);
            dataManagerWithCache.setCacheManager(cacheManager);

            createCacheTestTable();
            cachedUserProxy = dataManagerWithCache.entity(TestUser.class);
        }

        @AfterEach
        void tearDownCache() {
            dropCacheTestTable();
        }

        @Test
        @DisplayName("findById 应该从缓存命中数据")
        void shouldFindByIdFromCache() {
            // Given - 插入用户
            TestUser user = new TestUser();
            user.setName("cached-user");
            user.setEmail("cached@example.com");
            user = cachedUserProxy.insert(user);

            // When - 第一次查询（从数据库加载并缓存）
            Optional<TestUser> firstResult = cachedUserProxy.findById(user.getId());

            // Then
            assertThat(firstResult).isPresent();
            assertThat(firstResult.get().getName()).isEqualTo("cached-user");

            // When - 第二次查询（应从缓存命中）
            Optional<TestUser> secondResult = cachedUserProxy.findById(user.getId());

            // Then
            assertThat(secondResult).isPresent();
            assertThat(secondResult.get().getName()).isEqualTo("cached-user");
        }

        @Test
        @DisplayName("更新操作应该失效缓存")
        void shouldEvictCacheOnUpdate() {
            // Given - 插入用户
            TestUser user = new TestUser();
            user.setName("original-name");
            user.setEmail("original@example.com");
            user = cachedUserProxy.insert(user);

            // 首次查询，缓存数据
            Optional<TestUser> cached = cachedUserProxy.findById(user.getId());
            assertThat(cached).isPresent();
            assertThat(cached.get().getName()).isEqualTo("original-name");

            // When - 更新用户
            user.setName("updated-name");
            cachedUserProxy.update(user);

            // Then - 再次查询应获取新数据
            Optional<TestUser> afterUpdate = cachedUserProxy.findById(user.getId());
            assertThat(afterUpdate).isPresent();
            assertThat(afterUpdate.get().getName()).isEqualTo("updated-name");
        }

        @Test
        @DisplayName("删除操作应该失效缓存")
        void shouldEvictCacheOnDelete() {
            // Given - 插入用户
            TestUser user = new TestUser();
            user.setName("delete-cache-user");
            user.setEmail("delete-cache@example.com");
            user = cachedUserProxy.insert(user);

            // 首次查询，缓存数据
            Optional<TestUser> cached = cachedUserProxy.findById(user.getId());
            assertThat(cached).isPresent();

            // When - 删除用户
            cachedUserProxy.delete(user);

            // Then - 再次查询应返回空
            Optional<TestUser> afterDelete = cachedUserProxy.findById(user.getId());
            assertThat(afterDelete).isEmpty();
        }

        @Test
        @DisplayName("查询不存在的ID应缓存null标记")
        void shouldCacheNullForNonExistentId() {
            // Given - 一个不存在的ID
            Long nonExistentId = 99999L;

            // When - 首次查询（不存在的记录）
            Optional<TestUser> firstResult = cachedUserProxy.findById(nonExistentId);

            // Then
            assertThat(firstResult).isEmpty();

            // When - 再次查询（应从缓存的null标记返回）
            Optional<TestUser> secondResult = cachedUserProxy.findById(nonExistentId);

            // Then
            assertThat(secondResult).isEmpty();
        }

        @Test
        @DisplayName("禁用缓存时应直接查询数据库")
        void shouldQueryDatabaseWhenCacheDisabled() {
            // Given - 使用不带缓存的 DataManager
            JdbcDataManager noCacheDataManager = new JdbcDataManager(cacheDataSource);
            EntityProxy<TestUser> noCacheProxy = noCacheDataManager.entity(TestUser.class);

            TestUser user = new TestUser();
            user.setName("no-cache-user");
            user.setEmail("no-cache@example.com");
            user = noCacheProxy.insert(user);

            // When - 查询
            Optional<TestUser> result = noCacheProxy.findById(user.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("no-cache-user");
        }

        private void createCacheTestTable() {
            try (Connection conn = cacheDataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS test_user");
                stmt.execute("""
                    CREATE TABLE test_user (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(200)
                    )
                    """);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create cache test table", e);
            }
        }

        private void dropCacheTestTable() {
            try (Connection conn = cacheDataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS test_user");
            } catch (Exception ignored) {
            }
        }
    }

    // ==================== 事务内插入测试 ====================

    @Nested
    @DisplayName("事务内插入测试")
    class TransactionInsertTests {

        @Test
        @DisplayName("事务内插入应正确返回生成的主键")
        void shouldInsertAndReturnKeyInTransaction() {
            // Given
            TestUser user1 = new TestUser();
            user1.setName("tx-user-1");
            user1.setEmail("tx1@example.com");

            TestUser user2 = new TestUser();
            user2.setName("tx-user-2");
            user2.setEmail("tx2@example.com");

            // When - 在事务中插入两个用户
            List<TestUser> insertedUsers = dataManager.executeInTransaction(() -> {
                TestUser u1 = userProxy.insert(user1);
                TestUser u2 = userProxy.insert(user2);
                return List.of(u1, u2);
            });

            // Then
            assertThat(insertedUsers).hasSize(2);
            assertThat(insertedUsers.get(0).getId()).isNotNull();
            assertThat(insertedUsers.get(1).getId()).isNotNull();
            assertThat(insertedUsers.get(0).getId()).isNotEqualTo(insertedUsers.get(1).getId());

            // 验证数据已提交
            assertThat(userProxy.findById(insertedUsers.get(0).getId())).isPresent();
            assertThat(userProxy.findById(insertedUsers.get(1).getId())).isPresent();
        }

        @Test
        @DisplayName("事务内操作应正确执行")
        void shouldExecuteOperationsInTransaction() {
            // Given
            TestUser user1 = new TestUser();
            user1.setName("tx-user-1");
            user1.setEmail("tx1@example.com");

            TestUser user2 = new TestUser();
            user2.setName("tx-user-2");
            user2.setEmail("tx2@example.com");

            // When - 在事务中插入两个用户
            List<TestUser> insertedUsers = dataManager.executeInTransaction(() -> {
                TestUser inserted1 = userProxy.insert(user1);
                TestUser inserted2 = userProxy.insert(user2);
                return List.of(inserted1, inserted2);
            });

            // Then
            assertThat(insertedUsers).hasSize(2);
            assertThat(insertedUsers.get(0).getId()).isNotNull();
            assertThat(insertedUsers.get(1).getId()).isNotNull();
            assertThat(userProxy.findById(insertedUsers.get(0).getId())).isPresent();
            assertThat(userProxy.findById(insertedUsers.get(1).getId())).isPresent();
        }

        @Test
        @DisplayName("批量插入事务应正确处理")
        void shouldHandleBatchInsertInTransaction() {
            // Given
            List<TestUser> users = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                TestUser user = new TestUser();
                user.setName("batch-tx-user-" + i);
                user.setEmail("batch-tx" + i + "@example.com");
                users.add(user);
            }

            // When - 在事务中批量插入
            List<TestUser> inserted = dataManager.executeInTransaction(() -> userProxy.insertAll(users));

            // Then
            assertThat(inserted).hasSize(10);
            assertThat(inserted).allMatch(u -> u.getId() != null);
        }
    }

    // ==================== 条件查询边界测试 ====================

    @Nested
    @DisplayName("条件查询边界测试")
    class ConditionQueryBoundaryTests {

        @BeforeEach
        void setUpData() {
            TestUser u1 = new TestUser();
            u1.setName("Alice");
            u1.setEmail("alice@example.com");
            userProxy.insert(u1);

            TestUser u2 = new TestUser();
            u2.setName("Bob");
            u2.setEmail("bob@example.com");
            userProxy.insert(u2);

            TestUser u3 = new TestUser();
            u3.setName("Charlie");
            u3.setEmail("charlie@example.com");
            userProxy.insert(u3);
        }

        @Test
        @DisplayName("findOne 无结果时应返回空")
        void shouldReturnEmptyWhenNoMatch() {
            // Given
            Condition condition = Conditions.eq("name", "NonExistent");

            // When
            Optional<TestUser> result = userProxy.findOne(condition);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("findOne 单结果时应正确返回")
        void shouldReturnOneWhenSingleMatch() {
            // Given
            Condition condition = Conditions.eq("name", "Alice");

            // When
            Optional<TestUser> result = userProxy.findOne(condition);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("findOne 多结果时应抛出异常")
        void shouldThrowExceptionWhenMultipleResults() {
            // Given
            Condition condition = Conditions.like("email", "example");

            // When & Then
            assertThatThrownBy(() -> userProxy.findOne(condition))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Expected one result but got");
        }

        @Test
        @DisplayName("findFirst 无结果时应返回空")
        void shouldReturnEmptyWhenNoMatchForFindFirst() {
            // Given
            Condition condition = Conditions.eq("name", "NonExistent");

            // When
            Optional<TestUser> result = userProxy.findFirst(condition);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("findFirst 多结果时应返回第一条")
        void shouldReturnFirstWhenMultipleResults() {
            // Given
            Condition condition = Conditions.like("email", "example");

            // When
            Optional<TestUser> result = userProxy.findFirst(condition);

            // Then
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("findAll 空条件结果时应返回空列表")
        void shouldReturnEmptyListWhenNoMatch() {
            // Given
            Condition condition = Conditions.eq("name", "NonExistent");

            // When
            List<TestUser> results = userProxy.findAll(condition);

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("count 空条件结果时应返回0")
        void shouldReturnZeroCountWhenNoMatch() {
            // Given
            Condition condition = Conditions.eq("name", "NonExistent");

            // When
            long count = userProxy.count(condition);

            // Then
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("exists 空条件结果时应返回false")
        void shouldReturnFalseExistsWhenNoMatch() {
            // Given
            Condition condition = Conditions.eq("name", "NonExistent");

            // When
            boolean exists = userProxy.exists(condition);

            // Then
            assertThat(exists).isFalse();
        }
    }

    // ==================== 软删除过滤测试 ====================

    @Nested
    @DisplayName("软删除过滤测试")
    class SoftDeleteFilterTests {

        private EntityProxy<SoftDeleteUser> softDeleteProxy;
        private JdbcEntityProxy<SoftDeleteUser> jdbcSoftDeleteProxy;

        @BeforeEach
        void setUpSoftDeleteTable() {
            createSoftDeleteTable();
            softDeleteProxy = dataManager.entity(SoftDeleteUser.class);
            jdbcSoftDeleteProxy = (JdbcEntityProxy<SoftDeleteUser>) softDeleteProxy;
        }

        @AfterEach
        void tearDownSoftDeleteTable() {
            dropSoftDeleteTable();
        }

        @Test
        @DisplayName("includeDeleted 应查询包含已删除记录")
        void shouldIncludeDeletedRecordsWithFlag() {
            // Given - 插入两个用户
            SoftDeleteUser u1 = new SoftDeleteUser();
            u1.setName("active-user");
            u1.setEmail("active@example.com");
            softDeleteProxy.insert(u1);

            SoftDeleteUser u2 = new SoftDeleteUser();
            u2.setName("deleted-user");
            u2.setEmail("deleted@example.com");
            softDeleteProxy.insert(u2);

            // 软删除第二个用户
            softDeleteProxy.delete(u2);

            // When - 普通查询（不包含已删除）
            List<SoftDeleteUser> normalResults = softDeleteProxy.findAll();

            // Then
            assertThat(normalResults).hasSize(1);
            assertThat(normalResults.get(0).getName()).isEqualTo("active-user");

            // When - 使用 includeDeleted 查询
            List<SoftDeleteUser> allResults = jdbcSoftDeleteProxy.includeDeleted().findAll();

            // Then
            assertThat(allResults).hasSize(2);
        }

        @Test
        @DisplayName("includeDeleted findById 应查询已删除记录")
        void shouldFindDeletedByIdWithIncludeDeleted() {
            // Given
            SoftDeleteUser user = new SoftDeleteUser();
            user.setName("deleted-find-user");
            user.setEmail("deleted-find@example.com");
            user = softDeleteProxy.insert(user);

            // 软删除
            softDeleteProxy.delete(user);

            // When - 普通查询（找不到）
            Optional<SoftDeleteUser> normalResult = softDeleteProxy.findById(user.getId());

            // Then
            assertThat(normalResult).isEmpty();

            // When - 使用 includeDeleted 查询
            Optional<SoftDeleteUser> withDeletedResult = jdbcSoftDeleteProxy.includeDeleted().findById(user.getId());

            // Then
            assertThat(withDeletedResult).isPresent();
            assertThat(withDeletedResult.get().getName()).isEqualTo("deleted-find-user");
        }

        @Test
        @DisplayName("includeDeleted count 应包含已删除记录")
        void shouldCountDeletedWithIncludeDeleted() {
            // Given
            SoftDeleteUser u1 = new SoftDeleteUser();
            u1.setName("active-count");
            u1.setEmail("active-count@example.com");
            softDeleteProxy.insert(u1);

            SoftDeleteUser u2 = new SoftDeleteUser();
            u2.setName("deleted-count");
            u2.setEmail("deleted-count@example.com");
            softDeleteProxy.insert(u2);
            softDeleteProxy.delete(u2);

            // When - 普通计数
            long normalCount = softDeleteProxy.count();

            // Then
            assertThat(normalCount).isEqualTo(1);

            // When - 使用 includeDeleted 计数
            long allCount = jdbcSoftDeleteProxy.includeDeleted().count();

            // Then
            assertThat(allCount).isEqualTo(2);
        }

        @Test
        @DisplayName("includeDeleted 条件查询应包含已删除记录")
        void shouldIncludeDeletedInConditionQuery() {
            // Given
            SoftDeleteUser user = new SoftDeleteUser();
            user.setName("condition-deleted");
            user.setEmail("condition-deleted@example.com");
            user = softDeleteProxy.insert(user);
            softDeleteProxy.delete(user);

            // When - 普通条件查询
            Condition condition = Conditions.eq("name", "condition-deleted");
            List<SoftDeleteUser> normalResults = softDeleteProxy.findAll(condition);

            // Then
            assertThat(normalResults).isEmpty();

            // When - 使用 includeDeleted 条件查询
            List<SoftDeleteUser> allResults = jdbcSoftDeleteProxy.includeDeleted().findAll(condition);

            // Then
            assertThat(allResults).hasSize(1);
        }

        @Test
        @DisplayName("includeDeleted 分页查询应包含已删除记录")
        void shouldIncludeDeletedInPageQuery() {
            // Given
            SoftDeleteUser u1 = new SoftDeleteUser();
            u1.setName("page-active");
            u1.setEmail("page-active@example.com");
            softDeleteProxy.insert(u1);

            SoftDeleteUser u2 = new SoftDeleteUser();
            u2.setName("page-deleted");
            u2.setEmail("page-deleted@example.com");
            softDeleteProxy.insert(u2);
            softDeleteProxy.delete(u2);

            // When - 普通分页查询
            Condition condition = Conditions.like("email", "example");
            Page<SoftDeleteUser> normalPage = softDeleteProxy.findAll(condition, PageRequest.of(1, 10));

            // Then
            assertThat(normalPage.getTotal()).isEqualTo(1);

            // When - 使用 includeDeleted 分页查询
            Page<SoftDeleteUser> allPage = jdbcSoftDeleteProxy.includeDeleted().findAll(condition, PageRequest.of(1, 10));

            // Then
            assertThat(allPage.getTotal()).isEqualTo(2);
        }

        private void createSoftDeleteTable() {
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
                throw new RuntimeException("Failed to create soft delete table", e);
            }
        }

        private void dropSoftDeleteTable() {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS soft_delete_user");
            } catch (Exception ignored) {
            }
        }
    }

    // ==================== 关联查询测试 ====================

    @Nested
    @DisplayName("关联查询测试")
    class AssociationTests {

        private EntityProxy<TestDepartment> deptProxy;
        private EntityProxy<TestUserWithDept> userWithDeptProxy;
        private EntityProxy<TestUserWithRoles> userWithRolesProxy;
        private EntityProxy<TestRole> roleProxy;

        @BeforeEach
        void setUpAssociationTables() {
            createAssociationTables();
            deptProxy = dataManager.entity(TestDepartment.class);
            userWithDeptProxy = dataManager.entity(TestUserWithDept.class);
            userWithRolesProxy = dataManager.entity(TestUserWithRoles.class);
            roleProxy = dataManager.entity(TestRole.class);
        }

        @AfterEach
        void tearDownAssociationTables() {
            dropAssociationTables();
        }

        @Test
        @DisplayName("withAssociation 应正确配置关联加载")
        void shouldConfigureAssociationLoading() {
            // When
            EntityProxy<TestUserWithDept> configuredProxy = userWithDeptProxy.withAssociation("department");

            // Then
            assertThat(configuredProxy).isNotNull();
            assertThat(((JdbcEntityProxy<TestUserWithDept>) configuredProxy).getEagerFetchAssociations())
                .contains("department");
        }

        @Test
        @DisplayName("withAssociations 应配置多个关联加载")
        void shouldConfigureMultipleAssociations() {
            // When
            EntityProxy<TestUserWithDept> configuredProxy = userWithDeptProxy.withAssociations("department");

            // Then
            assertThat(((JdbcEntityProxy<TestUserWithDept>) configuredProxy).getEagerFetchAssociations())
                .contains("department");
        }

        @Test
        @DisplayName("withAssociation 不存在的关联应抛出异常")
        void shouldThrowWhenAssociationNotFound() {
            // When & Then
            assertThatThrownBy(() -> userWithDeptProxy.withAssociation("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Association 'nonexistent' not found");
        }

        @Test
        @DisplayName("fetch 应加载 ManyToOne 关联")
        void shouldFetchManyToOneAssociation() {
            // Given - 创建部门
            TestDepartment dept = new TestDepartment();
            dept.setName("研发部");
            dept = deptProxy.insert(dept);

            // Given - 创建用户
            TestUserWithDept user = new TestUserWithDept();
            user.setName("张三");
            user.setEmail("zhangsan@example.com");
            user.setDepartmentId(dept.getId());
            user = userWithDeptProxy.insert(user);

            // When
            TestDepartment fetchedDept = userWithDeptProxy.fetch(user, "department");

            // Then
            assertThat(fetchedDept).isNotNull();
            assertThat(fetchedDept.getName()).isEqualTo("研发部");
        }

        @Test
        @DisplayName("fetch 外键为null时应返回null")
        void shouldReturnNullWhenForeignKeyIsNull() {
            // Given - 创建用户但不关联部门
            TestUserWithDept user = new TestUserWithDept();
            user.setName("无部门用户");
            user.setEmail("nodept@example.com");
            user.setDepartmentId(null);
            user = userWithDeptProxy.insert(user);

            // When
            TestDepartment fetchedDept = userWithDeptProxy.fetch(user, "department");

            // Then
            assertThat(fetchedDept).isNull();
        }

        @Test
        @DisplayName("fetch 不存在的关联应抛出异常")
        void shouldThrowWhenFetchingNonexistentAssociation() {
            // Given
            TestUserWithDept user = new TestUserWithDept();
            user.setName("测试用户");
            user.setEmail("test@example.com");
            TestUserWithDept insertedUser = userWithDeptProxy.insert(user);

            // When & Then
            assertThatThrownBy(() -> userWithDeptProxy.fetch(insertedUser, "nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Association 'nonexistent' not found");
        }

        @Test
        @DisplayName("fetch 实体无ID时应抛出异常")
        void shouldThrowWhenEntityHasNoId() {
            // Given - 未插入的用户（无ID）
            TestUserWithDept user = new TestUserWithDept();
            user.setName("无ID用户");
            user.setEmail("noid@example.com");

            // When & Then
            assertThatThrownBy(() -> userWithDeptProxy.fetch(user, "department"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Entity must have an ID to fetch associations");
        }

        @Test
        @DisplayName("clearAssociations 应清除关联加载配置")
        void shouldClearAssociations() {
            // Given
            EntityProxy<TestUserWithDept> configuredProxy = userWithDeptProxy.withAssociation("department");

            // When
            EntityProxy<TestUserWithDept> clearedProxy = configuredProxy.clearAssociations();

            // Then
            assertThat(((JdbcEntityProxy<TestUserWithDept>) clearedProxy).getEagerFetchAssociations())
                .isEmpty();
        }

        @Test
        @DisplayName("fetchAll 应批量加载关联数据")
        void shouldFetchAllAssociations() {
            // Given - 创建部门
            TestDepartment dept = new TestDepartment();
            dept.setName("测试部门");
            dept = deptProxy.insert(dept);

            // Given - 创建多个用户
            TestUserWithDept user1 = new TestUserWithDept();
            user1.setName("用户1");
            user1.setEmail("user1@example.com");
            user1.setDepartmentId(dept.getId());
            user1 = userWithDeptProxy.insert(user1);

            TestUserWithDept user2 = new TestUserWithDept();
            user2.setName("用户2");
            user2.setEmail("user2@example.com");
            user2.setDepartmentId(dept.getId());
            user2 = userWithDeptProxy.insert(user2);

            // When
            userWithDeptProxy.fetchAll(List.of(user1, user2), "department");

            // Then - 验证没有异常抛出
        }

        @Test
        @DisplayName("fetchAll 不存在的关联应抛出异常")
        void shouldThrowWhenFetchAllNonexistentAssociation() {
            // Given
            TestUserWithDept user = new TestUserWithDept();
            user.setName("测试用户");
            user.setEmail("test@example.com");
            TestUserWithDept insertedUser = userWithDeptProxy.insert(user);

            // When & Then
            assertThatThrownBy(() -> userWithDeptProxy.fetchAll(List.of(insertedUser), "nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Association 'nonexistent' not found");
        }

        @Test
        @DisplayName("getEagerFetchAssociations 应返回不可修改集合")
        void shouldReturnUnmodifiableSet() {
            // Given
            EntityProxy<TestUserWithDept> configuredProxy = userWithDeptProxy.withAssociation("department");
            Set<String> associations = ((JdbcEntityProxy<TestUserWithDept>) configuredProxy).getEagerFetchAssociations();

            // When & Then
            assertThatThrownBy(() -> associations.add("new"))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        private void createAssociationTables() {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                // 部门表
                stmt.execute("""
                    CREATE TABLE test_department (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL
                    )
                    """);

                // 用户表（带部门外键）
                stmt.execute("""
                    CREATE TABLE test_user_with_dept (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(200),
                        department_id BIGINT
                    )
                    """);

                // 角色表
                stmt.execute("""
                    CREATE TABLE test_role (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(50) NOT NULL
                    )
                    """);

                // 用户角色中间表
                stmt.execute("""
                    CREATE TABLE test_user_with_roles_test_role (
                        test_user_with_roles_id BIGINT,
                        test_role_id BIGINT
                    )
                    """);

                // 用户实体（带角色列表）
                stmt.execute("""
                    CREATE TABLE test_user_with_roles (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(200)
                    )
                    """);

            } catch (Exception e) {
                throw new RuntimeException("Failed to create association tables", e);
            }
        }

        private void dropAssociationTables() {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS test_department");
                stmt.execute("DROP TABLE IF EXISTS test_user_with_dept");
                stmt.execute("DROP TABLE IF EXISTS test_role");
                stmt.execute("DROP TABLE IF EXISTS test_user_with_roles");
                stmt.execute("DROP TABLE IF EXISTS test_user_with_roles_test_role");
            } catch (Exception ignored) {
            }
        }
    }

    // ==================== 批量插入边界测试 ====================

    @Nested
    @DisplayName("批量插入边界测试")
    class BatchInsertBoundaryTests {

        @Test
        @DisplayName("批量插入混合有ID和无ID的实体")
        void shouldInsertMixedIdAndNoId() {
            // Given - 使用较大的ID以避免冲突
            TestUser userWithId = new TestUser();
            userWithId.setId(999900L);
            userWithId.setName("mixed-id-user");
            userWithId.setEmail("mixed-id@example.com");

            TestUser userWithoutId = new TestUser();
            userWithoutId.setName("mixed-no-id-user");
            userWithoutId.setEmail("mixed-no-id@example.com");

            // When
            List<TestUser> inserted = userProxy.insertAll(List.of(userWithId, userWithoutId));

            // Then
            assertThat(inserted).hasSize(2);
            // 第一个用户可能有预设ID或生成的ID
            assertThat(inserted.get(0).getId()).isNotNull();
            assertThat(inserted.get(1).getId()).isNotNull();
        }

        @Test
        @DisplayName("saveAll 应正确保存混合实体")
        void shouldSaveAllMixedEntities() {
            // Given
            TestUser newUser = new TestUser();
            newUser.setName("save-new");
            newUser.setEmail("save-new@example.com");

            TestUser existingUser = new TestUser();
            existingUser.setName("save-existing");
            existingUser.setEmail("save-existing@example.com");
            existingUser = userProxy.insert(existingUser);
            existingUser.setName("save-updated");

            // When
            List<TestUser> saved = userProxy.saveAll(List.of(newUser, existingUser));

            // Then
            assertThat(saved).hasSize(2);
            assertThat(saved.get(0).getId()).isNotNull();
            assertThat(saved.get(1).getName()).isEqualTo("save-updated");
        }

        @Test
        @DisplayName("withBatchSize 无效值应抛出异常")
        void shouldThrowForInvalidBatchSize() {
            // When & Then
            assertThatThrownBy(() -> ((JdbcEntityProxy<TestUser>) userProxy).withBatchSize(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("batchSize must be greater than 0");

            assertThatThrownBy(() -> ((JdbcEntityProxy<TestUser>) userProxy).withBatchSize(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("batchSize must be greater than 0");
        }

        @Test
        @DisplayName("getEntityClass 应返回正确的实体类")
        void shouldReturnCorrectEntityClass() {
            // When
            Class<TestUser> entityClass = ((JdbcEntityProxy<TestUser>) userProxy).getEntityClass();

            // Then
            assertThat(entityClass).isEqualTo(TestUser.class);
        }
    }

    // ==================== 删除操作边界测试 ====================

    @Nested
    @DisplayName("删除操作边界测试")
    class DeleteBoundaryTests {

        @Test
        @DisplayName("delete 无ID实体应不执行任何操作")
        void shouldNotDeleteEntityWithoutId() {
            // Given
            TestUser user = new TestUser();
            user.setName("no-id-delete");
            user.setEmail("no-id-delete@example.com");

            // When - 删除无ID实体（不应抛出异常）
            userProxy.delete(user);

            // Then - 数据库应为空
            assertThat(userProxy.count()).isZero();
        }

        @Test
        @DisplayName("deleteAll 空列表应正常处理")
        void shouldHandleEmptyDeleteAll() {
            // When
            userProxy.deleteAll(List.of());

            // Then - 不应抛出异常
        }

        @Test
        @DisplayName("deleteAllById 空列表应正常处理")
        void shouldHandleEmptyDeleteAllById() {
            // When
            userProxy.deleteAllById(List.of());

            // Then - 不应抛出异常
        }

        @Test
        @DisplayName("findAllById 空列表应返回空列表")
        void shouldHandleEmptyFindAllById() {
            // When
            List<TestUser> result = userProxy.findAllById(List.of());

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ==================== 实体类用于关联测试 ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestDepartment {
        private Long id;
        private String name;
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
}