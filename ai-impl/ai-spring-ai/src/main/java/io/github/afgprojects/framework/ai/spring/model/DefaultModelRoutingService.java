package io.github.afgprojects.framework.ai.spring.model;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AfgEmbeddingClient;
import io.github.afgprojects.framework.ai.core.api.chat.ChatClientRegistry;
import io.github.afgprojects.framework.ai.core.api.chat.EmbeddingClientRegistry;
import io.github.afgprojects.framework.ai.core.api.model.ModelInfo;
import io.github.afgprojects.framework.ai.core.api.model.ModelRegistry;
import io.github.afgprojects.framework.ai.core.api.model.ModelType;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 默认模型路由服务
 *
 * <p>根据模型名称或类型，将请求路由到合适的 ChatClient 或 EmbeddingClient。
 * 利用 {@link ChatClientRegistry#getOrDefault} 和 {@link EmbeddingClientRegistry#getOrDefault}
 * 实现优雅降级：指定模型不可用时自动回退到默认模型。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class DefaultModelRoutingService {

    private final ChatClientRegistry chatClientRegistry;
    private final EmbeddingClientRegistry embeddingClientRegistry;
    private final ModelRegistry modelRegistry;

    /**
     * 创建模型路由服务
     *
     * @param chatClientRegistry     ChatClient 注册表
     * @param embeddingClientRegistry EmbeddingClient 注册表
     * @param modelRegistry          模型元数据注册表
     */
    public DefaultModelRoutingService(
        @NonNull ChatClientRegistry chatClientRegistry,
        @NonNull EmbeddingClientRegistry embeddingClientRegistry,
        @NonNull ModelRegistry modelRegistry
    ) {
        this.chatClientRegistry = chatClientRegistry;
        this.embeddingClientRegistry = embeddingClientRegistry;
        this.modelRegistry = modelRegistry;
    }

    /**
     * 路由到 ChatClient
     */
    @NonNull
    public AfgChatClient routeChat(@Nullable String modelName) {
        if (modelName != null) {
            log.debug("Routing chat request to model: {}", modelName);
        } else {
            log.debug("Routing chat request to default model");
        }
        return chatClientRegistry.get(modelName).orElseGet(chatClientRegistry::getDefault);
    }

    /**
     * 路由到 EmbeddingClient
     */
    @NonNull
    public AfgEmbeddingClient routeEmbedding(@Nullable String modelName) {
        if (modelName != null) {
            log.debug("Routing embedding request to model: {}", modelName);
        } else {
            log.debug("Routing embedding request to default model");
        }
        return embeddingClientRegistry.get(modelName).orElseGet(embeddingClientRegistry::getDefault);
    }

    /**
     * 根据能力查找可用的 Chat 模型
     */
    @NonNull
    public List<ModelInfo> findChatModelsByCapability(@NonNull String capability) {
        return modelRegistry.listModels(ModelType.CHAT).stream()
            .filter(ModelInfo::available)
            .filter(info -> {
                Object cap = info.capabilities().get(capability);
                return cap != null && Boolean.TRUE.equals(cap);
            })
            .toList();
    }

    /**
     * 根据能力查找可用的 Embedding 模型
     */
    @NonNull
    public List<ModelInfo> findEmbeddingModelsByCapability(@NonNull String capability) {
        return modelRegistry.listModels(ModelType.EMBEDDING).stream()
            .filter(ModelInfo::available)
            .filter(info -> {
                Object cap = info.capabilities().get(capability);
                return cap != null && Boolean.TRUE.equals(cap);
            })
            .toList();
    }

    /**
     * 获取模型信息
     */
    @NonNull
    public Optional<ModelInfo> getModelInfo(@NonNull String modelName) {
        return modelRegistry.getModel(modelName);
    }

    /**
     * 获取默认 Chat 模型信息
     */
    @NonNull
    public Optional<ModelInfo> getDefaultChatModel() {
        return modelRegistry.getDefault(ModelType.CHAT);
    }

    /**
     * 获取默认 Embedding 模型信息
     */
    @NonNull
    public Optional<ModelInfo> getDefaultEmbeddingModel() {
        return modelRegistry.getDefault(ModelType.EMBEDDING);
    }

    /**
     * 列出所有可用的 Chat 模型
     */
    @NonNull
    public List<ModelInfo> listChatModels() {
        return modelRegistry.listModels(ModelType.CHAT).stream()
            .filter(ModelInfo::available)
            .toList();
    }

    /**
     * 列出所有可用的 Embedding 模型
     */
    @NonNull
    public List<ModelInfo> listEmbeddingModels() {
        return modelRegistry.listModels(ModelType.EMBEDDING).stream()
            .filter(ModelInfo::available)
            .toList();
    }
}
