package io.github.afgprojects.framework.data.core.id;

import java.util.UUID;

import org.jspecify.annotations.NonNull;

/**
 * UUID 生成器
 */
public class UuidGenerator implements IdentifierGenerator {

    /**
     * 是否使用无连字符格式
     */
    private final boolean noHyphens;

    /**
     * 构造 UUID 生成器（默认带连字符）
     */
    public UuidGenerator() {
        this(false);
    }

    /**
     * 构造 UUID 生成器
     *
     * @param noHyphens 是否使用无连字符格式
     */
    public UuidGenerator(boolean noHyphens) {
        this.noHyphens = noHyphens;
    }

    @Override
    public @NonNull Object generate() {
        UUID uuid = UUID.randomUUID();
        if (noHyphens) {
            return uuid.toString().replace("-", "");
        }
        return uuid.toString();
    }

    @Override
    public @NonNull String generateString() {
        return (String) generate();
    }

    @Override
    public @NonNull IdType getIdType() {
        return noHyphens ? IdType.UUID_HEX : IdType.UUID;
    }
}