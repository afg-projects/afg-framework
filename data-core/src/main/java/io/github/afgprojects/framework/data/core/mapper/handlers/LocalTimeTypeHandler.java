package io.github.afgprojects.framework.data.core.mapper.handlers;

import io.github.afgprojects.framework.data.core.mapper.TypeHandler;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.Date;

/**
 * LocalTime 类型处理器
 * <p>
 * 支持从以下类型转换为 LocalTime：
 * <ul>
 *   <li>LocalTime - 直接返回</li>
 *   <li>java.sql.Time - toLocalTime()</li>
 *   <li>java.sql.Timestamp - toLocalDateTime().toLocalTime()</li>
 *   <li>java.util.Date - 转换时间部分</li>
 *   <li>String - 尝试解析为 LocalTime</li>
 * </ul>
 */
public class LocalTimeTypeHandler implements TypeHandler<LocalTime> {

    @Override
    public Class<LocalTime> getType() {
        return LocalTime.class;
    }

    @Override
    public LocalTime convert(Object value, Class<LocalTime> targetType) {
        if (value == null) return null;

        if (value instanceof LocalTime lt) {
            return lt;
        }
        if (value instanceof Time time) {
            return time.toLocalTime();
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime().toLocalTime();
        }
        if (value instanceof Date date) {
            return new Timestamp(date.getTime()).toLocalDateTime().toLocalTime();
        }
        if (value instanceof String str) {
            try {
                return LocalTime.parse(str);
            } catch (Exception ignored) {
                // 解析失败返回 null
            }
        }

        return null;
    }

    @Override
    public int priority() {
        return 0;
    }
}
