package io.github.afgprojects.framework.data.core.mapper.handlers;

import io.github.afgprojects.framework.data.core.mapper.TypeHandler;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * Instant 类型处理器
 * <p>
 * 支持从以下类型转换为 Instant：
 * <ul>
 *   <li>Instant - 直接返回</li>
 *   <li>java.sql.Timestamp - toInstant()</li>
 *   <li>java.util.Date - toInstant()</li>
 *   <li>OffsetDateTime - toInstant()</li>
 *   <li>LocalDateTime - 使用系统默认时区转换为 Instant</li>
 * </ul>
 */
public class InstantTypeHandler implements TypeHandler<Instant> {

    @Override
    public Class<Instant> getType() {
        return Instant.class;
    }

    @Override
    public Instant convert(Object value, Class<Instant> targetType) {
        if (value == null) return null;

        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp ts) {
            return ts.toInstant();
        }
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof OffsetDateTime odt) {
            return odt.toInstant();
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt.atZone(ZoneId.systemDefault()).toInstant();
        }

        return null;
    }

    @Override
    public int priority() {
        return 0;
    }
}
