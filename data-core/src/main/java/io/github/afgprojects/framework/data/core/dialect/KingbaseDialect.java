package io.github.afgprojects.framework.data.core.dialect;

import org.jspecify.annotations.NonNull;

/**
 * KingbaseES 方言。
 * 继承 PostgreSQLDialect，覆盖金仓特有的类型映射。
 */
public class KingbaseDialect extends PostgreSQLDialect {

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return DatabaseType.KINGBASE;
    }
}
