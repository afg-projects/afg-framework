package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.dialect.DatabaseType;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JdbcDataManager 覆盖率补充测试
 * <p>
 * 针对低覆盖率分支和方法的测试，特别是 TransactionContextHolderImpl。
 * </p>
 */
@DisplayName("JdbcDataManager 覆盖率补充测试")
class JdbcDataManagerCoverageTest {

    private JdbcDataManager dataManager;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        dataSource = createDataSource();
        dataManager = new JdbcDataManager(dataSource);
    }

    @AfterEach
    void tearDown() {
        // 清理事务上下文
        JdbcDataManager.TransactionContextHolderImpl.clear();
    }

    // ==================== TransactionContextHolderImpl 测试 ====================

    @Nested
    @DisplayName("TransactionContextHolderImpl 测试")
    class TransactionContextHolderImplTests {

        @Test
        @DisplayName("setConnection 和 getConnection 应正确工作")
        void shouldSetAndGetConnection() throws SQLException {
            // Given
            Connection mockConnection = mock(Connection.class);

            // When
            JdbcDataManager.TransactionContextHolderImpl.setConnection(mockConnection);
            Connection result = JdbcDataManager.TransactionContextHolderImpl.getConnection();

            // Then
            assertThat(result).isSameAs(mockConnection);

            // Cleanup
            JdbcDataManager.TransactionContextHolderImpl.clear();
        }

        @Test
        @DisplayName("clear 应清除连接")
        void shouldClearConnection() throws SQLException {
            // Given
            Connection mockConnection = mock(Connection.class);
            JdbcDataManager.TransactionContextHolderImpl.setConnection(mockConnection);

            // When
            JdbcDataManager.TransactionContextHolderImpl.clear();

            // Then
            assertThat(JdbcDataManager.TransactionContextHolderImpl.getConnection()).isNull();
            assertThat(JdbcDataManager.TransactionContextHolderImpl.isInTransaction()).isFalse();
        }

        @Test
        @DisplayName("isInTransaction 应正确判断事务状态")
        void shouldCheckIsInTransaction() throws SQLException {
            // Given - 初始状态
            JdbcDataManager.TransactionContextHolderImpl.clear();
            assertThat(JdbcDataManager.TransactionContextHolderImpl.isInTransaction()).isFalse();

            // When - 设置连接
            Connection mockConnection = mock(Connection.class);
            JdbcDataManager.TransactionContextHolderImpl.setConnection(mockConnection);

            // Then
            assertThat(JdbcDataManager.TransactionContextHolderImpl.isInTransaction()).isTrue();

            // When - 清除
            JdbcDataManager.TransactionContextHolderImpl.clear();

            // Then
            assertThat(JdbcDataManager.TransactionContextHolderImpl.isInTransaction()).isFalse();
        }

        @Test
        @DisplayName("事务内执行操作应正确设置上下文")
        void shouldSetContextInTransaction() {
            // Given
            createTestTable();

            // When - 在事务中执行
            dataManager.executeInTransaction(() -> {
                // Then - 事务上下文应已设置
                assertThat(JdbcDataManager.TransactionContextHolderImpl.isInTransaction()).isTrue();
                assertThat(JdbcDataManager.TransactionContextHolderImpl.getConnection()).isNotNull();
            });

            // Then - 事务结束后上下文应清除
            assertThat(JdbcDataManager.TransactionContextHolderImpl.isInTransaction()).isFalse();
            assertThat(JdbcDataManager.TransactionContextHolderImpl.getConnection()).isNull();
        }

        @Test
        @DisplayName("只读模式下应正确设置上下文")
        void shouldSetContextInReadOnly() {
            // Given
            createTestTable();

            // When - 在只读模式中执行
            dataManager.executeInReadOnly(() -> {
                // Then - 事务上下文应已设置
                assertThat(JdbcDataManager.TransactionContextHolderImpl.isInTransaction()).isTrue();
                assertThat(JdbcDataManager.TransactionContextHolderImpl.getConnection()).isNotNull();
                return null;
            });

            // Then - 执行结束后上下文应清除
            assertThat(JdbcDataManager.TransactionContextHolderImpl.isInTransaction()).isFalse();
        }

        @Test
        @DisplayName("事务异常时应清除上下文")
        void shouldClearContextOnTransactionException() {
            // Given
            createTestTable();

            // When - 事务中抛出异常
            assertThatThrownBy(() -> {
                dataManager.executeInTransaction(() -> {
                    // 事务上下文应已设置
                    assertThat(JdbcDataManager.TransactionContextHolderImpl.isInTransaction()).isTrue();
                    throw new RuntimeException("Test exception");
                });
            }).isInstanceOf(RuntimeException.class)
              .hasMessageContaining("Transaction failed");

            // Then - 异常后上下文应清除
            assertThat(JdbcDataManager.TransactionContextHolderImpl.isInTransaction()).isFalse();
            assertThat(JdbcDataManager.TransactionContextHolderImpl.getConnection()).isNull();
        }

        @Test
        @DisplayName("只读模式异常时应清除上下文")
        void shouldClearContextOnReadOnlyException() {
            // Given
            createTestTable();

            // When - 只读模式中抛出异常
            assertThatThrownBy(() -> {
                dataManager.executeInReadOnly(() -> {
                    throw new RuntimeException("Test exception");
                });
            }).isInstanceOf(RuntimeException.class);

            // Then - 异常后上下文应清除
            assertThat(JdbcDataManager.TransactionContextHolderImpl.isInTransaction()).isFalse();
            assertThat(JdbcDataManager.TransactionContextHolderImpl.getConnection()).isNull();
        }
    }

    // ==================== executeInsertAndReturnKey 事务模式测试 ====================

    @Nested
    @DisplayName("executeInsertAndReturnKey 事务模式测试")
    class ExecuteInsertAndReturnKeyTransactionTests {

        @Test
        @DisplayName("事务模式下插入应返回生成的主键")
        void shouldReturnKeyInTransaction() {
            createTestTable();

            // When
            Long id = dataManager.executeInTransaction(() -> {
                return dataManager.executeInsertAndReturnKey(
                    "INSERT INTO test_table (name) VALUES (?)",
                    java.util.List.of("transaction-key")
                );
            });

            // Then
            assertThat(id).isNotNull();
            assertThat(id).isGreaterThan(0);
        }

        @Test
        @DisplayName("事务模式批量插入应正确获取主键")
        void shouldReturnKeysInTransactionBatch() {
            createTestTable();

            // When
            java.util.List<Long> ids = dataManager.executeInTransaction(() -> {
                long id1 = dataManager.executeInsertAndReturnKey(
                    "INSERT INTO test_table (name) VALUES (?)",
                    java.util.List.of("batch-tx-1")
                );
                long id2 = dataManager.executeInsertAndReturnKey(
                    "INSERT INTO test_table (name) VALUES (?)",
                    java.util.List.of("batch-tx-2")
                );
                return java.util.List.of(id1, id2);
            });

            // Then
            assertThat(ids).hasSize(2);
            assertThat(ids.get(0)).isNotEqualTo(ids.get(1));
        }
    }

    // ==================== 数据库类型检测测试 ====================

    @Nested
    @DisplayName("数据库类型检测测试")
    class DatabaseTypeDetectionTests {

        @Test
        @DisplayName("检测失败时应默认为 MySQL")
        void shouldDefaultToMySQLOnDetectionFailure() throws SQLException {
            // Given - 模拟连接失败
            DataSource mockDataSource = mock(DataSource.class);
            when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

            // When
            JdbcDataManager manager = new JdbcDataManager(mockDataSource);

            // Then
            assertThat(manager.getDatabaseType()).isEqualTo(DatabaseType.MYSQL);
        }
    }

    // ==================== 辅助方法 ====================

    private DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:dmcoverage;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void createTestTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS test_table (id SERIAL PRIMARY KEY, name VARCHAR(100))");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test table", e);
        }
    }
}
