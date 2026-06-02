package io.github.afgprojects.framework.data.core.mapper.handlers;

import io.github.afgprojects.framework.data.core.mapper.TypeHandler;

import java.sql.Date;
import java.time.YearMonth;

/**
 * YearMonth 类型处理器
 * <p>
 * 支持从以下类型转换为 YearMonth：
 * <ul>
 *   <li>YearMonth - 直接返回</li>
 *   <li>String - 尝试解析为 YearMonth（格式：yyyy-MM）</li>
 *   <li>java.sql.Date - 从日期提取 YearMonth</li>
 * </ul>
 */
public class YearMonthTypeHandler implements TypeHandler<YearMonth> {

    @Override
    public Class<YearMonth> getType() {
        return YearMonth.class;
    }

    @Override
    public YearMonth convert(Object value, Class<YearMonth> targetType) {
        if (value == null) return null;

        if (value instanceof YearMonth ym) {
            return ym;
        }
        if (value instanceof String str) {
            try {
                return YearMonth.parse(str);
            } catch (Exception ignored) {
                return null;
            }
        }
        if (value instanceof Date date) {
            return YearMonth.from(date.toLocalDate());
        }

        return null;
    }

    @Override
    public int priority() {
        return 0;
    }
}
