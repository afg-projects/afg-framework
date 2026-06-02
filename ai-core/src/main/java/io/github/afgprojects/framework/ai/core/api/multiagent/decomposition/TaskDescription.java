package io.github.afgprojects.framework.ai.core.api.multiagent.decomposition;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 任务描述
 */
public record TaskDescription(
        @NonNull String name,
        @NonNull String description,
        @Nullable String type,
        @Nullable Map<String, Object> parameters
) {
    /**
     * 创建简单任务描述
     */
    public static TaskDescription of(String name, String description) {
        return new TaskDescription(name, description, null, null);
    }
}
