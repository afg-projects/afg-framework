package io.github.afgprojects.framework.data.sql.parser;

import io.github.afgprojects.framework.data.core.dialect.DatabaseType;
import io.github.afgprojects.framework.data.core.sql.SqlStatement;
import io.github.afgprojects.framework.data.sql.util.ColumnFinder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * JSqlParser SQL 语句包装类
 */
public class JSqlParserStatement implements SqlStatement {

    private final Statement statement;
    private final DatabaseType databaseType;

    public JSqlParserStatement(Statement statement, DatabaseType databaseType) {
        this.statement = statement;
        this.databaseType = databaseType;
    }

    @Override
    public @NonNull SqlType getType() {
        if (statement instanceof Select) return SqlType.SELECT;
        if (statement instanceof Insert) return SqlType.INSERT;
        if (statement instanceof Update) return SqlType.UPDATE;
        if (statement instanceof Delete) return SqlType.DELETE;
        return SqlType.OTHER;
    }

    @Override
    public @NonNull List<String> getTables() {
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        Set<String> tableSet = tablesNamesFinder.getTables(statement);
        return new ArrayList<>(tableSet);
    }

    @Override
    public @NonNull List<String> getColumns() {
        List<String> columns = new ArrayList<>();
        // 使用 ColumnFinder 提取列名
        statement.accept(new ColumnFinder(columns));
        return columns;
    }

    @Override
    public @NonNull String getWhereClause() {
        Expression where = extractWhereExpression();
        return where != null ? where.toString() : "";
    }

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return databaseType;
    }

    @Override
    public @NonNull String toSql() {
        return statement.toString();
    }

    public Statement getRawStatement() {
        return statement;
    }

    /**
     * 提取 WHERE 表达式
     */
    private Expression extractWhereExpression() {
        if (statement instanceof Select select) {
            PlainSelect plainSelect = select.getPlainSelect();
            return plainSelect != null ? plainSelect.getWhere() : null;
        } else if (statement instanceof Update update) {
            return update.getWhere();
        } else if (statement instanceof Delete delete) {
            return delete.getWhere();
        }
        return null;
    }
}
