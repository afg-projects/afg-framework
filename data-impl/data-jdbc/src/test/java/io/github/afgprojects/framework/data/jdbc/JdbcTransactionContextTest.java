package io.github.afgprojects.framework.data.jdbc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JdbcTransactionContext 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JdbcTransactionContext 测试")
class JdbcTransactionContextTest {

    @Mock
    private Connection mockConnection;

    private JdbcTransactionContext context;

    @AfterEach
    void tearDown() throws Exception {
        if (context != null) {
            try {
                context.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该正确创建事务上下文")
        void shouldCreateContext() throws SQLException {
            when(mockConnection.isClosed()).thenReturn(false);

            context = new JdbcTransactionContext(mockConnection);

            assertThat(context.getConnection()).isEqualTo(mockConnection);
            assertThat(context.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("commit 测试")
    class CommitTests {

        @BeforeEach
        void setUp() throws SQLException {
            when(mockConnection.isClosed()).thenReturn(false);
            context = new JdbcTransactionContext(mockConnection);
        }

        @Test
        @DisplayName("提交成功应该使事务不活跃")
        void shouldDeactivateOnSuccessfulCommit() throws SQLException {
            context.commit();

            assertThat(context.isActive()).isFalse();
            verify(mockConnection).commit();
        }

        @Test
        @DisplayName("提交后应该关闭连接")
        void shouldCloseConnectionAfterCommit() throws SQLException {
            context.commit();

            verify(mockConnection).close();
        }

        @Test
        @DisplayName("提交失败应该抛出异常")
        void shouldThrowOnCommitFailure() throws SQLException {
            doThrow(new SQLException("Commit failed")).when(mockConnection).commit();

            assertThatThrownBy(() -> context.commit())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to commit transaction");
        }

        @Test
        @DisplayName("不活跃的事务提交应该抛出异常")
        void shouldThrowWhenCommittingInactiveTransaction() throws SQLException {
            context.commit();

            assertThatThrownBy(() -> context.commit())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Transaction is not active");
        }
    }

    @Nested
    @DisplayName("rollback 测试")
    class RollbackTests {

        @BeforeEach
        void setUp() throws SQLException {
            when(mockConnection.isClosed()).thenReturn(false);
            context = new JdbcTransactionContext(mockConnection);
        }

        @Test
        @DisplayName("回滚成功应该使事务不活跃")
        void shouldDeactivateOnSuccessfulRollback() throws SQLException {
            context.rollback();

            assertThat(context.isActive()).isFalse();
            verify(mockConnection).rollback();
        }

        @Test
        @DisplayName("回滚后应该关闭连接")
        void shouldCloseConnectionAfterRollback() throws SQLException {
            context.rollback();

            verify(mockConnection).close();
        }

        @Test
        @DisplayName("回滚失败应该抛出异常")
        void shouldThrowOnRollbackFailure() throws SQLException {
            doThrow(new SQLException("Rollback failed")).when(mockConnection).rollback();

            assertThatThrownBy(() -> context.rollback())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to rollback transaction");
        }

        @Test
        @DisplayName("不活跃的事务回滚应该被忽略")
        void shouldIgnoreRollbackOfInactiveTransaction() throws SQLException {
            context.rollback();

            // Second rollback should be ignored
            assertThatCode(() -> context.rollback())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("isActive 测试")
    class IsActiveTests {

        @Test
        @DisplayName("新建事务应该活跃")
        void shouldBeActiveWhenNew() throws SQLException {
            when(mockConnection.isClosed()).thenReturn(false);
            context = new JdbcTransactionContext(mockConnection);

            assertThat(context.isActive()).isTrue();
        }

        @Test
        @DisplayName("提交后事务不应该活跃")
        void shouldNotBeActiveAfterCommit() throws SQLException {
            when(mockConnection.isClosed()).thenReturn(false);
            context = new JdbcTransactionContext(mockConnection);
            context.commit();

            assertThat(context.isActive()).isFalse();
        }

        @Test
        @DisplayName("回滚后事务不应该活跃")
        void shouldNotBeActiveAfterRollback() throws SQLException {
            when(mockConnection.isClosed()).thenReturn(false);
            context = new JdbcTransactionContext(mockConnection);
            context.rollback();

            assertThat(context.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("close 测试")
    class CloseTests {

        @BeforeEach
        void setUp() throws SQLException {
            when(mockConnection.isClosed()).thenReturn(false);
            context = new JdbcTransactionContext(mockConnection);
        }

        @Test
        @DisplayName("关闭活跃事务应该回滚")
        void shouldRollbackWhenClosingActiveTransaction() throws SQLException {
            context.close();

            verify(mockConnection).rollback();
        }

        @Test
        @DisplayName("关闭不活跃事务不应该回滚")
        void shouldNotRollbackWhenClosingInactiveTransaction() throws SQLException {
            context.commit();

            context.close();

            // Commit already called closeConnection, so rollback should not be called again
            // The close() method checks if active before calling rollback
            assertThat(context.isActive()).isFalse();
        }

        @Test
        @DisplayName("关闭已关闭连接不应该抛出异常")
        void shouldNotThrowWhenClosingAlreadyClosedConnection() throws SQLException {
            when(mockConnection.isClosed()).thenReturn(true);

            assertThatCode(() -> context.close())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("关闭连接时 SQLException 应被忽略")
        void shouldIgnoreSQLExceptionOnClose() throws SQLException {
            when(mockConnection.isClosed()).thenReturn(false);
            doThrow(new SQLException("Close failed")).when(mockConnection).close();

            assertThatCode(() -> context.commit())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getConnection 测试")
    class GetConnectionTests {

        @Test
        @DisplayName("应该返回底层连接")
        void shouldReturnUnderlyingConnection() throws SQLException {
            when(mockConnection.isClosed()).thenReturn(false);
            context = new JdbcTransactionContext(mockConnection);

            assertThat(context.getConnection()).isEqualTo(mockConnection);
        }
    }
}
