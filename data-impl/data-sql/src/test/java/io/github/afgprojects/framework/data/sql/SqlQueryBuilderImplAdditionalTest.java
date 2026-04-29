package io.github.afgprojects.framework.data.sql;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.dialect.H2Dialect;
import io.github.afgprojects.framework.data.core.page.PageRequest;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.data.core.sql.SqlQueryBuilder;
import io.github.afgprojects.framework.data.sql.builder.SqlQueryBuilderImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SqlQueryBuilderImpl 补充测试
 * 重点覆盖分支覆盖率不足的部分
 */
@DisplayName("SqlQueryBuilderImpl Additional Tests")
class SqlQueryBuilderImplAdditionalTest {

    @Nested
    @DisplayName("Select with SFunction Tests")
    class SelectSFunctionTests {

        @Test
        @DisplayName("使用 SFunction 选择列")
        void testSelectWithSFunction() {
            SqlQueryBuilder builder = new SqlQueryBuilderImpl()
                    .select(Conditions.getFieldName(TestEntity::getId), Conditions.getFieldName(TestEntity::getName))
                    .from("users");

            String sql = builder.toSql();
            assertThat(sql).contains("SELECT");
            assertThat(sql).contains("FROM");
        }
    }

    @Nested
    @DisplayName("Join Type Tests")
    class JoinTypeTests {

        @Test
        @DisplayName("RIGHT JOIN 查询")
        void testRightJoin() {
            Condition joinCondition = Conditions.eq("u.id", "o.user_id");
            String sql = new SqlQueryBuilderImpl()
                    .select("u.id", "o.order_no")
                    .from("users", "u")
                    .rightJoin("orders", joinCondition)
                    .toSql();

            assertThat(sql).contains("RIGHT JOIN");
            assertThat(sql).contains("ON");
        }

        @Test
        @DisplayName("INNER JOIN 查询")
        void testInnerJoin() {
            Condition joinCondition = Conditions.eq("u.id", "o.user_id");
            String sql = new SqlQueryBuilderImpl()
                    .select("u.id", "o.order_no")
                    .from("users", "u")
                    .innerJoin("orders", joinCondition)
                    .toSql();

            assertThat(sql).contains("INNER JOIN");
            assertThat(sql).contains("ON");
        }

        @Test
        @DisplayName("LEFT JOIN 查询")
        void testLeftJoin() {
            Condition joinCondition = Conditions.eq("u.id", "o.user_id");
            String sql = new SqlQueryBuilderImpl()
                    .select("u.id", "o.order_no")
                    .from("users", "u")
                    .leftJoin("orders", "o", joinCondition)
                    .toSql();

            assertThat(sql).contains("LEFT JOIN");
            assertThat(sql).contains("ON");
        }

        @Test
        @DisplayName("JOIN without alias")
        void testJoinWithoutAlias() {
            Condition joinCondition = Conditions.eq("users.id", "orders.user_id");
            String sql = new SqlQueryBuilderImpl()
                    .select("*")
                    .from("users")
                    .join("orders", joinCondition)
                    .toSql();

            assertThat(sql).contains("JOIN");
        }

        @Test
        @DisplayName("LEFT JOIN without alias")
        void testLeftJoinWithoutAlias() {
            Condition joinCondition = Conditions.eq("users.id", "orders.user_id");
            String sql = new SqlQueryBuilderImpl()
                    .select("*")
                    .from("users")
                    .leftJoin("orders", joinCondition)
                    .toSql();

            assertThat(sql).contains("LEFT JOIN");
        }
    }

    @Nested
    @DisplayName("And/Or Condition Tests")
    class AndOrConditionTests {

        @Test
        @DisplayName("and() 方法 - 无前置条件")
        void testAndWithoutExistingCondition() {
            String sql = new SqlQueryBuilderImpl()
                    .select("*")
                    .from("users")
                    .and(Conditions.eq("status", 1))
                    .toSql();

            assertThat(sql).contains("WHERE status = ?");
        }

        @Test
        @DisplayName("and() 方法 - 有前置条件")
        void testAndWithExistingCondition() {
            String sql = new SqlQueryBuilderImpl()
                    .select("*")
                    .from("users")
                    .where(Conditions.eq("name", "test"))
                    .and(Conditions.eq("status", 1))
                    .toSql();

            assertThat(sql).contains("WHERE");
            assertThat(sql).contains("AND");
        }

        @Test
        @DisplayName("or() 方法 - 无前置条件")
        void testOrWithoutExistingCondition() {
            String sql = new SqlQueryBuilderImpl()
                    .select("*")
                    .from("users")
                    .or(Conditions.eq("status", 1))
                    .toSql();

            assertThat(sql).contains("WHERE status = ?");
        }

        @Test
        @DisplayName("or() 方法 - 有前置条件")
        void testOrWithExistingCondition() {
            String sql = new SqlQueryBuilderImpl()
                    .select("*")
                    .from("users")
                    .where(Conditions.eq("status", 1))
                    .or(Conditions.eq("status", 2))
                    .toSql();

            assertThat(sql).contains("WHERE");
            assertThat(sql).contains("OR");
        }
    }

    @Nested
    @DisplayName("Identifier Validation Tests")
    class IdentifierValidationTests {

        @Test
        @DisplayName("无效标识符应抛出异常")
        void testInvalidIdentifier() {
            assertThatThrownBy(() -> new SqlQueryBuilderImpl()
                    .rowNumberOver("category", "score; DROP TABLE users"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid identifier");
        }
    }

    @Nested
    @DisplayName("Default Value Format Tests")
    class DefaultValueFormatTests {

        @Test
        @DisplayName("LEAD with null default value")
        void testLeadWithNullDefault() {
            String sql = new SqlQueryBuilderImpl()
                    .select("id")
                    .lead("status", 1, null)
                    .from("orders")
                    .toSql();

            assertThat(sql).contains("LEAD(`status`, 1, NULL)");
        }

        @Test
        @DisplayName("LAG with null default value")
        void testLagWithNullDefault() {
            String sql = new SqlQueryBuilderImpl()
                    .select("id")
                    .lag("status", 1, null)
                    .from("orders")
                    .toSql();

            assertThat(sql).contains("LAG(`status`, 1, NULL)");
        }
    }

    @Nested
    @DisplayName("Dialect Tests")
    class DialectTests {

        @Test
        @DisplayName("使用自定义方言")
        void testCustomDialect() {
            SqlQueryBuilderImpl builder = new SqlQueryBuilderImpl(new H2Dialect());
            String sql = builder.select("*").from("users").toSql();

            assertThat(sql).contains("SELECT * FROM users");
        }
    }

    @Nested
    @DisplayName("Select All Tests")
    class SelectAllTests {

        @Test
        @DisplayName("selectAll() 方法")
        void testSelectAll() {
            String sql = new SqlQueryBuilderImpl()
                    .selectAll()
                    .from("users")
                    .toSql();

            assertThat(sql).isEqualTo("SELECT * FROM `users`");
        }
    }

    @Nested
    @DisplayName("Subquery Tests")
    class SubqueryTests {

        @Test
        @DisplayName("fromSubquery 子查询")
        void testFromSubquery() {
            SqlQueryBuilder subquery = new SqlQueryBuilderImpl()
                    .select("id", "name")
                    .from("users")
                    .where(Conditions.eq("status", 1));

            String sql = new SqlQueryBuilderImpl()
                    .select("*")
                    .fromSubquery(subquery, "active_users")
                    .toSql();

            // fromSubquery 将子查询放在括号中，dialect.quoteIdentifier 会对括号进行引用
            assertThat(sql).contains("SELECT * FROM");
            assertThat(sql).contains("active_users");
            assertThat(sql).contains("id, name");
        }
    }

    @Nested
    @DisplayName("Window Function Edge Cases Tests")
    class WindowFunctionEdgeCasesTests {

        @Test
        @DisplayName("over() without pending window function defaults to ROW_NUMBER()")
        void testOverWithoutPendingWindowFunction() {
            String sql = new SqlQueryBuilderImpl()
                    .select("id", "name")
                    .over()
                    .partitionBy("category")
                    .orderBy("score", Sort.Direction.DESC)
                    .end()
                    .from("products")
                    .toSql();

            assertThat(sql).contains("ROW_NUMBER()");
        }

        @Test
        @DisplayName("窗口函数无 PARTITION BY")
        void testWindowFunctionWithoutPartitionBy() {
            String sql = new SqlQueryBuilderImpl()
                    .select("id", "name")
                    .rowNumber()
                    .over()
                    .orderBy("score", Sort.Direction.DESC)
                    .end()
                    .from("products")
                    .toSql();

            assertThat(sql).contains("ROW_NUMBER() OVER (ORDER BY");
            assertThat(sql).doesNotContain("PARTITION BY");
        }

        @Test
        @DisplayName("窗口函数无 ORDER BY")
        void testWindowFunctionWithoutOrderBy() {
            String sql = new SqlQueryBuilderImpl()
                    .select("id", "name")
                    .rowNumber()
                    .over()
                    .partitionBy("category")
                    .end()
                    .from("products")
                    .toSql();

            assertThat(sql).contains("ROW_NUMBER() OVER (PARTITION BY");
            assertThat(sql).doesNotContain("ORDER BY");
        }

        @Test
        @DisplayName("rankOver with Sort parameter")
        void testRankOverWithSort() {
            String sql = new SqlQueryBuilderImpl()
                    .select("id", "name")
                    .rankOver("department", Sort.by(Sort.Order.desc("salary")))
                    .from("employees")
                    .toSql();

            assertThat(sql).contains("RANK() OVER");
            assertThat(sql).contains("PARTITION BY `department`");
            assertThat(sql).contains("ORDER BY `salary` DESC");
        }

        @Test
        @DisplayName("denseRankOver with Sort parameter")
        void testDenseRankOverWithSort() {
            String sql = new SqlQueryBuilderImpl()
                    .select("id", "name")
                    .denseRankOver("category", Sort.by(Sort.Order.asc("score")))
                    .from("products")
                    .toSql();

            assertThat(sql).contains("DENSE_RANK() OVER");
            assertThat(sql).contains("PARTITION BY `category`");
            assertThat(sql).contains("ORDER BY `score` ASC");
        }
    }

    @Nested
    @DisplayName("Fetch Methods Tests")
    class FetchMethodsTests {

        @Test
        @DisplayName("fetch() 抛出 UnsupportedOperationException")
        void testFetchThrowsException() {
            SqlQueryBuilder builder = new SqlQueryBuilderImpl().select("*").from("users");

            assertThatThrownBy(() -> builder.fetch(Object.class))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Use DataManager to execute query");
        }

        @Test
        @DisplayName("fetchOne() 抛出 UnsupportedOperationException")
        void testFetchOneThrowsException() {
            SqlQueryBuilder builder = new SqlQueryBuilderImpl().select("*").from("users");

            assertThatThrownBy(() -> builder.fetchOne(Object.class))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Use DataManager to execute query");
        }

        @Test
        @DisplayName("fetchFirst() 抛出 UnsupportedOperationException")
        void testFetchFirstThrowsException() {
            SqlQueryBuilder builder = new SqlQueryBuilderImpl().select("*").from("users");

            assertThatThrownBy(() -> builder.fetchFirst(Object.class))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Use DataManager to execute query");
        }

        @Test
        @DisplayName("fetchPage() 抛出 UnsupportedOperationException")
        void testFetchPageThrowsException() {
            SqlQueryBuilder builder = new SqlQueryBuilderImpl().select("*").from("users");

            assertThatThrownBy(() -> builder.fetchPage(Object.class, PageRequest.of(1, 10)))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Use DataManager to execute query");
        }

        @Test
        @DisplayName("fetchCount() 抛出 UnsupportedOperationException")
        void testFetchCountThrowsException() {
            SqlQueryBuilder builder = new SqlQueryBuilderImpl().select("*").from("users");

            assertThatThrownBy(builder::fetchCount)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Use DataManager to execute query");
        }
    }

    @Nested
    @DisplayName("Page with Sort Tests")
    class PageWithSortTests {

        @Test
        @DisplayName("page(PageRequest) with sort")
        void testPageWithSort() {
            PageRequest pageRequest = PageRequest.of(2, 10, Sort.by(Sort.Order.desc("created_at")));

            String sql = new SqlQueryBuilderImpl()
                    .select("*")
                    .from("users")
                    .page(pageRequest)
                    .toSql();

            assertThat(sql).contains("LIMIT 10");
            assertThat(sql).contains("OFFSET 10");
            assertThat(sql).contains("ORDER BY `created_at` DESC");
        }

        @Test
        @DisplayName("orderBy(Sort) 直接调用")
        void testOrderBySortDirect() {
            String sql = new SqlQueryBuilderImpl()
                    .select("*")
                    .from("users")
                    .orderBy(Sort.by(Sort.Order.desc("created_at")))
                    .toSql();

            assertThat(sql).contains("ORDER BY `created_at` DESC");
        }
    }

    @Nested
    @DisplayName("HAVING Parameters Tests")
    class HavingParametersTests {

        @Test
        @DisplayName("getParameters 包含 HAVING 条件参数")
        void testGetParametersWithHaving() {
            java.util.List<Object> params = new SqlQueryBuilderImpl()
                    .select("category")
                    .count()
                    .from("products")
                    .groupBy("category")
                    .having(Conditions.builder().gt("count", 10).build())
                    .getParameters();

            assertThat(params).containsExactly(10);
        }

        @Test
        @DisplayName("HAVING 与 WHERE 参数组合")
        void testHavingWithWhereParameters() {
            java.util.List<Object> params = new SqlQueryBuilderImpl()
                    .select("category")
                    .count()
                    .from("products")
                    .where(Conditions.eq("status", 1))
                    .groupBy("category")
                    .having(Conditions.builder().gt("count", 5).build())
                    .getParameters();

            assertThat(params).containsExactly(1, 5);
        }
    }

    @Nested
    @DisplayName("Empty Select Columns Tests")
    class EmptySelectColumnsTests {

        @Test
        @DisplayName("无 select 列时默认使用 *")
        void testNoSelectColumns() {
            String sql = new SqlQueryBuilderImpl()
                    .from("users")
                    .toSql();

            assertThat(sql).isEqualTo("SELECT * FROM `users`");
        }
    }

    @Nested
    @DisplayName("Multiple Partition Columns Tests")
    class MultiplePartitionColumnsTests {

        @Test
        @DisplayName("rowNumberOver with multiple partition columns")
        void testRowNumberOverMultiplePartitions() {
            String sql = new SqlQueryBuilderImpl()
                    .select("id", "name")
                    .rowNumberOver("department, team", "salary DESC")
                    .from("employees")
                    .toSql();

            assertThat(sql).contains("PARTITION BY `department`, `team`");
        }
    }

    // 测试实体类
    static class TestEntity {
        private Long id;
        private String name;

        public Long getId() { return id; }
        public String getName() { return name; }
    }
}
