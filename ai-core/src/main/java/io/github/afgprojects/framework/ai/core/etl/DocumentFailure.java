package io.github.afgprojects.framework.ai.core.etl;

import io.github.afgprojects.framework.ai.core.rag.Document;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 文档处理失败记录。
 *
 * @param document  失败的文档
 * @param stage     失败阶段（read, transform, write）
 * @param error     错误消息
 * @param exception 异常对象
 * @author AFG Projects
 * @since 1.0.0
 */
public record DocumentFailure(
    @Nullable Document document,
    @NonNull String stage,
    @NonNull String error,
    @Nullable Exception exception
) {
}