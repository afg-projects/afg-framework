package io.github.afgprojects.framework.data.sql.builder;

import io.github.afgprojects.framework.data.core.sql.SqlInsertBuilder;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.dialect.MySQLDialect;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL 插入构建器实现
 */
public class SqlInsertBuilderImpl implements SqlInsertBuilder {

    private final Dialect dialect;
    private String tableName;
    private List<String> columnNames = new ArrayList<>();
    private final List<List<Object>> rows = new ArrayList<>();

    public SqlInsertBuilderImpl() {
        this.dialect = new MySQLDialect();
    }

    public SqlInsertBuilderImpl(Dialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public @NonNull SqlInsertBuilder into(@NonNull String table) {
        this.tableName = table;
        return this;
    }

    @Override
    public @NonNull SqlInsertBuilder columns(@NonNull String... columns) {
        this.columnNames = new ArrayList<>(List.of(columns));
        return this;
    }

    @Override
    public @NonNull SqlInsertBuilder values(@NonNull Object... values) {
        rows.add(new ArrayList<>(List.of(values)));
        return this;
    }

    @Override
    public @NonNull SqlInsertBuilder row(@NonNull Object... values) {
        rows.add(new ArrayList<>(List.of(values)));
        return this;
    }

    @Override
    public @NonNull String toSql() {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(dialect.quoteIdentifier(tableName));

        if (!columnNames.isEmpty()) {
            List<String> quotedColumns = new ArrayList<>();
            for (String column : columnNames) {
                quotedColumns.add(dialect.quoteIdentifier(column));
            }
            sql.append(" (").append(String.join(", ", quotedColumns)).append(")");
        }

        sql.append(" VALUES ");

        List<String> valueParts = new ArrayList<>();
        for (List<Object> row : rows) {
            List<String> placeholders = new ArrayList<>();
            for (int i = 0; i < row.size(); i++) {
                placeholders.add("?");
            }
            valueParts.add("(" + String.join(", ", placeholders) + ")");
        }
        sql.append(String.join(", ", valueParts));

        return sql.toString();
    }

    @Override
    public @NonNull List<Object> getParameters() {
        List<Object> params = new ArrayList<>();
        for (List<Object> row : rows) {
            params.addAll(row);
        }
        return params;
    }

    @Override
    public int execute() {
        throw new UnsupportedOperationException("Use DataManager to execute insert");
    }

    @Override
    public long executeAndReturnKey() {
        throw new UnsupportedOperationException("Use DataManager to execute insert");
    }
}
