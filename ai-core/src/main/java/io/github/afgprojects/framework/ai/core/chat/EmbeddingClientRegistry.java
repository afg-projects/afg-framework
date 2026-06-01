package io.github.afgprojects.framework.ai.core.chat;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * 多嵌入模型注册表 -- 参照 ChatClientRegistry 模式
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface EmbeddingClientRegistry {

    void register(@NonNull String name, @NonNull AfgEmbeddingClient client);

    @NonNull
    Optional<AfgEmbeddingClient> get(@NonNull String name);

    @NonNull
    AfgEmbeddingClient getDefault();

    void setDefault(@NonNull String name);

    @NonNull
    List<String> listNames();

    void remove(@NonNull String name);
}