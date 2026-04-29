package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.sql.SqlDeleteBuilder;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.dialect.MySQLDialect;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL 删除构建器实现
 */
public class SqlDeleteBuilderImpl implements SqlDeleteBuilder {

    private final Dialect dialect;
    private String tableName;
    private Condition whereCondition;

    public SqlDeleteBuilderImpl() {
        this.dialect = new MySQLDialect();
    }

    public SqlDeleteBuilderImpl(Dialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public @NonNull SqlDeleteBuilder from(@NonNull String table) {
        this.tableName = table;
        return this;
    }

    @Override
    public @NonNull SqlDeleteBuilder where(@NonNull Condition condition) {
        this.whereCondition = condition;
        return this;
    }

    @Override
    public @NonNull String toSql() {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(dialect.quoteIdentifier(tableName));

        if (whereCondition != null && !whereCondition.isEmpty()) {
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(whereCondition);
            sql.append(" WHERE ").append(result.sql());
        }

        return sql.toString();
    }

    @Override
    public @NonNull List<Object> getParameters() {
        if (whereCondition != null && !whereCondition.isEmpty()) {
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            return new ArrayList<>(converter.convert(whereCondition).parameters());
        }
        return List.of();
    }

    @Override
    public int execute() {
        throw new UnsupportedOperationException("Use DataManager to execute delete");
    }
}