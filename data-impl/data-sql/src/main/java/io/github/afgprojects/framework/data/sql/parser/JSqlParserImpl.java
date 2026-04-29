package io.github.afgprojects.framework.data.sql.parser;

import io.github.afgprojects.framework.data.core.dialect.DatabaseType;
import io.github.afgprojects.framework.data.core.sql.SqlParser;
import io.github.afgprojects.framework.data.core.sql.SqlRewriteContext;
import io.github.afgprojects.framework.data.core.sql.SqlStatement;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.jspecify.annotations.NonNull;

/**
 * 基于 JSqlParser 的 SQL 解析器实现
 */
public class JSqlParserImpl implements SqlParser {

    private final DatabaseType databaseType;

    public JSqlParserImpl(DatabaseType databaseType) {
        this.databaseType = databaseType;
    }

    public JSqlParserImpl() {
        this(DatabaseType.MYSQL);
    }

    @Override
    public @NonNull SqlStatement parse(@NonNull String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            return new JSqlParserStatement(statement, databaseType);
        } catch (JSQLParserException e) {
            throw new IllegalArgumentException("SQL 解析失败: " + sql, e);
        }
    }

    @Override
    public @NonNull String rewrite(@NonNull String sql, @NonNull SqlRewriteContext context) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            SqlRewriter rewriter = new SqlRewriter(statement, context);
            return rewriter.rewrite();
        } catch (JSQLParserException e) {
            throw new IllegalArgumentException("SQL 解析失败: " + sql, e);
        }
    }

    @Override
    public boolean validate(@NonNull String sql) {
        try {
            CCJSqlParserUtil.parse(sql);
            return true;
        } catch (JSQLParserException e) {
            return false;
        }
    }
}
