package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.data.jdbc.entity.TestSoftDeleteItem;
import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;

/**
 * JdbcDataManager 软删除恢复集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 测试后自动回滚（@Transactional）。
 * </p>
 */
class JdbcDataManagerRestoreTest extends BaseDataTest {

    @Nested
    @DisplayName("restoreById 恢复软删除")
    class RestoreById {

        @Test
        @DisplayName("should restore soft deleted entity when restoreById")
        void shouldRestoreSoftDeletedEntity_whenRestoreById() {
            TestSoftDeleteItem item = TestSoftDeleteItem.create("restore-test", 10);
            TestSoftDeleteItem saved = dataManager.save(TestSoftDeleteItem.class, item);

            // 软删除
            dataManager.deleteById(TestSoftDeleteItem.class, saved.getId());
            assertThat(dataManager.findById(TestSoftDeleteItem.class, saved.getId())).isEmpty();

            // 恢复
            dataManager.restoreById(TestSoftDeleteItem.class, saved.getId());

            // 验证恢复后可以正常查询
            TestSoftDeleteItem restored = dataManager.findById(TestSoftDeleteItem.class, saved.getId())
                .orElseThrow();
            assertThat(restored.getDeleted()).isFalse();
            assertThat(restored.getName()).isEqualTo("restore-test");
        }

        @Test
        @DisplayName("should appear in findAll after restore")
        void shouldAppearInFindAll_afterRestore() {
            TestSoftDeleteItem saved = dataManager.save(TestSoftDeleteItem.class,
                TestSoftDeleteItem.create("find-restore-test", 20));

            dataManager.deleteById(TestSoftDeleteItem.class, saved.getId());
            assertThat(dataManager.findAll(TestSoftDeleteItem.class))
                .noneMatch(i -> i.getId().equals(saved.getId()));

            dataManager.restoreById(TestSoftDeleteItem.class, saved.getId());
            assertThat(dataManager.findAll(TestSoftDeleteItem.class))
                .anyMatch(i -> i.getId().equals(saved.getId()) && !i.getDeleted());
        }

        @Test
        @DisplayName("should restore entity with same id and data when restoreById")
        void shouldRestoreEntityWithSameIdAndData_whenRestoreById() {
            TestSoftDeleteItem item = TestSoftDeleteItem.create("data-check", 42);
            TestSoftDeleteItem saved = dataManager.save(TestSoftDeleteItem.class, item);
            String originalId = saved.getId();

            dataManager.deleteById(TestSoftDeleteItem.class, originalId);
            dataManager.restoreById(TestSoftDeleteItem.class, originalId);

            TestSoftDeleteItem restored = dataManager.findById(TestSoftDeleteItem.class, originalId)
                .orElseThrow();
            assertThat(restored.getId()).isEqualTo(originalId);
            assertThat(restored.getName()).isEqualTo("data-check");
            assertThat(restored.getQuantity()).isEqualTo(42);
        }
    }
}
