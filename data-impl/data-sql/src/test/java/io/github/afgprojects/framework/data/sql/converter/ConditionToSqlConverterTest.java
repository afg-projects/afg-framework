package io.github.afgprojects.framework.data.sql.converter;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.dialect.MySQLDialect;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter.SqlResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ConditionToSqlConverter 测试
 */
@DisplayName("ConditionToSqlConverter 测试")
class ConditionToSqlConverterTest {

    private final ConditionToSqlConverter converter = new ConditionToSqlConverter(new MySQLDialect());

    @Nested
    @DisplayName("空条件")
    class EmptyConditionTests {

        @Test
        @DisplayName("空条件返回 SqlResult.empty()")
        void shouldReturnEmptyForEmptyCondition() {
            Condition empty = Conditions.empty();
            SqlResult result = converter.convert(empty);
            assertThat(result.isEmpty()).isTrue();
            assertThat(result.sql()).isEmpty();
            assertThat(result.parameters()).isEmpty();
        }

        @Test
        @DisplayName("SqlResult.empty() isEmpty=true")
        void shouldReturnIsEmptyTrue() {
            SqlResult empty = SqlResult.empty();
            assertThat(empty.isEmpty()).isTrue();
            assertThat(empty.getWhereClause()).isEmpty();
        }

        @Test
        @DisplayName("SqlResult 非空 isEmpty=false, getWhereClause=WHERE ...")
        void shouldReturnIsNotEmptyWithWhereClause() {
            Condition condition = Conditions.eq("name", "test");
            SqlResult result = converter.convert(condition);
            assertThat(result.isEmpty()).isFalse();
            assertThat(result.getWhereClause()).isEqualTo("WHERE name = ?");
        }
    }

    @Nested
    @DisplayName("比较操作符")
    class ComparisonOperatorTests {

        @Test
        @DisplayName("EQ: field = ?")
        void shouldConvertEq() {
            SqlResult result = converter.convert(Conditions.eq("name", "test"));
            assertThat(result.sql()).isEqualTo("name = ?");
            assertThat(result.parameters()).containsExactly("test");
        }

        @Test
        @DisplayName("NE: field != ?")
        void shouldConvertNe() {
            SqlResult result = converter.convert(Conditions.ne("status", 0));
            assertThat(result.sql()).isEqualTo("status != ?");
            assertThat(result.parameters()).containsExactly(0);
        }

        @Test
        @DisplayName("GT: field > ?")
        void shouldConvertGt() {
            SqlResult result = converter.convert(Conditions.gt("age", 18));
            assertThat(result.sql()).isEqualTo("age > ?");
            assertThat(result.parameters()).containsExactly(18);
        }

        @Test
        @DisplayName("GE: field >= ?")
        void shouldConvertGe() {
            SqlResult result = converter.convert(Conditions.ge("age", 18));
            assertThat(result.sql()).isEqualTo("age >= ?");
            assertThat(result.parameters()).containsExactly(18);
        }

        @Test
        @DisplayName("LT: field < ?")
        void shouldConvertLt() {
            SqlResult result = converter.convert(Conditions.lt("age", 60));
            assertThat(result.sql()).isEqualTo("age < ?");
            assertThat(result.parameters()).containsExactly(60);
        }

        @Test
        @DisplayName("LE: field <= ?")
        void shouldConvertLe() {
            SqlResult result = converter.convert(Conditions.le("age", 60));
            assertThat(result.sql()).isEqualTo("age <= ?");
            assertThat(result.parameters()).containsExactly(60);
        }
    }

    @Nested
    @DisplayName("LIKE 操作符")
    class LikeOperatorTests {

        @Test
        @DisplayName("LIKE: field LIKE ? ESCAPE '!'")
        void shouldConvertLike() {
            SqlResult result = converter.convert(Conditions.like("name", "test"));
            assertThat(result.sql()).isEqualTo("name LIKE ? ESCAPE '!'");
            // Conditions.like wraps value with %...% and escapes wildcards
            assertThat(result.parameters()).hasSize(1);
            assertThat((String) result.parameters().get(0)).contains("test");
        }

        @Test
        @DisplayName("NOT LIKE: field NOT LIKE ? ESCAPE '!'")
        void shouldConvertNotLike() {
            SqlResult result = converter.convert(Conditions.notLike("name", "test"));
            assertThat(result.sql()).isEqualTo("name NOT LIKE ? ESCAPE '!'");
            assertThat(result.parameters()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("IN 操作符")
    class InOperatorTests {

        @Test
        @DisplayName("IN: field IN (?, ?, ?)")
        void shouldConvertIn() {
            SqlResult result = converter.convert(Conditions.in("status", List.of(1, 2, 3)));
            assertThat(result.sql()).isEqualTo("status IN (?, ?, ?)");
            assertThat(result.parameters()).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("NOT IN: field NOT IN (?, ?)")
        void shouldConvertNotIn() {
            SqlResult result = converter.convert(Conditions.notIn("status", List.of(0, 9)));
            assertThat(result.sql()).isEqualTo("status NOT IN (?, ?)");
            assertThat(result.parameters()).containsExactly(0, 9);
        }

        @Test
        @DisplayName("IN 非 Iterable 应抛 IllegalArgumentException")
        void shouldThrowExceptionForInWithNonIterable() {
            // 直接构建一个 IN 条件，value 不是 Iterable
            Condition condition = Conditions.builder().in("status", List.of(1, 2)).build();
            // 正常情况 Iterable 没问题，这里测试底层 converter 对非 Iterable 的处理
            // 通过构建 Criterion 直接测试
            io.github.afgprojects.framework.data.core.query.Criterion badCriterion =
                    io.github.afgprojects.framework.data.core.query.Criterion.of("status",
                            io.github.afgprojects.framework.data.core.query.Operator.IN, "not_iterable");
            Condition badCondition = new io.github.afgprojects.framework.data.core.query.ConditionImpl(
                    io.github.afgprojects.framework.data.core.query.LogicalOperator.AND, List.of(badCriterion));
            assertThatThrownBy(() -> converter.convert(badCondition))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("IN/NOT IN operator requires an Iterable value");
        }
    }

    @Nested
    @DisplayName("IS NULL / IS NOT NULL")
    class NullOperatorTests {

        @Test
        @DisplayName("IS NULL: field IS NULL (无参数)")
        void shouldConvertIsNull() {
            SqlResult result = converter.convert(Conditions.isNull("name"));
            assertThat(result.sql()).isEqualTo("name IS NULL");
            assertThat(result.parameters()).isEmpty();
        }

        @Test
        @DisplayName("IS NOT NULL: field IS NOT NULL (无参数)")
        void shouldConvertIsNotNull() {
            SqlResult result = converter.convert(Conditions.isNotNull("name"));
            assertThat(result.sql()).isEqualTo("name IS NOT NULL");
            assertThat(result.parameters()).isEmpty();
        }
    }

    @Nested
    @DisplayName("BETWEEN 操作符")
    class BetweenOperatorTests {

        @Test
        @DisplayName("BETWEEN: field BETWEEN ? AND ?")
        void shouldConvertBetween() {
            SqlResult result = converter.convert(Conditions.between("age", 18, 60));
            assertThat(result.sql()).isEqualTo("age BETWEEN ? AND ?");
            assertThat(result.parameters()).containsExactly(18, 60);
        }

        @Test
        @DisplayName("NOT BETWEEN: field NOT BETWEEN ? AND ?")
        void shouldConvertNotBetween() {
            SqlResult result = converter.convert(Conditions.notBetween("age", 18, 60));
            assertThat(result.sql()).isEqualTo("age NOT BETWEEN ? AND ?");
            assertThat(result.parameters()).containsExactly(18, 60);
        }

        @Test
        @DisplayName("BETWEEN 非 Comparable[2] 应抛 IllegalArgumentException")
        void shouldThrowExceptionForBetweenWithInvalidValue() {
            io.github.afgprojects.framework.data.core.query.Criterion badCriterion =
                    io.github.afgprojects.framework.data.core.query.Criterion.of("age",
                            io.github.afgprojects.framework.data.core.query.Operator.BETWEEN, "not_array");
            Condition badCondition = new io.github.afgprojects.framework.data.core.query.ConditionImpl(
                    io.github.afgprojects.framework.data.core.query.LogicalOperator.AND, List.of(badCriterion));
            assertThatThrownBy(() -> converter.convert(badCondition))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("BETWEEN/NOT BETWEEN requires a Comparable array of length 2");
        }

        @Test
        @DisplayName("BETWEEN 数组长度不为 2 应抛 IllegalArgumentException")
        void shouldThrowExceptionForBetweenWithWrongLength() {
            io.github.afgprojects.framework.data.core.query.Criterion badCriterion =
                    io.github.afgprojects.framework.data.core.query.Criterion.of("age",
                            io.github.afgprojects.framework.data.core.query.Operator.BETWEEN,
                            new Comparable[]{1, 2, 3});
            Condition badCondition = new io.github.afgprojects.framework.data.core.query.ConditionImpl(
                    io.github.afgprojects.framework.data.core.query.LogicalOperator.AND, List.of(badCriterion));
            assertThatThrownBy(() -> converter.convert(badCondition))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("BETWEEN/NOT BETWEEN requires a Comparable array of length 2");
        }
    }

    @Nested
    @DisplayName("逻辑组合")
    class LogicalCombinationTests {

        @Test
        @DisplayName("AND 组合: (field1 = ? AND field2 = ?)")
        void shouldConvertAndCombination() {
            Condition condition = Conditions.builder()
                    .eq("name", "test")
                    .eq("status", 1)
                    .build();
            SqlResult result = converter.convert(condition);
            assertThat(result.sql()).isEqualTo("(name = ? AND status = ?)");
            assertThat(result.parameters()).containsExactly("test", 1);
        }

        @Test
        @DisplayName("OR 组合: (field1 = ? OR field2 = ?)")
        void shouldConvertOrCombination() {
            Condition condition = Conditions.anyOf(
                    Conditions.eq("status", 1),
                    Conditions.eq("status", 2)
            );
            SqlResult result = converter.convert(condition);
            // anyOf creates nested conditions with individual parentheses around each criterion
            assertThat(result.sql()).contains("status = ?", "OR", "status = ?");
            assertThat(result.parameters()).containsExactly(1, 2);
        }

        @Test
        @DisplayName("嵌套条件")
        void shouldConvertNestedCondition() {
            Condition inner = Conditions.eq("status", 1);
            Condition outer = Conditions.eq("name", "test");
            Condition combined = inner.and(outer);
            SqlResult result = converter.convert(combined);
            assertThat(result.sql()).contains("status = ?", "name = ?");
            assertThat(result.parameters()).containsExactly(1, "test");
        }
    }

    @Nested
    @DisplayName("NOT 条件")
    class NotConditionTests {

        @Test
        @DisplayName("NOT (field = ?)")
        void shouldConvertNotCondition() {
            Condition condition = Conditions.eq("status", 1).not();
            SqlResult result = converter.convert(condition);
            assertThat(result.sql()).contains("NOT");
            assertThat(result.sql()).contains("status = ?");
            assertThat(result.parameters()).containsExactly(1);
        }
    }

    @Nested
    @DisplayName("DenyAllCondition")
    class DenyAllConditionTests {

        @Test
        @DisplayName("DenyAllCondition 生成 1 = 0 永假条件")
        void shouldHandleDenyAllCondition() {
            Condition denyAll = Conditions.none();
            assertThat(denyAll.isEmpty()).isFalse();
            // DenyAllCondition 应生成 1 = 0，确保拒绝所有数据访问
            SqlResult result = converter.convert(denyAll);
            assertThat(result.sql()).isEqualTo("1 = 0");
            assertThat(result.parameters()).isEmpty();
        }
    }

    @Nested
    @DisplayName("JSON 操作符")
    class JsonOperatorTests {

        @Test
        @DisplayName("JSON_CONTAINS with MySQL dialect")
        void shouldConvertJsonContainsWithMySqlDialect() {
            Condition condition = Conditions.jsonContains("tags", "\"java\"");
            SqlResult result = converter.convert(condition);
            assertThat(result.sql()).isEqualTo("JSON_CONTAINS(tags, ?)");
            assertThat(result.parameters()).containsExactly("\"java\"");
        }

        @Test
        @DisplayName("JSON_CONTAINED with MySQL dialect")
        void shouldConvertJsonContainedWithMySqlDialect() {
            Condition condition = Conditions.jsonContained("tags", "\"java\"");
            SqlResult result = converter.convert(condition);
            assertThat(result.sql()).isEqualTo("JSON_CONTAINS(?, tags)");
            assertThat(result.parameters()).containsExactly("\"java\"");
        }

        @Test
        @DisplayName("JSON_PATH with MySQL dialect")
        void shouldConvertJsonPathWithMySqlDialect() {
            Condition condition = Conditions.jsonPath("data", "$.name");
            SqlResult result = converter.convert(condition);
            assertThat(result.sql()).isEqualTo("JSON_EXTRACT(data, ?) IS NOT NULL");
            assertThat(result.parameters()).containsExactly("$.name");
        }

        }

    @Nested
    @DisplayName("SQL 注入防护")
    class SqlInjectionTests {

        @Test
        @DisplayName("非法字段名应抛 IllegalArgumentException")
        void shouldThrowExceptionForInvalidFieldName() {
            io.github.afgprojects.framework.data.core.query.Criterion badCriterion =
                    io.github.afgprojects.framework.data.core.query.Criterion.of("name; DROP TABLE user;--",
                            io.github.afgprojects.framework.data.core.query.Operator.EQ, "test");
            Condition badCondition = new io.github.afgprojects.framework.data.core.query.ConditionImpl(
                    io.github.afgprojects.framework.data.core.query.LogicalOperator.AND, List.of(badCriterion));
            assertThatThrownBy(() -> converter.convert(badCondition))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("column name");
        }
    }
}
