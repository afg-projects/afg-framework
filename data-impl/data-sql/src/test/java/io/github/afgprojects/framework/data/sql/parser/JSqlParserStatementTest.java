package io.github.afgprojects.framework.data.sql.parser;

import io.github.afgprojects.framework.data.core.dialect.DatabaseType;
import io.github.afgprojects.framework.data.core.sql.SqlStatement;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JSqlParserStatement 测试
 */
@DisplayName("JSqlParserStatement Tests")
class JSqlParserStatementTest {

    @Nested
    @DisplayName("getType() Tests")
    class GetTypeTests {

        @Test
        @DisplayName("getType() should return SELECT for select statement")
        void testGetTypeSelect() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id, name FROM users WHERE status = 1");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getType()).isEqualTo(SqlStatement.SqlType.SELECT);
        }

        @Test
        @DisplayName("getType() should return INSERT for insert statement")
        void testGetTypeInsert() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("INSERT INTO users (id, name) VALUES (1, 'test')");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getType()).isEqualTo(SqlStatement.SqlType.INSERT);
        }

        @Test
        @DisplayName("getType() should return UPDATE for update statement")
        void testGetTypeUpdate() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("UPDATE users SET name = 'new' WHERE id = 1");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getType()).isEqualTo(SqlStatement.SqlType.UPDATE);
        }

        @Test
        @DisplayName("getType() should return DELETE for delete statement")
        void testGetTypeDelete() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("DELETE FROM users WHERE id = 1");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getType()).isEqualTo(SqlStatement.SqlType.DELETE);
        }

        @Test
        @DisplayName("getType() should return OTHER for other statements")
        void testGetTypeOther() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("CREATE TABLE test (id INT)");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getType()).isEqualTo(SqlStatement.SqlType.OTHER);
        }
    }

    @Nested
    @DisplayName("getTables() Tests")
    class GetTablesTests {

        @Test
        @DisplayName("getTables() should extract all table names")
        void testGetTables() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT u.id, o.order_no FROM users u JOIN orders o ON u.id = o.user_id");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getTables()).contains("users", "orders");
        }
    }

    @Nested
    @DisplayName("getColumns() Tests")
    class GetColumnsTests {

        @Test
        @DisplayName("getColumns() should extract all column names")
        void testGetColumns() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id, name, status FROM users WHERE dept_id = 1");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getColumns()).contains("id", "name", "status", "dept_id");
        }
    }

    @Nested
    @DisplayName("getWhereClause() Tests")
    class GetWhereClauseTests {

        @Test
        @DisplayName("getWhereClause() should return WHERE expression for SELECT")
        void testGetWhereClauseSelect() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users WHERE status = 1");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getWhereClause()).contains("status = 1");
        }

        @Test
        @DisplayName("getWhereClause() should return WHERE expression for UPDATE")
        void testGetWhereClauseUpdate() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("UPDATE users SET name = 'test' WHERE id = 1");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getWhereClause()).contains("id = 1");
        }

        @Test
        @DisplayName("getWhereClause() should return WHERE expression for DELETE")
        void testGetWhereClauseDelete() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("DELETE FROM users WHERE id = 1");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getWhereClause()).contains("id = 1");
        }

        @Test
        @DisplayName("getWhereClause() should return empty string when no WHERE")
        void testGetWhereClauseEmpty() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getWhereClause()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Other Methods Tests")
    class OtherMethodsTests {

        @Test
        @DisplayName("toSql() should return SQL string")
        void testToSql() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.toSql()).contains("SELECT");
        }

        @Test
        @DisplayName("getDatabaseType() should return configured type")
        void testGetDatabaseType() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.POSTGRESQL);

            assertThat(sqlStmt.getDatabaseType()).isEqualTo(DatabaseType.POSTGRESQL);
        }

        @Test
        @DisplayName("getRawStatement() should return underlying statement")
        void testGetRawStatement() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getRawStatement()).isSameAs(stmt);
        }
    }
}
