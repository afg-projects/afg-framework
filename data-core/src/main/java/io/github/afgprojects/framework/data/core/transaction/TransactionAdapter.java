package io.github.afgprojects.framework.data.core.transaction;

import org.jspecify.annotations.NonNull;

import java.util.function.Supplier;

/**
 * 事务适配器接口
 * <p>
 * 定义事务管理的基本操作，支持不同的底层实现。
 * <p>
 * 实现类：
 * <ul>
 *   <li>{@link SpringTransactionAdapter} - Spring 声明式事务集成</li>
 *   <li>{@link JdbcTransactionAdapter} - JDBC 原生事务</li>
 * </ul>
 */
public interface TransactionAdapter {

    /**
     * 在事务中执行操作
     *
     * @param action 要执行的操作
     */
    void executeInTransaction(@NonNull Runnable action);

    /**
     * 在事务中执行操作并返回结果
     *
     * @param action 要执行的操作
     * @param <T>    返回类型
     * @return 操作结果
     */
    <T> T executeInTransaction(@NonNull Supplier<T> action);

    /**
     * 在只读事务中执行操作
     *
     * @param action 要执行的操作
     * @param <T>    返回类型
     * @return 操作结果
     */
    <T> T executeInReadOnly(@NonNull Supplier<T> action);
}