package io.github.afgprojects.framework.core.api.event;

import org.jspecify.annotations.NonNull;

/**
 * 事件订阅接口
 */
@FunctionalInterface
public interface EventSubscriber<T> {
    void onEvent(@NonNull DomainEvent<T> event);
}