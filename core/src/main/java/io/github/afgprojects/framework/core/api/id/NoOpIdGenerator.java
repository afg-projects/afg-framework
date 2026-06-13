package io.github.afgprojects.framework.core.api.id;

import java.util.concurrent.atomic.AtomicLong;

/**
 * NoOp ID 生成器实现
 * <p>
 * 本地降级实现，使用内存自增计数器生成 ID。
 * 不具备分布式安全性，仅适用于单机降级场景或不需要分布式 ID 的场景。
 * </p>
 * <p>
 * 由 {@code IdGeneratorAutoConfiguration} 在无其他 {@link IdGenerator} 实现时自动注册。
 * </p>
 *
 * @since 1.0.0
 */
public class NoOpIdGenerator implements IdGenerator {

    private final AtomicLong counter = new AtomicLong(1);

    @Override
    public long nextId() {
        return counter.getAndIncrement();
    }

    @Override
    public String nextIdAsString() {
        return String.valueOf(nextId());
    }

    @Override
    public IdGeneratorType getType() {
        return IdGeneratorType.UUID;
    }
}
