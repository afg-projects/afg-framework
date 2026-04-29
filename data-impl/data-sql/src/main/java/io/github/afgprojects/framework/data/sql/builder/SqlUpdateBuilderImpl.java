package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.condition.SFunction;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.sql.SqlUpdateBuilder;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.dialect.MySQLDialect;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL 更新构建器实现
 */
public class SqlUpdateBuilderImpl implements SqlUpdateBuilder {

    private final Dialect dialect;
    private String tableName;
    private final Map<String, Object> setValues = new LinkedHashMap<>();
    private Condition whereCondition;

    public SqlUpdateBuilderImpl() {
        this.dialect = new MySQLDialect();
    }

    public SqlUpdateBuilderImpl(Dialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public @NonNull SqlUpdateBuilder table(@NonNull String table) {
        this.tableName = table;
        return this;
    }

    @Override
    public @NonNull SqlUpdateBuilder set(@NonNull String column, Object value) {
        setValues.put(column, value);
        return this;
    }

    @Override
    public @NonNull SqlUpdateBuilder set(@NonNull Map<String, Object> values) {
        setValues.putAll(values);
        return this;
    }

    @Override
    public @NonNull SqlUpdateBuilder set(@NonNull SFunction<?, ?> getter, Object value) {
        String fieldName = Conditions.getFieldName(getter);
        setValues.put(fieldName, value);
        return this;
    }

    @Override
    public @NonNull SqlUpdateBuilder where(@NonNull Condition condition) {
        this.whereCondition = condition;
        return this;
    }

    @Override
    public @NonNull String toSql() {
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(dialect.quoteIdentifier(tableName));
        sql.append(" SET ");

        List<String> setParts = new ArrayList<>();
        for (String column : setValues.keySet()) {
            setParts.add(dialect.quoteIdentifier(column) + " = ?");
        }
        sql.append(String.join(", ", setParts));

        if (whereCondition != null && !whereCondition.isEmpty()) {
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(whereCondition);
            sql.append(" WHERE ").append(result.sql());
        }

        return sql.toString();
    }

    @Override
    public @NonNull List<Object> getParameters() {
        List<Object> params = new ArrayList<>(setValues.values());

        if (whereCondition != null && !whereCondition.isEmpty()) {
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            params.addAll(converter.convert(whereCondition).parameters());
        }

        return params;
    }

    @Override
    public int execute() {
        throw new UnsupportedOperationException("Use DataManager to execute update");
    }
}