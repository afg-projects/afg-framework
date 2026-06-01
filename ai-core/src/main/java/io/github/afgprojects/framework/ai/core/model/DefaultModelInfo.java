package io.github.afgprojects.framework.ai.core.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * ModelInfo 的默认 record 实现
 *
 * @author afg-projects
 * @since 1.0.0
 */
public record DefaultModelInfo(
    @NonNull String name,
    @NonNull ModelType type,
    @Nullable String provider,
    @Nullable String displayName,
    @Nullable Integer contextWindow,
    @Nullable Integer dimensions,
    @Nullable Integer maxOutputTokens,
    @Nullable Double inputPricePer1kTokens,
    @Nullable Double outputPricePer1kTokens,
    boolean available,
    @NonNull Map<String, Object> capabilities
) implements ModelInfo {

    public DefaultModelInfo {
        capabilities = capabilities != null ? Map.copyOf(capabilities) : Map.of();
    }

    public static DefaultModelInfo of(@NonNull String name, @NonNull ModelType type) {
        return new DefaultModelInfo(name, type, null, null, null, null, null, null, null, true, Map.of());
    }

    public static DefaultModelInfo of(@NonNull String name, @NonNull ModelType type, @Nullable String provider) {
        return new DefaultModelInfo(name, type, provider, null, null, null, null, null, null, true, Map.of());
    }
}