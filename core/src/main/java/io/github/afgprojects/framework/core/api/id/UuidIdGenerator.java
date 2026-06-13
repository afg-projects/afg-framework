package io.github.afgprojects.framework.core.api.id;

import java.util.UUID;

/**
 * UUID ID 生成器实现
 * <p>
 * 基于 {@link UUID#randomUUID()} 生成字符串型 ID。
 * 不保证有序性，适用于不需要排序的场景。
 * 仅支持字符串型 ID，数值型 ID 调用将抛出 {@link UnsupportedOperationException}。
 * </p>
 *
 * @since 1.0.0
 */
public class UuidIdGenerator implements IdGenerator {

    @Override
    public long nextId() {
        throw new UnsupportedOperationException(
                "UUID generator does not support numeric ID generation. Use nextIdAsString() instead.");
    }

    @Override
    public String nextIdAsString() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public IdGeneratorType getType() {
        return IdGeneratorType.UUID;
    }
}
