package io.github.afgprojects.framework.ai.spring.model;

import io.github.afgprojects.framework.ai.core.api.model.DefaultModelInfo;
import io.github.afgprojects.framework.ai.core.api.model.ModelInfo;
import io.github.afgprojects.framework.ai.core.api.model.ModelRegistry;
import io.github.afgprojects.framework.ai.core.api.model.ModelType;
import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring AI 模型注册表实现
 *
 * <p>自动检测 Spring AI 的 {@link ChatModel} 和 {@link EmbeddingModel} Bean，
 * 并将其注册到 AFG 框架的 {@link ModelRegistry} 中。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class SpringAiModelRegistry implements ModelRegistry {

    private final Map<String, ModelInfo> models = new ConcurrentHashMap<>();
    private final Map<ModelType, String> defaults = new ConcurrentHashMap<>();

    /**
     * 注册 ChatModel
     *
     * @param name      模型名称
     * @param chatModel ChatModel 实例
     * @param provider  提供商（如 openai, anthropic, ollama）
     */
    public void registerChatModel(@NonNull String name, @NonNull ChatModel chatModel, @Nullable String provider) {
        var info = DefaultModelInfo.of(name, ModelType.CHAT, provider);
        models.put(name, info);
    }

    /**
     * 注册 EmbeddingModel
     *
     * @param name           模型名称
     * @param embeddingModel EmbeddingModel 实例
     * @param provider       提供商
     */
    public void registerEmbeddingModel(@NonNull String name, @NonNull EmbeddingModel embeddingModel, @Nullable String provider) {
        var info = new DefaultModelInfo(
            name,
            ModelType.EMBEDDING,
            provider,
            null,
            null,
            extractDimensions(embeddingModel),
            null,
            null,
            null,
            true,
            Map.of()
        );
        models.put(name, info);
    }

    private @Nullable Integer extractDimensions(EmbeddingModel model) {
        // 尝试从模型获取维度信息，如果不可用则返回 null
        try {
            var dimensions = model.dimensions();
            if (dimensions > 0) {
                return dimensions;
            }
        } catch (Exception ignored) {
            // 某些模型可能不支持此方法
        }
        return null;
    }

    @Override
    public void registerModel(@NonNull String name, @NonNull ModelInfo info) {
        models.put(name, info);
    }

    @Override
    @NonNull
    public Optional<ModelInfo> getModel(@NonNull String name) {
        return Optional.ofNullable(models.get(name));
    }

    @Override
    @NonNull
    public List<ModelInfo> listModels() {
        return List.copyOf(models.values());
    }

    @Override
    @NonNull
    public List<ModelInfo> listModels(@NonNull ModelType type) {
        return models.values().stream()
                .filter(info -> info.type() == type)
                .toList();
    }

    @Override
    public void setDefault(@NonNull String name, @NonNull ModelType type) {
        if (!models.containsKey(name)) {
            throw new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "Model '" + name + "' not registered");
        }
        defaults.put(type, name);
    }

    @Override
    @NonNull
    public Optional<ModelInfo> getDefault(@NonNull ModelType type) {
        var defaultName = defaults.get(type);
        if (defaultName == null) {
            return listModels(type).stream().findFirst();
        }
        return Optional.ofNullable(models.get(defaultName));
    }

    @Override
    public void removeModel(@NonNull String name) {
        models.remove(name);
        defaults.entrySet().removeIf(e -> name.equals(e.getValue()));
    }
}
