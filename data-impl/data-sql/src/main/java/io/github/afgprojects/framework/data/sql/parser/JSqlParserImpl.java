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

    /**
     * SQL 截断长度限制，用于异常消息中避免泄露过长的 SQL
     */
    private static final int SQL_TRUNCATE_LENGTH = 100;

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
            throw new IllegalArgumentException("SQL 解析失败: " + truncateSql(sql), e);
        }
    }

    @Override
    public @NonNull String rewrite(@NonNull String sql, @NonNull SqlRewriteContext context) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            SqlRewriter rewriter = new SqlRewriter(statement, context);
            return rewriter.rewrite();
        } catch (JSQLParserException e) {
            throw new IllegalArgumentException("SQL 解析失败: " + truncateSql(sql), e);
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

    /**
     * 截断 SQL 字符串，用于异常消息中避免泄露敏感信息
     *
     * @param sql 原始 SQL
     * @return 截断后的 SQL，附带省略号表示被截断
     */
    private String truncateSql(String sql) {
        if (sql == null) {
            return "null";
        }
        if (sql.length() <= SQL_TRUNCATE_LENGTH) {
            return sql;
        }
        return sql.substring(0, SQL_TRUNCATE_LENGTH) + "... (truncated, total " + sql.length() + " chars)";
    }
}
