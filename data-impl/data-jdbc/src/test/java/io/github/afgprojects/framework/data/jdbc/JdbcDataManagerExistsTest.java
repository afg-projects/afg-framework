package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.jdbc.entity.TestSoftDeleteItem;
import io.github.afgprojects.framework.data.jdbc.entity.TestUser;
import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;

/**
 * JdbcDataManager existsById 集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 测试后自动回滚（@Transactional）。
 * </p>
 */
class JdbcDataManagerExistsTest extends BaseDataTest {

    @Nested
    @DisplayName("existsById 存在性判断")
    class ExistsById {

        @Test
        @DisplayName("should return true when existsById with existing id")
        void shouldReturnTrue_whenExistsByIdWithExistingId() {
            TestUser saved = dataManager.save(TestUser.class, createUser("exists-test"));

            assertThat(dataManager.existsById(TestUser.class, saved.getId())).isTrue();
        }

        @Test
        @DisplayName("should return false when existsById with non-existing id")
        void shouldReturnFalse_whenExistsByIdWithNonExistingId() {
            assertThat(dataManager.existsById(TestUser.class, "99999")).isFalse();
        }

        @Test
        @DisplayName("should return false when entity is soft deleted")
        void shouldReturnFalse_whenEntityIsSoftDeleted() {
            TestSoftDeleteItem item = TestSoftDeleteItem.create("exists-soft", 10);
            TestSoftDeleteItem saved = dataManager.save(TestSoftDeleteItem.class, item);

            dataManager.deleteById(TestSoftDeleteItem.class, saved.getId());

            assertThat(dataManager.existsById(TestSoftDeleteItem.class, saved.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByCondition 条件存在性判断")
    class ExistsByCondition {

        @Test
        @DisplayName("should return true when matching condition exists")
        void shouldReturnTrue_whenMatchingConditionExists() {
            dataManager.save(TestUser.class, createUser("cond-exists", 1));

            assertThat(dataManager.existsByCondition(TestUser.class,
                Conditions.builder(TestUser.class)
                    .eq(TestUser::getUsername, "cond-exists")
                    .build())).isTrue();
        }

        @Test
        @DisplayName("should return false when no matching condition")
        void shouldReturnFalse_whenNoMatchingCondition() {
            dataManager.save(TestUser.class, createUser("cond-no-exists", 1));

            assertThat(dataManager.existsByCondition(TestUser.class,
                Conditions.builder(TestUser.class)
                    .eq(TestUser::getUsername, "nonexistent-user")
                    .build())).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByField 字段存在性判断")
    class ExistsByField {

        @Test
        @DisplayName("should return true when field value matches")
        void shouldReturnTrue_whenFieldValueMatches() {
            dataManager.save(TestUser.class, createUser("field-exists"));

            assertThat(dataManager.existsByField(TestUser.class,
                TestUser::getUsername, "field-exists")).isTrue();
        }

        @Test
        @DisplayName("should return false when field value does not match")
        void shouldReturnFalse_whenFieldValueDoesNotMatch() {
            assertThat(dataManager.existsByField(TestUser.class,
                TestUser::getUsername, "no-such-user")).isFalse();
        }
    }

    // --- 辅助方法 ---

    private TestUser createUser(String username) {
        return createUser(username, 1);
    }

    private TestUser createUser(String username, int status) {
        TestUser user = new TestUser();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setStatus(status);
        return user;
    }
}
