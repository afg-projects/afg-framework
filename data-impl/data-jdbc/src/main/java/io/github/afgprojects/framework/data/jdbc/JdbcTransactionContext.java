/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.afgprojects.framework.data.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import io.github.afgprojects.framework.data.core.transaction.TransactionContext;
import lombok.extern.slf4j.Slf4j;

/**
 * JDBC 事务上下文实现。
 *
 * <p>管理 JDBC 连接的生命周期，包括事务的开始、提交、回滚和连接关闭。
 * 确保在 commit 失败时尝试回滚，避免数据不一致。
 */
@Slf4j
public class JdbcTransactionContext implements TransactionContext {

    private final Connection connection;
    private volatile boolean active;

    /**
     * 创建 JDBC 事务上下文。
     *
     * @param connection 数据库连接，必须已设置 autoCommit = false
     */
    public JdbcTransactionContext(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "Connection must not be null");
        this.active = true;
    }

    @Override
    public void commit() {
        if (!active) {
            throw new IllegalStateException("Transaction is not active");
        }
        try {
            connection.commit();
        } catch (SQLException e) {
            // commit 失败，尝试回滚以保证数据一致性
            try {
                connection.rollback();
                log.warn("Transaction commit failed, successfully rolled back", e);
            } catch (SQLException rollbackEx) {
                e.addSuppressed(rollbackEx);
                log.error("Transaction commit and rollback both failed", rollbackEx);
            }
            throw new RuntimeException("Failed to commit transaction", e);
        } finally {
            active = false;
            closeConnection();
        }
    }

    @Override
    public void rollback() {
        if (!active) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException e) {
            log.error("Failed to rollback transaction", e);
            throw new RuntimeException("Failed to rollback transaction", e);
        } finally {
            active = false;
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
        } else {
            closeConnection();
        }
    }

    /**
     * 获取底层数据库连接。
     *
     * @return JDBC 连接
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * 安全关闭数据库连接，记录异常而非吞掉。
     */
    private void closeConnection() {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            log.warn("Failed to close database connection", e);
        }
    }
}
