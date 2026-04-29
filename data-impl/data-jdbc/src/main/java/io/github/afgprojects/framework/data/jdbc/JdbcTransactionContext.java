package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.transaction.TransactionContext;
import lombok.Getter;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * JDBC 事务上下文实现
 */
public class JdbcTransactionContext implements TransactionContext {

    /**
     * -- GETTER --
     *  获取底层 Connection
     */
    @Getter
    private final Connection connection;
    private boolean active = true;

    public JdbcTransactionContext(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void commit() {
        if (!active) {
            throw new IllegalStateException("Transaction is not active");
        }
        try {
            connection.commit();
            active = false;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to commit transaction", e);
        } finally {
            closeConnection();
        }
    }

    @Override
    public void rollback() {
        if (!active) {
            return;  // 已经不活跃，忽略
        }
        try {
            connection.rollback();
            active = false;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to rollback transaction", e);
        } finally {
            closeConnection();
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void close() {
        if (active) {
            rollback();
        }
        closeConnection();
    }

    private void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
            // 忽略关闭异常
        }
    }
}
