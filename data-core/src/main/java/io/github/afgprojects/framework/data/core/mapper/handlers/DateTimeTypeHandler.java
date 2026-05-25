package io.github.afgprojects.framework.data.core.mapper.handlers;

import io.github.afgprojects.framework.data.core.mapper.TypeHandler;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateTimeTypeHandler implements TypeHandler<LocalDateTime> {
    @Override public Class<LocalDateTime> getType() { return LocalDateTime.class; }

    @Override
    public LocalDateTime convert(Object value, Class<LocalDateTime> targetType) {
        if (value instanceof Timestamp ts) return ts.toLocalDateTime();
        if (value instanceof java.sql.Date d) return d.toLocalDate().atStartOfDay();
        if (value instanceof java.util.Date d) return Instant.ofEpochMilli(d.getTime())
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
        return null;
    }
}