package io.github.afgprojects.framework.data.core.mapper.handlers;

import io.github.afgprojects.framework.data.core.mapper.TypeHandler;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * OffsetDateTime 类型处理器
 * <p>
 * 支持从以下类型转换为 OffsetDateTime：
 * <ul>
 *   <li>OffsetDateTime - 直接返回</li>
 *   <li>ZonedDateTime - 转换为 OffsetDateTime</li>
 *   <li>java.sql.Timestamp - 使用系统默认时区</li>
 *   <li>java.util.Date - 转为 Instant 后使用系统默认时区</li>
 *   <li>LocalDateTime - 使用系统默认时区</li>
 *   <li>microsoft.sql.DateTimeOffset - 反射调用 getOffsetDateTime()</li>
 * </ul>
 * 兼容 SQL Server DATETIMEOFFSET 类型（通过反射，避免编译依赖）。
 */
public class OffsetDateTimeTypeHandler implements TypeHandler<OffsetDateTime> {

    @Override
    public Class<OffsetDateTime> getType() {
        return OffsetDateTime.class;
    }

    @Override
    public OffsetDateTime convert(Object value, Class<OffsetDateTime> targetType) {
        if (value == null) return null;

        if (value instanceof OffsetDateTime odt) {
            return odt;
        }
        if (value instanceof ZonedDateTime zdt) {
            return zdt.toOffsetDateTime();
        }
        if (value instanceof Timestamp ts) {
            return ts.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt.atZone(ZoneId.systemDefault()).toOffsetDateTime();
        }

        // SQL Server DateTimeOffset（反射，避免编译依赖）
        if ("microsoft.sql.DateTimeOffset".equals(value.getClass().getName())) {
            try {
                Method method = value.getClass().getMethod("getOffsetDateTime");
                return (OffsetDateTime) method.invoke(value);
            } catch (Exception ignored) {
                // 反射失败则继续尝试其他转换
            }
        }

        return null;
    }

    @Override
    public int priority() {
        return 0;
    }
}
