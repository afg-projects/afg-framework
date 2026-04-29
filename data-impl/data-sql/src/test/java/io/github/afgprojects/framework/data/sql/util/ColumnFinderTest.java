package io.github.afgprojects.framework.data.sql.util;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ColumnFinder 测试
 */
@DisplayName("ColumnFinder Tests")
class ColumnFinderTest {

    private List<String> columns;
    private ColumnFinder finder;

    @BeforeEach
    void setUp() {
        columns = new ArrayList<>();
        finder = new ColumnFinder(columns);
    }

    @Nested
    @DisplayName("SELECT Statement Tests")
    class SelectStatementTests {

        @Test
        @DisplayName("Extract columns from simple SELECT")
        void testExtractColumnsFromSimpleSelect() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id, name, status FROM users");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "name", "status");
        }

        @Test
        @DisplayName("Extract columns from SELECT with WHERE clause")
        void testExtractColumnsFromSelectWithWhere() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id, name FROM users WHERE status = 1 AND deleted = false");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "name", "status", "deleted");
        }

        @Test
        @DisplayName("Extract columns from SELECT with JOIN")
        void testExtractColumnsFromSelectWithJoin() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT u.id, u.name, o.order_no FROM users u JOIN orders o ON u.id = o.user_id"
            );
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "name", "order_no", "user_id");
        }

        @Test
        @DisplayName("Extract columns from SELECT with GROUP BY")
        void testExtractColumnsFromSelectWithGroupBy() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT department, COUNT(*) FROM employees GROUP BY department"
            );
            stmt.accept(finder);

            assertThat(columns).contains("department");
        }

        @Test
        @DisplayName("Extract columns from SELECT with HAVING")
        void testExtractColumnsFromSelectWithHaving() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT department, COUNT(*) as cnt FROM employees GROUP BY department HAVING COUNT(*) > 5"
            );
            stmt.accept(finder);

            assertThat(columns).contains("department");
        }

        @Test
        @DisplayName("Extract columns from SELECT with ORDER BY")
        void testExtractColumnsFromSelectWithOrderBy() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id, name FROM users ORDER BY created_at DESC");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "name", "created_at");
        }

        @Test
        @DisplayName("Extract columns from SELECT with subquery")
        void testExtractColumnsFromSelectWithSubquery() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT id, name FROM users WHERE department_id IN (SELECT id FROM departments WHERE active = true)"
            );
            stmt.accept(finder);

            // 子查询中的列不会被提取到外层查询的列列表中
            assertThat(columns).containsExactlyInAnyOrder("id", "name", "department_id");
        }
    }

    @Nested
    @DisplayName("UPDATE Statement Tests")
    class UpdateStatementTests {

        @Test
        @DisplayName("Extract columns from UPDATE statement")
        void testExtractColumnsFromUpdate() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("UPDATE users SET name = 'test', status = 1 WHERE id = 1");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("name", "status", "id");
        }

        @Test
        @DisplayName("Extract columns from UPDATE with WHERE")
        void testExtractColumnsFromUpdateWithWhere() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "UPDATE users SET name = 'test', updated_at = NOW() WHERE id = 1 AND status = 0"
            );
            stmt.accept(finder);

            assertThat(columns).contains("name", "updated_at", "id", "status");
        }
    }

    @Nested
    @DisplayName("UNION/Set Operation Tests")
    class SetOperationTests {

        @Test
        @DisplayName("Extract columns from UNION query")
        void testExtractColumnsFromUnion() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT id, name FROM users UNION SELECT id, name FROM admins"
            );
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "name");
        }

        @Test
        @DisplayName("Extract columns from nested SetOperationList")
        void testExtractColumnsFromNestedSetOperation() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT id FROM users UNION SELECT id FROM admins UNION SELECT id FROM guests"
            );
            stmt.accept(finder);

            assertThat(columns).contains("id");
        }
    }

    @Nested
    @DisplayName("Expression Type Tests")
    class ExpressionTypeTests {

        @Test
        @DisplayName("Extract columns from function expressions")
        void testExtractColumnsFromFunctionExpression() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT UPPER(name), COUNT(id) FROM users");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("name", "id");
        }

        @Test
        @DisplayName("Extract columns from AND/OR expressions")
        void testExtractColumnsFromLogicalExpressions() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT id FROM users WHERE (status = 1 OR status = 2) AND deleted = false"
            );
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "status", "deleted");
        }

        @Test
        @DisplayName("Extract columns from comparison expressions")
        void testExtractColumnsFromComparisonExpressions() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users WHERE age > 18 AND salary >= 5000");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "age", "salary");
        }

        @Test
        @DisplayName("Extract columns from IN expression")
        void testExtractColumnsFromInExpression() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users WHERE status IN (1, 2, 3)");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "status");
        }

        @Test
        @DisplayName("Extract columns from BETWEEN expression")
        void testExtractColumnsFromBetweenExpression() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users WHERE age BETWEEN 18 AND 65");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "age");
        }

        @Test
        @DisplayName("Extract columns from LIKE expression")
        void testExtractColumnsFromLikeExpression() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users WHERE name LIKE '%test%'");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "name");
        }

        @Test
        @DisplayName("Extract columns from IS NULL expression")
        void testExtractColumnsFromIsNullExpression() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users WHERE deleted_at IS NULL");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "deleted_at");
        }

        @Test
        @DisplayName("Extract columns from arithmetic expressions")
        void testExtractColumnsFromArithmeticExpressions() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id, price * quantity as total FROM orders WHERE price + tax > 100");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "price", "quantity", "tax");
        }

        @Test
        @DisplayName("Extract columns from CASE expression")
        void testExtractColumnsFromCaseExpression() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT id, CASE WHEN status = 1 THEN 'active' ELSE 'inactive' END as status_text FROM users"
            );
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "status");
        }

        @Test
        @DisplayName("Extract columns from CASE expression with ELSE")
        void testExtractColumnsFromCaseExpressionWithElse() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse(
                "SELECT id, CASE status WHEN 1 THEN 'active' WHEN 0 THEN 'inactive' ELSE 'unknown' END as text FROM users"
            );
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "status");
        }

        @Test
        @DisplayName("Extract columns from NOT EQUALS expression")
        void testExtractColumnsFromNotEqualsExpression() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users WHERE status != 0");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "status");
        }

        @Test
        @DisplayName("Extract columns from LESS THAN expression")
        void testExtractColumnsFromLessThanExpression() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users WHERE age < 18");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "age");
        }

        @Test
        @DisplayName("Extract columns from LESS THAN EQUALS expression")
        void testExtractColumnsFromLessThanEqualsExpression() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users WHERE age <= 65");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "age");
        }

        @Test
        @DisplayName("Extract columns from GREATER THAN EQUALS expression")
        void testExtractColumnsFromGreaterThanEqualsExpression() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users WHERE age >= 18");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "age");
        }

        @Test
        @DisplayName("Extract columns from subtraction expression")
        void testExtractColumnsFromSubtractionExpression() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id, price - discount as final_price FROM products");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "price", "discount");
        }

        @Test
        @DisplayName("Extract columns from division expression")
        void testExtractColumnsFromDivisionExpression() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id, total / count as average FROM stats");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "total", "count");
        }

        @Test
        @DisplayName("Extract columns from parenthesized expression")
        void testExtractColumnsFromParenthesizedExpression() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id FROM users WHERE (status, type) IN ((1, 'A'), (2, 'B'))");
            stmt.accept(finder);

            assertThat(columns).contains("id", "status", "type");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Handle duplicate column names")
        void testDuplicateColumnNames() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT id, id, name FROM users WHERE id = 1");
            stmt.accept(finder);

            assertThat(columns).containsExactlyInAnyOrder("id", "name");
            assertThat(columns).hasSize(2);
        }

        @Test
        @DisplayName("Handle empty column list")
        void testEmptyColumnList() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT 1");
            stmt.accept(finder);

            assertThat(columns).isEmpty();
        }

        @Test
        @DisplayName("Handle SELECT *")
        void testSelectStar() throws Exception {
            Statement stmt = CCJSqlParserUtil.parse("SELECT * FROM users");
            stmt.accept(finder);

            assertThat(columns).isEmpty();
        }
    }
}
