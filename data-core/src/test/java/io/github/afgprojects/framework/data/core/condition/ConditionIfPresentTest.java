package io.github.afgprojects.framework.data.core.condition;

import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Criterion;
import io.github.afgprojects.framework.data.core.query.Operator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IfPresent 条件构建器测试
 */
@DisplayName("IfPresent 条件构建器测试")
class ConditionIfPresentTest {

    // ==================== ConditionBuilder (字符串字段名) ====================

    @Nested
    @DisplayName("ConditionBuilder IfPresent - null 值跳过")
    class BuilderNullSkipTests {

        @Test
        @DisplayName("eqIfPresent(field, null) 应跳过条件")
        void shouldSkipEqIfPresentWhenNull() {
            Condition condition = Conditions.builder()
                    .eqIfPresent("status", null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("neIfPresent(field, null) 应跳过条件")
        void shouldSkipNeIfPresentWhenNull() {
            Condition condition = Conditions.builder()
                    .neIfPresent("status", null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeIfPresent(field, null) 应跳过条件")
        void shouldSkipLikeIfPresentWhenNull() {
            Condition condition = Conditions.builder()
                    .likeIfPresent("name", null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeStartsWithIfPresent(field, null) 应跳过条件")
        void shouldSkipLikeStartsWithIfPresentWhenNull() {
            Condition condition = Conditions.builder()
                    .likeStartsWithIfPresent("name", null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeEndsWithIfPresent(field, null) 应跳过条件")
        void shouldSkipLikeEndsWithIfPresentWhenNull() {
            Condition condition = Conditions.builder()
                    .likeEndsWithIfPresent("name", null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notLikeIfPresent(field, null) 应跳过条件")
        void shouldSkipNotLikeIfPresentWhenNull() {
            Condition condition = Conditions.builder()
                    .notLikeIfPresent("name", null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("inIfPresent(field, null) 应跳过条件")
        void shouldSkipInIfPresentWhenNull() {
            Condition condition = Conditions.builder()
                    .inIfPresent("status", null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notInIfPresent(field, null) 应跳过条件")
        void shouldSkipNotInIfPresentWhenNull() {
            Condition condition = Conditions.builder()
                    .notInIfPresent("status", null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("betweenIfPresent(field, null, null) 应跳过条件")
        void shouldSkipBetweenIfPresentWhenBothNull() {
            Condition condition = Conditions.builder()
                    .betweenIfPresent("age", null, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("betweenIfPresent(field, from, null) 应跳过条件（部分 null）")
        void shouldSkipBetweenIfPresentWhenToIsNull() {
            Condition condition = Conditions.builder()
                    .betweenIfPresent("age", 18, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("betweenIfPresent(field, null, to) 应跳过条件（部分 null）")
        void shouldSkipBetweenIfPresentWhenFromIsNull() {
            Condition condition = Conditions.builder()
                    .betweenIfPresent("age", null, 65)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notBetweenIfPresent(field, null, null) 应跳过条件")
        void shouldSkipNotBetweenIfPresentWhenBothNull() {
            Condition condition = Conditions.builder()
                    .notBetweenIfPresent("age", null, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notBetweenIfPresent(field, from, null) 应跳过条件（部分 null）")
        void shouldSkipNotBetweenIfPresentWhenToIsNull() {
            Condition condition = Conditions.builder()
                    .notBetweenIfPresent("age", 18, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("gtIfPresent(field, null) 应跳过条件")
        void shouldSkipGtIfPresentWhenNull() {
            Condition condition = Conditions.builder()
                    .gtIfPresent("age", null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("geIfPresent(field, null) 应跳过条件")
        void shouldSkipGeIfPresentWhenNull() {
            Condition condition = Conditions.builder()
                    .geIfPresent("age", null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("ltIfPresent(field, null) 应跳过条件")
        void shouldSkipLtIfPresentWhenNull() {
            Condition condition = Conditions.builder()
                    .ltIfPresent("age", null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("leIfPresent(field, null) 应跳过条件")
        void shouldSkipLeIfPresentWhenNull() {
            Condition condition = Conditions.builder()
                    .leIfPresent("age", null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("ConditionBuilder IfPresent - 空字符串跳过")
    class BuilderEmptyStringSkipTests {

        @Test
        @DisplayName("likeIfPresent(field, \"\") 应跳过条件")
        void shouldSkipLikeIfPresentWhenEmpty() {
            Condition condition = Conditions.builder()
                    .likeIfPresent("name", "")
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeStartsWithIfPresent(field, \"\") 应跳过条件")
        void shouldSkipLikeStartsWithIfPresentWhenEmpty() {
            Condition condition = Conditions.builder()
                    .likeStartsWithIfPresent("name", "")
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeEndsWithIfPresent(field, \"\") 应跳过条件")
        void shouldSkipLikeEndsWithIfPresentWhenEmpty() {
            Condition condition = Conditions.builder()
                    .likeEndsWithIfPresent("name", "")
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notLikeIfPresent(field, \"\") 应跳过条件")
        void shouldSkipNotLikeIfPresentWhenEmpty() {
            Condition condition = Conditions.builder()
                    .notLikeIfPresent("name", "")
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("ConditionBuilder IfPresent - 空集合跳过")
    class BuilderEmptyCollectionSkipTests {

        @Test
        @DisplayName("inIfPresent(field, 空集合) 应跳过条件")
        void shouldSkipInIfPresentWhenEmpty() {
            Condition condition = Conditions.builder()
                    .inIfPresent("status", Collections.emptyList())
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notInIfPresent(field, 空集合) 应跳过条件")
        void shouldSkipNotInIfPresentWhenEmpty() {
            Condition condition = Conditions.builder()
                    .notInIfPresent("status", Collections.emptyList())
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("ConditionBuilder IfPresent - 正常值添加条件")
    class BuilderPresentValueTests {

        @Test
        @DisplayName("eqIfPresent(field, value) 应添加等于条件")
        void shouldAddEqIfPresentCondition() {
            Condition condition = Conditions.builder()
                    .eqIfPresent("status", 1)
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.EQ);
            assertThat(criterion.field()).isEqualTo("status");
            assertThat(criterion.value()).isEqualTo(1);
        }

        @Test
        @DisplayName("neIfPresent(field, value) 应添加不等于条件")
        void shouldAddNeIfPresentCondition() {
            Condition condition = Conditions.builder()
                    .neIfPresent("status", 0)
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.NE);
            assertThat(criterion.value()).isEqualTo(0);
        }

        @Test
        @DisplayName("likeIfPresent(field, value) 应添加 LIKE 条件并自动转义")
        void shouldAddLikeIfPresentCondition() {
            Condition condition = Conditions.builder()
                    .likeIfPresent("name", "test%value")
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.LIKE);
            assertThat(criterion.value()).isEqualTo("%test!%value%");
        }

        @Test
        @DisplayName("likeStartsWithIfPresent(field, value) 应添加前缀匹配条件")
        void shouldAddLikeStartsWithIfPresentCondition() {
            Condition condition = Conditions.builder()
                    .likeStartsWithIfPresent("name", "test")
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.LIKE_STARTS_WITH);
            assertThat(criterion.value()).isEqualTo("test%");
        }

        @Test
        @DisplayName("likeEndsWithIfPresent(field, value) 应添加后缀匹配条件")
        void shouldAddLikeEndsWithIfPresentCondition() {
            Condition condition = Conditions.builder()
                    .likeEndsWithIfPresent("name", "test")
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.LIKE_ENDS_WITH);
            assertThat(criterion.value()).isEqualTo("%test");
        }

        @Test
        @DisplayName("notLikeIfPresent(field, value) 应添加 NOT LIKE 条件")
        void shouldAddNotLikeIfPresentCondition() {
            Condition condition = Conditions.builder()
                    .notLikeIfPresent("name", "spam")
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.NOT_LIKE);
        }

        @Test
        @DisplayName("inIfPresent(field, values) 应添加 IN 条件")
        void shouldAddInIfPresentCondition() {
            Condition condition = Conditions.builder()
                    .inIfPresent("status", List.of(1, 2, 3))
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.IN);
            assertThat(criterion.field()).isEqualTo("status");
        }

        @Test
        @DisplayName("notInIfPresent(field, values) 应添加 NOT IN 条件")
        void shouldAddNotInIfPresentCondition() {
            Condition condition = Conditions.builder()
                    .notInIfPresent("status", List.of(0, -1))
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.NOT_IN);
        }

        @Test
        @DisplayName("betweenIfPresent(field, from, to) 应添加 BETWEEN 条件")
        void shouldAddBetweenIfPresentCondition() {
            Condition condition = Conditions.builder()
                    .betweenIfPresent("age", 18, 65)
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.BETWEEN);
            Comparable<?>[] values = (Comparable<?>[]) criterion.value();
            assertThat(values[0]).isEqualTo(18);
            assertThat(values[1]).isEqualTo(65);
        }

        @Test
        @DisplayName("notBetweenIfPresent(field, from, to) 应添加 NOT BETWEEN 条件")
        void shouldAddNotBetweenIfPresentCondition() {
            Condition condition = Conditions.builder()
                    .notBetweenIfPresent("age", 18, 65)
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.NOT_BETWEEN);
        }

        @Test
        @DisplayName("gtIfPresent(field, value) 应添加大于条件")
        void shouldAddGtIfPresentCondition() {
            Condition condition = Conditions.builder()
                    .gtIfPresent("age", 18)
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.GT);
            assertThat(criterion.value()).isEqualTo(18);
        }

        @Test
        @DisplayName("geIfPresent(field, value) 应添加大于等于条件")
        void shouldAddGeIfPresentCondition() {
            Condition condition = Conditions.builder()
                    .geIfPresent("age", 18)
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.GE);
        }

        @Test
        @DisplayName("ltIfPresent(field, value) 应添加小于条件")
        void shouldAddLtIfPresentCondition() {
            Condition condition = Conditions.builder()
                    .ltIfPresent("age", 65)
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.LT);
        }

        @Test
        @DisplayName("leIfPresent(field, value) 应添加小于等于条件")
        void shouldAddLeIfPresentCondition() {
            Condition condition = Conditions.builder()
                    .leIfPresent("age", 65)
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.LE);
        }
    }

    @Nested
    @DisplayName("ConditionBuilder IfPresent - 链式调用")
    class BuilderChainingTests {

        @Test
        @DisplayName("IfPresent 方法支持链式调用，null 值跳过不影响后续条件")
        void shouldChainIfPresentMethodsWithNullSkip() {
            Condition condition = Conditions.builder()
                    .eqIfPresent("status", 1)
                    .likeIfPresent("name", null)   // 跳过
                    .geIfPresent("age", 18)
                    .inIfPresent("dept", null)     // 跳过
                    .build();

            assertThat(condition.getCriteria()).hasSize(2);
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.EQ);
            assertThat(condition.getCriteria().get(1).operator()).isEqualTo(Operator.GE);
        }

        @Test
        @DisplayName("混合 IfPresent 和非 IfPresent 方法")
        void shouldMixIfPresentAndNonIfPresentMethods() {
            Condition condition = Conditions.builder()
                    .eq("status", 1)
                    .likeIfPresent("name", null)   // 跳过
                    .isNotNull("email")
                    .build();

            assertThat(condition.getCriteria()).hasSize(2);
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.EQ);
            assertThat(condition.getCriteria().get(1).operator()).isEqualTo(Operator.IS_NOT_NULL);
        }

        @Test
        @DisplayName("所有 IfPresent 值为 null 时返回空条件")
        void shouldReturnEmptyWhenAllIfPresentValuesAreNull() {
            Condition condition = Conditions.builder()
                    .eqIfPresent("status", null)
                    .likeIfPresent("name", null)
                    .gtIfPresent("age", null)
                    .build();

            assertThat(condition.isEmpty()).isTrue();
        }
    }

    // ==================== TypedConditionBuilder (Lambda 字段引用) ====================

    @Nested
    @DisplayName("TypedConditionBuilder IfPresent - null 值跳过")
    class TypedBuilderNullSkipTests {

        @Test
        @DisplayName("eqIfPresent(getter, null) 应跳过条件")
        void shouldSkipEqIfPresentWhenNull() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .eqIfPresent(TestEntity::getStatus, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("neIfPresent(getter, null) 应跳过条件")
        void shouldSkipNeIfPresentWhenNull() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .neIfPresent(TestEntity::getStatus, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeIfPresent(getter, null) 应跳过条件")
        void shouldSkipLikeIfPresentWhenNull() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .likeIfPresent(TestEntity::getName, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeStartsWithIfPresent(getter, null) 应跳过条件")
        void shouldSkipLikeStartsWithIfPresentWhenNull() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .likeStartsWithIfPresent(TestEntity::getName, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeEndsWithIfPresent(getter, null) 应跳过条件")
        void shouldSkipLikeEndsWithIfPresentWhenNull() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .likeEndsWithIfPresent(TestEntity::getName, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notLikeIfPresent(getter, null) 应跳过条件")
        void shouldSkipNotLikeIfPresentWhenNull() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .notLikeIfPresent(TestEntity::getName, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("inIfPresent(getter, null) 应跳过条件")
        void shouldSkipInIfPresentWhenNull() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .inIfPresent(TestEntity::getStatus, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notInIfPresent(getter, null) 应跳过条件")
        void shouldSkipNotInIfPresentWhenNull() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .notInIfPresent(TestEntity::getStatus, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("betweenIfPresent(getter, null, null) 应跳过条件")
        void shouldSkipBetweenIfPresentWhenBothNull() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .betweenIfPresent(TestEntity::getAge, null, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("betweenIfPresent(getter, from, null) 应跳过条件（部分 null）")
        void shouldSkipBetweenIfPresentWhenToIsNull() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .betweenIfPresent(TestEntity::getAge, 18, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("betweenIfPresent(getter, null, to) 应跳过条件（部分 null）")
        void shouldSkipBetweenIfPresentWhenFromIsNull() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .betweenIfPresent(TestEntity::getAge, null, 65)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notBetweenIfPresent(getter, null, null) 应跳过条件")
        void shouldSkipNotBetweenIfPresentWhenBothNull() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .notBetweenIfPresent(TestEntity::getAge, null, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("gtIfPresent(getter, null) 应跳过条件")
        void shouldSkipGtIfPresentWhenNull() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .gtIfPresent(TestEntity::getAge, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("geIfPresent(getter, null) 应跳过条件")
        void shouldSkipGeIfPresentWhenNull() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .geIfPresent(TestEntity::getAge, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("ltIfPresent(getter, null) 应跳过条件")
        void shouldSkipLtIfPresentWhenNull() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .ltIfPresent(TestEntity::getAge, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("leIfPresent(getter, null) 应跳过条件")
        void shouldSkipLeIfPresentWhenNull() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .leIfPresent(TestEntity::getAge, null)
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("TypedConditionBuilder IfPresent - 空字符串跳过")
    class TypedBuilderEmptyStringSkipTests {

        @Test
        @DisplayName("likeIfPresent(getter, \"\") 应跳过条件")
        void shouldSkipLikeIfPresentWhenEmpty() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .likeIfPresent(TestEntity::getName, "")
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeStartsWithIfPresent(getter, \"\") 应跳过条件")
        void shouldSkipLikeStartsWithIfPresentWhenEmpty() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .likeStartsWithIfPresent(TestEntity::getName, "")
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeEndsWithIfPresent(getter, \"\") 应跳过条件")
        void shouldSkipLikeEndsWithIfPresentWhenEmpty() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .likeEndsWithIfPresent(TestEntity::getName, "")
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notLikeIfPresent(getter, \"\") 应跳过条件")
        void shouldSkipNotLikeIfPresentWhenEmpty() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .notLikeIfPresent(TestEntity::getName, "")
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("TypedConditionBuilder IfPresent - 空集合跳过")
    class TypedBuilderEmptyCollectionSkipTests {

        @Test
        @DisplayName("inIfPresent(getter, 空集合) 应跳过条件")
        void shouldSkipInIfPresentWhenEmpty() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .inIfPresent(TestEntity::getStatus, Collections.emptyList())
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notInIfPresent(getter, 空集合) 应跳过条件")
        void shouldSkipNotInIfPresentWhenEmpty() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .notInIfPresent(TestEntity::getStatus, Collections.emptyList())
                    .build();
            assertThat(condition.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("TypedConditionBuilder IfPresent - 正常值添加条件")
    class TypedBuilderPresentValueTests {

        @Test
        @DisplayName("eqIfPresent(getter, value) 应添加等于条件")
        void shouldAddEqIfPresentCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .eqIfPresent(TestEntity::getStatus, 1)
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.EQ);
        }

        @Test
        @DisplayName("neIfPresent(getter, value) 应添加不等于条件")
        void shouldAddNeIfPresentCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .neIfPresent(TestEntity::getStatus, 0)
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.NE);
        }

        @Test
        @DisplayName("likeIfPresent(getter, value) 应添加 LIKE 条件")
        void shouldAddLikeIfPresentCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .likeIfPresent(TestEntity::getName, "test")
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.LIKE);
        }

        @Test
        @DisplayName("likeStartsWithIfPresent(getter, value) 应添加前缀匹配条件")
        void shouldAddLikeStartsWithIfPresentCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .likeStartsWithIfPresent(TestEntity::getName, "test")
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.LIKE_STARTS_WITH);
        }

        @Test
        @DisplayName("likeEndsWithIfPresent(getter, value) 应添加后缀匹配条件")
        void shouldAddLikeEndsWithIfPresentCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .likeEndsWithIfPresent(TestEntity::getName, "test")
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.LIKE_ENDS_WITH);
        }

        @Test
        @DisplayName("notLikeIfPresent(getter, value) 应添加 NOT LIKE 条件")
        void shouldAddNotLikeIfPresentCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .notLikeIfPresent(TestEntity::getName, "spam")
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.NOT_LIKE);
        }

        @Test
        @DisplayName("inIfPresent(getter, values) 应添加 IN 条件")
        void shouldAddInIfPresentCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .inIfPresent(TestEntity::getStatus, List.of(1, 2, 3))
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.IN);
        }

        @Test
        @DisplayName("notInIfPresent(getter, values) 应添加 NOT IN 条件")
        void shouldAddNotInIfPresentCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .notInIfPresent(TestEntity::getStatus, List.of(0, -1))
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.NOT_IN);
        }

        @Test
        @DisplayName("betweenIfPresent(getter, from, to) 应添加 BETWEEN 条件")
        void shouldAddBetweenIfPresentCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .betweenIfPresent(TestEntity::getAge, 18, 65)
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.BETWEEN);
        }

        @Test
        @DisplayName("notBetweenIfPresent(getter, from, to) 应添加 NOT BETWEEN 条件")
        void shouldAddNotBetweenIfPresentCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .notBetweenIfPresent(TestEntity::getAge, 18, 65)
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.NOT_BETWEEN);
        }

        @Test
        @DisplayName("gtIfPresent(getter, value) 应添加大于条件")
        void shouldAddGtIfPresentCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .gtIfPresent(TestEntity::getAge, 18)
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.GT);
        }

        @Test
        @DisplayName("geIfPresent(getter, value) 应添加大于等于条件")
        void shouldAddGeIfPresentCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .geIfPresent(TestEntity::getAge, 18)
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.GE);
        }

        @Test
        @DisplayName("ltIfPresent(getter, value) 应添加小于条件")
        void shouldAddLtIfPresentCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .ltIfPresent(TestEntity::getAge, 65)
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.LT);
        }

        @Test
        @DisplayName("leIfPresent(getter, value) 应添加小于等于条件")
        void shouldAddLeIfPresentCondition() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .leIfPresent(TestEntity::getAge, 65)
                    .build();
            assertThat(condition.getCriteria()).hasSize(1);
            Criterion criterion = condition.getCriteria().getFirst();
            assertThat(criterion.operator()).isEqualTo(Operator.LE);
        }
    }

    @Nested
    @DisplayName("TypedConditionBuilder IfPresent - 链式调用")
    class TypedBuilderChainingTests {

        @Test
        @DisplayName("IfPresent 方法支持链式调用，null 值跳过不影响后续条件")
        void shouldChainIfPresentMethodsWithNullSkip() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .eqIfPresent(TestEntity::getStatus, 1)
                    .likeIfPresent(TestEntity::getName, null)   // 跳过
                    .geIfPresent(TestEntity::getAge, 18)
                    .build();

            assertThat(condition.getCriteria()).hasSize(2);
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.EQ);
            assertThat(condition.getCriteria().get(1).operator()).isEqualTo(Operator.GE);
        }

        @Test
        @DisplayName("混合 IfPresent 和非 IfPresent 方法")
        void shouldMixIfPresentAndNonIfPresentMethods() {
            Condition condition = Conditions.builder(TestEntity.class)
                    .eq(TestEntity::getStatus, 1)
                    .likeIfPresent(TestEntity::getName, null)   // 跳过
                    .isNotNull(TestEntity::getEmail)
                    .build();

            assertThat(condition.getCriteria()).hasSize(2);
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.EQ);
            assertThat(condition.getCriteria().get(1).operator()).isEqualTo(Operator.IS_NOT_NULL);
        }
    }

    // ==================== Conditions 静态 IfPresent 方法 ====================

    @Nested
    @DisplayName("Conditions 静态 IfPresent 方法 - 字符串字段名")
    class StaticIfPresentTests {

        @Test
        @DisplayName("eqIfPresent(field, null) 应返回空条件")
        void shouldReturnEmptyForEqIfPresentNull() {
            Condition condition = Conditions.eqIfPresent("status", null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("eqIfPresent(field, value) 应创建等于条件")
        void shouldCreateEqIfPresentCondition() {
            Condition condition = Conditions.eqIfPresent("status", 1);
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.EQ);
            assertThat(condition.getCriteria().getFirst().value()).isEqualTo(1);
        }

        @Test
        @DisplayName("neIfPresent(field, null) 应返回空条件")
        void shouldReturnEmptyForNeIfPresentNull() {
            Condition condition = Conditions.neIfPresent("status", null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("neIfPresent(field, value) 应创建不等于条件")
        void shouldCreateNeIfPresentCondition() {
            Condition condition = Conditions.neIfPresent("status", 0);
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.NE);
        }

        @Test
        @DisplayName("likeIfPresent(field, null) 应返回空条件")
        void shouldReturnEmptyForLikeIfPresentNull() {
            Condition condition = Conditions.likeIfPresent("name", null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeIfPresent(field, \"\") 应返回空条件")
        void shouldReturnEmptyForLikeIfPresentEmpty() {
            Condition condition = Conditions.likeIfPresent("name", "");
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeIfPresent(field, value) 应创建 LIKE 条件")
        void shouldCreateLikeIfPresentCondition() {
            Condition condition = Conditions.likeIfPresent("name", "test");
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.LIKE);
        }

        @Test
        @DisplayName("likeStartsWithIfPresent(field, null) 应返回空条件")
        void shouldReturnEmptyForLikeStartsWithIfPresentNull() {
            Condition condition = Conditions.likeStartsWithIfPresent("name", null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeStartsWithIfPresent(field, \"\") 应返回空条件")
        void shouldReturnEmptyForLikeStartsWithIfPresentEmpty() {
            Condition condition = Conditions.likeStartsWithIfPresent("name", "");
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeEndsWithIfPresent(field, null) 应返回空条件")
        void shouldReturnEmptyForLikeEndsWithIfPresentNull() {
            Condition condition = Conditions.likeEndsWithIfPresent("name", null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeEndsWithIfPresent(field, \"\") 应返回空条件")
        void shouldReturnEmptyForLikeEndsWithIfPresentEmpty() {
            Condition condition = Conditions.likeEndsWithIfPresent("name", "");
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notLikeIfPresent(field, null) 应返回空条件")
        void shouldReturnEmptyForNotLikeIfPresentNull() {
            Condition condition = Conditions.notLikeIfPresent("name", null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notLikeIfPresent(field, \"\") 应返回空条件")
        void shouldReturnEmptyForNotLikeIfPresentEmpty() {
            Condition condition = Conditions.notLikeIfPresent("name", "");
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("inIfPresent(field, null) 应返回空条件")
        void shouldReturnEmptyForInIfPresentNull() {
            Condition condition = Conditions.inIfPresent("status", null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("inIfPresent(field, 空集合) 应返回空条件")
        void shouldReturnEmptyForInIfPresentEmpty() {
            Condition condition = Conditions.inIfPresent("status", Collections.emptyList());
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("inIfPresent(field, values) 应创建 IN 条件")
        void shouldCreateInIfPresentCondition() {
            Condition condition = Conditions.inIfPresent("status", List.of(1, 2, 3));
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.IN);
        }

        @Test
        @DisplayName("notInIfPresent(field, null) 应返回空条件")
        void shouldReturnEmptyForNotInIfPresentNull() {
            Condition condition = Conditions.notInIfPresent("status", null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notInIfPresent(field, 空集合) 应返回空条件")
        void shouldReturnEmptyForNotInIfPresentEmpty() {
            Condition condition = Conditions.notInIfPresent("status", Collections.emptyList());
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("betweenIfPresent(field, null, null) 应返回空条件")
        void shouldReturnEmptyForBetweenIfPresentBothNull() {
            Condition condition = Conditions.betweenIfPresent("age", null, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("betweenIfPresent(field, from, null) 应返回空条件（部分 null）")
        void shouldReturnEmptyForBetweenIfPresentPartialNull() {
            Condition condition = Conditions.betweenIfPresent("age", 18, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("betweenIfPresent(field, from, to) 应创建 BETWEEN 条件")
        void shouldCreateBetweenIfPresentCondition() {
            Condition condition = Conditions.betweenIfPresent("age", 18, 65);
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.BETWEEN);
        }

        @Test
        @DisplayName("notBetweenIfPresent(field, null, null) 应返回空条件")
        void shouldReturnEmptyForNotBetweenIfPresentBothNull() {
            Condition condition = Conditions.notBetweenIfPresent("age", null, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notBetweenIfPresent(field, from, to) 应创建 NOT BETWEEN 条件")
        void shouldCreateNotBetweenIfPresentCondition() {
            Condition condition = Conditions.notBetweenIfPresent("age", 18, 65);
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.NOT_BETWEEN);
        }

        @Test
        @DisplayName("gtIfPresent(field, null) 应返回空条件")
        void shouldReturnEmptyForGtIfPresentNull() {
            Condition condition = Conditions.gtIfPresent("age", null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("gtIfPresent(field, value) 应创建大于条件")
        void shouldCreateGtIfPresentCondition() {
            Condition condition = Conditions.gtIfPresent("age", 18);
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.GT);
        }

        @Test
        @DisplayName("geIfPresent(field, null) 应返回空条件")
        void shouldReturnEmptyForGeIfPresentNull() {
            Condition condition = Conditions.geIfPresent("age", null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("geIfPresent(field, value) 应创建大于等于条件")
        void shouldCreateGeIfPresentCondition() {
            Condition condition = Conditions.geIfPresent("age", 18);
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.GE);
        }

        @Test
        @DisplayName("ltIfPresent(field, null) 应返回空条件")
        void shouldReturnEmptyForLtIfPresentNull() {
            Condition condition = Conditions.ltIfPresent("age", null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("ltIfPresent(field, value) 应创建小于条件")
        void shouldCreateLtIfPresentCondition() {
            Condition condition = Conditions.ltIfPresent("age", 65);
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.LT);
        }

        @Test
        @DisplayName("leIfPresent(field, null) 应返回空条件")
        void shouldReturnEmptyForLeIfPresentNull() {
            Condition condition = Conditions.leIfPresent("age", null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("leIfPresent(field, value) 应创建小于等于条件")
        void shouldCreateLeIfPresentCondition() {
            Condition condition = Conditions.leIfPresent("age", 65);
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.LE);
        }
    }

    @Nested
    @DisplayName("Conditions 静态 IfPresent 方法 - 类型化")
    class StaticTypedIfPresentTests {

        @Test
        @DisplayName("eqIfPresent(Class, getter, null) 应返回空条件")
        void shouldReturnEmptyForTypedEqIfPresentNull() {
            Condition condition = Conditions.eqIfPresent(TestEntity.class, TestEntity::getStatus, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("eqIfPresent(Class, getter, value) 应创建等于条件")
        void shouldCreateTypedEqIfPresentCondition() {
            Condition condition = Conditions.eqIfPresent(TestEntity.class, TestEntity::getStatus, 1);
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.EQ);
        }

        @Test
        @DisplayName("neIfPresent(Class, getter, null) 应返回空条件")
        void shouldReturnEmptyForTypedNeIfPresentNull() {
            Condition condition = Conditions.neIfPresent(TestEntity.class, TestEntity::getStatus, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeIfPresent(Class, getter, null) 应返回空条件")
        void shouldReturnEmptyForTypedLikeIfPresentNull() {
            Condition condition = Conditions.likeIfPresent(TestEntity.class, TestEntity::getName, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeIfPresent(Class, getter, \"\") 应返回空条件")
        void shouldReturnEmptyForTypedLikeIfPresentEmpty() {
            Condition condition = Conditions.likeIfPresent(TestEntity.class, TestEntity::getName, "");
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeIfPresent(Class, getter, value) 应创建 LIKE 条件")
        void shouldCreateTypedLikeIfPresentCondition() {
            Condition condition = Conditions.likeIfPresent(TestEntity.class, TestEntity::getName, "test");
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.LIKE);
        }

        @Test
        @DisplayName("likeStartsWithIfPresent(Class, getter, null) 应返回空条件")
        void shouldReturnEmptyForTypedLikeStartsWithIfPresentNull() {
            Condition condition = Conditions.likeStartsWithIfPresent(TestEntity.class, TestEntity::getName, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeStartsWithIfPresent(Class, getter, \"\") 应返回空条件")
        void shouldReturnEmptyForTypedLikeStartsWithIfPresentEmpty() {
            Condition condition = Conditions.likeStartsWithIfPresent(TestEntity.class, TestEntity::getName, "");
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("likeEndsWithIfPresent(Class, getter, null) 应返回空条件")
        void shouldReturnEmptyForTypedLikeEndsWithIfPresentNull() {
            Condition condition = Conditions.likeEndsWithIfPresent(TestEntity.class, TestEntity::getName, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notLikeIfPresent(Class, getter, null) 应返回空条件")
        void shouldReturnEmptyForTypedNotLikeIfPresentNull() {
            Condition condition = Conditions.notLikeIfPresent(TestEntity.class, TestEntity::getName, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notLikeIfPresent(Class, getter, \"\") 应返回空条件")
        void shouldReturnEmptyForTypedNotLikeIfPresentEmpty() {
            Condition condition = Conditions.notLikeIfPresent(TestEntity.class, TestEntity::getName, "");
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("inIfPresent(Class, getter, null) 应返回空条件")
        void shouldReturnEmptyForTypedInIfPresentNull() {
            Condition condition = Conditions.inIfPresent(TestEntity.class, TestEntity::getStatus, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("inIfPresent(Class, getter, 空集合) 应返回空条件")
        void shouldReturnEmptyForTypedInIfPresentEmpty() {
            Condition condition = Conditions.inIfPresent(TestEntity.class, TestEntity::getStatus, Collections.emptyList());
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("inIfPresent(Class, getter, values) 应创建 IN 条件")
        void shouldCreateTypedInIfPresentCondition() {
            Condition condition = Conditions.inIfPresent(TestEntity.class, TestEntity::getStatus, List.of(1, 2, 3));
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.IN);
        }

        @Test
        @DisplayName("notInIfPresent(Class, getter, null) 应返回空条件")
        void shouldReturnEmptyForTypedNotInIfPresentNull() {
            Condition condition = Conditions.notInIfPresent(TestEntity.class, TestEntity::getStatus, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notInIfPresent(Class, getter, 空集合) 应返回空条件")
        void shouldReturnEmptyForTypedNotInIfPresentEmpty() {
            Condition condition = Conditions.notInIfPresent(TestEntity.class, TestEntity::getStatus, Collections.emptyList());
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("betweenIfPresent(Class, getter, null, null) 应返回空条件")
        void shouldReturnEmptyForTypedBetweenIfPresentBothNull() {
            Condition condition = Conditions.betweenIfPresent(TestEntity.class, TestEntity::getAge, null, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("betweenIfPresent(Class, getter, from, null) 应返回空条件（部分 null）")
        void shouldReturnEmptyForTypedBetweenIfPresentPartialNull() {
            Condition condition = Conditions.betweenIfPresent(TestEntity.class, TestEntity::getAge, 18, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("betweenIfPresent(Class, getter, from, to) 应创建 BETWEEN 条件")
        void shouldCreateTypedBetweenIfPresentCondition() {
            Condition condition = Conditions.betweenIfPresent(TestEntity.class, TestEntity::getAge, 18, 65);
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.BETWEEN);
        }

        @Test
        @DisplayName("notBetweenIfPresent(Class, getter, null, null) 应返回空条件")
        void shouldReturnEmptyForTypedNotBetweenIfPresentBothNull() {
            Condition condition = Conditions.notBetweenIfPresent(TestEntity.class, TestEntity::getAge, null, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("notBetweenIfPresent(Class, getter, from, to) 应创建 NOT BETWEEN 条件")
        void shouldCreateTypedNotBetweenIfPresentCondition() {
            Condition condition = Conditions.notBetweenIfPresent(TestEntity.class, TestEntity::getAge, 18, 65);
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.NOT_BETWEEN);
        }

        @Test
        @DisplayName("gtIfPresent(Class, getter, null) 应返回空条件")
        void shouldReturnEmptyForTypedGtIfPresentNull() {
            Condition condition = Conditions.gtIfPresent(TestEntity.class, TestEntity::getAge, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("gtIfPresent(Class, getter, value) 应创建大于条件")
        void shouldCreateTypedGtIfPresentCondition() {
            Condition condition = Conditions.gtIfPresent(TestEntity.class, TestEntity::getAge, 18);
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.GT);
        }

        @Test
        @DisplayName("geIfPresent(Class, getter, null) 应返回空条件")
        void shouldReturnEmptyForTypedGeIfPresentNull() {
            Condition condition = Conditions.geIfPresent(TestEntity.class, TestEntity::getAge, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("geIfPresent(Class, getter, value) 应创建大于等于条件")
        void shouldCreateTypedGeIfPresentCondition() {
            Condition condition = Conditions.geIfPresent(TestEntity.class, TestEntity::getAge, 18);
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.GE);
        }

        @Test
        @DisplayName("ltIfPresent(Class, getter, null) 应返回空条件")
        void shouldReturnEmptyForTypedLtIfPresentNull() {
            Condition condition = Conditions.ltIfPresent(TestEntity.class, TestEntity::getAge, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("ltIfPresent(Class, getter, value) 应创建小于条件")
        void shouldCreateTypedLtIfPresentCondition() {
            Condition condition = Conditions.ltIfPresent(TestEntity.class, TestEntity::getAge, 65);
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.LT);
        }

        @Test
        @DisplayName("leIfPresent(Class, getter, null) 应返回空条件")
        void shouldReturnEmptyForTypedLeIfPresentNull() {
            Condition condition = Conditions.leIfPresent(TestEntity.class, TestEntity::getAge, null);
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("leIfPresent(Class, getter, value) 应创建小于等于条件")
        void shouldCreateTypedLeIfPresentCondition() {
            Condition condition = Conditions.leIfPresent(TestEntity.class, TestEntity::getAge, 65);
            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.LE);
        }
    }

    // ==================== IfPresent vs 非 IfPresent 行为对比 ====================

    @Nested
    @DisplayName("IfPresent vs 非 IfPresent 行为对比")
    class IfPresentVsNonIfPresentTests {

        @Test
        @DisplayName("eq(null) 转换为 IS NULL，eqIfPresent(null) 跳过条件")
        void shouldDifferForEqNull() {
            Condition eqCondition = Conditions.eq("status", null);
            Condition eqIfPresentCondition = Conditions.eqIfPresent("status", null);

            assertThat(eqCondition.getCriteria().getFirst().operator()).isEqualTo(Operator.IS_NULL);
            assertThat(eqIfPresentCondition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("ne(null) 转换为 IS NOT NULL，neIfPresent(null) 跳过条件")
        void shouldDifferForNeNull() {
            Condition neCondition = Conditions.ne("status", null);
            Condition neIfPresentCondition = Conditions.neIfPresent("status", null);

            assertThat(neCondition.getCriteria().getFirst().operator()).isEqualTo(Operator.IS_NOT_NULL);
            assertThat(neIfPresentCondition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("like(null) 转换为 IS NULL，likeIfPresent(null) 跳过条件")
        void shouldDifferForLikeNull() {
            Condition likeCondition = Conditions.like("name", null);
            Condition likeIfPresentCondition = Conditions.likeIfPresent("name", null);

            assertThat(likeCondition.getCriteria().getFirst().operator()).isEqualTo(Operator.IS_NULL);
            assertThat(likeIfPresentCondition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("in(空集合) 返回 none()，inIfPresent(空集合) 跳过条件")
        void shouldDifferForInEmptyCollection() {
            Condition inCondition = Conditions.in("status", Collections.emptyList());
            Condition inIfPresentCondition = Conditions.inIfPresent("status", Collections.emptyList());

            assertThat(inCondition).isInstanceOf(io.github.afgprojects.framework.data.core.query.DenyAllCondition.class);
            assertThat(inIfPresentCondition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("非 null 值时 IfPresent 和非 IfPresent 结果一致")
        void shouldProduceSameResultForNonNullValue() {
            Condition eqCondition = Conditions.eq("status", 1);
            Condition eqIfPresentCondition = Conditions.eqIfPresent("status", 1);

            assertThat(eqCondition.getCriteria().getFirst().operator()).isEqualTo(Operator.EQ);
            assertThat(eqIfPresentCondition.getCriteria().getFirst().operator()).isEqualTo(Operator.EQ);
            assertThat(eqCondition.getCriteria().getFirst().value()).isEqualTo(1);
            assertThat(eqIfPresentCondition.getCriteria().getFirst().value()).isEqualTo(1);
        }
    }

    // ==================== 动态查询场景模拟 ====================

    @Nested
    @DisplayName("动态查询场景模拟")
    class DynamicQueryScenarioTests {

        @Test
        @DisplayName("模拟前端搜索：部分条件为空")
        void shouldBuildDynamicSearchCondition() {
            String nameKeyword = "test";
            Integer status = null;
            Integer minAge = 18;
            Integer maxAge = null;
            List<Integer> deptIds = Arrays.asList(1, 2, 3);

            Condition condition = Conditions.builder()
                    .likeIfPresent("name", nameKeyword)         // 添加
                    .eqIfPresent("status", status)              // 跳过
                    .geIfPresent("age", minAge)                 // 添加
                    .ltIfPresent("age", maxAge)                 // 跳过
                    .inIfPresent("dept_id", deptIds)            // 添加
                    .build();

            assertThat(condition.getCriteria()).hasSize(3);
            assertThat(condition.getCriteria().get(0).operator()).isEqualTo(Operator.LIKE);
            assertThat(condition.getCriteria().get(1).operator()).isEqualTo(Operator.GE);
            assertThat(condition.getCriteria().get(2).operator()).isEqualTo(Operator.IN);
        }

        @Test
        @DisplayName("模拟前端搜索：所有条件为空")
        void shouldBuildEmptyWhenAllSearchParamsAreNull() {
            String nameKeyword = null;
            Integer status = null;
            Integer minAge = null;

            Condition condition = Conditions.builder()
                    .likeIfPresent("name", nameKeyword)
                    .eqIfPresent("status", status)
                    .geIfPresent("age", minAge)
                    .build();

            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("模拟前端搜索：between 范围查询，仅部分有值")
        void shouldSkipBetweenWhenPartialNull() {
            Integer minPrice = 100;
            Integer maxPrice = null;

            Condition condition = Conditions.builder()
                    .betweenIfPresent("price", minPrice, maxPrice)  // 跳过
                    .geIfPresent("price", minPrice)                 // 添加
                    .build();

            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.GE);
        }

        @Test
        @DisplayName("模拟前端搜索：空字符串关键词")
        void shouldSkipEmptyStringKeyword() {
            String keyword = "";

            Condition condition = Conditions.builder()
                    .likeIfPresent("name", keyword)     // 跳过（空字符串）
                    .eqIfPresent("status", 1)           // 添加
                    .build();

            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().getFirst().operator()).isEqualTo(Operator.EQ);
        }
    }

    /**
     * 测试用实体类
     */
    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    static class TestEntity {
        private String name;
        private Integer status;
        private Integer age;
        private String email;
    }
}
