package io.github.afgprojects.framework.data.core.dialect;

import org.jspecify.annotations.NonNull;

/**
 * H2 数据库方言。
 */
public class H2Dialect extends AbstractDialect {

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return DatabaseType.H2;
    }

    @Override
    public @NonNull String getIdentifierQuote() {
        return "\"";
    }

    @Override
    public @NonNull String getPaginationSql(@NonNull String sql, long offset, long limit) {
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public @NonNull String getAutoIncrementSyntax() {
        return "AUTO_INCREMENT";
    }


    @Override
    public @NonNull String getSequenceNextValueSql(@NonNull String sequenceName) {
        return "NEXT VALUE FOR " + quoteIdentifier(sequenceName);
    }
}
