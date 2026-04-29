package io.github.afgprojects.framework.core.batch;

import java.io.Serial;
import java.io.Serializable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 批量操作错误详情
 * <p>
 * 记录单个元素处理失败时的详细信息
 * </p>
 *
 * @param index   元素索引
 * @param item    失败的元素（字符串表示）
 * @param error   错误信息
 * @param cause   异常类型（可选）
 */
@SuppressWarnings("PMD.ShortClassName")
public record BatchError(
        int index,
        @Nullable String item,
        @NonNull String error,
        @Nullable String cause)
        implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建简单的错误信息
     *
     * @param index 元素索引
     * @param error 错误信息
     * @return 错误详情
     */
    public static BatchError of(int index, @NonNull String error) {
        return new BatchError(index, null, error, null);
    }

    /**
     * 创建包含元素的错误信息
     *
     * @param index 元素索引
     * @param item  失败的元素
     * @param error 错误信息
     * @return 错误详情
     */
    public static BatchError of(int index, @Nullable String item, @NonNull String error) {
        return new BatchError(index, item, error, null);
    }

    /**
     * 创建包含异常的错误信息
     *
     * @param index     元素索引
     * @param item      失败的元素
     * @param error     错误信息
     * @param exception 异常
     * @return 错误详情
     */
    public static BatchError of(int index, @Nullable String item, @NonNull String error, @Nullable Throwable exception) {
        return new BatchError(index, item, error, exception != null ? exception.getClass().getName() : null);
    }
}
