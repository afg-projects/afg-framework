package io.github.afgprojects.framework.core.batch;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 批量操作接口
 * <p>
 * 定义批量处理单个元素的操作逻辑
 * </p>
 *
 * @param <T> 元素类型
 * @param <R> 结果类型
 */
@FunctionalInterface
public interface BatchOperation<T, R> {

    /**
     * 处理单个元素
     *
     * @param item   待处理的元素
     * @param index  元素在批次中的索引（从 0 开始）
     * @return 处理结果
     * @throws Exception 处理过程中发生的异常
     */
    @Nullable
    R execute(@NonNull T item, int index) throws Exception;
}
