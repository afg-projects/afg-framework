package io.github.afgprojects.framework.ai.model;

import io.github.afgprojects.framework.ai.core.model.LlmClient;
import io.github.afgprojects.framework.ai.core.model.LlmClientRegistry;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认 LLM 客户端注册表实现
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultLlmClientRegistry implements LlmClientRegistry {

    private final Map<String, LlmClient> clients = new ConcurrentHashMap<>();
    private volatile String defaultName = null;

    @Override
    public void register(@NonNull String name, @NonNull LlmClient client) {
        if (clients.containsKey(name)) {
            throw new IllegalArgumentException("LLM client already registered: " + name);
        }
        clients.put(name, client);
        if (defaultName == null) {
            defaultName = name;
        }
    }

    @Override
    public void registerOrReplace(@NonNull String name, @NonNull LlmClient client) {
        boolean wasEmpty = clients.isEmpty();
        clients.put(name, client);
        if (wasEmpty) {
            defaultName = name;
        }
    }

    @Override
    public @NonNull Optional<LlmClient> get(@NonNull String name) {
        return Optional.ofNullable(clients.get(name));
    }

    @Override
    public @NonNull LlmClient getDefault() {
        if (clients.isEmpty()) {
            throw new IllegalStateException("No LLM client registered");
        }
        if (defaultName == null || !clients.containsKey(defaultName)) {
            defaultName = clients.keySet().iterator().next();
        }
        return clients.get(defaultName);
    }

    @Override
    public void setDefault(@NonNull String name) {
        if (!clients.containsKey(name)) {
            throw new IllegalArgumentException("LLM client not found: " + name);
        }
        this.defaultName = name;
    }

    @Override
    public boolean exists(@NonNull String name) {
        return clients.containsKey(name);
    }

    @Override
    public boolean unregister(@NonNull String name) {
        LlmClient removed = clients.remove(name);
        if (removed != null && name.equals(defaultName)) {
            defaultName = clients.isEmpty() ? null : clients.keySet().iterator().next();
        }
        return removed != null;
    }

    @Override
    public @NonNull Collection<String> getNames() {
        return Collections.unmodifiableSet(clients.keySet());
    }

    @Override
    public @NonNull Map<String, LlmClient> getAll() {
        return Collections.unmodifiableMap(clients);
    }

    @Override
    public int size() {
        return clients.size();
    }

    @Override
    public void clear() {
        clients.clear();
        defaultName = null;
    }
}
