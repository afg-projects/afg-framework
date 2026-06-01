package io.github.afgprojects.framework.data.core.transaction;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * JDBC {@link TransactionAdapter} 实现，不依赖 Spring TX。
 */
public class JdbcTransactionAdapter implements TransactionAdapter {

    private static final Logger log = LoggerFactory.getLogger(JdbcTransactionAdapter.class);

    private final DataSource dataSource;

    public JdbcTransactionAdapter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <T> T executeInTransaction(@NonNull Supplier<T> action) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = action.get();
                conn.commit();
                return result;
            } catch (Exception e) {
                conn.rollback();
                log.error("Transaction rolled back due to error", e);
                throw new RuntimeException("Transaction failed", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get connection", e);
        }
    }

    @Override
    public void executeInTransaction(@NonNull Runnable action) {
        executeInTransaction(() -> {
            action.run();
            return null;
        });
    }

    @Override
    public <T> T executeInReadOnly(@NonNull Supplier<T> action) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(true);
            conn.setReadOnly(true);
            try {
                return action.get();
            } catch (Exception e) {
                log.error("Read-only operation failed", e);
                throw new RuntimeException("Read-only operation failed", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get connection", e);
        }
    }
}