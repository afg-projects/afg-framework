package io.github.afgprojects.framework.data.core.dialect;

import org.jspecify.annotations.NonNull;

/**
 * OceanBase 方言。
 * 继承 MySQLDialect，覆盖 OceanBase 特有的函数。
 */
public class OceanBaseDialect extends MySQLDialect {

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return DatabaseType.OCEANBASE;
    }

    @Override
    public @NonNull String getCurrentTimeFunction() {
        return "NOW()";
    }

    @Override
    public @NonNull String getCurrentTimestampFunction() {
        return "NOW(6)";
    }
}