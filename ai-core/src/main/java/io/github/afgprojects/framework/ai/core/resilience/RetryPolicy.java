package io.github.afgprojects.framework.ai.core.resilience;

import org.jspecify.annotations.NonNull;

/**
 * 重试策略接口
 *
 * <p>定义 LLM 调用失败时的重试行为：
 * <ul>
 *   <li>最大重试次数</li>
 *   <li>重试间隔策略（固定、指数退避）</li>
 *   <li>可重试的异常类型</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface RetryPolicy {

    /**
     * 判断是否应该重试
     *
     * @param exception    导致失败的异常
     * @param retryCount   当前已重试次数
     * @return 是否应该继续重试
     */
    boolean shouldRetry(@NonNull Exception exception, int retryCount);

    /**
     * 计算下次重试的等待时间
     *
     * @param retryCount 当前已重试次数
     * @return 等待时间（毫秒）
     */
    long getWaitTime(int retryCount);

    /**
     * 获取最大重试次数
     *
     * @return 最大重试次数
     */
    int getMaxRetries();

    /**
     * 执行带重试的操作
     *
     * @param operation 要执行的操作
     * @param <T>       返回类型
     * @return 操作结果
     * @throws Exception 如果所有重试都失败
     */
    @NonNull
    <T> T execute(@NonNull RetryableOperation<T> operation) throws Exception;

    /**
     * 可重试的操作
     *
     * @param <T> 返回类型
     */
    @FunctionalInterface
    interface RetryableOperation<T> {
        T execute() throws Exception;
    }
}