package io.github.afgprojects.framework.data.sql;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Condition 转 SQL 转换器测试
 */
@DisplayName("ConditionToSqlConverter Tests")
class ConditionToSqlConverterTest {

    @Nested
    @DisplayName("Basic Operator Tests")
    class BasicOperatorTests {

        @Test
        @DisplayName("转换简单等于条件")
        void testConvertEqCondition() {
            Condition condition = Conditions.eq("name", "test");
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).isEqualTo("name = ?");
            assertThat(result.parameters()).containsExactly("test");
        }

        @Test
        @DisplayName("转换不等于条件")
        void testConvertNeCondition() {
            Condition condition = Conditions.builder().ne("status", 0).build();
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).isEqualTo("status != ?");
            assertThat(result.parameters()).containsExactly(0);
        }

        @Test
        @DisplayName("转换大于条件")
        void testConvertGtCondition() {
            Condition condition = Conditions.builder().gt("age", 18).build();
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).isEqualTo("age > ?");
            assertThat(result.parameters()).containsExactly(18);
        }

        @Test
        @DisplayName("转换大于等于条件")
        void testConvertGeCondition() {
            Condition condition = Conditions.builder().ge("age", 18).build();
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).isEqualTo("age >= ?");
            assertThat(result.parameters()).containsExactly(18);
        }

        @Test
        @DisplayName("转换小于条件")
        void testConvertLtCondition() {
            Condition condition = Conditions.builder().lt("age", 65).build();
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).isEqualTo("age < ?");
            assertThat(result.parameters()).containsExactly(65);
        }

        @Test
        @DisplayName("转换小于等于条件")
        void testConvertLeCondition() {
            Condition condition = Conditions.builder().le("age", 65).build();
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).isEqualTo("age <= ?");
            assertThat(result.parameters()).containsExactly(65);
        }
    }

    @Nested
    @DisplayName("LIKE Operator Tests")
    class LikeOperatorTests {

        @Test
        @DisplayName("转换 LIKE 条件")
        void testConvertLikeCondition() {
            Condition condition = Conditions.like("name", "test");
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).isEqualTo("name LIKE ?");
            assertThat(result.parameters()).containsExactly("%test%");
        }

        @Test
        @DisplayName("转换 NOT LIKE 条件")
        void testConvertNotLikeCondition() {
            Condition condition = Conditions.builder().notLike("name", "test").build();
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).isEqualTo("name NOT LIKE ?");
            // notLike() 不会自动添加 %，与 like() 不同
            assertThat(result.parameters()).containsExactly("test");
        }
    }

    @Nested
    @DisplayName("IN Operator Tests")
    class InOperatorTests {

        @Test
        @DisplayName("转换 IN 条件")
        void testConvertInCondition() {
            Condition condition = Conditions.in("status", java.util.List.of(1, 2, 3));
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).isEqualTo("status IN (?, ?, ?)");
            assertThat(result.parameters()).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("转换 NOT IN 条件")
        void testConvertNotInCondition() {
            Condition condition = Conditions.builder().notIn("status", java.util.List.of(0, -1)).build();
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).isEqualTo("status NOT IN (?, ?)");
            assertThat(result.parameters()).containsExactly(0, -1);
        }
    }

    @Nested
    @DisplayName("NULL Operator Tests")
    class NullOperatorTests {

        @Test
        @DisplayName("转换 IS NULL 条件")
        void testConvertIsNullCondition() {
            Condition condition = Conditions.builder().isNull("deleted_at").build();
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).isEqualTo("deleted_at IS NULL");
            assertThat(result.parameters()).isEmpty();
        }

        @Test
        @DisplayName("转换 IS NOT NULL 条件")
        void testConvertIsNotNullCondition() {
            Condition condition = Conditions.builder().isNotNull("created_at").build();
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).isEqualTo("created_at IS NOT NULL");
            assertThat(result.parameters()).isEmpty();
        }
    }

    @Nested
    @DisplayName("BETWEEN Operator Tests")
    class BetweenOperatorTests {

        @Test
        @DisplayName("转换 BETWEEN 条件")
        void testConvertBetweenCondition() {
            Condition condition = Conditions.builder().between("age", 18, 30).build();
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).isEqualTo("age BETWEEN ? AND ?");
            assertThat(result.parameters()).containsExactly(18, 30);
        }

        @Test
        @DisplayName("转换 NOT BETWEEN 条件")
        void testConvertNotBetweenCondition() {
            Condition condition = Conditions.builder().notBetween("age", 0, 17).build();
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).isEqualTo("age NOT BETWEEN ? AND ?");
            assertThat(result.parameters()).containsExactly(0, 17);
        }
    }

    @Nested
    @DisplayName("Logical Operator Tests")
    class LogicalOperatorTests {

        @Test
        @DisplayName("转换 AND 组合条件")
        void testConvertAndCondition() {
            Condition condition = Conditions.builder()
                .eq("name", "test")
                .eq("status", 1)
                .build();
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).isEqualTo("(name = ? AND status = ?)");
            assertThat(result.parameters()).containsExactly("test", 1);
        }

        @Test
        @DisplayName("转换 OR 组合条件")
        void testConvertOrCondition() {
            Condition condition = Conditions.eq("status", 1).or(Conditions.eq("status", 2));
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).contains("OR");
            assertThat(result.parameters()).containsExactly(1, 2);
        }

        @Test
        @DisplayName("转换嵌套 AND 条件")
        void testConvertNestedConditionAnd() {
            Condition nested = Conditions.builder()
                .eq("a", 1)
                .eq("b", 2)
                .build();
            Condition condition = Conditions.builder()
                .eq("c", 3)
                .and(nested)
                .build();

            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).contains("AND");
            assertThat(result.parameters()).containsExactly(3, 1, 2);
        }

        @Test
        @DisplayName("转换嵌套 OR 条件")
        void testConvertNestedConditionOr() {
            // 使用 Condition.or() 方法创建 OR 条件
            Condition left = Conditions.eq("c", 3);
            Condition right = Conditions.builder()
                .eq("a", 1)
                .eq("b", 2)
                .build();
            Condition condition = left.or(right);

            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).contains("OR");
            assertThat(result.parameters()).containsExactly(3, 1, 2);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("无效字段名应该抛出异常")
        void testInvalidFieldName() {
            Condition condition = Conditions.eq("invalid-field", "test");
            ConditionToSqlConverter converter = new ConditionToSqlConverter();

            assertThatThrownBy(() -> converter.convert(condition))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid field name");
        }

        @Test
        @DisplayName("空字段名应该抛出异常")
        void testEmptyFieldName() {
            Condition condition = Conditions.eq("", "test");
            ConditionToSqlConverter converter = new ConditionToSqlConverter();

            assertThatThrownBy(() -> converter.convert(condition))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Field name cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("SqlResult Tests")
    class SqlResultTests {

        @Test
        @DisplayName("转换空条件")
        void testConvertEmptyCondition() {
            Condition condition = Conditions.empty();
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).isEmpty();
            assertThat(result.parameters()).isEmpty();
        }

        @Test
        @DisplayName("SqlResult isEmpty() should work correctly")
        void testSqlResultIsEmpty() {
            ConditionToSqlConverter.SqlResult emptyResult = ConditionToSqlConverter.SqlResult.empty();
            ConditionToSqlConverter.SqlResult nonEmptyResult = new ConditionToSqlConverter.SqlResult("name = ?", java.util.List.of("test"));

            assertThat(emptyResult.isEmpty()).isTrue();
            assertThat(nonEmptyResult.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("SqlResult getWhereClause() should work correctly")
        void testSqlResultGetWhereClause() {
            ConditionToSqlConverter.SqlResult result = new ConditionToSqlConverter.SqlResult("name = ?", java.util.List.of("test"));

            assertThat(result.getWhereClause()).isEqualTo("WHERE name = ?");
        }

        @Test
        @DisplayName("SqlResult empty() should return empty result")
        void testSqlResultEmptyStatic() {
            ConditionToSqlConverter.SqlResult result = ConditionToSqlConverter.SqlResult.empty();

            assertThat(result.sql()).isEmpty();
            assertThat(result.parameters()).isEmpty();
            assertThat(result.isEmpty()).isTrue();
            assertThat(result.getWhereClause()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Table.Field Format Tests")
    class TableFieldFormatTests {

        @Test
        @DisplayName("支持 table.field 格式的字段名")
        void testTableFieldFormat() {
            Condition condition = Conditions.eq("users.name", "test");
            ConditionToSqlConverter converter = new ConditionToSqlConverter();
            ConditionToSqlConverter.SqlResult result = converter.convert(condition);

            assertThat(result.sql()).isEqualTo("users.name = ?");
            assertThat(result.parameters()).containsExactly("test");
        }
    }
}