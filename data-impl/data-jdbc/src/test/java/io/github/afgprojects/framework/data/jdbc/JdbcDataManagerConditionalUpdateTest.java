package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.jdbc.entity.TestUser;
import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;

/**
 * JdbcDataManager 条件更新集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 测试后自动回滚（@Transactional）。
 * </p>
 */
class JdbcDataManagerConditionalUpdateTest extends BaseDataTest {

    @Nested
    @DisplayName("updateAll 条件批量更新")
    class UpdateAll {

        @Test
        @DisplayName("should update matching records when updateAll with condition")
        void shouldUpdateMatchingRecords_whenUpdateAllWithCondition() {
            // 使用唯一前缀，确保条件匹配仅限这些记录
            String prefix = "cupd-" + System.nanoTime();
            TestUser user1 = dataManager.save(TestUser.class, createUser(prefix + "-1"));
            TestUser user2 = dataManager.save(TestUser.class, createUser(prefix + "-2"));
            TestUser user3 = dataManager.save(TestUser.class, createUser(prefix + "-3"));

            // 使用 likeStartsWith 匹配前缀（like 会自动包裹 %value%）
            long updated = dataManager.updateAll(TestUser.class,
                Conditions.builder(TestUser.class)
                    .likeStartsWith(TestUser::getUsername, prefix)
                    .build(),
                Map.of("email", "updated@test.com"));

            assertThat(updated).isEqualTo(3);

            // 验证 email 已更新
            TestUser updated1 = dataManager.findById(TestUser.class, user1.getId()).orElseThrow();
            assertThat(updated1.getEmail()).isEqualTo("updated@test.com");
        }

        @Test
        @DisplayName("should return zero when updateAll with no matching records")
        void shouldReturnZero_whenUpdateAllWithNoMatchingRecords() {
            long updated = dataManager.updateAll(TestUser.class,
                Conditions.builder(TestUser.class)
                    .eq(TestUser::getUsername, "nonexistent-for-update-" + System.nanoTime())
                    .build(),
                Map.of("status", 99));

            assertThat(updated).isEqualTo(0);
        }

        @Test
        @DisplayName("should update multiple fields when updateAll with multiple fields")
        void shouldUpdateMultipleFields_whenUpdateAllWithMultipleFields() {
            String uniqueName = "mupd-" + System.nanoTime();
            dataManager.save(TestUser.class, createUser(uniqueName));

            long updated = dataManager.updateAll(TestUser.class,
                Conditions.builder(TestUser.class)
                    .eq(TestUser::getUsername, uniqueName)
                    .build(),
                Map.of("status", 2, "email", "updated@test.com"));

            assertThat(updated).isGreaterThanOrEqualTo(1);

            TestUser result = dataManager.findOneByField(TestUser.class,
                TestUser::getUsername, uniqueName).orElseThrow();
            assertThat(result.getStatus()).isEqualTo(2);
            assertThat(result.getEmail()).isEqualTo("updated@test.com");
        }
    }

    // --- 辅助方法 ---

    private TestUser createUser(String username) {
        TestUser user = new TestUser();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setStatus(1);
        return user;
    }
}
