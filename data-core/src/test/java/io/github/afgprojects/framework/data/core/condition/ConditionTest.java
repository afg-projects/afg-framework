package io.github.afgprojects.framework.data.core.condition;

import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.LogicalOperator;
import io.github.afgprojects.framework.data.core.query.Operator;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConditionTest {

    @Data
    static class TestEntity {
        private String name;
        private String email;
        private Integer age;
        private Boolean active;
    }

    @Nested
    @DisplayName("Conditions 静态工厂方法测试")
    class ConditionsStaticMethodsTests {

        @Test
        @DisplayName("empty() 创建空条件")
        void shouldCreateEmptyCondition() {
            Condition condition = Conditions.empty();
            assertThat(condition.isEmpty()).isTrue();
            assertThat(condition.getCriteria()).isEmpty();
        }

        @Test
        @DisplayName("eq() 创建等于条件")
        void shouldCreateEqCondition() {
            Condition condition = Conditions.eq("name", "test");
            assertThat(condition.isEmpty()).isFalse();
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().get(0).field()).isEqualTo("name");
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.EQ);
            assertThat(condition.getCriteria().get(0).value()).isEqualTo("test");
        }

        @Test
        @DisplayName("like() 创建 LIKE 条件")
        void shouldCreateLikeCondition() {
            Condition condition = Conditions.like("name", "test");
            assertThat(condition.isEmpty()).isFalse();
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.LIKE);
            assertThat(condition.getCriteria().get(0).value()).isEqualTo("%test%");
        }

        @Test
        @DisplayName("in() 创建 IN 条件")
        void shouldCreateInCondition() {
            List<String> values = Arrays.asList("a", "b", "c");
            Condition condition = Conditions.in("name", values);
            assertThat(condition.isEmpty()).isFalse();
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.IN);
        }
    }

    @Nested
    @DisplayName("Conditions.builder() 条件构建器测试")
    class ConditionBuilderTests {

        @Test
        @DisplayName("构建多条件 AND 组合")
        void shouldBuildConditionWithBuilder() {
            Condition condition = Conditions.builder()
                .eq("name", "test")
                .like("email", "example")
                .build();

            assertThat(condition.isEmpty()).isFalse();
            assertThat(condition.getCriteria()).hasSize(2);
            assertThat(condition.getOperator()).isEqualTo(LogicalOperator.AND);
        }

        @Test
        @DisplayName("ne() 不等于条件")
        void shouldCreateNeCondition() {
            Condition condition = Conditions.builder()
                .ne("status", 0)
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.NE);
        }

        @Test
        @DisplayName("gt() 大于条件")
        void shouldCreateGtCondition() {
            Condition condition = Conditions.builder()
                .gt("age", 18)
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.GT);
        }

        @Test
        @DisplayName("ge() 大于等于条件")
        void shouldCreateGeCondition() {
            Condition condition = Conditions.builder()
                .ge("age", 18)
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.GE);
        }

        @Test
        @DisplayName("lt() 小于条件")
        void shouldCreateLtCondition() {
            Condition condition = Conditions.builder()
                .lt("age", 65)
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.LT);
        }

        @Test
        @DisplayName("le() 小于等于条件")
        void shouldCreateLeCondition() {
            Condition condition = Conditions.builder()
                .le("age", 65)
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.LE);
        }

        @Test
        @DisplayName("like() 自动添加百分号")
        void shouldAddPercentForLike() {
            Condition condition = Conditions.builder()
                .like("name", "test")
                .build();
            assertThat(condition.getCriteria().get(0).value()).isEqualTo("%test%");
        }

        @Test
        @DisplayName("like() 不重复添加百分号")
        void shouldNotDuplicatePercentForLike() {
            Condition condition = Conditions.builder()
                .like("name", "%test%")
                .build();
            assertThat(condition.getCriteria().get(0).value()).isEqualTo("%test%");
        }

        @Test
        @DisplayName("like() null 值处理")
        void shouldHandleNullForLike() {
            Condition condition = Conditions.builder()
                .like("name", null)
                .build();
            assertThat(condition.getCriteria().get(0).value()).isNull();
        }

        @Test
        @DisplayName("likeLeft() 左模糊匹配")
        void shouldCreateLikeLeftCondition() {
            Condition condition = Conditions.builder()
                .likeLeft("name", "test")
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.LIKE_LEFT);
            assertThat(condition.getCriteria().get(0).value()).isEqualTo("test%");
        }

        @Test
        @DisplayName("likeLeft() 不重复添加百分号")
        void shouldNotDuplicatePercentForLikeLeft() {
            Condition condition = Conditions.builder()
                .likeLeft("name", "test%")
                .build();
            assertThat(condition.getCriteria().get(0).value()).isEqualTo("test%");
        }

        @Test
        @DisplayName("likeRight() 右模糊匹配")
        void shouldCreateLikeRightCondition() {
            Condition condition = Conditions.builder()
                .likeRight("name", "test")
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.LIKE_RIGHT);
            assertThat(condition.getCriteria().get(0).value()).isEqualTo("%test");
        }

        @Test
        @DisplayName("likeRight() 不重复添加百分号")
        void shouldNotDuplicatePercentForLikeRight() {
            Condition condition = Conditions.builder()
                .likeRight("name", "%test")
                .build();
            assertThat(condition.getCriteria().get(0).value()).isEqualTo("%test");
        }

        @Test
        @DisplayName("notLike() 不模糊匹配")
        void shouldCreateNotLikeCondition() {
            Condition condition = Conditions.builder()
                .notLike("name", "test")
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.NOT_LIKE);
        }

        @Test
        @DisplayName("in() IN 条件")
        void shouldCreateInCondition() {
            Condition condition = Conditions.builder()
                .in("id", Arrays.asList(1, 2, 3))
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.IN);
        }

        @Test
        @DisplayName("notIn() NOT IN 条件")
        void shouldCreateNotInCondition() {
            Condition condition = Conditions.builder()
                .notIn("id", Arrays.asList(1, 2, 3))
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.NOT_IN);
        }

        @Test
        @DisplayName("isNull() IS NULL 条件")
        void shouldCreateIsNullCondition() {
            Condition condition = Conditions.builder()
                .isNull("deletedAt")
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.IS_NULL);
        }

        @Test
        @DisplayName("isNotNull() IS NOT NULL 条件")
        void shouldCreateIsNotNullCondition() {
            Condition condition = Conditions.builder()
                .isNotNull("createdAt")
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.IS_NOT_NULL);
        }

        @Test
        @DisplayName("between() BETWEEN 条件")
        void shouldCreateBetweenCondition() {
            Condition condition = Conditions.builder()
                .between("age", 18, 65)
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.BETWEEN);
            Comparable<?>[] values = (Comparable<?>[]) condition.getCriteria().get(0).value();
            assertThat(values[0]).isEqualTo(18);
            assertThat(values[1]).isEqualTo(65);
        }

        @Test
        @DisplayName("notBetween() NOT BETWEEN 条件")
        void shouldCreateNotBetweenCondition() {
            Condition condition = Conditions.builder()
                .notBetween("age", 0, 17)
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.NOT_BETWEEN);
        }

        @Test
        @DisplayName("and() 嵌套 AND 条件")
        void shouldCreateNestedAndCondition() {
            Condition nested = Conditions.builder()
                .eq("a", 1)
                .eq("b", 2)
                .build();
            Condition condition = Conditions.builder()
                .eq("name", "test")
                .and(nested)
                .build();
            assertThat(condition.getCriteria()).hasSize(2);
        }

        @Test
        @DisplayName("and() 忽略空条件")
        void shouldIgnoreEmptyAndCondition() {
            Condition condition = Conditions.builder()
                .eq("name", "test")
                .and(Condition.empty())
                .build();
            assertThat(condition.getCriteria()).hasSize(1);
        }

        @Test
        @DisplayName("or() 嵌套 OR 条件")
        void shouldCreateNestedOrCondition() {
            Condition nested = Conditions.builder()
                .eq("a", 1)
                .eq("b", 2)
                .build();
            Condition condition = Conditions.builder()
                .eq("name", "test")
                .or(nested)
                .build();
            assertThat(condition.getCriteria()).hasSize(2);
        }

        @Test
        @DisplayName("or() 忽略空条件")
        void shouldIgnoreEmptyOrCondition() {
            Condition condition = Conditions.builder()
                .eq("name", "test")
                .or(Condition.empty())
                .build();
            assertThat(condition.getCriteria()).hasSize(1);
        }

        @Test
        @DisplayName("空构建器返回空条件")
        void shouldReturnEmptyForEmptyBuilder() {
            Condition condition = Conditions.builder().build();
            assertThat(condition.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("Conditions.builder(Class) 类型化构建器测试")
    class TypedConditionBuilderTests {

        @Test
        @DisplayName("eq() 使用 getter 方法")
        void shouldCreateTypedEqCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                .eq(TestEntity::getName, "test")
                .build();
            assertThat(condition.getCriteria().get(0).field()).isEqualTo("name");
        }

        @Test
        @DisplayName("ne() 使用 getter 方法")
        void shouldCreateTypedNeCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                .ne(TestEntity::getAge, 0)
                .build();
            assertThat(condition.getCriteria().get(0).field()).isEqualTo("age");
        }

        @Test
        @DisplayName("gt() 使用 getter 方法")
        void shouldCreateTypedGtCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                .gt(TestEntity::getAge, 18)
                .build();
            assertThat(condition.getCriteria().get(0).field()).isEqualTo("age");
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.GT);
        }

        @Test
        @DisplayName("ge() 使用 getter 方法")
        void shouldCreateTypedGeCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                .ge(TestEntity::getAge, 18)
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.GE);
        }

        @Test
        @DisplayName("lt() 使用 getter 方法")
        void shouldCreateTypedLtCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                .lt(TestEntity::getAge, 65)
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.LT);
        }

        @Test
        @DisplayName("le() 使用 getter 方法")
        void shouldCreateTypedLeCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                .le(TestEntity::getAge, 65)
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.LE);
        }

        @Test
        @DisplayName("like() 使用 getter 方法")
        void shouldCreateTypedLikeCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                .like(TestEntity::getName, "test")
                .build();
            assertThat(condition.getCriteria().get(0).field()).isEqualTo("name");
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.LIKE);
        }

        @Test
        @DisplayName("likeLeft() 使用 getter 方法")
        void shouldCreateTypedLikeLeftCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                .likeLeft(TestEntity::getName, "test")
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.LIKE_LEFT);
        }

        @Test
        @DisplayName("likeRight() 使用 getter 方法")
        void shouldCreateTypedLikeRightCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                .likeRight(TestEntity::getName, "test")
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.LIKE_RIGHT);
        }

        @Test
        @DisplayName("notLike() 使用 getter 方法")
        void shouldCreateTypedNotLikeCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                .notLike(TestEntity::getName, "test")
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.NOT_LIKE);
        }

        @Test
        @DisplayName("in() 使用 getter 方法")
        void shouldCreateTypedInCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                .in(TestEntity::getAge, Arrays.asList(1, 2, 3))
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.IN);
        }

        @Test
        @DisplayName("notIn() 使用 getter 方法")
        void shouldCreateTypedNotInCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                .notIn(TestEntity::getAge, Arrays.asList(1, 2, 3))
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.NOT_IN);
        }

        @Test
        @DisplayName("isNull() 使用 getter 方法")
        void shouldCreateTypedIsNullCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                .isNull(TestEntity::getEmail)
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.IS_NULL);
        }

        @Test
        @DisplayName("isNotNull() 使用 getter 方法")
        void shouldCreateTypedIsNotNullCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                .isNotNull(TestEntity::getEmail)
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.IS_NOT_NULL);
        }

        @Test
        @DisplayName("between() 使用 getter 方法")
        void shouldCreateTypedBetweenCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                .between(TestEntity::getAge, 18, 65)
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.BETWEEN);
        }

        @Test
        @DisplayName("notBetween() 使用 getter 方法")
        void shouldCreateTypedNotBetweenCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                .notBetween(TestEntity::getAge, 0, 17)
                .build();
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.NOT_BETWEEN);
        }

        @Test
        @DisplayName("and() 嵌套条件")
        void shouldCreateTypedNestedAndCondition() {
            Condition nested = Conditions.builder(TestEntity.class)
                .eq(TestEntity::getName, "test")
                .build();
            Condition condition = Conditions.builder(TestEntity.class)
                .eq(TestEntity::getEmail, "test@example.com")
                .and(nested)
                .build();
            assertThat(condition.getCriteria()).hasSize(2);
        }

        @Test
        @DisplayName("or() 嵌套条件")
        void shouldCreateTypedNestedOrCondition() {
            Condition nested = Conditions.builder(TestEntity.class)
                .eq(TestEntity::getName, "test")
                .build();
            Condition condition = Conditions.builder(TestEntity.class)
                .eq(TestEntity::getEmail, "test@example.com")
                .or(nested)
                .build();
            assertThat(condition.getCriteria()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Conditions.getFieldName() 方法测试")
    class GetFieldNameTests {

        @Test
        @DisplayName("从 get 方法获取字段名")
        void shouldGetFieldNameFromGetMethod() {
            String fieldName = Conditions.getFieldName(TestEntity::getName);
            assertThat(fieldName).isEqualTo("name");
        }

        @Test
        @DisplayName("从 is 方法获取字段名")
        void shouldGetFieldNameFromIsMethod() {
            String fieldName = Conditions.getFieldName(TestEntity::getActive);
            assertThat(fieldName).isEqualTo("active");
        }
    }

    @Nested
    @DisplayName("Condition 组合测试")
    class ConditionCombinationTests {

        @Test
        @DisplayName("shouldCombineConditionsWithAnd")
        void shouldCombineConditionsWithAnd() {
            Condition c1 = Conditions.builder().eq("name", "test").build();
            Condition c2 = Conditions.builder().eq("status", 1).build();
            Condition combined = c1.and(c2);

            assertThat(combined.getOperator()).isEqualTo(LogicalOperator.AND);
        }

        @Test
        @DisplayName("shouldCombineConditionsWithOr")
        void shouldCombineConditionsWithOr() {
            Condition c1 = Conditions.builder().eq("name", "test").build();
            Condition c2 = Conditions.builder().eq("name", "demo").build();
            Condition combined = c1.or(c2);

            assertThat(combined.getOperator()).isEqualTo(LogicalOperator.OR);
        }
    }
}