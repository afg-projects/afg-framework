package io.github.afgprojects.framework.data.core.condition;

import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Criterion;
import io.github.afgprojects.framework.data.core.query.DenyAllCondition;
import io.github.afgprojects.framework.data.core.query.LogicalOperator;
import io.github.afgprojects.framework.data.core.query.Operator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Conditions 测试
 */
@DisplayName("Conditions 测试")
class ConditionsTest {

    @Nested
    @DisplayName("escapeLikeWildcards 方法")
    class EscapeLikeWildcardsTests {

        @Test
        @DisplayName("null 输入应返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(Conditions.escapeLikeWildcards(null)).isNull();
        }

        @Test
        @DisplayName("空字符串应返回空字符串")
        void shouldReturnEmptyForEmptyInput() {
            assertThat(Conditions.escapeLikeWildcards("")).isEmpty();
        }

        @ParameterizedTest
        @CsvSource({
            "test%value, test!%value",
            "test_value, test!_value",
            "normal, normal",
            "100%, 100!%",
            "a_b, a!_b"
        })
        @DisplayName("应正确转义 LIKE 通配符")
        void shouldEscapeWildcards(String input, String expected) {
            assertThat(Conditions.escapeLikeWildcards(input)).isEqualTo(expected);
        }

        @Test
        @DisplayName("应正确转义感叹号")
        void shouldEscapeExclamationMark() {
            // 输入: test!path → 输出: test!!path
            assertThat(Conditions.escapeLikeWildcards("test!path")).isEqualTo("test!!path");
        }

        @Test
        @DisplayName("应处理包含多个通配符的字符串")
        void shouldHandleMultipleWildcards() {
            // 输入: test%_value! → 输出: test!%!_value!!
            assertThat(Conditions.escapeLikeWildcards("test%_value!")).isEqualTo("test!%!_value!!");
        }
    }

    @Nested
    @DisplayName("null 值智能转换")
    class NullValueConversionTests {

        @Test
        @DisplayName("eq(field, null) 应转换为 IS_NULL 条件")
        void shouldConvertEqNullToIsNull() {
            Condition condition = Conditions.eq("status", null);
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.IS_NULL);
            assertThat(criterion.field()).isEqualTo("status");
        }

        @Test
        @DisplayName("ne(field, null) 应转换为 IS_NOT_NULL 条件")
        void shouldConvertNeNullToIsNotNull() {
            Condition condition = Conditions.ne("status", null);
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.IS_NOT_NULL);
            assertThat(criterion.field()).isEqualTo("status");
        }

        @Test
        @DisplayName("like(field, null) 应转换为 IS_NULL 条件")
        void shouldConvertLikeNullToIsNull() {
            Condition condition = Conditions.like("name", null);
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.IS_NULL);
        }

        @Test
        @DisplayName("notLike(field, null) 应转换为 IS_NOT_NULL 条件")
        void shouldConvertNotLikeNullToIsNotNull() {
            Condition condition = Conditions.notLike("name", null);
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.IS_NOT_NULL);
        }

        @Test
        @DisplayName("likeStartsWith(field, null) 应转换为 IS_NULL 条件")
        void shouldConvertLikeStartsWithNullToIsNull() {
            Condition condition = Conditions.likeStartsWith("name", null);
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.IS_NULL);
        }

        @Test
        @DisplayName("likeEndsWith(field, null) 应转换为 IS_NULL 条件")
        void shouldConvertLikeEndsWithNullToIsNull() {
            Condition condition = Conditions.likeEndsWith("name", null);
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.IS_NULL);
        }
    }

    @Nested
    @DisplayName("IN/notIn 空集合处理")
    class InEmptyCollectionTests {

        @Test
        @DisplayName("in(field, null) 应转换为 IS_NULL 条件")
        void shouldConvertInNullToIsNull() {
            Condition condition = Conditions.in("status", null);
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.IS_NULL);
        }

        @Test
        @DisplayName("in(field, 空集合) 应返回 DenyAllCondition")
        void shouldReturnDenyAllForEmptyCollection() {
            Condition condition = Conditions.in("status", Collections.emptyList());
            assertThat(condition).isInstanceOf(DenyAllCondition.class);
        }

        @Test
        @DisplayName("notIn(field, null) 应转换为 IS_NOT_NULL 条件")
        void shouldConvertNotInNullToIsNotNull() {
            Condition condition = Conditions.notIn("status", null);
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.IS_NOT_NULL);
        }

        @Test
        @DisplayName("notIn(field, 空集合) 应返回空条件（允许所有）")
        void shouldReturnEmptyForEmptyCollection() {
            Condition condition = Conditions.notIn("status", Collections.emptyList());
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("in(field, 非空集合) 应创建 IN 条件")
        void shouldCreateInConditionForNonEmptyCollection() {
            List<Integer> values = Arrays.asList(1, 2, 3);
            Condition condition = Conditions.in("status", values);
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.IN);
            assertThat(criterion.field()).isEqualTo("status");
        }
    }

    @Nested
    @DisplayName("条件构建器工厂")
    class BuilderFactoryTests {

        @Test
        @DisplayName("builder() 应创建 ConditionBuilder")
        void shouldCreateConditionBuilder() {
            var builder = Conditions.builder();
            assertThat(builder).isNotNull();
            Condition condition = builder.eq("status", 1).build();
            assertThat(condition.getCriteria()).hasSize(1);
        }

        @Test
        @DisplayName("builder(Class) 应创建 TypedConditionBuilder")
        void shouldCreateTypedConditionBuilder() {
            var builder = Conditions.builder(TestEntity.class);
            assertThat(builder).isNotNull();
        }

        @Test
        @DisplayName("empty() 应返回空条件")
        void shouldReturnEmptyCondition() {
            Condition condition = Conditions.empty();
            assertThat(condition.isEmpty()).isTrue();
            assertThat(condition.getCriteria()).isEmpty();
        }

        @Test
        @DisplayName("all() 应返回空条件（匹配所有）")
        void shouldReturnAllCondition() {
            Condition condition = Conditions.all();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("none() 应返回 DenyAllCondition")
        void shouldReturnDenyAllCondition() {
            Condition condition = Conditions.none();
            assertThat(condition).isInstanceOf(DenyAllCondition.class);
        }
    }

    @Nested
    @DisplayName("anyOf 组合")
    class AnyOfTests {

        @Test
        @DisplayName("anyOf(null) 应返回空条件")
        void shouldReturnEmptyForNullInput() {
            Condition condition = Conditions.anyOf((Condition[]) null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("anyOf() 应返回空条件")
        void shouldReturnEmptyForEmptyInput() {
            Condition condition = Conditions.anyOf();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("anyOf(c1) 应直接返回 c1")
        void shouldReturnSingleCondition() {
            Condition c1 = Conditions.eq("status", 1);
            Condition result = Conditions.anyOf(c1);
            assertThat(result).isSameAs(c1);
        }

        @Test
        @DisplayName("anyOf(c1, c2) 应创建 OR 组合条件")
        void shouldCreateOrCondition() {
            Condition c1 = Conditions.eq("status", 1);
            Condition c2 = Conditions.eq("status", 2);
            Condition result = Conditions.anyOf(c1, c2);
            assertThat(result.getOperator()).isEqualTo(LogicalOperator.AND);
            assertThat(result.getCriteria()).hasSize(2);
            // 验证第二个 criterion 的 nextOperator 是 OR
            Criterion first = result.getCriteria().get(0);
            assertThat(first.nestedCondition()).isNotNull();
            assertThat(first.nextOperator()).isEqualTo(LogicalOperator.OR);
        }
    }

    @Nested
    @DisplayName("allOf 组合")
    class AllOfTests {

        @Test
        @DisplayName("allOf(null) 应返回空条件")
        void shouldReturnEmptyForNullInput() {
            Condition condition = Conditions.allOf((Condition[]) null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("allOf() 应返回空条件")
        void shouldReturnEmptyForEmptyInput() {
            Condition condition = Conditions.allOf();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("allOf(c1) 应直接返回 c1")
        void shouldReturnSingleCondition() {
            Condition c1 = Conditions.eq("status", 1);
            Condition result = Conditions.allOf(c1);
            assertThat(result).isSameAs(c1);
        }

        @Test
        @DisplayName("allOf(c1, c2) 应创建 AND 组合条件")
        void shouldCreateAndCondition() {
            Condition c1 = Conditions.eq("status", 1);
            Condition c2 = Conditions.eq("type", "A");
            Condition result = Conditions.allOf(c1, c2);
            assertThat(result.getOperator()).isEqualTo(LogicalOperator.AND);
            assertThat(result.getCriteria()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("简单条件创建")
    class SimpleConditionTests {

        @Test
        @DisplayName("eq(field, value) 应创建等于条件")
        void shouldCreateEqCondition() {
            Condition condition = Conditions.eq("status", 1);
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.field()).isEqualTo("status");
            assertThat(criterion.operator()).isEqualTo(Operator.EQ);
            assertThat(criterion.value()).isEqualTo(1);
        }

        @Test
        @DisplayName("gt(field, value) 应创建大于条件")
        void shouldCreateGtCondition() {
            Condition condition = Conditions.gt("age", 18);
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.GT);
            assertThat(criterion.value()).isEqualTo(18);
        }

        @Test
        @DisplayName("ge(field, value) 应创建大于等于条件")
        void shouldCreateGeCondition() {
            Condition condition = Conditions.ge("age", 18);
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.GE);
        }

        @Test
        @DisplayName("lt(field, value) 应创建小于条件")
        void shouldCreateLtCondition() {
            Condition condition = Conditions.lt("age", 65);
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.LT);
        }

        @Test
        @DisplayName("le(field, value) 应创建小于等于条件")
        void shouldCreateLeCondition() {
            Condition condition = Conditions.le("age", 65);
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.LE);
        }

        @Test
        @DisplayName("like(field, value) 应创建 LIKE 条件并自动转义")
        void shouldCreateLikeConditionWithEscaping() {
            Condition condition = Conditions.like("name", "test%value");
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.LIKE);
            // 值应该被转义并添加 % 前后缀: %test!%value%
            assertThat(criterion.value()).isEqualTo("%test!%value%");
        }

        @Test
        @DisplayName("likeStartsWith(field, value) 应创建前缀匹配条件")
        void shouldCreateLikeStartsWithCondition() {
            Condition condition = Conditions.likeStartsWith("name", "test");
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.LIKE_STARTS_WITH);
            assertThat(criterion.value()).isEqualTo("test%");
        }

        @Test
        @DisplayName("likeEndsWith(field, value) 应创建后缀匹配条件")
        void shouldCreateLikeEndsWithCondition() {
            Condition condition = Conditions.likeEndsWith("name", "test");
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.LIKE_ENDS_WITH);
            assertThat(criterion.value()).isEqualTo("%test");
        }

        @Test
        @DisplayName("between(field, from, to) 应创建 BETWEEN 条件")
        void shouldCreateBetweenCondition() {
            Condition condition = Conditions.between("age", 18, 65);
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.BETWEEN);
            Comparable<?>[] values = (Comparable<?>[]) criterion.value();
            assertThat(values[0]).isEqualTo(18);
            assertThat(values[1]).isEqualTo(65);
        }

        @Test
        @DisplayName("notBetween(field, from, to) 应创建 NOT BETWEEN 条件")
        void shouldCreateNotBetweenCondition() {
            Condition condition = Conditions.notBetween("age", 18, 65);
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.NOT_BETWEEN);
        }

        @Test
        @DisplayName("isNull(field) 应创建 IS NULL 条件")
        void shouldCreateIsNullCondition() {
            Condition condition = Conditions.isNull("deletedAt");
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.IS_NULL);
        }

        @Test
        @DisplayName("isNotNull(field) 应创建 IS NOT NULL 条件")
        void shouldCreateIsNotNullCondition() {
            Condition condition = Conditions.isNotNull("createdAt");
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.IS_NOT_NULL);
        }
    }

    @Nested
    @DisplayName("JSON 条件创建")
    class JsonConditionTests {

        @Test
        @DisplayName("jsonContains(field, value) 应创建 JSON CONTAINS 条件")
        void shouldCreateJsonContainsCondition() {
            Condition condition = Conditions.jsonContains("tags", "important");
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.JSON_CONTAINS);
            assertThat(criterion.value()).isEqualTo("important");
        }

        @Test
        @DisplayName("jsonContained(field, value) 应创建 JSON CONTAINED 条件")
        void shouldCreateJsonContainedCondition() {
            Condition condition = Conditions.jsonContained("metadata", "{\"key\":\"value\"}");
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.JSON_CONTAINED);
        }

        @Test
        @DisplayName("jsonPath(field, path) 应创建 JSON PATH 条件")
        void shouldCreateJsonPathCondition() {
            Condition condition = Conditions.jsonPath("data", "$.name");
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.JSON_PATH);
            assertThat(criterion.value()).isEqualTo("$.name");
        }

        @Test
        @DisplayName("jsonPath(field, null) 应转换为 IS NULL 条件")
        void shouldConvertJsonPathNullToIsNull() {
            Condition condition = Conditions.jsonPath("data", null);
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().get(0);
            assertThat(criterion.operator()).isEqualTo(Operator.IS_NULL);
        }
    }

    @Nested
    @DisplayName("条件构建器链式调用")
    class BuilderChainTests {

        @Test
        @DisplayName("应支持链式调用构建复杂条件")
        void shouldSupportChainedCalls() {
            Condition condition = Conditions.builder()
                .eq("status", 1)
                .like("name", "test")
                .ge("age", 18)
                .build();

            assertThat(condition.getCriteria()).hasSize(3);
            assertThat(condition.getOperator()).isEqualTo(LogicalOperator.AND);
        }

        @Test
        @DisplayName("应支持嵌套条件")
        void shouldSupportNestedConditions() {
            Condition inner = Conditions.builder()
                .eq("status", 1)
                .or(Conditions.eq("status", 2))
                .build();

            Condition outer = Conditions.builder()
                .eq("deleted", false)
                .and(inner)
                .build();

            assertThat(outer.getCriteria()).hasSize(2);
        }
    }

    /**
     * 测试用实体类
     */
    static class TestEntity {
        private Long id;
        private String name;
        private Integer status;

        public Long getId() { return id; }
        public String getName() { return name; }
        public Integer getStatus() { return status; }
    }
}
