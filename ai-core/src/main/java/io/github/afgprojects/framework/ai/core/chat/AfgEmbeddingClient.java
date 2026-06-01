package io.github.afgprojects.framework.ai.core.chat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 文本向量化通用接口 -- 与 AfgChatClient 对齐的嵌入模型抽象
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface AfgEmbeddingClient {

    /**
     * 批量文本向量化
     */
    @NonNull
    List<float[]> embed(@NonNull List<String> texts);

    /**
     * 单文本向量化
     */
    @NonNull
    float[] embed(@NonNull String text);

    /**
     * 切换模型 -- 返回使用指定模型的新实例
     */
    @NonNull
    AfgEmbeddingClient withModel(@Nullable String modelName);
}