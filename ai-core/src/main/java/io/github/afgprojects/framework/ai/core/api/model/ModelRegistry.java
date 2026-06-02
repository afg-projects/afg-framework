package io.github.afgprojects.framework.ai.core.api.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 统一模型注册接口 -- 整合 ChatClientRegistry 和 EmbeddingClientRegistry 的模型元数据视图
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface ModelRegistry {

    void registerModel(@NonNull String name, @NonNull ModelInfo info);

    @NonNull
    Optional<ModelInfo> getModel(@NonNull String name);

    @NonNull
    List<ModelInfo> listModels();

    @NonNull
    List<ModelInfo> listModels(@NonNull ModelType type);

    void setDefault(@NonNull String name, @NonNull ModelType type);

    @NonNull
    Optional<ModelInfo> getDefault(@NonNull ModelType type);

    void removeModel(@NonNull String name);
}