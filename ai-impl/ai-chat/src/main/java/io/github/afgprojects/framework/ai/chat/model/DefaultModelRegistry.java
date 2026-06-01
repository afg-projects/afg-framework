package io.github.afgprojects.framework.ai.chat.model;

import io.github.afgprojects.framework.ai.core.model.DefaultModelInfo;
import io.github.afgprojects.framework.ai.core.model.ModelInfo;
import io.github.afgprojects.framework.ai.core.model.ModelRegistry;
import io.github.afgprojects.framework.ai.core.model.ModelType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ModelRegistry 默认实现 -- 管理模型元数据
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultModelRegistry implements ModelRegistry {

    private final Map<String, ModelInfo> models = new ConcurrentHashMap<>();
    private final Map<ModelType, String> defaults = new ConcurrentHashMap<>();

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
            throw new IllegalArgumentException("Model '" + name + "' not registered");
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