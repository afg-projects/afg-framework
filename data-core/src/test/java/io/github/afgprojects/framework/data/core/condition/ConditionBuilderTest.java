package io.github.afgprojects.framework.data.core.condition;

import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Criterion;
import io.github.afgprojects.framework.data.core.query.LogicalOperator;
import io.github.afgprojects.framework.data.core.query.Operator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConditionBuilder 单元测试
 */
@DisplayName("ConditionBuilder 测试")
class ConditionBuilderTest {

    // ========== 字符串 ConditionBuilder ==========

    @Nested
    @DisplayName("字符串 ConditionBuilder - 基础操作符")
    class StringBuilderBasicOperatorTests {

        @Test
        @DisplayName("eq 生成等于条件")
        void shouldGenerateEqCondition() {
            Condition condition = Conditions.builder().eq("name", "test").build();

            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.field()).isEqualTo("name");
            assertThat(criterion.operator()).isEqualTo(Operator.EQ);
            assertThat(criterion.value()).isEqualTo("test");
        }

        @Test
        @DisplayName("ne 生成不等于条件")
        void shouldGenerateNeCondition() {
            Condition condition = Conditions.builder().ne("status", 0).build();

            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.NE);
            assertThat(criterion.value()).isEqualTo(0);
        }

        @Test
        @DisplayName("gt 生成大于条件")
        void shouldGenerateGtCondition() {
            Condition condition = Conditions.builder().gt("age", 18).build();

            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.GT);
            assertThat(criterion.value()).isEqualTo(18);
        }

        @Test
        @DisplayName("ge 生成大于等于条件")
        void shouldGenerateGeCondition() {
            Condition condition = Conditions.builder().ge("age", 18).build();

            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.GE);
        }

        @Test
        @DisplayName("lt 生成小于条件")
        void shouldGenerateLtCondition() {
            Condition condition = Conditions.builder().lt("age", 65).build();

            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.LT);
        }

        @Test
        @DisplayName("le 生成小于等于条件")
        void shouldGenerateLeCondition() {
            Condition condition = Conditions.builder().le("age", 65).build();

            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.LE);
        }

        @Test
        @DisplayName("like 生成 LIKE 条件")
        void shouldGenerateLikeCondition() {
            Condition condition = Conditions.builder().like("name", "test").build();

            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.LIKE);
            // like 方法自动添加 % 包裹和转义
            assertThat(criterion.value()).isEqualTo("%test%");
        }

        @Test
        @DisplayName("likeStartsWith 生成前缀匹配条件")
        void shouldGenerateLikeStartsWithCondition() {
            Condition condition = Conditions.builder().likeStartsWith("name", "test").build();

            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.LIKE_STARTS_WITH);
            assertThat(criterion.value()).isEqualTo("test%");
        }

        @Test
        @DisplayName("likeEndsWith 生成后缀匹配条件")
        void shouldGenerateLikeEndsWithCondition() {
            Condition condition = Conditions.builder().likeEndsWith("name", "test").build();

            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.LIKE_ENDS_WITH);
            assertThat(criterion.value()).isEqualTo("%test");
        }

        @Test
        @DisplayName("notLike 生成 NOT LIKE 条件")
        void shouldGenerateNotLikeCondition() {
            Condition condition = Conditions.builder().notLike("name", "spam").build();

            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.NOT_LIKE);
        }

        @Test
        @DisplayName("in 生成 IN 条件")
        void shouldGenerateInCondition() {
            Condition condition = Conditions.builder().in("status", List.of(1, 2, 3)).build();

            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.IN);
        }

        @Test
        @DisplayName("notIn 生成 NOT IN 条件")
        void shouldGenerateNotInCondition() {
            Condition condition = Conditions.builder().notIn("status", List.of(0, -1)).build();

            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.NOT_IN);
        }

        @Test
        @DisplayName("between 生成 BETWEEN 条件")
        void shouldGenerateBetweenCondition() {
            Condition condition = Conditions.builder().between("age", 18, 65).build();

            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.BETWEEN);
            assertThat(criterion.value()).isInstanceOf(Comparable[].class);
        }

        @Test
        @DisplayName("notBetween 生成 NOT BETWEEN 条件")
        void shouldGenerateNotBetweenCondition() {
            Condition condition = Conditions.builder().notBetween("age", 18, 65).build();

            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.NOT_BETWEEN);
        }

        @Test
        @DisplayName("isNull 生成 IS NULL 条件")
        void shouldGenerateIsNullCondition() {
            Condition condition = Conditions.builder().isNull("email").build();

            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.IS_NULL);
            assertThat(criterion.value()).isNull();
        }

        @Test
        @DisplayName("isNotNull 生成 IS NOT NULL 条件")
        void shouldGenerateIsNotNullCondition() {
            Condition condition = Conditions.builder().isNotNull("email").build();

            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.IS_NOT_NULL);
            assertThat(criterion.value()).isNull();
        }
    }

    @Nested
    @DisplayName("字符串 ConditionBuilder - AND/OR 组合")
    class StringBuilderCombinationTests {

        @Test
        @DisplayName("多个条件默认 AND 组合")
        void shouldCombineWithAndByDefault() {
            Condition condition = Conditions.builder()
                    .eq("name", "test")
                    .eq("status", 1)
                    .build();

            assertThat(condition.getCriteria()).hasSize(2);
            assertThat(condition.getOperator()).isEqualTo(LogicalOperator.AND);
        }

        @Test
        @DisplayName("and(Condition) 嵌套 AND 条件")
        void shouldCombineWithNestedAndCondition() {
            Condition nested = Conditions.builder().eq("role", "admin").build();
            Condition condition = Conditions.builder()
                    .eq("name", "test")
                    .and(nested)
                    .build();

            assertThat(condition.getCriteria()).hasSize(2);
            assertThat(condition.getOperator()).isEqualTo(LogicalOperator.AND);
        }

        @Test
        @DisplayName("or(Condition) 嵌套 OR 条件")
        void shouldCombineWithNestedOrCondition() {
            Condition nested = Conditions.builder().eq("status", 2).build();
            Condition condition = Conditions.builder()
                    .eq("status", 1)
                    .or(nested)
                    .build();

            assertThat(condition.getCriteria()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("字符串 ConditionBuilder - build 返回 AND 条件")
    class StringBuilderBuildTests {

        @Test
        @DisplayName("单条件 build 返回含一个条件的 Condition")
        void shouldReturnSingleCriterionCondition() {
            Condition condition = Conditions.builder().eq("name", "test").build();

            assertThat(condition.getCriteria()).hasSize(1);
        }

        @Test
        @DisplayName("多条件 build 返回 AND 连接的 Condition")
        void shouldReturnAndConnectedCondition() {
            Condition condition = Conditions.builder()
                    .eq("name", "test")
                    .gt("age", 18)
                    .build();

            assertThat(condition.getCriteria()).hasSize(2);
            assertThat(condition.getOperator()).isEqualTo(LogicalOperator.AND);
        }

        @Test
        @DisplayName("空构建器 build 返回空条件")
        void shouldReturnEmptyCondition() {
            Condition condition = Conditions.builder().build();

            assertThat(condition.isEmpty()).isTrue();
        }
    }

    // ========== Conditions 静态方法 ==========

    @Nested
    @DisplayName("Conditions 静态工厂方法")
    class ConditionsStaticMethodTests {

        @Test
        @DisplayName("Conditions.eq 创建等于条件")
        void shouldCreateEqConditionStatically() {
            Condition condition = Conditions.eq("name", "test");

            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.EQ);
        }

        @Test
        @DisplayName("Conditions.isNull 创建 IS NULL 条件")
        void shouldCreateIsNullConditionStatically() {
            Condition condition = Conditions.isNull("email");

            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.IS_NULL);
        }

        @Test
        @DisplayName("Conditions.isNotNull 创建 IS NOT NULL 条件")
        void shouldCreateIsNotNullConditionStatically() {
            Condition condition = Conditions.isNotNull("email");

            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.IS_NOT_NULL);
        }

        @Test
        @DisplayName("Conditions.empty 创建空条件")
        void shouldCreateEmptyConditionStatically() {
            Condition condition = Conditions.empty();

            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Conditions.none 创建永假条件")
        void shouldCreateDenyAllConditionStatically() {
            Condition condition = Conditions.none();

            assertThat(condition.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("Conditions.allOf 创建 AND 组合条件")
        void shouldCreateAllOfConditionStatically() {
            Condition c1 = Conditions.eq("name", "test");
            Condition c2 = Conditions.eq("status", 1);
            Condition combined = Conditions.allOf(c1, c2);

            assertThat(combined.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("Conditions.anyOf 创建 OR 组合条件")
        void shouldCreateAnyOfConditionStatically() {
            Condition c1 = Conditions.eq("status", 1);
            Condition c2 = Conditions.eq("status", 2);
            Condition combined = Conditions.anyOf(c1, c2);

            assertThat(combined.isEmpty()).isFalse();
        }
    }

    // ========== TypedConditionBuilder ==========

    @Nested
    @DisplayName("TypedConditionBuilder - 类型安全操作")
    class TypedConditionBuilderTests {

        @Test
        @DisplayName("builder(Class) 创建类型安全构建器")
        void shouldCreateTypedBuilder() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .eq(TestEntity::getName, "test")
                    .build();

            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.EQ);
        }

        @Test
        @DisplayName("TypedConditionBuilder ne 操作")
        void shouldGenerateNeConditionWithTypedBuilder() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .ne(TestEntity::getStatus, 0)
                    .build();

            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.NE);
        }

        @Test
        @DisplayName("TypedConditionBuilder gt 操作")
        void shouldGenerateGtConditionWithTypedBuilder() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .gt(TestEntity::getAge, 18)
                    .build();

            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.GT);
        }

        @Test
        @DisplayName("TypedConditionBuilder like 操作")
        void shouldGenerateLikeConditionWithTypedBuilder() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .like(TestEntity::getName, "test")
                    .build();

            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.LIKE);
        }

        @Test
        @DisplayName("TypedConditionBuilder isNull 操作")
        void shouldGenerateIsNullConditionWithTypedBuilder() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .isNull(TestEntity::getEmail)
                    .build();

            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.IS_NULL);
        }

        @Test
        @DisplayName("TypedConditionBuilder isNotNull 操作")
        void shouldGenerateIsNotNullConditionWithTypedBuilder() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .isNotNull(TestEntity::getEmail)
                    .build();

            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.IS_NOT_NULL);
        }

        @Test
        @DisplayName("TypedConditionBuilder in 操作")
        void shouldGenerateInConditionWithTypedBuilder() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .in(TestEntity::getStatus, List.of(1, 2, 3))
                    .build();

            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.IN);
        }

        @Test
        @DisplayName("TypedConditionBuilder between 操作")
        void shouldGenerateBetweenConditionWithTypedBuilder() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .between(TestEntity::getAge, 18, 65)
                    .build();

            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.BETWEEN);
        }

        @Test
        @DisplayName("TypedConditionBuilder 多条件组合")
        void shouldCombineMultipleConditionsWithTypedBuilder() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .eq(TestEntity::getName, "test")
                    .gt(TestEntity::getAge, 18)
                    .isNotNull(TestEntity::getEmail)
                    .build();

            assertThat(condition.getCriteria()).hasSize(3);
            assertThat(condition.getOperator()).isEqualTo(LogicalOperator.AND);
        }
    }

    @Nested
    @DisplayName("TypedConditionBuilder.checkFieldType 类型安全检查")
    class TypeSafetyCheckTests {

        @Test
        @DisplayName("null 值始终兼容")
        void shouldAcceptNullValue() {
            // null 值不抛异常
            TypedConditionBuilder.checkFieldType("status", Integer.class, null);
            // 没有异常即通过
        }

        @Test
        @DisplayName("类型匹配通过")
        void shouldAcceptMatchingType() {
            TypedConditionBuilder.checkFieldType("status", Integer.class, 1);
            // 没有异常即通过
        }

        @Test
        @DisplayName("子类类型通过")
        void shouldAcceptSubtype() {
            TypedConditionBuilder.checkFieldType("name", String.class, "test");
            // 没有异常即通过
        }

        @Test
        @DisplayName("原始类型与包装类型兼容")
        void shouldAcceptPrimitiveAndWrapper() {
            TypedConditionBuilder.checkFieldType("age", int.class, Integer.valueOf(18));
            // 没有异常即通过
        }

        @Test
        @DisplayName("包装类型与原始类型兼容")
        void shouldAcceptWrapperAndPrimitive() {
            TypedConditionBuilder.checkFieldType("age", Integer.class, 18);
            // 没有异常即通过 (int 18 自动装箱为 Integer)
        }

        @Test
        @DisplayName("类型不匹配抛 IllegalArgumentException")
        void shouldRejectMismatchedType() {
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    TypedConditionBuilder.checkFieldType("status", Integer.class, "wrong_type")
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Type mismatch");
        }
    }

    @Nested
    @DisplayName("null 值转换")
    class NullValueConversionTests {

        @Test
        @DisplayName("Conditions.eq null 值转为 IS NULL")
        void shouldConvertEqNullToIsNullStatically() {
            Condition condition = Conditions.eq("email", null);

            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.IS_NULL);
        }

        @Test
        @DisplayName("Conditions.ne null 值转为 IS NOT NULL")
        void shouldConvertNeNullToIsNotNullStatically() {
            Condition condition = Conditions.ne("email", null);

            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.IS_NOT_NULL);
        }

        @Test
        @DisplayName("Conditions.like null 值转为 IS NULL")
        void shouldConvertLikeNullToIsNullStatically() {
            Condition condition = Conditions.like("name", null);

            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.IS_NULL);
        }

        @Test
        @DisplayName("Conditions.in null 集合转为 IS NULL")
        void shouldConvertInNullToIsNullStatically() {
            Condition condition = Conditions.in("status", null);

            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.IS_NULL);
        }
    }

    @Nested
    @DisplayName("原始/包装类型兼容")
    class PrimitiveTypeTests {

        @Test
        @DisplayName("int 值作为条件值")
        void shouldAcceptIntValue() {
            Condition condition = Conditions.builder().eq("age", 18).build();

            assertThat(condition.getCriteria().getFirst().value()).isEqualTo(18);
        }

        @Test
        @DisplayName("Integer 值作为条件值")
        void shouldAcceptIntegerValue() {
            Condition condition = Conditions.builder().eq("age", Integer.valueOf(18)).build();

            assertThat(condition.getCriteria().getFirst().value()).isEqualTo(18);
        }

        @Test
        @DisplayName("long 值作为条件值")
        void shouldAcceptLongValue() {
            Condition condition = Conditions.builder().eq("id", 1L).build();

            assertThat(condition.getCriteria().getFirst().value()).isEqualTo(1L);
        }

        @Test
        @DisplayName("double 值作为条件值")
        void shouldAcceptDoubleValue() {
            Condition condition = Conditions.builder().gt("price", 99.9).build();

            assertThat(condition.getCriteria().getFirst().value()).isEqualTo(99.9);
        }

        @Test
        @DisplayName("boolean 值作为条件值")
        void shouldAcceptBooleanValue() {
            Condition condition = Conditions.builder().eq("deleted", false).build();

            assertThat(condition.getCriteria().getFirst().value()).isEqualTo(false);
        }
    }

    /**
     * 测试用实体类，用于 TypedConditionBuilder 类型安全测试
     */
    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    public static class TestEntity {
        private String name;
        private Integer status;
        private Integer age;
        private String email;
    }
}