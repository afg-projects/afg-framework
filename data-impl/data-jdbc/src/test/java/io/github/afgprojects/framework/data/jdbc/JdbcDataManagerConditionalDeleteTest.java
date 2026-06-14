package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.jdbc.entity.TestSoftDeleteItem;
import io.github.afgprojects.framework.data.jdbc.entity.TestUser;
import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;

/**
 * JdbcDataManager 条件删除集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 测试后自动回滚（@Transactional）。
 * </p>
 */
class JdbcDataManagerConditionalDeleteTest extends BaseDataTest {

    @Nested
    @DisplayName("deleteByCondition 条件删除")
    class DeleteByCondition {

        @Test
        @DisplayName("should delete matching entities when deleteByCondition")
        void shouldDeleteMatchingEntities_whenDeleteByCondition() {
            String prefix = "cdel-" + System.nanoTime() + "-";
            dataManager.save(TestUser.class, createUser(prefix + "1", 0));
            dataManager.save(TestUser.class, createUser(prefix + "2", 0));
            dataManager.save(TestUser.class, createUser(prefix + "3", 1));

            long deleted = dataManager.deleteByCondition(TestUser.class,
                Conditions.builder(TestUser.class)
                    .likeStartsWith(TestUser::getUsername, prefix)
                    .eq(TestUser::getStatus, 0)
                    .build());

            assertThat(deleted).isEqualTo(2);

            List<TestUser> remaining = dataManager.findList(TestUser.class,
                Conditions.builder(TestUser.class)
                    .likeStartsWith(TestUser::getUsername, prefix)
                    .build());
            assertThat(remaining).allMatch(u -> u.getStatus() == 1);
        }

        @Test
        @DisplayName("should return zero when deleteByCondition with no matching records")
        void shouldReturnZero_whenDeleteByConditionWithNoMatchingRecords() {
            long deleted = dataManager.deleteByCondition(TestUser.class,
                Conditions.builder(TestUser.class)
                    .eq(TestUser::getUsername, "nonexistent-for-delete-" + System.nanoTime())
                    .build());

            assertThat(deleted).isEqualTo(0);
        }

        @Test
        @DisplayName("should soft delete when deleteByCondition on soft delete entity")
        void shouldSoftDelete_whenDeleteByConditionOnSoftDeleteEntity() {
            TestSoftDeleteItem item1 = dataManager.save(TestSoftDeleteItem.class,
                TestSoftDeleteItem.create("soft-cond-del-1", 10));
            TestSoftDeleteItem item2 = dataManager.save(TestSoftDeleteItem.class,
                TestSoftDeleteItem.create("soft-cond-del-2", 20));

            long deleted = dataManager.deleteByCondition(TestSoftDeleteItem.class,
                Conditions.builder(TestSoftDeleteItem.class)
                    .eq(TestSoftDeleteItem::getName, "soft-cond-del-1")
                    .build());

            assertThat(deleted).isGreaterThanOrEqualTo(1);

            // 已软删除的记录不在正常查询中
            List<TestSoftDeleteItem> active = dataManager.findAll(TestSoftDeleteItem.class);
            assertThat(active).noneMatch(i -> i.getId().equals(item1.getId()));
            assertThat(active).anyMatch(i -> i.getId().equals(item2.getId()));
        }
    }

    // --- 辅助方法 ---

    private TestUser createUser(String username, int status) {
        TestUser user = new TestUser();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setStatus(status);
        return user;
    }
}
