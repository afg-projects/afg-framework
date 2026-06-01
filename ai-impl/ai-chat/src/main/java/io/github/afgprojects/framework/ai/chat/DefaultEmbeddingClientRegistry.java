package io.github.afgprojects.framework.ai.chat;

import io.github.afgprojects.framework.ai.core.chat.AfgEmbeddingClient;
import io.github.afgprojects.framework.ai.core.chat.EmbeddingClientRegistry;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EmbeddingClientRegistry 默认实现 -- 参照 DefaultChatClientRegistry
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultEmbeddingClientRegistry implements EmbeddingClientRegistry {

    private final Map<String, AfgEmbeddingClient> clients = new ConcurrentHashMap<>();
    private volatile String defaultName;

    @Override
    public void register(@NonNull String name, @NonNull AfgEmbeddingClient client) {
        clients.put(name, client);
        if (defaultName == null) {
            defaultName = name;
        }
    }

    @Override
    @NonNull
    public Optional<AfgEmbeddingClient> get(@NonNull String name) {
        return Optional.ofNullable(clients.get(name));
    }

    @Override
    @NonNull
    public AfgEmbeddingClient getDefault() {
        if (defaultName == null) {
            throw new IllegalStateException("No EmbeddingClient registered");
        }
        var client = clients.get(defaultName);
        if (client == null) {
            throw new IllegalStateException("Default EmbeddingClient '" + defaultName + "' not found");
        }
        return client;
    }

    @Override
    public void setDefault(@NonNull String name) {
        if (!clients.containsKey(name)) {
            throw new IllegalArgumentException("EmbeddingClient '" + name + "' not registered");
        }
        this.defaultName = name;
    }

    @Override
    @NonNull
    public List<String> listNames() {
        return List.copyOf(clients.keySet());
    }

    @Override
    public void remove(@NonNull String name) {
        clients.remove(name);
        if (name.equals(defaultName)) {
            defaultName = clients.isEmpty() ? null : clients.keySet().iterator().next();
        }
    }
}