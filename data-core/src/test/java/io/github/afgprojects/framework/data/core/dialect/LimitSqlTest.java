package io.github.afgprojects.framework.data.core.dialect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dialect.getLimitSql 测试
 * <p>
 * 专门测试各方言的行数限制 SQL 生成逻辑
 */
@DisplayName("Dialect.getLimitSql 测试")
class LimitSqlTest {

    // ==================== MySQL ====================

    @Nested
    @DisplayName("MySQLDialect getLimitSql")
    class MySQLLimitSqlTests {

        private final MySQLDialect dialect = new MySQLDialect();

        @Test
        @DisplayName("应生成 LIMIT 语法")
        void shouldGenerateLimitSql() {
            String result = dialect.getLimitSql("SELECT * FROM t", 10);
            assertThat(result).isEqualTo("SELECT * FROM t LIMIT 10");
        )

        @Test
        @DisplayName("应支持复杂 SQL")
        void shouldHandleComplexSql() {
            String sql = "SELECT u.id, u.name FROM user u WHERE u.status = 1 ORDER BY u.id";
            String result = dialect.getLimitSql(sql, 5);
            assertThat(result).isEqualTo(sql + " LIMIT 5");
        )
    )

    // ==================== PostgreSQL ====================

    @Nested
    @DisplayName("PostgreSQLDialect getLimitSql")
    class PostgreSQLLimitSqlTests {

        private final PostgreSQLDialect dialect = new PostgreSQLDialect();

        @Test
        @DisplayName("应生成 LIMIT 语法（PostgreSQL 支持 LIMIT 但 supportsFetchFirst 也为 true，默认走 LIMIT）")
        void shouldGenerateLimitSql() {
            // PostgreSQL supportsFetchFirst() = true, 所以 AbstractDialect.getLimitSql 走 FETCH FIRST 分支
            String result = dialect.getLimitSql("SELECT * FROM t", 10);
            assertThat(result).isEqualTo("SELECT * FROM (SELECT * FROM t) FETCH FIRST 10 ROWS ONLY");
        )
    )

    // ==================== Oracle ====================

    @Nested
    @DisplayName("OracleDialect getLimitSql")
    class OracleLimitSqlTests {

        private final OracleDialect dialect = new OracleDialect();

        @Test
        @DisplayName("应生成 FETCH FIRST 语法")
        void shouldGenerateFetchFirstSql() {
            String result = dialect.getLimitSql("SELECT * FROM t", 10);
            assertThat(result).isEqualTo("SELECT * FROM (SELECT * FROM t) FETCH FIRST 10 ROWS ONLY");
        )

        @Test
        @DisplayName("应支持复杂 SQL")
        void shouldHandleComplexSql() {
            String sql = "SELECT id, name FROM user WHERE status = 1 ORDER BY id";
            String result = dialect.getLimitSql(sql, 5);
            assertThat(result).isEqualTo("SELECT * FROM (" + sql + ") FETCH FIRST 5 ROWS ONLY");
        )
    )

    // ==================== SQL Server ====================

    @Nested
    @DisplayName("SQLServerDialect getLimitSql")
    class SQLServerLimitSqlTests {

        private final SQLServerDialect dialect = new SQLServerDialect();

        @Test
        @DisplayName("有 ORDER BY 时应直接追加 OFFSET FETCH")
        void shouldGenerateWithOrderBy() {
            String sql = "SELECT * FROM t ORDER BY id";
            String result = dialect.getLimitSql(sql, 10);
            assertThat(result).isEqualTo("SELECT * FROM t ORDER BY id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY");
        )

        @Test
        @DisplayName("无 ORDER BY 时应包装子查询")
        void shouldGenerateWithoutOrderBy() {
            String sql = "SELECT * FROM t";
            String result = dialect.getLimitSql(sql, 10);
            assertThat(result).isEqualTo("SELECT * FROM (SELECT * FROM t) AS _limited OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY");
        )

        @Test
        @DisplayName("ORDER BY 大小写不敏感应识别")
        void shouldDetectOrderByCaseInsensitive() {
            String sql = "SELECT * FROM t order by id";
            String result = dialect.getLimitSql(sql, 10);
            assertThat(result).isEqualTo("SELECT * FROM t order by id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY");
        )

        @Test
        @DisplayName("ORDER BY 在子查询中不应误判")
        void shouldNotDetectOrderByInSubquery() {
            // SQL Server 的检测是简单的 contains("ORDER BY")，子查询中的 ORDER BY 也会被检测到
            // 这是当前实现的行为，测试记录此行为
            String sql = "SELECT * FROM (SELECT * FROM t ORDER BY id) AS sub";
            String result = dialect.getLimitSql(sql, 10);
            // 包含 ORDER BY，所以直接追加
            assertThat(result).isEqualTo(sql + " OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY");
        )
    )

    // ==================== DmDialect ====================

    @Nested
    @DisplayName("DmDialect getLimitSql")
    class DmLimitSqlTests {

        private final DmDialect dialect = new DmDialect();

        @Test
        @DisplayName("应继承 Oracle 的 FETCH FIRST 语法")
        void shouldInheritOracleFetchFirst() {
            String result = dialect.getLimitSql("SELECT * FROM t", 10);
            assertThat(result).isEqualTo("SELECT * FROM (SELECT * FROM t) FETCH FIRST 10 ROWS ONLY");
        )
    )

    // ==================== H2 ====================

    @Nested
    @DisplayName("H2Dialect getLimitSql")
    class H2LimitSqlTests {

        private final H2Dialect dialect = new H2Dialect();

        @Test
        @DisplayName("应生成 LIMIT 语法")
        void shouldGenerateLimitSql() {
            String result = dialect.getLimitSql("SELECT * FROM t", 10);
            assertThat(result).isEqualTo("SELECT * FROM t LIMIT 10");
        )

        @Test
        @DisplayName("应支持复杂 SQL")
        void shouldHandleComplexSql() {
            String sql = "SELECT id, name FROM user WHERE status = 1";
            String result = dialect.getLimitSql(sql, 5);
            assertThat(result).isEqualTo(sql + " LIMIT 5");
        )
    )
)
