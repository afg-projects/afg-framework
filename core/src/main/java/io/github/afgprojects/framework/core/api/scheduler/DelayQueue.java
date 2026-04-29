package io.github.afgprojects.framework.core.api.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NonNull;

/**
 * 延迟队列接口
 * 具体实现由 afg-redis 或 core 内置的 HashedWheelTimer 提供
 */
public interface DelayQueue<T> {
    @NonNull String offer(@NonNull String taskId, @NonNull T payload, @NonNull Duration delay);
    @NonNull String offerAt(@NonNull String taskId, @NonNull T payload, @NonNull Instant executeTime);
    boolean cancel(@NonNull String taskId);
    long size();
    long pendingCount();
    void registerProcessor(@NonNull DelayTaskProcessor<T> processor);
    void start();
    void stop();

    @FunctionalInterface
    interface DelayTaskProcessor<T> {
        CompletableFuture<Void> process(@NonNull String taskId, @NonNull T payload);
    }
}
