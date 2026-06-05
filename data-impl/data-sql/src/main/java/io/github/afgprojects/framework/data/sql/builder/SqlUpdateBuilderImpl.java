package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.condition.SFunction;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.sql.SqlUpdateBuilder;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.dialect.MySQLDialect;
import io.github.afgprojects.framework.data.core.security.SqlIdentifierValidator;
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
    private final ConditionToSqlConverter conditionConverter;

    public SqlUpdateBuilderImpl() {
        this.dialect = new MySQLDialect();
        this.conditionConverter = new ConditionToSqlConverter(dialect);
    }

    public SqlUpdateBuilderImpl(Dialect dialect) {
        this.dialect = dialect;
        this.conditionConverter = new ConditionToSqlConverter(dialect);
    }

    @Override
    public @NonNull SqlUpdateBuilder table(@NonNull String table) {
        validateIdentifier(table, "table name");
        this.tableName = table;
        return this;
    }

    @Override
    public @NonNull SqlUpdateBuilder set(@NonNull String column, Object value) {
        validateIdentifier(column, "column name");
        setValues.put(column, value);
        return this;
    }

    @Override
    public @NonNull SqlUpdateBuilder set(@NonNull Map<String, Object> values) {
        for (String column : values.keySet()) {
            validateIdentifier(column, "column name");
        }
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
            ConditionToSqlConverter.SqlResult result = conditionConverter.convert(whereCondition);
            sql.append(" WHERE ").append(result.sql());
        }

        return sql.toString();
    }

    @Override
    public @NonNull List<Object> getParameters() {
        List<Object> params = new ArrayList<>(setValues.values());

        if (whereCondition != null && !whereCondition.isEmpty()) {
            params.addAll(conditionConverter.convert(whereCondition).parameters());
        }

        return params;
    }

    @Override
    public int execute() {
        throw new UnsupportedOperationException("Use DataManager to execute update");
    }

    /**
     * 验证标识符合法性，防止 SQL 注入
     *
     * @param identifier 标识符
     * @param type       标识符类型描述（用于错误消息）
     * @throws IllegalArgumentException 如果标识符非法
     */
    private void validateIdentifier(String identifier, String type) {
        SqlIdentifierValidator.validateSimpleIdentifier(identifier, type);
    }
}