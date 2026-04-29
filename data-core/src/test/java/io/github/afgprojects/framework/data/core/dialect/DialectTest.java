package io.github.afgprojects.framework.data.core.dialect;

import io.github.afgprojects.framework.data.core.page.PageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据库方言综合测试
 * <p>
 * 重点覆盖边界情况、异常处理和关键业务逻辑
 */
@DisplayName("Dialect 综合测试")
class DialectTest {

    // ==================== MySQL 方言测试 ====================

    @Nested
    @DisplayName("MySQLDialect 完整测试")
    class MySQLDialectFullTests {

        private final MySQLDialect dialect = new MySQLDialect();

        @Test
        @DisplayName("应生成 MySQL 分页 SQL")
        void shouldGenerateMySQLPagination() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, 20, 10);

            assertThat(paged).isEqualTo("SELECT * FROM user LIMIT 10 OFFSET 20");
        }

        @Test
        @DisplayName("应使用 PageRequest 生成分页 SQL")
        void shouldGeneratePaginationWithPageRequest() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, PageRequest.of(3, 20));

            assertThat(paged).isEqualTo("SELECT * FROM user LIMIT 20 OFFSET 40");
        }

        @Test
        @DisplayName("应正确引用标识符")
        void shouldQuoteIdentifier() {
            assertThat(dialect.quoteIdentifier("name")).isEqualTo("`name`");
            assertThat(dialect.quoteIdentifier("user_name")).isEqualTo("`user_name`");
        }

        @Test
        @DisplayName("应返回正确的标识符引用字符")
        void shouldReturnIdentifierQuote() {
            assertThat(dialect.getIdentifierQuote()).isEqualTo("`");
        }

        @Test
        @DisplayName("应返回正确的时间函数")
        void shouldGetCurrentTimeFunction() {
            assertThat(dialect.getCurrentTimeFunction()).isEqualTo("NOW()");
            assertThat(dialect.getCurrentDateFunction()).isEqualTo("CURDATE()");
            assertThat(dialect.getCurrentTimestampFunction()).isEqualTo("NOW()");
        }

        @Test
        @DisplayName("应支持序列和自增")
        void shouldCheckSequenceSupport() {
            assertThat(dialect.supportsSequence()).isTrue();
            assertThat(dialect.supportsAutoIncrement()).isTrue();
        }

        @Test
        @DisplayName("应返回自增语法")
        void shouldReturnAutoIncrementSyntax() {
            assertThat(dialect.getAutoIncrementSyntax()).isEqualTo("AUTO_INCREMENT");
        }

        @Test
        @DisplayName("应生成序列查询 SQL")
        void shouldGetMySQLSequenceNextValue() {
            assertThat(dialect.getSequenceNextValueSql("user_seq")).isEqualTo("SELECT NEXTVAL(`user_seq`)");
        }

        @Test
        @DisplayName("应正确映射所有 Java 类型到 SQL 类型")
        void shouldGetSqlType() {
            // 常用类型
            assertThat(dialect.getSqlType(String.class)).isEqualTo("VARCHAR(255)");
            assertThat(dialect.getSqlType(Long.class)).isEqualTo("BIGINT");
            assertThat(dialect.getSqlType(LocalDateTime.class)).isEqualTo("DATETIME");
            assertThat(dialect.getSqlType(BigDecimal.class)).isEqualTo("DECIMAL(19,4)");

            // 基本类型
            assertThat(dialect.getSqlType(int.class)).isEqualTo("INT");
            assertThat(dialect.getSqlType(long.class)).isEqualTo("BIGINT");
            assertThat(dialect.getSqlType(double.class)).isEqualTo("DOUBLE");
            assertThat(dialect.getSqlType(float.class)).isEqualTo("FLOAT");
            assertThat(dialect.getSqlType(boolean.class)).isEqualTo("TINYINT(1)");

            // 包装类型
            assertThat(dialect.getSqlType(Integer.class)).isEqualTo("INT");
            assertThat(dialect.getSqlType(Double.class)).isEqualTo("DOUBLE");
            assertThat(dialect.getSqlType(Float.class)).isEqualTo("FLOAT");
            assertThat(dialect.getSqlType(Boolean.class)).isEqualTo("TINYINT(1)");

            // 时间类型
            assertThat(dialect.getSqlType(LocalDate.class)).isEqualTo("DATE");
            assertThat(dialect.getSqlType(LocalTime.class)).isEqualTo("TIME");

            // 二进制类型
            assertThat(dialect.getSqlType(byte[].class)).isEqualTo("BLOB");

            // 未知类型
            assertThat(dialect.getSqlType(Object.class)).isEqualTo("VARCHAR(255)");
        }

        @Test
        @DisplayName("应返回 LIKE 通配符")
        void shouldReturnLikeWildcard() {
            assertThat(dialect.getLikeWildcard()).isEqualTo("%");
        }

        @Test
        @DisplayName("应支持 FOR UPDATE")
        void shouldSupportForUpdate() {
            assertThat(dialect.supportsForUpdate()).isTrue();
            assertThat(dialect.getForUpdateSyntax()).isEqualTo("FOR UPDATE");
        }

        @Test
        @DisplayName("分页能力测试")
        void shouldCheckPaginationCapabilities() {
            assertThat(dialect.supportsLimitOffset()).isTrue();
            assertThat(dialect.supportsFetchFirst()).isFalse();
        }
    }

    // ==================== PostgreSQL 方言测试 ====================

    @Nested
    @DisplayName("PostgreSQLDialect 完整测试")
    class PostgreSQLDialectFullTests {

        private final PostgreSQLDialect dialect = new PostgreSQLDialect();

        @Test
        @DisplayName("应生成 PostgreSQL 分页 SQL")
        void shouldGeneratePostgreSQLPagination() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, PageRequest.of(3, 20));

            assertThat(paged).isEqualTo("SELECT * FROM user LIMIT 20 OFFSET 40");
        }

        @Test
        @DisplayName("应正确引用标识符")
        void shouldQuoteIdentifier() {
            assertThat(dialect.quoteIdentifier("name")).isEqualTo("\"name\"");
            assertThat(dialect.getIdentifierQuote()).isEqualTo("\"");
        }

        @Test
        @DisplayName("应返回正确的时间函数")
        void shouldGetCurrentTimeFunction() {
            assertThat(dialect.getCurrentTimeFunction()).isEqualTo("CURRENT_TIME");
            assertThat(dialect.getCurrentDateFunction()).isEqualTo("CURRENT_DATE");
            assertThat(dialect.getCurrentTimestampFunction()).isEqualTo("CURRENT_TIMESTAMP");
        }

        @Test
        @DisplayName("应支持序列和自增")
        void shouldCheckSequenceSupport() {
            assertThat(dialect.supportsSequence()).isTrue();
            assertThat(dialect.supportsAutoIncrement()).isTrue();
        }

        @Test
        @DisplayName("应返回自增语法")
        void shouldReturnAutoIncrementSyntax() {
            assertThat(dialect.getAutoIncrementSyntax()).isEqualTo("GENERATED ALWAYS AS IDENTITY");
        }

        @Test
        @DisplayName("应生成序列查询 SQL")
        void shouldGetSequenceNextValue() {
            assertThat(dialect.getSequenceNextValueSql("user_seq")).isEqualTo("SELECT nextval('user_seq')");
        }

        @Test
        @DisplayName("应正确映射所有 Java 类型到 SQL 类型")
        void shouldGetSqlType() {
            // 基本类型
            assertThat(dialect.getSqlType(int.class)).isEqualTo("INTEGER");
            assertThat(dialect.getSqlType(long.class)).isEqualTo("BIGINT");
            assertThat(dialect.getSqlType(double.class)).isEqualTo("DOUBLE PRECISION");
            assertThat(dialect.getSqlType(float.class)).isEqualTo("REAL");
            assertThat(dialect.getSqlType(boolean.class)).isEqualTo("BOOLEAN");

            // 包装类型
            assertThat(dialect.getSqlType(Integer.class)).isEqualTo("INTEGER");
            assertThat(dialect.getSqlType(Long.class)).isEqualTo("BIGINT");
            assertThat(dialect.getSqlType(Double.class)).isEqualTo("DOUBLE PRECISION");
            assertThat(dialect.getSqlType(Float.class)).isEqualTo("REAL");
            assertThat(dialect.getSqlType(Boolean.class)).isEqualTo("BOOLEAN");

            // 时间类型
            assertThat(dialect.getSqlType(LocalDateTime.class)).isEqualTo("TIMESTAMP");
            assertThat(dialect.getSqlType(LocalDate.class)).isEqualTo("DATE");
            assertThat(dialect.getSqlType(LocalTime.class)).isEqualTo("TIME");

            // 其他类型
            assertThat(dialect.getSqlType(String.class)).isEqualTo("VARCHAR(255)");
            assertThat(dialect.getSqlType(BigDecimal.class)).isEqualTo("NUMERIC(19,4)");
            assertThat(dialect.getSqlType(byte[].class)).isEqualTo("BYTEA");
            assertThat(dialect.getSqlType(Object.class)).isEqualTo("VARCHAR(255)");
        }

        @Test
        @DisplayName("应返回 LIKE 通配符")
        void shouldReturnLikeWildcard() {
            assertThat(dialect.getLikeWildcard()).isEqualTo("%");
        }

        @Test
        @DisplayName("应支持 FOR UPDATE")
        void shouldSupportForUpdate() {
            assertThat(dialect.supportsForUpdate()).isTrue();
            assertThat(dialect.getForUpdateSyntax()).isEqualTo("FOR UPDATE");
        }

        @Test
        @DisplayName("分页能力测试")
        void shouldCheckPaginationCapabilities() {
            assertThat(dialect.supportsLimitOffset()).isTrue();
            assertThat(dialect.supportsFetchFirst()).isTrue();
        }

        @Test
        @DisplayName("应返回 PostgreSQL 数据库类型")
        void shouldReturnDatabaseType() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.POSTGRESQL);
        }
    }

    // ==================== Oracle 方言测试 ====================

    @Nested
    @DisplayName("OracleDialect 完整测试")
    class OracleDialectFullTests {

        private final OracleDialect dialect = new OracleDialect();

        @Test
        @DisplayName("应生成 Oracle 分页 SQL")
        void shouldGenerateOraclePagination() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, 20, 10);

            assertThat(paged).isEqualTo("SELECT * FROM (SELECT * FROM user) OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY");
        }

        @Test
        @DisplayName("应使用 PageRequest 生成分页 SQL")
        void shouldGeneratePaginationWithPageRequest() {
            String sql = "SELECT * FROM user ORDER BY id";
            String paged = dialect.getPaginationSql(sql, PageRequest.of(2, 15));

            assertThat(paged).isEqualTo("SELECT * FROM (SELECT * FROM user ORDER BY id) OFFSET 15 ROWS FETCH NEXT 15 ROWS ONLY");
        }

        @Test
        @DisplayName("应正确引用标识符")
        void shouldQuoteIdentifier() {
            assertThat(dialect.quoteIdentifier("name")).isEqualTo("\"name\"");
            assertThat(dialect.getIdentifierQuote()).isEqualTo("\"");
        }

        @Test
        @DisplayName("应返回正确的时间函数")
        void shouldGetCurrentTimeFunction() {
            assertThat(dialect.getCurrentTimeFunction()).isEqualTo("SYSDATE");
            assertThat(dialect.getCurrentDateFunction()).isEqualTo("SYSDATE");
            assertThat(dialect.getCurrentTimestampFunction()).isEqualTo("SYSTIMESTAMP");
        }

        @Test
        @DisplayName("应支持序列和自增")
        void shouldCheckSequenceSupport() {
            assertThat(dialect.supportsSequence()).isTrue();
            assertThat(dialect.supportsAutoIncrement()).isTrue();
        }

        @Test
        @DisplayName("应返回自增语法")
        void shouldReturnAutoIncrementSyntax() {
            assertThat(dialect.getAutoIncrementSyntax()).isEqualTo("GENERATED BY DEFAULT AS IDENTITY");
        }

        @Test
        @DisplayName("应生成序列查询 SQL")
        void shouldGetSequenceNextValue() {
            assertThat(dialect.getSequenceNextValueSql("user_seq")).isEqualTo("SELECT user_seq.NEXTVAL FROM DUAL");
        }

        @Test
        @DisplayName("应正确映射所有 Java 类型到 SQL 类型")
        void shouldGetSqlType() {
            // 基本类型
            assertThat(dialect.getSqlType(int.class)).isEqualTo("NUMBER(10)");
            assertThat(dialect.getSqlType(long.class)).isEqualTo("NUMBER(19)");
            assertThat(dialect.getSqlType(double.class)).isEqualTo("NUMBER(19,4)");
            assertThat(dialect.getSqlType(float.class)).isEqualTo("NUMBER(19,4)");
            assertThat(dialect.getSqlType(boolean.class)).isEqualTo("NUMBER(1)");

            // 包装类型
            assertThat(dialect.getSqlType(Integer.class)).isEqualTo("NUMBER(10)");
            assertThat(dialect.getSqlType(Long.class)).isEqualTo("NUMBER(19)");
            assertThat(dialect.getSqlType(Double.class)).isEqualTo("NUMBER(19,4)");
            assertThat(dialect.getSqlType(Float.class)).isEqualTo("NUMBER(19,4)");
            assertThat(dialect.getSqlType(Boolean.class)).isEqualTo("NUMBER(1)");

            // 时间类型
            assertThat(dialect.getSqlType(LocalDateTime.class)).isEqualTo("TIMESTAMP");
            assertThat(dialect.getSqlType(LocalDate.class)).isEqualTo("DATE");
            assertThat(dialect.getSqlType(LocalTime.class)).isEqualTo("TIMESTAMP");

            // 其他类型
            assertThat(dialect.getSqlType(String.class)).isEqualTo("VARCHAR2(255)");
            assertThat(dialect.getSqlType(BigDecimal.class)).isEqualTo("NUMBER(19,4)");
            assertThat(dialect.getSqlType(byte[].class)).isEqualTo("BLOB");
            assertThat(dialect.getSqlType(Object.class)).isEqualTo("VARCHAR2(255)");
        }

        @Test
        @DisplayName("应返回 LIKE 通配符")
        void shouldReturnLikeWildcard() {
            assertThat(dialect.getLikeWildcard()).isEqualTo("%");
        }

        @Test
        @DisplayName("应支持 FOR UPDATE")
        void shouldSupportForUpdate() {
            assertThat(dialect.supportsForUpdate()).isTrue();
            assertThat(dialect.getForUpdateSyntax()).isEqualTo("FOR UPDATE");
        }

        @Test
        @DisplayName("分页能力测试")
        void shouldCheckPaginationCapabilities() {
            assertThat(dialect.supportsLimitOffset()).isFalse();
            assertThat(dialect.supportsFetchFirst()).isTrue();
        }

        @Test
        @DisplayName("应返回 Oracle 数据库类型")
        void shouldReturnDatabaseType() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.ORACLE);
        }
    }

    // ==================== SQL Server 方言测试 ====================

    @Nested
    @DisplayName("SQLServerDialect 完整测试")
    class SQLServerDialectFullTests {

        private final SQLServerDialect dialect = new SQLServerDialect();

        @Test
        @DisplayName("应生成带 ORDER BY 的分页 SQL")
        void shouldGenerateSQLServerPagination() {
            String sql = "SELECT * FROM user ORDER BY id";
            String paged = dialect.getPaginationSql(sql, 20, 10);

            assertThat(paged).isEqualTo("SELECT * FROM user ORDER BY id OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY");
        }

        @Test
        @DisplayName("应生成不带 ORDER BY 的分页 SQL（包装子查询）")
        void shouldGenerateSQLServerPaginationWithoutOrderBy() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, 0, 10);

            assertThat(paged).isEqualTo("SELECT * FROM (SELECT * FROM user) AS _paged OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY");
        }

        @Test
        @DisplayName("应使用 PageRequest 生成分页 SQL")
        void shouldGenerateSQLServerPaginationWithPageRequest() {
            String sql = "SELECT * FROM user ORDER BY name";
            String paged = dialect.getPaginationSql(sql, PageRequest.of(3, 20));

            assertThat(paged).isEqualTo("SELECT * FROM user ORDER BY name OFFSET 40 ROWS FETCH NEXT 20 ROWS ONLY");
        }

        @Test
        @DisplayName("应正确引用标识符（处理右括号转义）")
        void shouldQuoteSQLServerIdentifier() {
            assertThat(dialect.quoteIdentifier("name")).isEqualTo("[name]");
            assertThat(dialect.quoteIdentifier("column]name")).isEqualTo("[column]]name]");
            assertThat(dialect.quoteIdentifier("]column")).isEqualTo("[]]column]");
            assertThat(dialect.getIdentifierQuote()).isEqualTo("[");
        }

        @Test
        @DisplayName("应返回正确的时间函数")
        void shouldGetSQLServerTimeFunction() {
            assertThat(dialect.getCurrentTimeFunction()).isEqualTo("CONVERT(TIME, GETDATE())");
            assertThat(dialect.getCurrentDateFunction()).isEqualTo("CONVERT(DATE, GETDATE())");
            assertThat(dialect.getCurrentTimestampFunction()).isEqualTo("GETDATE()");
        }

        @Test
        @DisplayName("应支持序列和自增")
        void shouldCheckSQLServerSequenceSupport() {
            assertThat(dialect.supportsSequence()).isTrue();
            assertThat(dialect.supportsAutoIncrement()).isTrue();
        }

        @Test
        @DisplayName("应返回自增语法")
        void shouldReturnAutoIncrementSyntax() {
            assertThat(dialect.getAutoIncrementSyntax()).isEqualTo("IDENTITY(1,1)");
        }

        @Test
        @DisplayName("应生成序列查询 SQL")
        void shouldGetSQLServerSequenceNextValue() {
            assertThat(dialect.getSequenceNextValueSql("user_seq")).isEqualTo("SELECT NEXT VALUE FOR [user_seq]");
        }

        @Test
        @DisplayName("应正确映射所有 Java 类型到 SQL 类型")
        void shouldGetSQLServerSqlType() {
            // 基本类型
            assertThat(dialect.getSqlType(int.class)).isEqualTo("INT");
            assertThat(dialect.getSqlType(long.class)).isEqualTo("BIGINT");
            assertThat(dialect.getSqlType(double.class)).isEqualTo("FLOAT");
            assertThat(dialect.getSqlType(float.class)).isEqualTo("REAL");
            assertThat(dialect.getSqlType(boolean.class)).isEqualTo("BIT");

            // 包装类型
            assertThat(dialect.getSqlType(Integer.class)).isEqualTo("INT");
            assertThat(dialect.getSqlType(Long.class)).isEqualTo("BIGINT");
            assertThat(dialect.getSqlType(Double.class)).isEqualTo("FLOAT");
            assertThat(dialect.getSqlType(Float.class)).isEqualTo("REAL");
            assertThat(dialect.getSqlType(Boolean.class)).isEqualTo("BIT");

            // 时间类型
            assertThat(dialect.getSqlType(LocalDateTime.class)).isEqualTo("DATETIME2");
            assertThat(dialect.getSqlType(LocalDate.class)).isEqualTo("DATE");
            assertThat(dialect.getSqlType(LocalTime.class)).isEqualTo("TIME");

            // 其他类型
            assertThat(dialect.getSqlType(String.class)).isEqualTo("NVARCHAR(255)");
            assertThat(dialect.getSqlType(BigDecimal.class)).isEqualTo("DECIMAL(19,4)");
            assertThat(dialect.getSqlType(byte[].class)).isEqualTo("VARBINARY(MAX)");
            assertThat(dialect.getSqlType(Object.class)).isEqualTo("NVARCHAR(255)");
        }

        @Test
        @DisplayName("应返回 LIKE 通配符")
        void shouldReturnLikeWildcard() {
            assertThat(dialect.getLikeWildcard()).isEqualTo("%");
        }

        @Test
        @DisplayName("应支持 FOR UPDATE")
        void shouldGetSQLServerForUpdateSyntax() {
            assertThat(dialect.supportsForUpdate()).isTrue();
            assertThat(dialect.getForUpdateSyntax()).isEqualTo("WITH (UPDLOCK, ROWLOCK)");
        }

        @Test
        @DisplayName("分页能力测试")
        void shouldSQLServerSupportFetchFirst() {
            assertThat(dialect.supportsFetchFirst()).isTrue();
            assertThat(dialect.supportsLimitOffset()).isFalse();
        }

        @Test
        @DisplayName("应返回 SQL Server 数据库类型")
        void shouldReturnDatabaseType() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.SQLSERVER);
        }
    }

    // ==================== 达梦（DM）方言测试 ====================

    @Nested
    @DisplayName("DmDialect 完整测试")
    class DmDialectFullTests {

        private final DmDialect dialect = new DmDialect();

        @Test
        @DisplayName("应返回达梦数据库类型")
        void shouldDmExtendOracleLike() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.DM);
            assertThat(dialect.supportsFetchFirst()).isTrue();
            assertThat(dialect.supportsSequence()).isTrue();
            assertThat(dialect.supportsAutoIncrement()).isTrue();
            assertThat(dialect.quoteIdentifier("name")).isEqualTo("\"name\"");
        }

        @Test
        @DisplayName("应生成达梦分页 SQL")
        void shouldGenerateDmPagination() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, 20, 10);

            assertThat(paged).isEqualTo("SELECT * FROM (SELECT * FROM user) OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY");
        }

        @Test
        @DisplayName("应使用 PageRequest 生成分页 SQL")
        void shouldGenerateDmPaginationWithPageRequest() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, PageRequest.of(3, 20));

            assertThat(paged).isEqualTo("SELECT * FROM (SELECT * FROM user) OFFSET 40 ROWS FETCH NEXT 20 ROWS ONLY");
        }

        @Test
        @DisplayName("应返回正确的时间函数")
        void shouldGetDmTimeFunction() {
            assertThat(dialect.getCurrentTimeFunction()).isEqualTo("SYSDATE");
            assertThat(dialect.getCurrentDateFunction()).isEqualTo("SYSDATE");
            assertThat(dialect.getCurrentTimestampFunction()).isEqualTo("SYSTIMESTAMP");
        }

        @Test
        @DisplayName("应返回自增语法")
        void shouldReturnAutoIncrementSyntax() {
            assertThat(dialect.getAutoIncrementSyntax()).isEqualTo("IDENTITY");
        }

        @Test
        @DisplayName("应生成序列查询 SQL")
        void shouldGetDmSequenceNextValue() {
            assertThat(dialect.getSequenceNextValueSql("user_seq")).isEqualTo("SELECT user_seq.NEXTVAL FROM DUAL");
        }

        @Test
        @DisplayName("应正确映射所有 Java 类型到 SQL 类型")
        void shouldGetDmSqlType() {
            // 基本类型
            assertThat(dialect.getSqlType(int.class)).isEqualTo("INTEGER");
            assertThat(dialect.getSqlType(long.class)).isEqualTo("BIGINT");
            assertThat(dialect.getSqlType(double.class)).isEqualTo("DOUBLE");
            assertThat(dialect.getSqlType(float.class)).isEqualTo("FLOAT");
            assertThat(dialect.getSqlType(boolean.class)).isEqualTo("BIT");

            // 包装类型
            assertThat(dialect.getSqlType(Integer.class)).isEqualTo("INTEGER");
            assertThat(dialect.getSqlType(Long.class)).isEqualTo("BIGINT");
            assertThat(dialect.getSqlType(Double.class)).isEqualTo("DOUBLE");
            assertThat(dialect.getSqlType(Float.class)).isEqualTo("FLOAT");
            assertThat(dialect.getSqlType(Boolean.class)).isEqualTo("BIT");

            // 时间类型
            assertThat(dialect.getSqlType(LocalDateTime.class)).isEqualTo("TIMESTAMP");
            assertThat(dialect.getSqlType(LocalDate.class)).isEqualTo("DATE");
            assertThat(dialect.getSqlType(LocalTime.class)).isEqualTo("TIME");

            // 其他类型
            assertThat(dialect.getSqlType(String.class)).isEqualTo("VARCHAR2(255)");
            assertThat(dialect.getSqlType(BigDecimal.class)).isEqualTo("DECIMAL(19,4)");
            assertThat(dialect.getSqlType(byte[].class)).isEqualTo("BLOB");
            assertThat(dialect.getSqlType(Object.class)).isEqualTo("VARCHAR2(255)");
        }

        @Test
        @DisplayName("应返回 LIKE 通配符")
        void shouldReturnLikeWildcard() {
            assertThat(dialect.getLikeWildcard()).isEqualTo("%");
        }

        @Test
        @DisplayName("应支持 FOR UPDATE")
        void shouldSupportForUpdate() {
            assertThat(dialect.supportsForUpdate()).isTrue();
            assertThat(dialect.getForUpdateSyntax()).isEqualTo("FOR UPDATE");
        }

        @Test
        @DisplayName("分页能力测试")
        void shouldCheckPaginationCapabilities() {
            assertThat(dialect.supportsLimitOffset()).isFalse();
            assertThat(dialect.supportsFetchFirst()).isTrue();
        }

        @Test
        @DisplayName("应返回标识符引用字符")
        void shouldReturnIdentifierQuote() {
            assertThat(dialect.getIdentifierQuote()).isEqualTo("\"");
        }
    }

    // ==================== 金仓（Kingbase）方言测试 ====================

    @Nested
    @DisplayName("KingbaseDialect 完整测试")
    class KingbaseDialectFullTests {

        private final KingbaseDialect dialect = new KingbaseDialect();

        @Test
        @DisplayName("应继承 PostgreSQL 特性")
        void shouldKingbaseExtendPostgreSQL() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.KINGBASE);
            assertThat(dialect.supportsLimitOffset()).isTrue();
            assertThat(dialect.supportsSequence()).isTrue();
            assertThat(dialect.quoteIdentifier("name")).isEqualTo("\"name\"");
        }

        @Test
        @DisplayName("应生成金仓分页 SQL")
        void shouldGenerateKingbasePagination() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, 20, 10);

            assertThat(paged).isEqualTo("SELECT * FROM user LIMIT 10 OFFSET 20");
        }

        @Test
        @DisplayName("应使用 PageRequest 生成分页 SQL")
        void shouldGenerateKingbasePaginationWithPageRequest() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, PageRequest.of(2, 15));

            assertThat(paged).isEqualTo("SELECT * FROM user LIMIT 15 OFFSET 15");
        }

        @Test
        @DisplayName("应返回正确的时间函数")
        void shouldGetKingbaseTimeFunction() {
            assertThat(dialect.getCurrentTimeFunction()).isEqualTo("CURRENT_TIME");
            assertThat(dialect.getCurrentTimestampFunction()).isEqualTo("CURRENT_TIMESTAMP");
        }

        @Test
        @DisplayName("应正确映射所有 Java 类型到 SQL 类型")
        void shouldGetKingbaseSqlType() {
            // 基本类型
            assertThat(dialect.getSqlType(int.class)).isEqualTo("INTEGER");
            assertThat(dialect.getSqlType(long.class)).isEqualTo("BIGINT");
            assertThat(dialect.getSqlType(boolean.class)).isEqualTo("BOOLEAN");

            // 包装类型
            assertThat(dialect.getSqlType(Integer.class)).isEqualTo("INTEGER");
            assertThat(dialect.getSqlType(Long.class)).isEqualTo("BIGINT");
            assertThat(dialect.getSqlType(Boolean.class)).isEqualTo("BOOLEAN");

            // 其他类型
            assertThat(dialect.getSqlType(String.class)).isEqualTo("VARCHAR(255)");
            assertThat(dialect.getSqlType(LocalDateTime.class)).isEqualTo("TIMESTAMP");
            assertThat(dialect.getSqlType(LocalDate.class)).isEqualTo("DATE");
            assertThat(dialect.getSqlType(LocalTime.class)).isEqualTo("TIME");
            assertThat(dialect.getSqlType(BigDecimal.class)).isEqualTo("NUMERIC(19,4)");
            assertThat(dialect.getSqlType(byte[].class)).isEqualTo("BYTEA");
        }

        @Test
        @DisplayName("应支持 FOR UPDATE")
        void shouldSupportForUpdate() {
            assertThat(dialect.supportsForUpdate()).isTrue();
            assertThat(dialect.getForUpdateSyntax()).isEqualTo("FOR UPDATE");
        }

        @Test
        @DisplayName("分页能力测试")
        void shouldCheckPaginationCapabilities() {
            assertThat(dialect.supportsLimitOffset()).isTrue();
            assertThat(dialect.supportsFetchFirst()).isTrue();
        }
    }

    // ==================== GaussDB 方言测试 ====================

    @Nested
    @DisplayName("GaussDBDialect 完整测试")
    class GaussDBDialectFullTests {

        private final GaussDBDialect dialect = new GaussDBDialect();

        @Test
        @DisplayName("应继承 PostgreSQL 特性")
        void shouldGaussDBExtendPostgreSQL() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.GAUSSDB);
            assertThat(dialect.supportsLimitOffset()).isTrue();
            assertThat(dialect.supportsSequence()).isTrue();
            assertThat(dialect.quoteIdentifier("name")).isEqualTo("\"name\"");
        }

        @Test
        @DisplayName("应生成 GaussDB 分页 SQL")
        void shouldGenerateGaussDBPagination() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, 20, 10);

            assertThat(paged).isEqualTo("SELECT * FROM user LIMIT 10 OFFSET 20");
        }

        @Test
        @DisplayName("应使用 PageRequest 生成分页 SQL")
        void shouldGenerateGaussDBPaginationWithPageRequest() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, PageRequest.of(5, 25));

            assertThat(paged).isEqualTo("SELECT * FROM user LIMIT 25 OFFSET 100");
        }

        @Test
        @DisplayName("应正确映射所有 Java 类型到 SQL 类型")
        void shouldGetGaussDBSqlType() {
            // GaussDB 支持 Oracle 兼容类型
            // 基本类型
            assertThat(dialect.getSqlType(int.class)).isEqualTo("INTEGER");
            assertThat(dialect.getSqlType(long.class)).isEqualTo("BIGINT");
            assertThat(dialect.getSqlType(double.class)).isEqualTo("DOUBLE PRECISION");
            assertThat(dialect.getSqlType(float.class)).isEqualTo("REAL");
            assertThat(dialect.getSqlType(boolean.class)).isEqualTo("BOOLEAN");

            // 包装类型
            assertThat(dialect.getSqlType(Integer.class)).isEqualTo("INTEGER");
            assertThat(dialect.getSqlType(Long.class)).isEqualTo("BIGINT");
            assertThat(dialect.getSqlType(Double.class)).isEqualTo("DOUBLE PRECISION");
            assertThat(dialect.getSqlType(Float.class)).isEqualTo("REAL");
            assertThat(dialect.getSqlType(Boolean.class)).isEqualTo("BOOLEAN");

            // 时间类型
            assertThat(dialect.getSqlType(LocalDateTime.class)).isEqualTo("TIMESTAMP");
            assertThat(dialect.getSqlType(LocalDate.class)).isEqualTo("DATE");
            assertThat(dialect.getSqlType(LocalTime.class)).isEqualTo("TIME");

            // 其他类型
            assertThat(dialect.getSqlType(String.class)).isEqualTo("VARCHAR2(255)");
            assertThat(dialect.getSqlType(BigDecimal.class)).isEqualTo("NUMERIC(19,4)");
            assertThat(dialect.getSqlType(byte[].class)).isEqualTo("BYTEA");
            assertThat(dialect.getSqlType(Object.class)).isEqualTo("VARCHAR2(255)");
        }

        @Test
        @DisplayName("应返回正确的时间函数")
        void shouldGetGaussDBTimeFunction() {
            assertThat(dialect.getCurrentTimeFunction()).isEqualTo("CURRENT_TIME");
            assertThat(dialect.getCurrentDateFunction()).isEqualTo("CURRENT_DATE");
            assertThat(dialect.getCurrentTimestampFunction()).isEqualTo("CURRENT_TIMESTAMP");
        }

        @Test
        @DisplayName("应支持 FOR UPDATE")
        void shouldSupportForUpdate() {
            assertThat(dialect.supportsForUpdate()).isTrue();
            assertThat(dialect.getForUpdateSyntax()).isEqualTo("FOR UPDATE");
        }

        @Test
        @DisplayName("分页能力测试")
        void shouldCheckPaginationCapabilities() {
            assertThat(dialect.supportsLimitOffset()).isTrue();
            assertThat(dialect.supportsFetchFirst()).isTrue();
        }
    }

    // ==================== OceanBase 方言测试 ====================

    @Nested
    @DisplayName("OceanBaseDialect 完整测试")
    class OceanBaseDialectFullTests {

        private final OceanBaseDialect dialect = new OceanBaseDialect();

        @Test
        @DisplayName("应继承 MySQL 特性")
        void shouldOceanBaseExtendMySQL() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.OCEANBASE);
            assertThat(dialect.supportsLimitOffset()).isTrue();
            assertThat(dialect.quoteIdentifier("name")).isEqualTo("`name`");
        }

        @Test
        @DisplayName("应生成 OceanBase 分页 SQL")
        void shouldGenerateOceanBasePagination() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, 20, 10);

            assertThat(paged).isEqualTo("SELECT * FROM user LIMIT 10 OFFSET 20");
        }

        @Test
        @DisplayName("应使用 PageRequest 生成分页 SQL")
        void shouldGeneratePaginationWithPageRequest() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, PageRequest.of(2, 15));

            assertThat(paged).isEqualTo("SELECT * FROM user LIMIT 15 OFFSET 15");
        }

        @Test
        @DisplayName("应返回正确的时间函数")
        void shouldGetTimeFunction() {
            assertThat(dialect.getCurrentTimeFunction()).isEqualTo("NOW()");
            assertThat(dialect.getCurrentTimestampFunction()).isEqualTo("NOW(6)");
            // 继承自 MySQL
            assertThat(dialect.getCurrentDateFunction()).isEqualTo("CURDATE()");
        }

        @Test
        @DisplayName("应支持序列和自增")
        void shouldSupportSequenceAndAutoIncrement() {
            assertThat(dialect.supportsSequence()).isTrue();
            assertThat(dialect.supportsAutoIncrement()).isTrue();
        }

        @Test
        @DisplayName("应返回自增语法")
        void shouldReturnAutoIncrementSyntax() {
            assertThat(dialect.getAutoIncrementSyntax()).isEqualTo("AUTO_INCREMENT");
        }

        @Test
        @DisplayName("应支持 FOR UPDATE")
        void shouldSupportForUpdate() {
            assertThat(dialect.supportsForUpdate()).isTrue();
            assertThat(dialect.getForUpdateSyntax()).isEqualTo("FOR UPDATE");
        }

        @Test
        @DisplayName("分页能力测试")
        void shouldCheckPaginationCapabilities() {
            assertThat(dialect.supportsLimitOffset()).isTrue();
            assertThat(dialect.supportsFetchFirst()).isFalse();
        }
    }

    // ==================== openGauss 方言测试 ====================

    @Nested
    @DisplayName("OpenGaussDialect 完整测试")
    class OpenGaussDialectFullTests {

        private final OpenGaussDialect dialect = new OpenGaussDialect();

        @Test
        @DisplayName("应继承 PostgreSQL 特性")
        void shouldOpenGaussExtendPostgreSQL() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.OPENGAUSS);
            assertThat(dialect.supportsSequence()).isTrue();
            assertThat(dialect.quoteIdentifier("name")).isEqualTo("\"name\"");
        }

        @Test
        @DisplayName("应生成 openGauss 分页 SQL")
        void shouldGenerateOpenGaussPagination() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, 20, 10);

            assertThat(paged).isEqualTo("SELECT * FROM user LIMIT 10 OFFSET 20");
        }

        @Test
        @DisplayName("应使用 PageRequest 生成分页 SQL")
        void shouldGeneratePaginationWithPageRequest() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, PageRequest.of(3, 20));

            assertThat(paged).isEqualTo("SELECT * FROM user LIMIT 20 OFFSET 40");
        }

        @Test
        @DisplayName("应返回正确的时间函数")
        void shouldGetTimeFunction() {
            assertThat(dialect.getCurrentTimeFunction()).isEqualTo("CURRENT_TIME");
            assertThat(dialect.getCurrentTimestampFunction()).isEqualTo("CURRENT_TIMESTAMP");
            // 继承自 PostgreSQL
            assertThat(dialect.getCurrentDateFunction()).isEqualTo("CURRENT_DATE");
        }

        @Test
        @DisplayName("应正确映射所有 Java 类型到 SQL 类型")
        void shouldGetSqlType() {
            // 基本类型
            assertThat(dialect.getSqlType(int.class)).isEqualTo("INTEGER");
            assertThat(dialect.getSqlType(long.class)).isEqualTo("BIGINT");
            assertThat(dialect.getSqlType(boolean.class)).isEqualTo("BOOLEAN");

            // 包装类型
            assertThat(dialect.getSqlType(Integer.class)).isEqualTo("INTEGER");
            assertThat(dialect.getSqlType(Long.class)).isEqualTo("BIGINT");
            assertThat(dialect.getSqlType(Boolean.class)).isEqualTo("BOOLEAN");

            // 其他类型
            assertThat(dialect.getSqlType(String.class)).isEqualTo("VARCHAR(255)");
            assertThat(dialect.getSqlType(LocalDateTime.class)).isEqualTo("TIMESTAMP");
            assertThat(dialect.getSqlType(LocalDate.class)).isEqualTo("DATE");
            assertThat(dialect.getSqlType(LocalTime.class)).isEqualTo("TIME");
            assertThat(dialect.getSqlType(BigDecimal.class)).isEqualTo("NUMERIC(19,4)");
            assertThat(dialect.getSqlType(byte[].class)).isEqualTo("BYTEA");
        }

        @Test
        @DisplayName("应支持 FOR UPDATE")
        void shouldSupportForUpdate() {
            assertThat(dialect.supportsForUpdate()).isTrue();
            assertThat(dialect.getForUpdateSyntax()).isEqualTo("FOR UPDATE");
        }

        @Test
        @DisplayName("分页能力测试")
        void shouldCheckPaginationCapabilities() {
            assertThat(dialect.supportsLimitOffset()).isTrue();
            assertThat(dialect.supportsFetchFirst()).isTrue();
        }
    }

    // ==================== DatabaseType 枚举测试 ====================

    @Nested
    @DisplayName("DatabaseType 枚举完整测试")
    class DatabaseTypeFullTests {

        @Test
        @DisplayName("应根据代码获取数据库类型")
        void shouldGetByCode() {
            assertThat(DatabaseType.fromCode("mysql")).isEqualTo(DatabaseType.MYSQL);
            assertThat(DatabaseType.fromCode("MYSQL")).isEqualTo(DatabaseType.MYSQL);
            assertThat(DatabaseType.fromCode("postgresql")).isEqualTo(DatabaseType.POSTGRESQL);
            assertThat(DatabaseType.fromCode("POSTGRESQL")).isEqualTo(DatabaseType.POSTGRESQL);
            assertThat(DatabaseType.fromCode("oracle")).isEqualTo(DatabaseType.ORACLE);
            assertThat(DatabaseType.fromCode("sqlserver")).isEqualTo(DatabaseType.SQLSERVER);
            assertThat(DatabaseType.fromCode("sqlite")).isEqualTo(DatabaseType.SQLITE);
            assertThat(DatabaseType.fromCode("h2")).isEqualTo(DatabaseType.H2);
            assertThat(DatabaseType.fromCode("oceanbase")).isEqualTo(DatabaseType.OCEANBASE);
            assertThat(DatabaseType.fromCode("opengauss")).isEqualTo(DatabaseType.OPENGAUSS);
            assertThat(DatabaseType.fromCode("dm")).isEqualTo(DatabaseType.DM);
            assertThat(DatabaseType.fromCode("kingbase")).isEqualTo(DatabaseType.KINGBASE);
            assertThat(DatabaseType.fromCode("gaussdb")).isEqualTo(DatabaseType.GAUSSDB);
            assertThat(DatabaseType.fromCode("unknown")).isEqualTo(DatabaseType.UNKNOWN);
        }

        @Test
        @DisplayName("null 代码应返回 UNKNOWN")
        void shouldReturnUnknownForNullCode() {
            assertThat(DatabaseType.fromCode(null)).isEqualTo(DatabaseType.UNKNOWN);
        }

        @Test
        @DisplayName("无效代码应返回 UNKNOWN")
        void shouldReturnUnknownForInvalidCode() {
            assertThat(DatabaseType.fromCode("invalid")).isEqualTo(DatabaseType.UNKNOWN);
            assertThat(DatabaseType.fromCode("")).isEqualTo(DatabaseType.UNKNOWN);
            assertThat(DatabaseType.fromCode("nosql")).isEqualTo(DatabaseType.UNKNOWN);
        }

        @Test
        @DisplayName("应检查 MySQL 系列")
        void shouldCheckMySQLFamily() {
            assertThat(DatabaseType.MYSQL.isMySQLFamily()).isTrue();
            assertThat(DatabaseType.OCEANBASE.isMySQLFamily()).isTrue();
            assertThat(DatabaseType.POSTGRESQL.isMySQLFamily()).isFalse();
            assertThat(DatabaseType.ORACLE.isMySQLFamily()).isFalse();
            assertThat(DatabaseType.DM.isMySQLFamily()).isFalse();
        }

        @Test
        @DisplayName("应检查 PostgreSQL 系列")
        void shouldCheckPostgreSQLFamily() {
            assertThat(DatabaseType.POSTGRESQL.isPostgreSQLFamily()).isTrue();
            assertThat(DatabaseType.OPENGAUSS.isPostgreSQLFamily()).isTrue();
            assertThat(DatabaseType.GAUSSDB.isPostgreSQLFamily()).isTrue();
            assertThat(DatabaseType.MYSQL.isPostgreSQLFamily()).isFalse();
            assertThat(DatabaseType.DM.isPostgreSQLFamily()).isFalse();
            assertThat(DatabaseType.KINGBASE.isPostgreSQLFamily()).isFalse();
        }

        @Test
        @DisplayName("应检查国产数据库")
        void shouldChineseDatabaseFlagsWork() {
            assertThat(DatabaseType.DM.isChineseDatabase()).isTrue();
            assertThat(DatabaseType.KINGBASE.isChineseDatabase()).isTrue();
            assertThat(DatabaseType.GAUSSDB.isChineseDatabase()).isTrue();
            assertThat(DatabaseType.OCEANBASE.isChineseDatabase()).isTrue();
            assertThat(DatabaseType.OPENGAUSS.isChineseDatabase()).isTrue();
            assertThat(DatabaseType.MYSQL.isChineseDatabase()).isFalse();
            assertThat(DatabaseType.POSTGRESQL.isChineseDatabase()).isFalse();
            assertThat(DatabaseType.ORACLE.isChineseDatabase()).isFalse();
            assertThat(DatabaseType.SQLSERVER.isChineseDatabase()).isFalse();
            assertThat(DatabaseType.H2.isChineseDatabase()).isFalse();
            assertThat(DatabaseType.SQLITE.isChineseDatabase()).isFalse();
            assertThat(DatabaseType.UNKNOWN.isChineseDatabase()).isFalse();
        }

        @Test
        @DisplayName("应返回正确的代码和名称")
        void shouldReturnCodeAndName() {
            assertThat(DatabaseType.MYSQL.getCode()).isEqualTo("mysql");
            assertThat(DatabaseType.MYSQL.getName()).isEqualTo("MySQL");
            assertThat(DatabaseType.POSTGRESQL.getCode()).isEqualTo("postgresql");
            assertThat(DatabaseType.POSTGRESQL.getName()).isEqualTo("PostgreSQL");
            assertThat(DatabaseType.DM.getCode()).isEqualTo("dm");
            assertThat(DatabaseType.DM.getName()).isEqualTo("DM Database");
            assertThat(DatabaseType.KINGBASE.getCode()).isEqualTo("kingbase");
            assertThat(DatabaseType.KINGBASE.getName()).isEqualTo("KingbaseES");
        }
    }
}
