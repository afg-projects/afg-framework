package io.github.afgprojects.framework.ai.core.resilience;

import io.github.afgprojects.framework.ai.core.api.resilience.FallbackStrategy;
import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.Nullable;

/**
 * 默认值降级策略
 *
 * <p>当主操作失败时返回预设的默认值，适用于可容忍空值或占位值的场景。
 *
 * @param <T> 返回类型
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class DefaultValueFallbackStrategy<T> implements FallbackStrategy<T> {

    private final @Nullable T defaultValue;

    public DefaultValueFallbackStrategy() {
        this(null);
    }

    public DefaultValueFallbackStrategy(@Nullable T defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public @Nullable T fallback(Exception exception, FallbackStrategy.FallbackContext context) {
        log.warn("Fallback triggered for request [{}]: {}", context.getRequestId(), exception.getMessage());
        return defaultValue;
    }

    @Override
    public boolean shouldFallback(Exception exception) {
        return true;
    }
}
