package microsoft.sql;

import java.time.OffsetDateTime;

/**
 * DateTimeOffset 测试替身
 * <p>
 * 模拟 SQL Server JDBC 驱动中的 DateTimeOffset 类，用于单元测试。
 * 仅实现 {@code getOffsetDateTime()} 方法，满足 {@code AbstractResultSetMapper} 的反射调用。
 */
public class DateTimeOffset {

    private final OffsetDateTime offsetDateTime;

    private DateTimeOffset(OffsetDateTime offsetDateTime) {
        this.offsetDateTime = offsetDateTime;
    }

    public static DateTimeOffset valueOf(OffsetDateTime offsetDateTime) {
        return new DateTimeOffset(offsetDateTime);
    }

    public OffsetDateTime getOffsetDateTime() {
        return offsetDateTime;
    }
}
