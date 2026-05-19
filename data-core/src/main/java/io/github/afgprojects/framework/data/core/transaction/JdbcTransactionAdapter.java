package io.github.afgprojects.framework.data.core.transaction;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * JDBC 原生事务适配器
 * <p>
 * 提供基于 JDBC Connection 的事务管理，适用于不使用 Spring 事务管理的场景。
 * <p>
 * 使用示例：
 * <pre>
 * JdbcTransactionAdapter adapter = new JdbcTransactionAdapter(dataSource);
 *
 * adapter.executeInTransaction(() -> {
 *     // 业务逻辑
 * });
 * </pre>
 */
@Slf4j
public class JdbcTransactionAdapter implements TransactionAdapter {

    private final DataSource dataSource;
    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    /**
     * 创建 JDBC 事务适配器
     *
     * @param dataSource 数据源
     */
    public JdbcTransactionAdapter(@NonNull DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void executeInTransaction(@NonNull Runnable action) {
        executeInTransaction(() -> {
            action.run();
            return null;
        });
    }

    @Override
    @SuppressWarnings("PMD.UseTryWithResources")
    public <T> T executeInTransaction(@NonNull Supplier<T> action) {
        Connection connection = null;
        boolean originalAutoCommit = true;
        try {
            connection = dataSource.getConnection();
            connectionHolder.set(connection);
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            T result = action.get();
            connection.commit();
            return result;
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
            }
            throw new TransactionException("Transaction failed", e);
        } finally {
            connectionHolder.remove();
            if (connection != null) {
                try {
                    connection.setAutoCommit(originalAutoCommit);
                    connection.close();
                } catch (SQLException e) {
                    log.warn("Failed to close connection or restore autoCommit", e);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("PMD.UseTryWithResources")
    public <T> T executeInReadOnly(@NonNull Supplier<T> action) {
        Connection connection = null;
        boolean originalReadOnly = false;
        boolean originalAutoCommit = true;
        try {
            connection = dataSource.getConnection();
            connectionHolder.set(connection);
            originalReadOnly = connection.isReadOnly();
            originalAutoCommit = connection.getAutoCommit();
            connection.setReadOnly(true);
            connection.setAutoCommit(true);

            return action.get();
        } catch (SQLException e) {
            throw new TransactionException("Read-only transaction failed", e);
        } finally {
            connectionHolder.remove();
            if (connection != null) {
                try {
                    connection.setReadOnly(originalReadOnly);
                    connection.setAutoCommit(originalAutoCommit);
                    connection.close();
                } catch (SQLException e) {
                    log.warn("Failed to close connection or restore read-only/autoCommit settings", e);
                }
            }
        }
    }

    /**
     * 获取当前线程绑定的数据库连接
     * <p>
     * 用于在事务内部获取当前连接，避免创建新连接。
     *
     * @return 当前连接，如果不在事务中则返回 null
     */
    public Connection getCurrentConnection() {
        return connectionHolder.get();
    }

    /**
     * 获取数据源
     *
     * @return 数据源
     */
    @NonNull
    public DataSource getDataSource() {
        return dataSource;
    }
}