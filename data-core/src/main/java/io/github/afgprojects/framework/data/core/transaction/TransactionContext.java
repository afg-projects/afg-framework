package io.github.afgprojects.framework.data.core.transaction;

/**
 * 事务上下文接口
 * <p>
 * 提供事务控制能力
 */
public interface TransactionContext extends AutoCloseable {

    /**
     * 提交事务
     */
    void commit();

    /**
     * 回滚事务
     */
    void rollback();

    /**
     * 检查事务是否活跃
     *
     * @return 事务是否活跃
     */
    boolean isActive();

    /**
     * 关闭事务上下文
     * <p>
     * 如果事务仍然活跃，将执行回滚
     */
    @Override
    void close();
}