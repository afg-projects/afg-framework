package io.github.afgprojects.framework.core.api.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * NoOp 延迟队列降级实现
 * <p>
 * 当没有 Redis 等延迟队列后端时，提供本地降级。
 * 所有入队操作返回 taskId（但不会执行），查询返回 0。
 */
public class NoOpDelayQueue<T> implements DelayQueue<T> {

    @Override
    @NonNull
    public String offer(@NonNull String taskId, @NonNull T payload, @NonNull Duration delay) {
        return taskId;
    }

    @Override
    @NonNull
    public String offerAt(@NonNull String taskId, @NonNull T payload, @NonNull Instant executeTime) {
        return taskId;
    }

    @Override
    public boolean cancel(@NonNull String taskId) {
        return false;
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public long pendingCount() {
        return 0;
    }

    @Override
    public void registerProcessor(@NonNull DelayTaskProcessor<T> processor) {
        // no-op: 处理器不会被调用
    }

    @Override
    public void start() {
        // no-op
    }

    @Override
    public void stop() {
        // no-op
    }
}
