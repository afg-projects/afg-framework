package io.github.afgprojects.framework.data.core.mapper.handlers;

import io.github.afgprojects.framework.data.core.mapper.TypeHandler;
import java.sql.Date;
import java.time.LocalDate;

public class LocalDateTypeHandler implements TypeHandler<LocalDate> {
    @Override public Class<LocalDate> getType() { return LocalDate.class; }

    @Override
    public LocalDate convert(Object value, Class<LocalDate> targetType) {
        if (value instanceof Date d) return d.toLocalDate();
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        if (value instanceof java.util.Date d) return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        return null;
    }
}