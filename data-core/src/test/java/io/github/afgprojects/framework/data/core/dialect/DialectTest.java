package io.github.afgprojects.framework.data.core.dialect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Dialect 测试
 */
@DisplayName("Dialect 测试")
class DialectTest {

    @Nested
    @DisplayName("MySQLDialect")
    class MySQLDialectTests {

        private final MySQLDialect dialect = new MySQLDialect();

        @Test
        @DisplayName("getDatabaseType 应返回 MYSQL")
        void shouldReturnMysql_whenGetDatabaseType() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.MYSQL);
        }

        @Test
        @DisplayName("quoteIdentifier 应使用反引号")
        void shouldUseBacktick_whenQuoteIdentifier() {
            assertThat(dialect.quoteIdentifier("name")).isEqualTo("`name`");
        }

        @Test
        @DisplayName("getIdentifierQuote 应返回反引号")
        void shouldReturnBacktick_whenGetIdentifierQuote() {
            assertThat(dialect.getIdentifierQuote()).isEqualTo("`");
        }

        @Test
        @DisplayName("分页 SQL 应使用 LIMIT OFFSET")
        void shouldUseLimitOffset_whenPagination() {
            String result = dialect.getPaginationSql("SELECT * FROM user", 10, 20);
            assertThat(result).isEqualTo("SELECT * FROM user LIMIT 20 OFFSET 10");
        }

        @Test
        @DisplayName("getCurrentTimeFunction 应返回 NOW()")
        void shouldReturnNow_whenGetCurrentTime() {
            assertThat(dialect.getCurrentTimeFunction()).isEqualTo("NOW()");
        }

        @Test
        @DisplayName("getCurrentTimestampFunction 应返回 NOW()")
        void shouldReturnNow_whenGetCurrentTimestamp() {
            assertThat(dialect.getCurrentTimestampFunction()).isEqualTo("NOW()");
        }

        @Test
        @DisplayName("supportsLimitOffset 应返回 true")
        void shouldReturnTrue_whenSupportsLimitOffset() {
            assertThat(dialect.supportsLimitOffset()).isTrue();
        }

        @Test
        @DisplayName("JSON contains 应使用 JSON_CONTAINS 函数")
        void shouldUseJsonContains_whenGetJsonContainsExpression() {
            assertThat(dialect.getJsonContainsExpression("col"))
                    .isEqualTo("JSON_CONTAINS(col, ?)");
        }

        @Test
        @DisplayName("JSON contained 应使用反向 JSON_CONTAINS")
        void shouldUseReverseJsonContains_whenGetJsonContainedExpression() {
            assertThat(dialect.getJsonContainedExpression("col"))
                    .isEqualTo("JSON_CONTAINS(?, col)");
        }

        @Test
        @DisplayName("JSON path 应使用 JSON_EXTRACT")
        void shouldUseJsonExtract_whenGetJsonPathExpression() {
            assertThat(dialect.getJsonPathExpression("col"))
                    .isEqualTo("JSON_EXTRACT(col, ?) IS NOT NULL");
        }
    }

    @Nested
    @DisplayName("PostgreSQLDialect")
    class PostgreSQLDialectTests {

        private final PostgreSQLDialect dialect = new PostgreSQLDialect();

        @Test
        @DisplayName("getDatabaseType 应返回 POSTGRESQL")
        void shouldReturnPostgresql_whenGetDatabaseType() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.POSTGRESQL);
        }

        @Test
        @DisplayName("quoteIdentifier 应使用双引号")
        void shouldUseDoubleQuote_whenQuoteIdentifier() {
            assertThat(dialect.quoteIdentifier("name")).isEqualTo("\"name\"");
        }

        @Test
        @DisplayName("分页 SQL 应使用 LIMIT OFFSET")
        void shouldUseLimitOffset_whenPagination() {
            String result = dialect.getPaginationSql("SELECT * FROM user", 10, 20);
            assertThat(result).isEqualTo("SELECT * FROM user LIMIT 20 OFFSET 10");
        }

        @Test
        @DisplayName("supportsFetchFirst 应返回 true")
        void shouldReturnTrue_whenSupportsFetchFirst() {
            assertThat(dialect.supportsFetchFirst()).isTrue();
        }

        @Test
        @DisplayName("getAutoIncrementSyntax 应返回 GENERATED ALWAYS AS IDENTITY")
        void shouldReturnGeneratedAlwaysAsIdentity_whenGetAutoIncrement() {
            assertThat(dialect.getAutoIncrementSyntax()).isEqualTo("GENERATED ALWAYS AS IDENTITY");
        }
    }

    @Nested
    @DisplayName("OracleDialect")
    class OracleDialectTests {

        private final OracleDialect dialect = new OracleDialect();

        @Test
        @DisplayName("getDatabaseType 应返回 ORACLE")
        void shouldReturnOracle_whenGetDatabaseType() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.ORACLE);
        }

        @Test
        @DisplayName("分页 SQL 应使用 OFFSET...FETCH")
        void shouldUseOffsetFetch_whenPagination() {
            String result = dialect.getPaginationSql("SELECT * FROM user", 10, 20);
            assertThat(result).isEqualTo("SELECT * FROM (SELECT * FROM user) OFFSET 10 ROWS FETCH NEXT 20 ROWS ONLY");
        }

        @Test
        @DisplayName("supportsLimitOffset 应返回 false")
        void shouldReturnFalse_whenSupportsLimitOffset() {
            assertThat(dialect.supportsLimitOffset()).isFalse();
        }

        @Test
        @DisplayName("supportsFetchFirst 应返回 true")
        void shouldReturnTrue_whenSupportsFetchFirst() {
            assertThat(dialect.supportsFetchFirst()).isTrue();
        }

        @Test
        @DisplayName("getCurrentTimeFunction 应返回 SYSDATE")
        void shouldReturnSysdate_whenGetCurrentTime() {
            assertThat(dialect.getCurrentTimeFunction()).isEqualTo("SYSDATE");
        }

        @Test
        @DisplayName("getCurrentDateFunction 应返回 SYSDATE")
        void shouldReturnSysdate_whenGetCurrentDate() {
            assertThat(dialect.getCurrentDateFunction()).isEqualTo("SYSDATE");
        }

        @Test
        @DisplayName("getCurrentTimestampFunction 应返回 SYSTIMESTAMP")
        void shouldReturnSystimestamp_whenGetCurrentTimestamp() {
            assertThat(dialect.getCurrentTimestampFunction()).isEqualTo("SYSTIMESTAMP");
        }

        @Test
        @DisplayName("getAutoIncrementSyntax 应返回 GENERATED BY DEFAULT AS IDENTITY")
        void shouldReturnGeneratedByDefault_whenGetAutoIncrement() {
            assertThat(dialect.getAutoIncrementSyntax()).isEqualTo("GENERATED BY DEFAULT AS IDENTITY");
        }
    }

    @Nested
    @DisplayName("SQLServerDialect")
    class SQLServerDialectTests {

        private final SQLServerDialect dialect = new SQLServerDialect();

        @Test
        @DisplayName("getDatabaseType 应返回 SQLSERVER")
        void shouldReturnSqlServer_whenGetDatabaseType() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.SQLSERVER);
        }

        @Test
        @DisplayName("quoteIdentifier 应使用方括号")
        void shouldUseSquareBrackets_whenQuoteIdentifier() {
            assertThat(dialect.quoteIdentifier("name")).isEqualTo("[name]");
        }

        @Test
        @DisplayName("quoteIdentifier 应转义方括号")
        void shouldEscapeBrackets_whenQuoteIdentifier() {
            assertThat(dialect.quoteIdentifier("na]me")).isEqualTo("[na]]me]");
        }

        @Test
        @DisplayName("分页 SQL 无 ORDER BY 时应包装子查询")
        void shouldWrapSubquery_whenPaginationWithoutOrderBy() {
            String result = dialect.getPaginationSql("SELECT * FROM user", 10, 20);
            assertThat(result).contains("AS _paged").contains("OFFSET 10 ROWS").contains("FETCH NEXT 20 ROWS ONLY");
        }

        @Test
        @DisplayName("分页 SQL 有 ORDER BY 时应直接追加")
        void shouldAppendDirectly_whenPaginationWithOrderBy() {
            String result = dialect.getPaginationSql("SELECT * FROM user ORDER BY id", 10, 20);
            assertThat(result).isEqualTo("SELECT * FROM user ORDER BY id OFFSET 10 ROWS FETCH NEXT 20 ROWS ONLY");
        }

        @Test
        @DisplayName("getCurrentTimeFunction 应返回 CONVERT(TIME, GETDATE())")
        void shouldReturnConvertTimeGetdate_whenGetCurrentTime() {
            assertThat(dialect.getCurrentTimeFunction()).isEqualTo("CONVERT(TIME, GETDATE())");
        }

        @Test
        @DisplayName("getCurrentTimestampFunction 应返回 GETDATE()")
        void shouldReturnGetdate_whenGetCurrentTimestamp() {
            assertThat(dialect.getCurrentTimestampFunction()).isEqualTo("GETDATE()");
        }

        @Test
        @DisplayName("getForUpdateSyntax 应返回 WITH (UPDLOCK, ROWLOCK)")
        void shouldReturnWithUpdlock_whenGetForUpdate() {
            assertThat(dialect.getForUpdateSyntax()).isEqualTo("WITH (UPDLOCK, ROWLOCK)");
        }
    }

    @Nested
    @DisplayName("H2Dialect")
    class H2DialectTests {

        private final H2Dialect dialect = new H2Dialect();

        @Test
        @DisplayName("getDatabaseType 应返回 H2")
        void shouldReturnH2_whenGetDatabaseType() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.H2);
        }

        @Test
        @DisplayName("quoteIdentifier 应使用双引号")
        void shouldUseDoubleQuote_whenQuoteIdentifier() {
            assertThat(dialect.quoteIdentifier("name")).isEqualTo("\"name\"");
        }

        @Test
        @DisplayName("分页 SQL 应使用 LIMIT OFFSET")
        void shouldUseLimitOffset_whenPagination() {
            String result = dialect.getPaginationSql("SELECT * FROM user", 10, 20);
            assertThat(result).isEqualTo("SELECT * FROM user LIMIT 20 OFFSET 10");
        }

        @Test
        @DisplayName("getSequenceNextValueSql 应使用 NEXT VALUE FOR")
        void shouldUseNextValueFor_whenGetSequence() {
            assertThat(dialect.getSequenceNextValueSql("seq_name"))
                    .isEqualTo("NEXT VALUE FOR \"seq_name\"");
        }
    }

    @Nested
    @DisplayName("DmDialect")
    class DmDialectTests {

        private final DmDialect dialect = new DmDialect();

        @Test
        @DisplayName("getDatabaseType 应返回 DM")
        void shouldReturnDm_whenGetDatabaseType() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.DM);
        }

        @Test
        @DisplayName("getAutoIncrementSyntax 应返回 IDENTITY")
        void shouldReturnIdentity_whenGetAutoIncrement() {
            assertThat(dialect.getAutoIncrementSyntax()).isEqualTo("IDENTITY");
        }

        @Test
        @DisplayName("应继承 OracleDialect 的分页行为")
        void shouldInheritOraclePagination() {
            String result = dialect.getPaginationSql("SELECT * FROM user", 10, 20);
            assertThat(result).contains("OFFSET 10 ROWS").contains("FETCH NEXT 20 ROWS ONLY");
        }
    }

    @Nested
    @DisplayName("GaussDBDialect")
    class GaussDBDialectTests {

        private final GaussDBDialect dialect = new GaussDBDialect();

        @Test
        @DisplayName("getDatabaseType 应返回 GAUSSDB")
        void shouldReturnGaussdb_whenGetDatabaseType() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.GAUSSDB);
        }

        @Test
        @DisplayName("应继承 PostgreSQLDialect 的分页行为")
        void shouldInheritPostgreSQLPagination() {
            String result = dialect.getPaginationSql("SELECT * FROM user", 10, 20);
            assertThat(result).isEqualTo("SELECT * FROM user LIMIT 20 OFFSET 10");
        }
    }

    @Nested
    @DisplayName("OceanBaseDialect")
    class OceanBaseDialectTests {

        private final OceanBaseDialect dialect = new OceanBaseDialect();

        @Test
        @DisplayName("getDatabaseType 应返回 OCEANBASE")
        void shouldReturnOceanbase_whenGetDatabaseType() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.OCEANBASE);
        }

        @Test
        @DisplayName("getCurrentTimestampFunction 应返回 NOW(6)")
        void shouldReturnNow6_whenGetCurrentTimestamp() {
            assertThat(dialect.getCurrentTimestampFunction()).isEqualTo("NOW(6)");
        }

        @Test
        @DisplayName("应继承 MySQLDialect 的分页行为")
        void shouldInheritMySQLPagination() {
            String result = dialect.getPaginationSql("SELECT * FROM user", 10, 20);
            assertThat(result).isEqualTo("SELECT * FROM user LIMIT 20 OFFSET 10");
        }
    }

    @Nested
    @DisplayName("KingbaseDialect")
    class KingbaseDialectTests {

        private final KingbaseDialect dialect = new KingbaseDialect();

        @Test
        @DisplayName("getDatabaseType 应返回 KINGBASE")
        void shouldReturnKingbase_whenGetDatabaseType() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.KINGBASE);
        }

        @Test
        @DisplayName("应继承 PostgreSQLDialect 的分页行为")
        void shouldInheritPostgreSQLPagination() {
            String result = dialect.getPaginationSql("SELECT * FROM user", 10, 20);
            assertThat(result).isEqualTo("SELECT * FROM user LIMIT 20 OFFSET 10");
        }
    }

    @Nested
    @DisplayName("OpenGaussDialect")
    class OpenGaussDialectTests {

        private final OpenGaussDialect dialect = new OpenGaussDialect();

        @Test
        @DisplayName("getDatabaseType 应返回 OPENGAUSS")
        void shouldReturnOpengauss_whenGetDatabaseType() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.OPENGAUSS);
        }

        @Test
        @DisplayName("应继承 PostgreSQLDialect 的分页行为")
        void shouldInheritPostgreSQLPagination() {
            String result = dialect.getPaginationSql("SELECT * FROM user", 10, 20);
            assertThat(result).isEqualTo("SELECT * FROM user LIMIT 20 OFFSET 10");
        }
    }

    @Nested
    @DisplayName("DatabaseType 枚举")
    class DatabaseTypeTests {

        @Test
        @DisplayName("fromCode 匹配应返回对应类型")
        void shouldReturnCorrectType_whenFromCode() {
            assertThat(DatabaseType.fromCode("mysql")).isEqualTo(DatabaseType.MYSQL);
            assertThat(DatabaseType.fromCode("postgresql")).isEqualTo(DatabaseType.POSTGRESQL);
            assertThat(DatabaseType.fromCode("oracle")).isEqualTo(DatabaseType.ORACLE);
            assertThat(DatabaseType.fromCode("sqlserver")).isEqualTo(DatabaseType.SQLSERVER);
        }

        @Test
        @DisplayName("fromCode 大小写不敏感")
        void shouldBeCaseInsensitive_whenFromCode() {
            assertThat(DatabaseType.fromCode("MYSQL")).isEqualTo(DatabaseType.MYSQL);
            assertThat(DatabaseType.fromCode("PostgreSQL")).isEqualTo(DatabaseType.POSTGRESQL);
        }

        @Test
        @DisplayName("fromCode null 应返回 UNKNOWN")
        void shouldReturnUnknown_whenNull() {
            assertThat(DatabaseType.fromCode(null)).isEqualTo(DatabaseType.UNKNOWN);
        }

        @Test
        @DisplayName("fromCode 未知代码应返回 UNKNOWN")
        void shouldReturnUnknown_whenUnknownCode() {
            assertThat(DatabaseType.fromCode("unknown_db")).isEqualTo(DatabaseType.UNKNOWN);
        }

        @Test
        @DisplayName("isMySQLFamily 应包含 MYSQL 和 OCEANBASE")
        void shouldIncludeMysqlAndOceanbase_whenIsMySQLFamily() {
            assertThat(DatabaseType.MYSQL.isMySQLFamily()).isTrue();
            assertThat(DatabaseType.OCEANBASE.isMySQLFamily()).isTrue();
            assertThat(DatabaseType.POSTGRESQL.isMySQLFamily()).isFalse();
        }

        @Test
        @DisplayName("isPostgreSQLFamily 应包含 POSTGRESQL、OPENGAUSS 和 GAUSSDB")
        void shouldIncludePostgreSQLAndVariants_whenIsPostgreSQLFamily() {
            assertThat(DatabaseType.POSTGRESQL.isPostgreSQLFamily()).isTrue();
            assertThat(DatabaseType.OPENGAUSS.isPostgreSQLFamily()).isTrue();
            assertThat(DatabaseType.GAUSSDB.isPostgreSQLFamily()).isTrue();
            assertThat(DatabaseType.MYSQL.isPostgreSQLFamily()).isFalse();
        }

        @Test
        @DisplayName("isChineseDatabase 应包含 OCEANBASE、OPENGAUSS、DM、KINGBASE、GAUSSDB")
        void shouldIncludeChineseDatabases_whenIsChineseDatabase() {
            assertThat(DatabaseType.OCEANBASE.isChineseDatabase()).isTrue();
            assertThat(DatabaseType.OPENGAUSS.isChineseDatabase()).isTrue();
            assertThat(DatabaseType.DM.isChineseDatabase()).isTrue();
            assertThat(DatabaseType.KINGBASE.isChineseDatabase()).isTrue();
            assertThat(DatabaseType.GAUSSDB.isChineseDatabase()).isTrue();
            assertThat(DatabaseType.MYSQL.isChineseDatabase()).isFalse();
            assertThat(DatabaseType.POSTGRESQL.isChineseDatabase()).isFalse();
        }
    }
}
