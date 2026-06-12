package io.github.afgprojects.framework.core.api.event;

import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.NonNull;

/**
 * NoOp 事件发布器降级实现
 * <p>
 * 当没有 RabbitMQ 等事件后端时，提供本地降级。
 * 所有发布操作静默丢弃，异步发布返回已完成的 Future。
 */
public class NoOpEventPublisher<T> implements EventPublisher<T> {

    @Override
    public void publish(@NonNull MessageEvent<T> event) {
        // no-op: 事件静默丢弃
    }

    @Override
    public CompletableFuture<Void> publishAsync(@NonNull MessageEvent<T> event) {
        return CompletableFuture.completedFuture(null);
    }
}
