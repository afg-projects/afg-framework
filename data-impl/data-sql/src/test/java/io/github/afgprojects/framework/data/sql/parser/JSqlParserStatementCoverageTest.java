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
 * JSqlParserStatement additional coverage tests
 * <p>
 * Tests for edge cases to improve code coverage
 */
@DisplayName("JSqlParserStatement Coverage Tests")
class JSqlParserStatementCoverageTest {

    @Nested
    @DisplayName("getWhereClause() Edge Cases")
    class GetWhereClauseEdgeCases {

        @Test
        @DisplayName("getWhereClause() should return empty string for INSERT statement")
        void testGetWhereClauseForInsert() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("INSERT INTO users (id, name) VALUES (1, 'test')");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getWhereClause()).isEmpty();
        }

        @Test
        @DisplayName("getWhereClause() should return empty string for CREATE statement")
        void testGetWhereClauseForCreate() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("CREATE TABLE test (id INT PRIMARY KEY)");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getWhereClause()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getTables() Edge Cases")
    class GetTablesEdgeCases {

        @Test
        @DisplayName("getTables() should extract table from INSERT statement")
        void testGetTablesForInsert() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("INSERT INTO products (id, name) VALUES (1, 'item')");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getTables()).contains("products");
        }

        @Test
        @DisplayName("getTables() should extract table from DELETE statement")
        void testGetTablesForDelete() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("DELETE FROM temp_data WHERE expired = 1");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getTables()).contains("temp_data");
        }

        @Test
        @DisplayName("getTables() should extract tables from subquery")
        void testGetTablesWithSubquery() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT * FROM (SELECT id FROM users) AS subq"
            );
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getTables()).contains("users");
        }
    }

    @Nested
    @DisplayName("getColumns() Edge Cases")
    class GetColumnsEdgeCases {

        @Test
        @DisplayName("getColumns() should extract columns from UPDATE SET clause")
        void testGetColumnsForUpdate() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "UPDATE employees SET salary = 50000, department = 'IT' WHERE id = 1"
            );
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getColumns()).contains("salary", "department", "id");
        }

        @Test
        @DisplayName("getColumns() should extract columns with ORDER BY")
        void testGetColumnsWithOrderBy() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT id, name FROM users ORDER BY created_at DESC, name ASC"
            );
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getColumns()).contains("id", "name", "created_at");
        }

        @Test
        @DisplayName("getColumns() should extract columns with GROUP BY and HAVING")
        void testGetColumnsWithGroupByHaving() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT department, COUNT(*) as cnt FROM employees GROUP BY department HAVING COUNT(*) > 5"
            );
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getColumns()).contains("department");
        }

        @Test
        @DisplayName("getColumns() should extract columns with JOIN ON clause")
        void testGetColumnsWithJoin() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT u.name, o.order_no FROM users u JOIN orders o ON u.id = o.user_id"
            );
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getColumns()).contains("name", "order_no", "id", "user_id");
        }

        @Test
        @DisplayName("getColumns() should extract columns from CASE expression")
        void testGetColumnsWithCaseExpression() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT id, CASE WHEN status = 1 THEN 'active' ELSE 'inactive' END as status_text FROM users"
            );
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getColumns()).contains("id", "status");
        }

        @Test
        @DisplayName("getColumns() should extract columns with arithmetic operations")
        void testGetColumnsWithArithmetic() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT id, price * quantity as total FROM orders WHERE discount > 0"
            );
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getColumns()).contains("id", "price", "quantity", "discount");
        }

        @Test
        @DisplayName("getColumns() should extract columns with IN clause")
        void testGetColumnsWithInClause() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT id, name FROM users WHERE department_id IN (1, 2, 3)"
            );
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getColumns()).contains("id", "name", "department_id");
        }

        @Test
        @DisplayName("getColumns() should extract columns with BETWEEN clause")
        void testGetColumnsWithBetween() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT id FROM orders WHERE created_at BETWEEN '2024-01-01' AND '2024-12-31'"
            );
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getColumns()).contains("id", "created_at");
        }

        @Test
        @DisplayName("getColumns() should extract columns with LIKE clause")
        void testGetColumnsWithLike() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT id, name FROM users WHERE email LIKE '%@example.com'"
            );
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getColumns()).contains("id", "name", "email");
        }

        @Test
        @DisplayName("getColumns() should extract columns with IS NULL clause")
        void testGetColumnsWithIsNull() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT id FROM users WHERE deleted_at IS NULL"
            );
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getColumns()).contains("id", "deleted_at");
        }
    }

    @Nested
    @DisplayName("toSql() Tests")
    class ToSqlTests {

        @Test
        @DisplayName("toSql() should return normalized SQL string")
        void testToSqlNormalized() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT   id,   name   FROM    users");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            String sql = sqlStmt.toSql();
            assertThat(sql).contains("SELECT").contains("id").contains("name").contains("FROM").contains("users");
        }

        @Test
        @DisplayName("toSql() should handle complex query")
        void testToSqlComplex() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT u.id, u.name, COUNT(o.id) as order_count " +
                "FROM users u LEFT JOIN orders o ON u.id = o.user_id " +
                "WHERE u.status = 'active' GROUP BY u.id, u.name HAVING COUNT(o.id) > 0"
            );
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            String sql = sqlStmt.toSql();
            assertThat(sql).isNotEmpty();
            assertThat(sql).contains("SELECT").contains("users").contains("orders");
        }
    }

    @Nested
    @DisplayName("DatabaseType Tests")
    class DatabaseTypeTests {

        @Test
        @DisplayName("getDatabaseType() should return H2")
        void testGetDatabaseTypeH2() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.H2);

            assertThat(sqlStmt.getDatabaseType()).isEqualTo(DatabaseType.H2);
        }

        @Test
        @DisplayName("getDatabaseType() should return POSTGRESQL")
        void testGetDatabaseTypePostgreSQL() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.POSTGRESQL);

            assertThat(sqlStmt.getDatabaseType()).isEqualTo(DatabaseType.POSTGRESQL);
        }

        @Test
        @DisplayName("getDatabaseType() should return ORACLE")
        void testGetDatabaseTypeOracle() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.ORACLE);

            assertThat(sqlStmt.getDatabaseType()).isEqualTo(DatabaseType.ORACLE);
        }

        @Test
        @DisplayName("getDatabaseType() should return SQLSERVER")
        void testGetDatabaseTypeSqlServer() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.SQLSERVER);

            assertThat(sqlStmt.getDatabaseType()).isEqualTo(DatabaseType.SQLSERVER);
        }
    }

    @Nested
    @DisplayName("getRawStatement() Tests")
    class GetRawStatementTests {

        @Test
        @DisplayName("getRawStatement() should return the original Statement object")
        void testGetRawStatement() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users WHERE status = 1");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getRawStatement()).isSameAs(stmt);
        }

        @Test
        @DisplayName("getRawStatement() should allow downcasting for advanced operations")
        void testGetRawStatementDowncast() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getRawStatement()).isInstanceOf(net.sf.jsqlparser.statement.select.Select.class);
        }
    }

    @Nested
    @DisplayName("getType() Additional Tests")
    class GetTypeAdditionalTests {

        @Test
        @DisplayName("getType() should return OTHER for DROP statement")
        void testGetTypeDrop() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("DROP TABLE IF EXISTS temp_table");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getType()).isEqualTo(SqlStatement.SqlType.OTHER);
        }

        @Test
        @DisplayName("getType() should return OTHER for ALTER statement")
        void testGetTypeAlter() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("ALTER TABLE users ADD COLUMN email VARCHAR(255)");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getType()).isEqualTo(SqlStatement.SqlType.OTHER);
        }

        @Test
        @DisplayName("getType() should return OTHER for TRUNCATE statement")
        void testGetTypeTruncate() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("TRUNCATE TABLE logs");
            JSqlParserStatement sqlStmt = new JSqlParserStatement(stmt, DatabaseType.MYSQL);

            assertThat(sqlStmt.getType()).isEqualTo(SqlStatement.SqlType.OTHER);
        }
    }
}
