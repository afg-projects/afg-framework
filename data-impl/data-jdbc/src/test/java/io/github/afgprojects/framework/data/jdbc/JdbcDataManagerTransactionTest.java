package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.afgprojects.framework.data.jdbc.entity.TestUser;
import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;

/**
 * JdbcDataManager 事务管理集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 测试后自动回滚（@Transactional）。
 * </p>
 */
class JdbcDataManagerTransactionTest extends BaseDataTest {

    @Nested
    @DisplayName("executeInTransaction 编程式事务")
    class ExecuteInTransaction {

        @Test
        @DisplayName("should commit when executeInTransaction with successful operation")
        void shouldCommit_whenExecuteInTransactionWithSuccessfulOperation() {
            TestUser user = dataManager.executeInTransaction(() -> {
                TestUser u = createUser("tx-commit");
                return dataManager.save(TestUser.class, u);
            });

            assertThat(user.getId()).isNotNull();
            assertThat(dataManager.findById(TestUser.class, user.getId())).isPresent();
        }

        @Test
        @DisplayName("should return result when executeInTransaction with Supplier")
        void shouldReturnResult_whenExecuteInTransactionWithSupplier() {
            Long count = dataManager.executeInTransaction(() ->
                dataManager.count(TestUser.class));

            assertThat(count).isNotNull();
            assertThat(count).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should execute Runnable when executeInTransaction without return value")
        void shouldExecuteRunnable_whenExecuteInTransactionWithoutReturnValue() {
            dataManager.executeInTransaction(() -> {
                dataManager.save(TestUser.class, createUser("tx-runnable"));
            });

            assertThat(dataManager.findOneByField(TestUser.class,
                TestUser::getUsername, "tx-runnable")).isPresent();
        }

        @Test
        @DisplayName("should rollback when exception thrown in executeInTransaction")
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        void shouldRollback_whenExceptionThrownInExecuteInTransaction() {
            Long testUserId = null;
            try {
                testUserId = dataManager.executeInTransaction(() -> {
                    TestUser saved = dataManager.save(TestUser.class, createUser("tx-rollback"));
                    // 抛出异常触发回滚
                    throw new RuntimeException("Intentional failure for test");
                });
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("Intentional failure for test");
            }

            // 事务回滚后数据不应存在
            if (testUserId != null) {
                assertThat(dataManager.findById(TestUser.class, testUserId)).isEmpty();
            }
            assertThat(dataManager.findOneByField(TestUser.class,
                TestUser::getUsername, "tx-rollback")).isEmpty();
        }
    }

    @Nested
    @DisplayName("executeInReadOnly 只读事务")
    class ExecuteInReadOnly {

        @Test
        @DisplayName("should return result when executeInReadOnly")
        void shouldReturnResult_whenExecuteInReadOnly() {
            dataManager.save(TestUser.class, createUser("readonly-query"));

            long count = dataManager.executeInReadOnly(() ->
                dataManager.count(TestUser.class));

            assertThat(count).isGreaterThanOrEqualTo(1);
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
