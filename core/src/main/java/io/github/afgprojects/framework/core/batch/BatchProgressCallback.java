package io.github.afgprojects.framework.core.batch;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 批量操作进度回调接口
 * <p>
 * 用于监控批量操作的执行进度
 * </p>
 *
 * @param <T> 元素类型
 * @param <R> 结果类型
 */
public interface BatchProgressCallback<T, R> {

    /**
     * 单个元素处理完成时调用
     *
     * @param item    已处理的元素
     * @param index   元素索引
     * @param result  处理结果（可能为 null）
     * @param success 是否成功
     */
    void onItemComplete(@NonNull T item, int index, @Nullable R result, boolean success);

    /**
     * 批次处理完成时调用
     *
     * @param completed 已完成的元素数量
     * @param total     总元素数量
     */
    void onBatchProgress(int completed, int total);

    /**
     * 所有处理完成时调用
     *
     * @param result 批量处理结果
     */
    void onComplete(@NonNull BatchResult<R> result);
}
