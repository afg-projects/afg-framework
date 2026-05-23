package io.github.afgprojects.framework.ai.core.etl;

import org.jspecify.annotations.NonNull;

/**
 * 错误处理器接口。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface ErrorHandler {

    /**
     * 处理错误。
     *
     * @param document 发生错误的文档（可能为 null）
     * @param error    异常
     * @param context  执行上下文
     * @return true 继续处理，false 停止处理
     */
    boolean handle(@NonNull Document document, @NonNull Exception error, @NonNull EtlContext context);

    /**
     * 是否支持该阶段的错误处理。
     *
     * @param stage 阶段名称
     * @return 是否支持
     */
    default boolean supports(@NonNull String stage) {
        return true;
    }
}