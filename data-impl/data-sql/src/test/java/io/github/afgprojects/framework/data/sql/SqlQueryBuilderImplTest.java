package io.github.afgprojects.framework.data.sql;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Sort;
import io.github.afgprojects.framework.data.core.sql.SqlQueryBuilder;
import io.github.afgprojects.framework.data.sql.builder.SqlQueryBuilderImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SQL 查询构建器测试
 */
class SqlQueryBuilderImplTest {

    @Test
    @DisplayName("构建简单 SELECT 查询")
    void testBuildSimpleSelect() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "name")
            .from("users")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, name FROM `users`");
    }

    @Test
    @DisplayName("构建带 WHERE 条件的查询")
    void testBuildSelectWithWhere() {
        Condition condition = Conditions.eq("status", 1);
        String sql = new SqlQueryBuilderImpl()
            .select("*")
            .from("users")
            .where(condition)
            .toSql();

        assertThat(sql).isEqualTo("SELECT * FROM `users` WHERE status = ?");
    }

    @Test
    @DisplayName("构建带 ORDER BY 的查询")
    void testBuildSelectWithOrderBy() {
        String sql = new SqlQueryBuilderImpl()
            .select("*")
            .from("users")
            .orderBy("created_at", Sort.Direction.DESC)
            .toSql();

        assertThat(sql).isEqualTo("SELECT * FROM `users` ORDER BY `created_at` DESC");
    }

    @Test
    @DisplayName("构建带 LIMIT 和 OFFSET 的查询")
    void testBuildSelectWithLimitOffset() {
        String sql = new SqlQueryBuilderImpl()
            .select("*")
            .from("users")
            .limit(10)
            .offset(20)
            .toSql();

        assertThat(sql).isEqualTo("SELECT * FROM `users` LIMIT 10 OFFSET 20");
    }

    @Test
    @DisplayName("构建分页查询")
    void testBuildPageQuery() {
        String sql = new SqlQueryBuilderImpl()
            .select("*")
            .from("users")
            .page(2, 10)  // 第2页，每页10条
            .toSql();

        assertThat(sql).isEqualTo("SELECT * FROM `users` LIMIT 10 OFFSET 10");
    }

    @Test
    @DisplayName("构建 DISTINCT 查询")
    void testBuildDistinctSelect() {
        String sql = new SqlQueryBuilderImpl()
            .select("category")
            .distinct()
            .from("products")
            .toSql();

        assertThat(sql).isEqualTo("SELECT DISTINCT category FROM `products`");
    }

    @Test
    @DisplayName("构建 JOIN 查询")
    void testBuildJoinQuery() {
        Condition joinCondition = Conditions.eq("u.id", "o.user_id");
        String sql = new SqlQueryBuilderImpl()
            .select("u.id", "u.name", "o.order_no")
            .from("users", "u")
            .join("orders", "o", joinCondition)
            .toSql();

        assertThat(sql).contains("JOIN");
        assertThat(sql).contains("ON");
    }

    @Test
    @DisplayName("构建 GROUP BY 查询")
    void testBuildGroupByQuery() {
        String sql = new SqlQueryBuilderImpl()
            .select("category", "COUNT(*)")
            .from("products")
            .groupBy("category")
            .toSql();

        assertThat(sql).isEqualTo("SELECT category, COUNT(*) FROM `products` GROUP BY `category`");
    }

    @Test
    @DisplayName("获取查询参数")
    void testGetParameters() {
        Condition condition = Conditions.builder()
            .eq("status", 1)
            .like("name", "test")
            .build();

        java.util.List<Object> params = new SqlQueryBuilderImpl()
            .select("*")
            .from("users")
            .where(condition)
            .getParameters();

        assertThat(params).containsExactly(1, "%test%");
    }

    @Test
    @DisplayName("构建带表别名的查询")
    void testBuildSelectWithAlias() {
        String sql = new SqlQueryBuilderImpl()
            .select("u.id", "u.name")
            .from("users", "u")
            .toSql();

        assertThat(sql).isEqualTo("SELECT u.id, u.name FROM `users` u");
    }

    @Test
    @DisplayName("构建 EXISTS 子查询")
    void testBuildExistsSubquery() {
        SqlQueryBuilder subquery = new SqlQueryBuilderImpl()
            .select("1")
            .from("orders")
            .where(Conditions.eq("user_id", "users.id"));

        String sql = new SqlQueryBuilderImpl()
            .select("*")
            .from("users")
            .exists(subquery)
            .toSql();

        assertThat(sql).isEqualTo("SELECT * FROM `users` WHERE EXISTS (SELECT 1 FROM `orders` WHERE user_id = ?)");
    }

    @Test
    @DisplayName("构建 NOT EXISTS 子查询")
    void testBuildNotExistsSubquery() {
        SqlQueryBuilder subquery = new SqlQueryBuilderImpl()
            .select("1")
            .from("orders")
            .where(Conditions.eq("user_id", "users.id"));

        String sql = new SqlQueryBuilderImpl()
            .select("*")
            .from("users")
            .notExists(subquery)
            .toSql();

        assertThat(sql).isEqualTo("SELECT * FROM `users` WHERE NOT EXISTS (SELECT 1 FROM `orders` WHERE user_id = ?)");
    }

    @Test
    @DisplayName("构建带 WHERE 条件和 EXISTS 子查询")
    void testBuildWhereWithExists() {
        SqlQueryBuilder subquery = new SqlQueryBuilderImpl()
            .select("1")
            .from("orders")
            .where(Conditions.eq("user_id", "users.id"));

        String sql = new SqlQueryBuilderImpl()
            .select("*")
            .from("users")
            .where(Conditions.eq("status", 1))
            .exists(subquery)
            .toSql();

        assertThat(sql).isEqualTo("SELECT * FROM `users` WHERE status = ? AND EXISTS (SELECT 1 FROM `orders` WHERE user_id = ?)");
    }

    @Test
    @DisplayName("EXISTS 子查询参数绑定")
    void testExistsSubqueryParameters() {
        SqlQueryBuilder subquery = new SqlQueryBuilderImpl()
            .select("1")
            .from("orders")
            .where(Conditions.builder()
                .eq("user_id", "users.id")
                .eq("status", 1)
                .build());

        java.util.List<Object> params = new SqlQueryBuilderImpl()
            .select("*")
            .from("users")
            .where(Conditions.eq("status", 2))
            .exists(subquery)
            .getParameters();

        assertThat(params).containsExactly(2, "users.id", 1);
    }

    @Test
    @DisplayName("多个 EXISTS 子查询")
    void testMultipleExistsSubqueries() {
        SqlQueryBuilder subquery1 = new SqlQueryBuilderImpl()
            .select("1")
            .from("orders")
            .where(Conditions.eq("user_id", "users.id"));

        SqlQueryBuilder subquery2 = new SqlQueryBuilderImpl()
            .select("1")
            .from("reviews")
            .where(Conditions.eq("user_id", "users.id"));

        String sql = new SqlQueryBuilderImpl()
            .select("*")
            .from("users")
            .exists(subquery1)
            .exists(subquery2)
            .toSql();

        assertThat(sql).isEqualTo("SELECT * FROM `users` WHERE EXISTS (SELECT 1 FROM `orders` WHERE user_id = ?) AND EXISTS (SELECT 1 FROM `reviews` WHERE user_id = ?)");
    }

    // ==================== 聚合函数测试 ====================

    @Test
    @DisplayName("COUNT(column) 聚合函数")
    void testCountColumn() {
        String sql = new SqlQueryBuilderImpl()
            .count("id")
            .from("users")
            .toSql();

        assertThat(sql).isEqualTo("SELECT COUNT(`id`) FROM `users`");
    }

    @Test
    @DisplayName("COUNT(*) 聚合函数")
    void testCountAll() {
        String sql = new SqlQueryBuilderImpl()
            .count()
            .from("users")
            .toSql();

        assertThat(sql).isEqualTo("SELECT COUNT(*) FROM `users`");
    }

    @Test
    @DisplayName("COUNT(DISTINCT column) 聚合函数")
    void testCountDistinct() {
        String sql = new SqlQueryBuilderImpl()
            .countDistinct("user_id")
            .from("orders")
            .toSql();

        assertThat(sql).isEqualTo("SELECT COUNT(DISTINCT `user_id`) FROM `orders`");
    }

    @Test
    @DisplayName("SUM(column) 聚合函数")
    void testSum() {
        String sql = new SqlQueryBuilderImpl()
            .sum("amount")
            .from("orders")
            .toSql();

        assertThat(sql).isEqualTo("SELECT SUM(`amount`) FROM `orders`");
    }

    @Test
    @DisplayName("AVG(column) 聚合函数")
    void testAvg() {
        String sql = new SqlQueryBuilderImpl()
            .avg("price")
            .from("products")
            .toSql();

        assertThat(sql).isEqualTo("SELECT AVG(`price`) FROM `products`");
    }

    @Test
    @DisplayName("MAX(column) 聚合函数")
    void testMax() {
        String sql = new SqlQueryBuilderImpl()
            .max("created_at")
            .from("users")
            .toSql();

        assertThat(sql).isEqualTo("SELECT MAX(`created_at`) FROM `users`");
    }

    @Test
    @DisplayName("MIN(column) 聚合函数")
    void testMin() {
        String sql = new SqlQueryBuilderImpl()
            .min("price")
            .from("products")
            .toSql();

        assertThat(sql).isEqualTo("SELECT MIN(`price`) FROM `products`");
    }

    @Test
    @DisplayName("聚合函数与 GROUP BY 组合")
    void testAggregateWithGroupBy() {
        String sql = new SqlQueryBuilderImpl()
            .select("category")
            .count()
            .sum("price")
            .avg("price")
            .from("products")
            .groupBy("category")
            .toSql();

        assertThat(sql).isEqualTo("SELECT category, COUNT(*), SUM(`price`), AVG(`price`) FROM `products` GROUP BY `category`");
    }

    @Test
    @DisplayName("聚合函数与 WHERE 条件组合")
    void testAggregateWithWhere() {
        String sql = new SqlQueryBuilderImpl()
            .count()
            .from("users")
            .where(Conditions.eq("status", 1))
            .toSql();

        assertThat(sql).isEqualTo("SELECT COUNT(*) FROM `users` WHERE status = ?");
    }

    @Test
    @DisplayName("聚合函数与 HAVING 条件组合")
    void testAggregateWithHaving() {
        String sql = new SqlQueryBuilderImpl()
            .select("category")
            .count()
            .from("products")
            .groupBy("category")
            .having(Conditions.builder().gt("count", 10).build())
            .toSql();

        assertThat(sql).isEqualTo("SELECT category, COUNT(*) FROM `products` GROUP BY `category` HAVING count > ?");
    }

    @Test
    @DisplayName("多个聚合函数")
    void testMultipleAggregates() {
        String sql = new SqlQueryBuilderImpl()
            .count()
            .countDistinct("user_id")
            .sum("amount")
            .avg("amount")
            .max("amount")
            .min("amount")
            .from("orders")
            .toSql();

        assertThat(sql).isEqualTo("SELECT COUNT(*), COUNT(DISTINCT `user_id`), SUM(`amount`), AVG(`amount`), MAX(`amount`), MIN(`amount`) FROM `orders`");
    }

    // ==================== CTE (公共表表达式) 测试 ====================

    @Test
    @DisplayName("简单 CTE (WITH 子句)")
    void testSimpleCte() {
        SqlQueryBuilder cte = new SqlQueryBuilderImpl()
            .select("id", "name")
            .from("users")
            .where(Conditions.eq("status", 1));

        String sql = new SqlQueryBuilderImpl()
            .with("active_users", cte)
            .select("*")
            .from("active_users")
            .toSql();

        assertThat(sql).isEqualTo("WITH `active_users` AS (SELECT id, name FROM `users` WHERE status = ?) SELECT * FROM `active_users`");
    }

    @Test
    @DisplayName("多个 CTE")
    void testMultipleCtes() {
        SqlQueryBuilder cte1 = new SqlQueryBuilderImpl()
            .select("id", "name")
            .from("users")
            .where(Conditions.eq("status", 1));

        SqlQueryBuilder cte2 = new SqlQueryBuilderImpl()
            .select("user_id", "COUNT(*)")
            .from("orders")
            .groupBy("user_id");

        String sql = new SqlQueryBuilderImpl()
            .with("active_users", cte1)
            .with("order_counts", cte2)
            .select("u.name", "oc.count")
            .from("active_users", "u")
            .join("order_counts", "oc", Conditions.eq("u.id", "oc.user_id"))
            .toSql();

        // JOIN 条件中，第二个参数会被作为参数占位符处理
        assertThat(sql).isEqualTo("WITH `active_users` AS (SELECT id, name FROM `users` WHERE status = ?), `order_counts` AS (SELECT user_id, COUNT(*) FROM `orders` GROUP BY `user_id`) SELECT u.name, oc.count FROM `active_users` u JOIN `order_counts` oc ON u.id = ?");
    }

    @Test
    @DisplayName("递归 CTE (WITH RECURSIVE)")
    void testRecursiveCte() {
        SqlQueryBuilder cte = new SqlQueryBuilderImpl()
            .select("id", "parent_id", "name")
            .from("categories")
            .where(Conditions.eq("parent_id", 0));

        String sql = new SqlQueryBuilderImpl()
            .withRecursive("category_tree", cte)
            .select("*")
            .from("category_tree")
            .toSql();

        assertThat(sql).isEqualTo("WITH RECURSIVE `category_tree` AS (SELECT id, parent_id, name FROM `categories` WHERE parent_id = ?) SELECT * FROM `category_tree`");
    }

    @Test
    @DisplayName("带列名的 CTE")
    void testCteWithColumnNames() {
        SqlQueryBuilder cte = new SqlQueryBuilderImpl()
            .select("user_id", "SUM(amount)")
            .from("orders")
            .groupBy("user_id");

        String sql = new SqlQueryBuilderImpl()
            .withColumnNames("user_totals", new String[]{"uid", "total"}, cte)
            .select("u.name", "ut.total")
            .from("users", "u")
            .join("user_totals", "ut", Conditions.eq("u.id", "ut.uid"))
            .toSql();

        // JOIN 条件中，第二个参数会被作为参数占位符处理
        assertThat(sql).isEqualTo("WITH `user_totals`(`uid`, `total`) AS (SELECT user_id, SUM(amount) FROM `orders` GROUP BY `user_id`) SELECT u.name, ut.total FROM `users` u JOIN `user_totals` ut ON u.id = ?");
    }

    @Test
    @DisplayName("带列名的递归 CTE")
    void testRecursiveCteWithColumnNames() {
        SqlQueryBuilder cte = new SqlQueryBuilderImpl()
            .select("id", "parent_id", "level")
            .from("employees")
            .where(Conditions.builder().isNull("parent_id").build());

        String sql = new SqlQueryBuilderImpl()
            .withRecursiveColumnNames("employee_hierarchy", new String[]{"emp_id", "manager_id", "depth"}, cte)
            .select("*")
            .from("employee_hierarchy")
            .toSql();

        assertThat(sql).isEqualTo("WITH RECURSIVE `employee_hierarchy`(`emp_id`, `manager_id`, `depth`) AS (SELECT id, parent_id, level FROM `employees` WHERE parent_id IS NULL) SELECT * FROM `employee_hierarchy`");
    }

    @Test
    @DisplayName("CTE 与 WHERE 条件组合")
    void testCteWithWhere() {
        SqlQueryBuilder cte = new SqlQueryBuilderImpl()
            .select("user_id", "COUNT(*)")
            .from("orders")
            .groupBy("user_id");

        String sql = new SqlQueryBuilderImpl()
            .with("order_counts", cte)
            .select("u.name", "oc.count")
            .from("users", "u")
            .join("order_counts", "oc", Conditions.eq("u.id", "oc.user_id"))
            .where(Conditions.builder().gt("oc.count", 10).build())
            .toSql();

        // JOIN 条件中，第二个参数会被作为参数占位符处理
        assertThat(sql).isEqualTo("WITH `order_counts` AS (SELECT user_id, COUNT(*) FROM `orders` GROUP BY `user_id`) SELECT u.name, oc.count FROM `users` u JOIN `order_counts` oc ON u.id = ? WHERE oc.count > ?");
    }

    @Test
    @DisplayName("CTE 参数绑定")
    void testCteParameters() {
        SqlQueryBuilder cte = new SqlQueryBuilderImpl()
            .select("id", "name")
            .from("users")
            .where(Conditions.builder()
                .eq("status", 1)
                .gt("age", 18)
                .build());

        java.util.List<Object> params = new SqlQueryBuilderImpl()
            .with("active_users", cte)
            .select("*")
            .from("active_users")
            .where(Conditions.eq("department", "IT"))
            .getParameters();

        assertThat(params).containsExactly(1, 18, "IT");
    }

    @Test
    @DisplayName("多个 CTE 参数绑定")
    void testMultipleCteParameters() {
        SqlQueryBuilder cte1 = new SqlQueryBuilderImpl()
            .select("id")
            .from("users")
            .where(Conditions.eq("status", 1));

        SqlQueryBuilder cte2 = new SqlQueryBuilderImpl()
            .select("user_id")
            .from("orders")
            .where(Conditions.builder().gt("amount", 1000).build());

        java.util.List<Object> params = new SqlQueryBuilderImpl()
            .with("active_users", cte1)
            .with("big_orders", cte2)
            .select("*")
            .from("active_users")
            .where(Conditions.eq("role", "admin"))
            .getParameters();

        assertThat(params).containsExactly(1, 1000, "admin");
    }

    @Test
    @DisplayName("CTE 与 ORDER BY/LIMIT 组合")
    void testCteWithOrderAndLimit() {
        SqlQueryBuilder cte = new SqlQueryBuilderImpl()
            .select("user_id", "SUM(amount)")
            .from("orders")
            .groupBy("user_id");

        String sql = new SqlQueryBuilderImpl()
            .with("user_totals", cte)
            .select("*")
            .from("user_totals")
            .orderBy("total", Sort.Direction.DESC)
            .limit(10)
            .toSql();

        assertThat(sql).isEqualTo("WITH `user_totals` AS (SELECT user_id, SUM(amount) FROM `orders` GROUP BY `user_id`) SELECT * FROM `user_totals` ORDER BY `total` DESC LIMIT 10");
    }

    // ==================== 窗口函数测试 ====================

    @Test
    @DisplayName("ROW_NUMBER() 窗口函数")
    void testRowNumber() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "name")
            .rowNumber()
            .from("users")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, name, ROW_NUMBER() FROM `users`");
    }

    @Test
    @DisplayName("RANK() 窗口函数")
    void testRank() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "name")
            .rank()
            .from("users")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, name, RANK() FROM `users`");
    }

    @Test
    @DisplayName("DENSE_RANK() 窗口函数")
    void testDenseRank() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "name")
            .denseRank()
            .from("users")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, name, DENSE_RANK() FROM `users`");
    }

    @Test
    @DisplayName("LEAD(column) 窗口函数")
    void testLead() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "created_at")
            .lead("created_at")
            .from("orders")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, created_at, LEAD(`created_at`) FROM `orders`");
    }

    @Test
    @DisplayName("LEAD(column, offset) 窗口函数")
    void testLeadWithOffset() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "amount")
            .lead("amount", 2)
            .from("orders")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, amount, LEAD(`amount`, 2) FROM `orders`");
    }

    @Test
    @DisplayName("LEAD(column, offset, defaultValue) 窗口函数")
    void testLeadWithDefault() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "amount")
            .lead("amount", 1, 0)
            .from("orders")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, amount, LEAD(`amount`, 1, 0) FROM `orders`");
    }

    @Test
    @DisplayName("LEAD with string default value")
    void testLeadWithStringDefault() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "status")
            .lead("status", 1, "N/A")
            .from("orders")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, status, LEAD(`status`, 1, 'N/A') FROM `orders`");
    }

    @Test
    @DisplayName("LEAD with string containing single quote - SQL injection prevention")
    void testLeadWithSingleQuoteInDefaultValue() {
        // 测试单引号转义，防止 SQL 注入
        String sql = new SqlQueryBuilderImpl()
            .select("id", "status")
            .lead("status", 1, "O'Reilly")
            .from("orders")
            .toSql();

        // 单引号应该被转义为两个单引号
        assertThat(sql).isEqualTo("SELECT id, status, LEAD(`status`, 1, 'O''Reilly') FROM `orders`");
    }

    @Test
    @DisplayName("LAG with string containing single quote - SQL injection prevention")
    void testLagWithSingleQuoteInDefaultValue() {
        // 测试单引号转义，防止 SQL 注入
        String sql = new SqlQueryBuilderImpl()
            .select("id", "status")
            .lag("status", 1, "'; DROP TABLE users; --")
            .from("orders")
            .toSql();

        // 单引号应该被转义为两个单引号，防止 SQL 注入
        assertThat(sql).isEqualTo("SELECT id, status, LAG(`status`, 1, '''; DROP TABLE users; --') FROM `orders`");
    }

    @Test
    @DisplayName("LAG(column) 窗口函数")
    void testLag() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "created_at")
            .lag("created_at")
            .from("orders")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, created_at, LAG(`created_at`) FROM `orders`");
    }

    @Test
    @DisplayName("LAG(column, offset) 窗口函数")
    void testLagWithOffset() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "amount")
            .lag("amount", 2)
            .from("orders")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, amount, LAG(`amount`, 2) FROM `orders`");
    }

    @Test
    @DisplayName("LAG(column, offset, defaultValue) 窗口函数")
    void testLagWithDefault() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "amount")
            .lag("amount", 1, 0)
            .from("orders")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, amount, LAG(`amount`, 1, 0) FROM `orders`");
    }

    @Test
    @DisplayName("ROW_NUMBER() OVER(PARTITION BY ... ORDER BY ...) 字符串参数")
    void testRowNumberOverStringParams() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "name", "score")
            .rowNumberOver("category", "score DESC")
            .from("products")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, name, score, ROW_NUMBER() OVER (PARTITION BY `category` ORDER BY `score` DESC) FROM `products`");
    }

    @Test
    @DisplayName("ROW_NUMBER() OVER(PARTITION BY ... ORDER BY ...) Sort 参数")
    void testRowNumberOverSortParams() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "name", "score")
            .rowNumberOver("category", Sort.desc("score"))
            .from("products")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, name, score, ROW_NUMBER() OVER (PARTITION BY `category` ORDER BY `score` DESC) FROM `products`");
    }

    @Test
    @DisplayName("RANK() OVER(PARTITION BY ... ORDER BY ...)")
    void testRankOver() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "name", "score")
            .rankOver("department", "salary DESC")
            .from("employees")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, name, score, RANK() OVER (PARTITION BY `department` ORDER BY `salary` DESC) FROM `employees`");
    }

    @Test
    @DisplayName("DENSE_RANK() OVER(PARTITION BY ... ORDER BY ...)")
    void testDenseRankOver() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "name", "score")
            .denseRankOver("category", "score DESC")
            .from("products")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, name, score, DENSE_RANK() OVER (PARTITION BY `category` ORDER BY `score` DESC) FROM `products`");
    }

    @Test
    @DisplayName("窗口函数与 OVER() 构建器 - 基本用法")
    void testWindowFunctionWithOverBuilder() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "name", "score")
            .rowNumber()
            .over()
            .partitionBy("category")
            .orderBy("score", Sort.Direction.DESC)
            .end()
            .from("products")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, name, score, ROW_NUMBER() OVER (PARTITION BY `category` ORDER BY `score` DESC) FROM `products`");
    }

    @Test
    @DisplayName("窗口函数与 OVER() 构建器 - 多个分区列")
    void testWindowFunctionWithOverBuilderMultiplePartitions() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "name", "salary")
            .rank()
            .over()
            .partitionBy("department", "team")
            .orderBy("salary", Sort.Direction.DESC)
            .end()
            .from("employees")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, name, salary, RANK() OVER (PARTITION BY `department`, `team` ORDER BY `salary` DESC) FROM `employees`");
    }

    @Test
    @DisplayName("窗口函数与 OVER() 构建器 - 使用 Sort 对象")
    void testWindowFunctionWithOverBuilderSortObject() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "name", "score")
            .denseRank()
            .over()
            .partitionBy("category")
            .orderBy(Sort.by(Sort.Order.desc("score"), Sort.Order.asc("name")))
            .end()
            .from("products")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, name, score, DENSE_RANK() OVER (PARTITION BY `category` ORDER BY `score` DESC, `name` ASC) FROM `products`");
    }

    @Test
    @DisplayName("LEAD 与 OVER() 构建器")
    void testLeadWithOverBuilder() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "amount")
            .lead("amount")
            .over()
            .orderBy("created_at", Sort.Direction.ASC)
            .end()
            .from("orders")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, amount, LEAD(`amount`) OVER (ORDER BY `created_at` ASC) FROM `orders`");
    }

    @Test
    @DisplayName("LAG 与 OVER() 构建器")
    void testLagWithOverBuilder() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "amount")
            .lag("amount", 1, 0)
            .over()
            .partitionBy("user_id")
            .orderBy("created_at", Sort.Direction.ASC)
            .end()
            .from("orders")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, amount, LAG(`amount`, 1, 0) OVER (PARTITION BY `user_id` ORDER BY `created_at` ASC) FROM `orders`");
    }

    @Test
    @DisplayName("多个窗口函数")
    void testMultipleWindowFunctions() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "name", "score")
            .rowNumberOver("category", "score DESC")
            .rankOver("category", "score DESC")
            .denseRankOver("category", "score DESC")
            .from("products")
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, name, score, ROW_NUMBER() OVER (PARTITION BY `category` ORDER BY `score` DESC), RANK() OVER (PARTITION BY `category` ORDER BY `score` DESC), DENSE_RANK() OVER (PARTITION BY `category` ORDER BY `score` DESC) FROM `products`");
    }

    @Test
    @DisplayName("窗口函数与 WHERE 条件组合")
    void testWindowFunctionWithWhere() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "name", "score")
            .rowNumberOver("category", "score DESC")
            .from("products")
            .where(Conditions.eq("status", 1))
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, name, score, ROW_NUMBER() OVER (PARTITION BY `category` ORDER BY `score` DESC) FROM `products` WHERE status = ?");
    }

    @Test
    @DisplayName("窗口函数与 ORDER BY 组合")
    void testWindowFunctionWithOrderBy() {
        String sql = new SqlQueryBuilderImpl()
            .select("id", "name", "score")
            .rowNumberOver("category", "score DESC")
            .from("products")
            .orderBy("category", Sort.Direction.ASC)
            .toSql();

        assertThat(sql).isEqualTo("SELECT id, name, score, ROW_NUMBER() OVER (PARTITION BY `category` ORDER BY `score` DESC) FROM `products` ORDER BY `category` ASC");
    }
}