package io.github.afgprojects.framework.ai.core.chat;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.ChatClientRegistry;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChatClientRegistry 默认实现
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultChatClientRegistry implements ChatClientRegistry {

    private final Map<String, AfgChatClient> clients = new ConcurrentHashMap<>();
    private volatile String defaultName;

    @Override
    public void register(@NonNull String name, @NonNull AfgChatClient client) {
        clients.put(name, client);
        if (defaultName == null) {
            defaultName = name;
        }
    }

    @Override
    @NonNull
    public Optional<AfgChatClient> get(@NonNull String name) {
        return Optional.ofNullable(clients.get(name));
    }

    @Override
    @NonNull
    public AfgChatClient getDefault() {
        if (defaultName == null) {
            throw new IllegalStateException("No ChatClient registered");
        }
        var client = clients.get(defaultName);
        if (client == null) {
            throw new IllegalStateException("Default ChatClient '" + defaultName + "' not found");
        }
        return client;
    }

    @Override
    public void setDefault(@NonNull String name) {
        if (!clients.containsKey(name)) {
            throw new IllegalArgumentException("ChatClient '" + name + "' not registered");
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
    }
}
