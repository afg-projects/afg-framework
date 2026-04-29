package io.github.afgprojects.framework.data.core.dialect;

import io.github.afgprojects.framework.data.core.page.PageRequest;
import org.jspecify.annotations.NonNull;

/**
 * OceanBase 数据库方言
 * <p>
 * OceanBase 兼容 MySQL 模式，语法与 MySQL 基本一致
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
        return "NOW(6)";  // OceanBase 支持微秒精度
    }
}