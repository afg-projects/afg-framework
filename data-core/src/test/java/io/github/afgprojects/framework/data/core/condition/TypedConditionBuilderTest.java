package io.github.afgprojects.framework.data.core.condition;

import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.LogicalOperator;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TypedConditionBuilder Lambda 条件构建器测试
 */
@DisplayName("Lambda 条件构建器测试")
class TypedConditionBuilderTest {

    /**
     * 测试用户实体
     */
    @Data
    static class User {
        private Long id;
        private String name;
        private String email;
        private Integer status;
        private Integer age;
        private LocalDateTime createTime;
        private Boolean active;
    }

    @Nested
    @DisplayName("字段名提取测试")
    class FieldNameExtractionTest {

        @Test
        @DisplayName("应该从 getter 方法引用提取字段名")
        void shouldExtractFieldNameFromGetter() {
            assertThat(Conditions.getFieldName(User::getId)).isEqualTo("id");
            assertThat(Conditions.getFieldName(User::getName)).isEqualTo("name");
            assertThat(Conditions.getFieldName(User::getEmail)).isEqualTo("email");
            assertThat(Conditions.getFieldName(User::getStatus)).isEqualTo("status");
        }

        @Test
        @DisplayName("应该从 is 前缀的布尔 getter 提取字段名")
        void shouldExtractFieldNameFromBooleanGetter() {
            assertThat(Conditions.getFieldName(User::getActive)).isEqualTo("active");
        }
    }

    @Nested
    @DisplayName("类型化条件构建测试")
    class TypedConditionBuildTest {

        @Test
        @DisplayName("应该创建空条件")
        void shouldCreateEmptyCondition() {
            Condition condition = Conditions.builder(User.class).build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("应该构建等于条件")
        void shouldBuildEqCondition() {
            Condition condition = Conditions.builder(User.class)
                .eq(User::getName, "admin")
                .build();

            assertThat(condition.isEmpty()).isFalse();
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().get(0).field()).isEqualTo("name");
            assertThat(condition.getCriteria().get(0).value()).isEqualTo("admin");
        }

        @Test
        @DisplayName("应该构建不等于条件")
        void shouldBuildNeCondition() {
            Condition condition = Conditions.builder(User.class)
                .ne(User::getStatus, 0)
                .build();

            assertThat(condition.getCriteria().get(0).field()).isEqualTo("status");
            assertThat(condition.getCriteria().get(0).value()).isEqualTo(0);
        }

        @Test
        @DisplayName("应该构建比较条件")
        void shouldBuildComparisonConditions() {
            Condition condition = Conditions.builder(User.class)
                .gt(User::getAge, 18)
                .ge(User::getAge, 18)
                .lt(User::getAge, 60)
                .le(User::getAge, 60)
                .build();

            assertThat(condition.getCriteria()).hasSize(4);
        }

        @Test
        @DisplayName("应该构建 LIKE 条件")
        void shouldBuildLikeConditions() {
            Condition condition = Conditions.builder(User.class)
                .like(User::getName, "张")
                .likeLeft(User::getEmail, "test@")
                .likeRight(User::getEmail, "@example.com")
                .notLike(User::getName, "admin")
                .build();

            assertThat(condition.getCriteria()).hasSize(4);
            assertThat(condition.getCriteria().get(0).value()).isEqualTo("%张%");
            assertThat(condition.getCriteria().get(1).value()).isEqualTo("test@%");
            assertThat(condition.getCriteria().get(2).value()).isEqualTo("%@example.com");
        }

        @Test
        @DisplayName("应该构建 IN 条件")
        void shouldBuildInConditions() {
            List<Long> ids = List.of(1L, 2L, 3L);
            Condition condition = Conditions.builder(User.class)
                .in(User::getId, ids)
                .notIn(User::getStatus, List.of(0, -1))
                .build();

            assertThat(condition.getCriteria()).hasSize(2);
        }

        @Test
        @DisplayName("应该构建 NULL 条件")
        void shouldBuildNullConditions() {
            Condition condition = Conditions.builder(User.class)
                .isNull(User::getEmail)
                .isNotNull(User::getName)
                .build();

            assertThat(condition.getCriteria()).hasSize(2);
        }

        @Test
        @DisplayName("应该构建 BETWEEN 条件")
        void shouldBuildBetweenConditions() {
            Condition condition = Conditions.builder(User.class)
                .between(User::getAge, 18, 60)
                .notBetween(User::getStatus, 0, 10)
                .build();

            assertThat(condition.getCriteria()).hasSize(2);
        }

        @Test
        @DisplayName("应该构建复合条件")
        void shouldBuildComplexCondition() {
            Condition condition = Conditions.builder(User.class)
                .eq(User::getStatus, 1)
                .like(User::getName, "张")
                .ge(User::getAge, 18)
                .isNotNull(User::getEmail)
                .build();

            assertThat(condition.getCriteria()).hasSize(4);
            assertThat(condition.getOperator()).isEqualTo(LogicalOperator.AND);
        }
    }

    @Nested
    @DisplayName("条件组合测试")
    class ConditionCombinationTest {

        @Test
        @DisplayName("应该使用 AND 组合条件")
        void shouldCombineWithAnd() {
            Condition c1 = Conditions.builder(User.class)
                .eq(User::getName, "admin")
                .build();
            Condition c2 = Conditions.builder(User.class)
                .eq(User::getStatus, 1)
                .build();

            Condition combined = c1.and(c2);

            assertThat(combined.getOperator()).isEqualTo(LogicalOperator.AND);
        }

        @Test
        @DisplayName("应该使用 OR 组合条件")
        void shouldCombineWithOr() {
            Condition c1 = Conditions.builder(User.class)
                .eq(User::getName, "admin")
                .build();
            Condition c2 = Conditions.builder(User.class)
                .eq(User::getName, "root")
                .build();

            Condition combined = c1.or(c2);

            assertThat(combined.getOperator()).isEqualTo(LogicalOperator.OR);
        }

        @Test
        @DisplayName("应该在构建器中使用 AND 嵌套条件")
        void shouldNestConditionWithAnd() {
            Condition nested = Conditions.builder(User.class)
                .eq(User::getStatus, 1)
                .build();

            Condition condition = Conditions.builder(User.class)
                .eq(User::getName, "admin")
                .and(nested)
                .build();

            assertThat(condition.getCriteria()).hasSize(2);
        }

        @Test
        @DisplayName("应该在构建器中使用 OR 嵌套条件")
        void shouldNestConditionWithOr() {
            Condition nested = Conditions.builder(User.class)
                .eq(User::getName, "admin")
                .build();

            Condition condition = Conditions.builder(User.class)
                .eq(User::getStatus, 1)
                .or(nested)
                .build();

            assertThat(condition.getCriteria()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("类型安全测试")
    class TypeSafetyTest {

        @Test
        @DisplayName("Lambda 条件应该与字符串条件生成相同结果")
        void lambdaShouldMatchStringCondition() {
            Condition lambdaCondition = Conditions.builder(User.class)
                .eq(User::getName, "admin")
                .ge(User::getAge, 18)
                .build();

            Condition stringCondition = Conditions.builder()
                .eq("name", "admin")
                .ge("age", 18)
                .build();

            assertThat(lambdaCondition.getCriteria()).hasSize(stringCondition.getCriteria().size());
            assertThat(lambdaCondition.getCriteria().get(0).field())
                .isEqualTo(stringCondition.getCriteria().get(0).field());
            assertThat(lambdaCondition.getCriteria().get(1).field())
                .isEqualTo(stringCondition.getCriteria().get(1).field());
        }
    }
}
