package io.github.afgprojects.framework.data.core.mapper.handlers;

import io.github.afgprojects.framework.data.core.mapper.TypeHandler;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * UUID 类型处理器
 * <p>
 * 支持从以下类型转换为 UUID：
 * <ul>
 *   <li>UUID - 直接返回</li>
 *   <li>String - UUID.fromString()</li>
 *   <li>byte[] - 转换为 UUID（16 字节，兼容 PostgreSQL UUID 列类型）</li>
 * </ul>
 * PostgreSQL 原生 UUID 列类型返回 java.util.UUID 对象，
 * 其他数据库通常以 String 或 byte[] 形式返回。
 */
public class UUIDTypeHandler implements TypeHandler<UUID> {

    @Override
    public Class<UUID> getType() {
        return UUID.class;
    }

    @Override
    public UUID convert(Object value, Class<UUID> targetType) {
        if (value == null) return null;

        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String str) {
            try {
                return UUID.fromString(str);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        if (value instanceof byte[] bytes) {
            if (bytes.length == 16) {
                ByteBuffer bb = ByteBuffer.wrap(bytes);
                long mostSigBits = bb.getLong();
                long leastSigBits = bb.getLong();
                return new UUID(mostSigBits, leastSigBits);
            }
        }

        return null;
    }

    @Override
    public int priority() {
        return 5;
    }
}
