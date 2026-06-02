package io.github.afgprojects.framework.data.core.mapper.handlers;

import io.github.afgprojects.framework.data.core.mapper.TypeHandler;

import java.sql.Date;
import java.time.Year;

/**
 * Year 类型处理器
 * <p>
 * 支持从以下类型转换为 Year：
 * <ul>
 *   <li>Year - 直接返回</li>
 *   <li>Number - intValue() 作为年份</li>
 *   <li>String - 尝试解析为 Year</li>
 *   <li>java.sql.Date - 从日期提取年份</li>
 * </ul>
 */
public class YearTypeHandler implements TypeHandler<Year> {

    @Override
    public Class<Year> getType() {
        return Year.class;
    }

    @Override
    public Year convert(Object value, Class<Year> targetType) {
        if (value == null) return null;

        if (value instanceof Year year) {
            return year;
        }
        if (value instanceof Number number) {
            return Year.of(number.intValue());
        }
        if (value instanceof String str) {
            try {
                return Year.parse(str);
            } catch (Exception ignored) {
                try {
                    return Year.of(Integer.parseInt(str));
                } catch (Exception ignored2) {
                    return null;
                }
            }
        }
        if (value instanceof Date date) {
            return Year.from(date.toLocalDate());
        }

        return null;
    }

    @Override
    public int priority() {
        return 0;
    }
}
