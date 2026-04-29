package io.github.afgprojects.framework.data.core.dialect;

import io.github.afgprojects.framework.data.core.page.PageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H2 数据库方言测试
 * <p>
 * 覆盖所有 Dialect 接口方法的边界情况
 */
@DisplayName("H2Dialect 测试")
class H2DialectTest {

    private final H2Dialect dialect = new H2Dialect();

    @Nested
    @DisplayName("数据库类型测试")
    class DatabaseTypeTests {

        @Test
        @DisplayName("应返回 H2 数据库类型")
        void shouldReturnH2DatabaseType() {
            assertThat(dialect.getDatabaseType()).isEqualTo(DatabaseType.H2);
        }
    }

    @Nested
    @DisplayName("分页 SQL 测试")
    class PaginationTests {

        @Test
        @DisplayName("应生成正确的分页 SQL")
        void shouldGeneratePaginationSql() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, 20, 10);

            assertThat(paged).isEqualTo("SELECT * FROM user LIMIT 10 OFFSET 20");
        }

        @Test
        @DisplayName("应支持 offset 为 0 的情况")
        void shouldHandleZeroOffset() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, 0, 10);

            assertThat(paged).isEqualTo("SELECT * FROM user LIMIT 10 OFFSET 0");
        }

        @Test
        @DisplayName("应支持大偏移量")
        void shouldHandleLargeOffset() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, 1000000L, 100);

            assertThat(paged).isEqualTo("SELECT * FROM user LIMIT 100 OFFSET 1000000");
        }

        @Test
        @DisplayName("应使用 PageRequest 生成分页 SQL")
        void shouldGeneratePaginationWithPageRequest() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, PageRequest.of(3, 20));

            // page 3, size 20 -> offset = (3-1) * 20 = 40
            assertThat(paged).isEqualTo("SELECT * FROM user LIMIT 20 OFFSET 40");
        }

        @Test
        @DisplayName("应使用第一页 PageRequest 生成分页 SQL")
        void shouldGeneratePaginationWithFirstPage() {
            String sql = "SELECT * FROM user";
            String paged = dialect.getPaginationSql(sql, PageRequest.of(1, 15));

            assertThat(paged).isEqualTo("SELECT * FROM user LIMIT 15 OFFSET 0");
        }

        @Test
        @DisplayName("应支持复杂 SQL 的分页")
        void shouldHandleComplexSql() {
            String sql = "SELECT u.id, u.name, r.role_name FROM user u JOIN role r ON u.role_id = r.id WHERE u.status = 1 ORDER BY u.id";
            String paged = dialect.getPaginationSql(sql, 50, 20);

            assertThat(paged).isEqualTo(sql + " LIMIT 20 OFFSET 50");
        }
    }

    @Nested
    @DisplayName("分页能力测试")
    class PaginationCapabilityTests {

        @Test
        @DisplayName("应支持 LIMIT OFFSET 语法")
        void shouldSupportLimitOffset() {
            assertThat(dialect.supportsLimitOffset()).isTrue();
        }

        @Test
        @DisplayName("应支持 FETCH FIRST 语法")
        void shouldSupportFetchFirst() {
            assertThat(dialect.supportsFetchFirst()).isTrue();
        }
    }

    @Nested
    @DisplayName("标识符引用测试")
    class IdentifierQuoteTests {

        @Test
        @DisplayName("应返回空字符串作为标识符引用字符")
        void shouldReturnEmptyQuote() {
            assertThat(dialect.getIdentifierQuote()).isEmpty();
        }

        @Test
        @DisplayName("应不引用标识符")
        void shouldNotQuoteIdentifier() {
            assertThat(dialect.quoteIdentifier("name")).isEqualTo("name");
        }

        @Test
        @DisplayName("应正确处理包含特殊字符的标识符")
        void shouldHandleSpecialCharacters() {
            assertThat(dialect.quoteIdentifier("user_name")).isEqualTo("user_name");
            assertThat(dialect.quoteIdentifier("columnName")).isEqualTo("columnName");
        }

        @Test
        @DisplayName("应正确处理中文标识符")
        void shouldHandleChineseIdentifier() {
            assertThat(dialect.quoteIdentifier("用户名")).isEqualTo("用户名");
        }
    }

    @Nested
    @DisplayName("时间函数测试")
    class TimeFunctionTests {

        @Test
        @DisplayName("应返回正确的时间函数")
        void shouldReturnCurrentTimeFunction() {
            assertThat(dialect.getCurrentTimeFunction()).isEqualTo("CURRENT_TIME");
        }

        @Test
        @DisplayName("应返回正确的日期函数")
        void shouldReturnCurrentDateFunction() {
            assertThat(dialect.getCurrentDateFunction()).isEqualTo("CURRENT_DATE");
        }

        @Test
        @DisplayName("应返回正确的时间戳函数")
        void shouldReturnCurrentTimestampFunction() {
            assertThat(dialect.getCurrentTimestampFunction()).isEqualTo("CURRENT_TIMESTAMP");
        }
    }

    @Nested
    @DisplayName("主键生成测试")
    class PrimaryKeyGenerationTests {

        @Test
        @DisplayName("应支持自增主键")
        void shouldSupportAutoIncrement() {
            assertThat(dialect.supportsAutoIncrement()).isTrue();
        }

        @Test
        @DisplayName("应支持序列")
        void shouldSupportSequence() {
            assertThat(dialect.supportsSequence()).isTrue();
        }

        @Test
        @DisplayName("应返回正确的自增语法")
        void shouldReturnAutoIncrementSyntax() {
            assertThat(dialect.getAutoIncrementSyntax()).isEqualTo("AUTO_INCREMENT");
        }

        @Test
        @DisplayName("应生成正确的序列查询 SQL")
        void shouldGenerateSequenceNextValueSql() {
            assertThat(dialect.getSequenceNextValueSql("user_seq")).isEqualTo("SELECT NEXT VALUE FOR user_seq");
        }

        @Test
        @DisplayName("应支持带模式的序列名")
        void shouldHandleSequenceWithSchema() {
            assertThat(dialect.getSequenceNextValueSql("public.user_seq")).isEqualTo("SELECT NEXT VALUE FOR public.user_seq");
        }
    }

    @Nested
    @DisplayName("SQL 类型映射测试")
    class SqlTypeMappingTests {

        @Test
        @DisplayName("应正确映射 String 类型")
        void shouldMapStringType() {
            assertThat(dialect.getSqlType(String.class)).isEqualTo("VARCHAR(255)");
        }

        @Test
        @DisplayName("应正确映射 Integer 类型（包装类和基本类型）")
        void shouldMapIntegerType() {
            assertThat(dialect.getSqlType(Integer.class)).isEqualTo("INT");
            assertThat(dialect.getSqlType(int.class)).isEqualTo("INT");
        }

        @Test
        @DisplayName("应正确映射 Long 类型（包装类和基本类型）")
        void shouldMapLongType() {
            assertThat(dialect.getSqlType(Long.class)).isEqualTo("BIGINT");
            assertThat(dialect.getSqlType(long.class)).isEqualTo("BIGINT");
        }

        @Test
        @DisplayName("应正确映射 Double 类型（包装类和基本类型）")
        void shouldMapDoubleType() {
            assertThat(dialect.getSqlType(Double.class)).isEqualTo("DOUBLE");
            assertThat(dialect.getSqlType(double.class)).isEqualTo("DOUBLE");
        }

        @Test
        @DisplayName("应正确映射 Float 类型（包装类和基本类型）")
        void shouldMapFloatType() {
            assertThat(dialect.getSqlType(Float.class)).isEqualTo("REAL");
            assertThat(dialect.getSqlType(float.class)).isEqualTo("REAL");
        }

        @Test
        @DisplayName("应正确映射 Boolean 类型（包装类和基本类型）")
        void shouldMapBooleanType() {
            assertThat(dialect.getSqlType(Boolean.class)).isEqualTo("BOOLEAN");
            assertThat(dialect.getSqlType(boolean.class)).isEqualTo("BOOLEAN");
        }

        @Test
        @DisplayName("应正确映射时间日期类型")
        void shouldMapDateTimeTypes() {
            assertThat(dialect.getSqlType(LocalDateTime.class)).isEqualTo("TIMESTAMP");
            assertThat(dialect.getSqlType(LocalDate.class)).isEqualTo("DATE");
            assertThat(dialect.getSqlType(LocalTime.class)).isEqualTo("TIME");
        }

        @Test
        @DisplayName("应正确映射 BigDecimal 类型")
        void shouldMapBigDecimalType() {
            assertThat(dialect.getSqlType(BigDecimal.class)).isEqualTo("DECIMAL(19,4)");
        }

        @Test
        @DisplayName("应正确映射 byte[] 类型")
        void shouldMapByteArrayType() {
            assertThat(dialect.getSqlType(byte[].class)).isEqualTo("BINARY");
        }

        @Test
        @DisplayName("未知类型应返回 VARCHAR(255)")
        void shouldReturnDefaultForUnknownType() {
            assertThat(dialect.getSqlType(Object.class)).isEqualTo("VARCHAR(255)");
            assertThat(dialect.getSqlType(java.util.UUID.class)).isEqualTo("VARCHAR(255)");
        }
    }

    @Nested
    @DisplayName("其他功能测试")
    class OtherFunctionTests {

        @Test
        @DisplayName("应返回正确的 LIKE 通配符")
        void shouldReturnLikeWildcard() {
            assertThat(dialect.getLikeWildcard()).isEqualTo("%");
        }

        @Test
        @DisplayName("应支持 FOR UPDATE")
        void shouldSupportForUpdate() {
            assertThat(dialect.supportsForUpdate()).isTrue();
        }

        @Test
        @DisplayName("应返回正确的 FOR UPDATE 语法")
        void shouldReturnForUpdateSyntax() {
            assertThat(dialect.getForUpdateSyntax()).isEqualTo("FOR UPDATE");
        }
    }
}
