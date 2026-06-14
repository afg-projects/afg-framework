package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.jdbc.entity.TestSoftDeleteItem;
import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;

/**
 * JdbcDataManager includeDeleted 查询集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 测试后自动回滚（@Transactional）。
 * </p>
 */
class JdbcDataManagerIncludeDeletedTest extends BaseDataTest {

    @Nested
    @DisplayName("includeDeleted 查询已软删除记录")
    class IncludeDeletedQuery {

        @Test
        @DisplayName("should include soft deleted records when query with includeDeleted")
        void shouldIncludeSoftDeletedRecords_whenQueryWithIncludeDeleted() {
            TestSoftDeleteItem active = dataManager.save(TestSoftDeleteItem.class,
                TestSoftDeleteItem.create("active-item", 10));
            TestSoftDeleteItem toDelete = dataManager.save(TestSoftDeleteItem.class,
                TestSoftDeleteItem.create("deleted-item", 20));

            // 软删除
            dataManager.deleteById(TestSoftDeleteItem.class, toDelete.getId());

            // 不包含已删除：只返回 active
            List<TestSoftDeleteItem> activeOnly = dataManager.entity(TestSoftDeleteItem.class)
                .query()
                .list();
            assertThat(activeOnly).anyMatch(i -> i.getId().equals(active.getId()));
            assertThat(activeOnly).noneMatch(i -> i.getId().equals(toDelete.getId()));

            // includeDeleted：返回所有
            List<TestSoftDeleteItem> allItems = dataManager.entity(TestSoftDeleteItem.class)
                .includeDeleted()
                .list();
            assertThat(allItems).anyMatch(i -> i.getId().equals(active.getId()));
            assertThat(allItems).anyMatch(i -> i.getId().equals(toDelete.getId()));
        }

        @Test
        @DisplayName("should return deleted flag as true when includeDeleted shows soft deleted record")
        void shouldReturnDeletedFlagAsTrue_whenIncludeDeletedShowsSoftDeletedRecord() {
            TestSoftDeleteItem item = dataManager.save(TestSoftDeleteItem.class,
                TestSoftDeleteItem.create("flag-test", 30));
            dataManager.deleteById(TestSoftDeleteItem.class, item.getId());

            List<TestSoftDeleteItem> allItems = dataManager.entity(TestSoftDeleteItem.class)
                .includeDeleted()
                .where(Conditions.eq("id", item.getId()))
                .list();

            assertThat(allItems).hasSize(1);
            assertThat(allItems.get(0).getDeleted()).isTrue();
        }

        @Test
        @DisplayName("should filter by condition on deleted records when includeDeleted")
        void shouldFilterByConditionOnDeletedRecords_whenIncludeDeleted() {
            TestSoftDeleteItem item1 = dataManager.save(TestSoftDeleteItem.class,
                TestSoftDeleteItem.create("delete-cond-1", 100));
            TestSoftDeleteItem item2 = dataManager.save(TestSoftDeleteItem.class,
                TestSoftDeleteItem.create("delete-cond-2", 200));

            dataManager.deleteById(TestSoftDeleteItem.class, item1.getId());
            dataManager.deleteById(TestSoftDeleteItem.class, item2.getId());

            // includeDeleted + condition：只返回 quantity > 150 的已删除记录
            List<TestSoftDeleteItem> result = dataManager.entity(TestSoftDeleteItem.class)
                .includeDeleted()
                .where(Conditions.builder(TestSoftDeleteItem.class)
                    .gt(TestSoftDeleteItem::getQuantity, 150)
                    .build())
                .list();

            assertThat(result).anyMatch(i -> i.getId().equals(item2.getId()) && i.getDeleted());
            assertThat(result).noneMatch(i -> i.getId().equals(item1.getId()));
        }

        @Test
        @DisplayName("should only return non-deleted records when query without includeDeleted")
        void shouldOnlyReturnNonDeletedRecords_whenQueryWithoutIncludeDeleted() {
            TestSoftDeleteItem active = dataManager.save(TestSoftDeleteItem.class,
                TestSoftDeleteItem.create("still-active", 50));
            TestSoftDeleteItem toDelete = dataManager.save(TestSoftDeleteItem.class,
                TestSoftDeleteItem.create("will-delete", 60));

            dataManager.deleteById(TestSoftDeleteItem.class, toDelete.getId());

            List<TestSoftDeleteItem> result = dataManager.entity(TestSoftDeleteItem.class)
                .query()
                .list();

            assertThat(result).allMatch(i -> !i.getDeleted());
        }
    }
}
