package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import io.github.afgprojects.framework.data.core.scope.TenantScope;
import io.github.afgprojects.framework.data.jdbc.entity.TestTenantItem;
import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;

/**
 * JdbcDataManager 多租户隔离集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 测试后自动回滚（@Transactional）。
 * </p>
 */
class JdbcDataManagerTenantIsolationTest extends BaseDataTest {

    @BeforeEach
    void clearTenantContext() {
        dataManager.getTenantContextHolder().clear();
    }

    @AfterEach
    void resetTenantContext() {
        dataManager.getTenantContextHolder().clear();
    }

    @Nested
    @DisplayName("租户上下文保存")
    class TenantContextSave {

        @Test
        @DisplayName("should save entity with tenant id when tenant context is set")
        void shouldSaveEntityWithTenantId_whenTenantContextIsSet() {
            try (TenantScope scope = dataManager.getTenantContextHolder().scope("tenant-001")) {
                TestTenantItem item = new TestTenantItem();
                item.setName("item-1");
                item.setQuantity(10);

                TestTenantItem saved = dataManager.save(TestTenantItem.class, item);

                assertThat(saved.getTenantId()).isEqualTo("tenant-001");
            }
        }

        @Test
        @DisplayName("should save entity with explicit tenant id when no tenant context")
        void shouldSaveEntityWithExplicitTenantId_whenNoTenantContext() {
            TestTenantItem item = TestTenantItem.create("item-2", 20, "tenant-002");

            TestTenantItem saved = dataManager.save(TestTenantItem.class, item);

            assertThat(saved.getTenantId()).isEqualTo("tenant-002");
        }
    }

    @Nested
    @DisplayName("查询自动过滤")
    class QueryAutoFiltering {

        @Test
        @DisplayName("should only return entities of current tenant when query with tenant context")
        void shouldOnlyReturnEntitiesOfCurrentTenant_whenQueryWithTenantContext() {
            // 准备：为两个租户创建数据
            dataManager.save(TestTenantItem.class, TestTenantItem.create("tenant1-item1", 10, "tenant-001"));
            dataManager.save(TestTenantItem.class, TestTenantItem.create("tenant1-item2", 20, "tenant-001"));
            dataManager.save(TestTenantItem.class, TestTenantItem.create("tenant2-item1", 30, "tenant-002"));

            // 在 tenant-001 上下文中查询
            try (TenantScope scope = dataManager.getTenantContextHolder().scope("tenant-001")) {
                List<TestTenantItem> items = dataManager.findAll(TestTenantItem.class);

                assertThat(items).isNotEmpty();
                assertThat(items).allMatch(item -> "tenant-001".equals(item.getTenantId()));
            }
        }

        @Test
        @DisplayName("should return empty when no data for current tenant")
        void shouldReturnEmpty_whenNoDataForCurrentTenant() {
            // 准备：只为 tenant-001 创建数据
            dataManager.save(TestTenantItem.class, TestTenantItem.create("tenant1-item1", 10, "tenant-001"));

            // 在 tenant-999 上下文中查询
            try (TenantScope scope = dataManager.getTenantContextHolder().scope("tenant-999")) {
                List<TestTenantItem> items = dataManager.findAll(TestTenantItem.class);

                assertThat(items).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("跨租户不可见")
    class CrossTenantIsolation {

        @Test
        @DisplayName("should not see other tenant data when findById across tenants")
        void shouldNotSeeOtherTenantData_whenFindByIdAcrossTenants() {
            TestTenantItem saved = dataManager.save(TestTenantItem.class, TestTenantItem.create("isolated-item", 50, "tenant-001"));

            // 在不同租户上下文中查询，应该找不到
            try (TenantScope scope = dataManager.getTenantContextHolder().scope("tenant-002")) {
                assertThat(dataManager.findById(TestTenantItem.class, saved.getId())).isEmpty();
            }

            // 在原租户上下文中可以找到
            try (TenantScope scope = dataManager.getTenantContextHolder().scope("tenant-001")) {
                assertThat(dataManager.findById(TestTenantItem.class, saved.getId())).isPresent();
            }
        }

        @Test
        @DisplayName("should filter by tenant in condition query")
        void shouldFilterByTenantInConditionQuery() {
            dataManager.save(TestTenantItem.class, TestTenantItem.create("tenant1-a", 10, "tenant-001"));
            dataManager.save(TestTenantItem.class, TestTenantItem.create("tenant1-b", 20, "tenant-001"));
            dataManager.save(TestTenantItem.class, TestTenantItem.create("tenant2-a", 30, "tenant-002"));

            try (TenantScope scope = dataManager.getTenantContextHolder().scope("tenant-001")) {
                List<TestTenantItem> items = dataManager.entity(TestTenantItem.class)
                    .query()
                    .list();

                assertThat(items).allMatch(item -> "tenant-001".equals(item.getTenantId()));
            }
        }
    }

    @Nested
    @DisplayName("作用域恢复")
    class ScopeRestoration {

        @Test
        @DisplayName("should restore previous tenant id when scope is closed")
        void shouldRestorePreviousTenantId_whenScopeIsClosed() {
            TenantContextHolder holder = dataManager.getTenantContextHolder();

            // 设置初始租户
            try (TenantScope scope1 = holder.scope("tenant-initial")) {
                assertThat(holder.getTenantId()).isEqualTo("tenant-initial");

                // 嵌套作用域
                try (TenantScope scope2 = holder.scope("tenant-nested")) {
                    assertThat(holder.getTenantId()).isEqualTo("tenant-nested");
                }

                // 嵌套作用域关闭后恢复
                assertThat(holder.getTenantId()).isEqualTo("tenant-initial");
            }

            // 外层作用域关闭后清除
            assertThat(holder.getTenantId()).isNull();
        }

        @Test
        @DisplayName("should work with DataManager tenantScope method")
        void shouldWorkWithDataManagerTenantScopeMethod() {
            TenantContextHolder holder = dataManager.getTenantContextHolder();

            assertThat(holder.getTenantId()).isNull();

            try (TenantScope scope = dataManager.tenantScope("tenant-via-dm")) {
                assertThat(holder.getTenantId()).isEqualTo("tenant-via-dm");
                assertThat(scope.getTenantId()).isEqualTo("tenant-via-dm");
            }

            assertThat(holder.getTenantId()).isNull();
        }
    }
}
