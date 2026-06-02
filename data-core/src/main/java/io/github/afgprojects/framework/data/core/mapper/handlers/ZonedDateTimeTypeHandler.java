package io.github.afgprojects.framework.data.core.mapper.handlers;

import io.github.afgprojects.framework.data.core.mapper.TypeHandler;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * ZonedDateTime 类型处理器
 * <p>
 * 支持从以下类型转换为 ZonedDateTime：
 * <ul>
 *   <li>ZonedDateTime - 直接返回</li>
 *   <li>OffsetDateTime - toZonedDateTime()</li>
 *   <li>java.sql.Timestamp - 使用系统默认时区</li>
 *   <li>java.util.Date - 使用系统默认时区</li>
 *   <li>LocalDateTime - 使用系统默认时区</li>
 *   <li>microsoft.sql.DateTimeOffset - 反射调用 getOffsetDateTime().toZonedDateTime()</li>
 * </ul>
 */
public class ZonedDateTimeTypeHandler implements TypeHandler<ZonedDateTime> {

    @Override
    public Class<ZonedDateTime> getType() {
        return ZonedDateTime.class;
    }

    @Override
    public ZonedDateTime convert(Object value, Class<ZonedDateTime> targetType) {
        if (value == null) return null;

        if (value instanceof ZonedDateTime zdt) {
            return zdt;
        }
        if (value instanceof OffsetDateTime odt) {
            return odt.toZonedDateTime();
        }
        if (value instanceof Timestamp ts) {
            return ts.toInstant().atZone(ZoneId.systemDefault());
        }
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault());
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt.atZone(ZoneId.systemDefault());
        }

        // SQL Server DateTimeOffset（反射，避免编译依赖）
        if ("microsoft.sql.DateTimeOffset".equals(value.getClass().getName())) {
            try {
                Method method = value.getClass().getMethod("getOffsetDateTime");
                OffsetDateTime odt = (OffsetDateTime) method.invoke(value);
                return odt.toZonedDateTime();
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
