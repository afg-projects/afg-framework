package io.github.afgprojects.framework.data.core.query;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConditionImpl 测试
 */
@DisplayName("ConditionImpl 测试")
class ConditionImplTest {

    @Nested
    @DisplayName("空条件行为")
    class EmptyConditionTests {

        @Test
        @DisplayName("Condition.empty() 的 isEmpty() 应为 true")
        void shouldReturnTrueForIsEmpty() {
            Condition empty = Condition.empty();
            assertThat(empty.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Condition.empty() 的 getCriteria() 应为空列表")
        void shouldReturnEmptyCriteria() {
            Condition empty = Condition.empty();
            assertThat(empty.getCriteria()).isEmpty();
        }

        @Test
        @DisplayName("Condition.empty() 的 getOperator() 应为 AND")
        void shouldReturnAndOperator() {
            Condition empty = Condition.empty();
            assertThat(empty.getOperator()).isEqualTo(LogicalOperator.AND);
        }
    }

    @Nested
    @DisplayName("add 方法")
    class AddTests {

        @Test
        @DisplayName("应添加 Criterion 到现有条件")
        void shouldAddCriterion() {
            Condition condition = Conditions.eq("status", 1);
            Criterion newCriterion = Criterion.of("type", Operator.EQ, "A");
            Condition result = condition.add(newCriterion);

            assertThat(result.getCriteria()).hasSize(2);
            assertThat(result.getCriteria().get(0).field()).isEqualTo("status");
            assertThat(result.getCriteria().get(1).field()).isEqualTo("type");
        }

        @Test
        @DisplayName("add 应返回新条件，不修改原条件")
        void shouldReturnNewConditionWithoutModifyingOriginal() {
            Condition original = Conditions.eq("status", 1);
            Criterion newCriterion = Criterion.of("type", Operator.EQ, "A");
            Condition result = original.add(newCriterion);

            assertThat(original.getCriteria()).hasSize(1);
            assertThat(result.getCriteria()).hasSize(2);
            assertThat(result).isNotSameAs(original);
        }
    }

    @Nested
    @DisplayName("and 方法合并优化")
    class AndMergeTests {

        @Test
        @DisplayName("this.empty().and(other) 应返回 other")
        void shouldReturnOtherWhenThisIsEmpty() {
            Condition empty = Condition.empty();
            Condition other = Conditions.eq("status", 1);
            Condition result = empty.and(other);

            assertThat(result).isSameAs(other);
        }

        @Test
        @DisplayName("this.and(empty) 应返回 this")
        void shouldReturnThisWhenOtherIsEmpty() {
            Condition condition = Conditions.eq("status", 1);
            Condition empty = Condition.empty();
            Condition result = condition.and(empty);

            assertThat(result).isSameAs(condition);
        }

        @Test
        @DisplayName("两个 AND 条件合并应合并 criteria")
        void shouldMergeCriteriaWhenBothAreAnd() {
            Condition c1 = Conditions.eq("status", 1);
            Condition c2 = Conditions.eq("type", "A");
            Condition result = c1.and(c2);

            assertThat(result.getOperator()).isEqualTo(LogicalOperator.AND);
            assertThat(result.getCriteria()).hasSize(2);
            assertThat(result.getCriteria().get(0).field()).isEqualTo("status");
            assertThat(result.getCriteria().get(1).field()).isEqualTo("type");
        }

        @Test
        @DisplayName("AND + 非AND条件 应使用嵌套条件保持逻辑结构")
        void shouldUseNestedConditionForAndWithNonAnd() {
            Condition andCondition = Conditions.eq("status", 1);
            // 构建 OR 条件: 使用 ConditionImpl 直接创建 OR 条件
            Condition orCondition = new ConditionImpl(LogicalOperator.OR, List.of(
                Criterion.of("type", Operator.EQ, "A"),
                Criterion.of("type", Operator.EQ, "B")
            ));
            Condition result = andCondition.and(orCondition);

            assertThat(result.getOperator()).isEqualTo(LogicalOperator.AND);
            assertThat(result.getCriteria()).hasSize(2);
            // 两个都应该是嵌套条件
            assertThat(result.getCriteria().get(0).isNested()).isTrue();
            assertThat(result.getCriteria().get(1).isNested()).isTrue();
        }
    }

    @Nested
    @DisplayName("or 方法合并优化")
    class OrMergeTests {

        @Test
        @DisplayName("this.empty().or(other) 应返回 other")
        void shouldReturnOtherWhenThisIsEmpty() {
            Condition empty = Condition.empty();
            Condition other = Conditions.eq("status", 1);
            Condition result = empty.or(other);

            assertThat(result).isSameAs(other);
        }

        @Test
        @DisplayName("this.or(empty) 应返回 this")
        void shouldReturnThisWhenOtherIsEmpty() {
            Condition condition = Conditions.eq("status", 1);
            Condition empty = Condition.empty();
            Condition result = condition.or(empty);

            assertThat(result).isSameAs(condition);
        }

        @Test
        @DisplayName("两个 OR 条件合并应合并 criteria")
        void shouldMergeCriteriaWhenBothAreOr() {
            // 直接创建两个 OR 条件
            Condition c1 = new ConditionImpl(LogicalOperator.OR, List.of(
                Criterion.of("status", Operator.EQ, 1),
                Criterion.of("status", Operator.EQ, 2)
            ));
            Condition c2 = new ConditionImpl(LogicalOperator.OR, List.of(
                Criterion.of("status", Operator.EQ, 3),
                Criterion.of("status", Operator.EQ, 4)
            ));

            Condition result = c1.or(c2);

            assertThat(result.getOperator()).isEqualTo(LogicalOperator.OR);
            assertThat(result.getCriteria()).hasSize(4);
        }

        @Test
        @DisplayName("AND + OR 应使用嵌套条件保持逻辑结构")
        void shouldUseNestedConditionForAndOr() {
            Condition andCondition = Conditions.eq("status", 1);
            Condition orCondition = Conditions.builder()
                .eq("type", "A")
                .or(Conditions.eq("type", "B"))
                .build();
            Condition result = andCondition.or(orCondition);

            assertThat(result.getOperator()).isEqualTo(LogicalOperator.OR);
            assertThat(result.getCriteria()).hasSize(2);
            assertThat(result.getCriteria().get(0).isNested()).isTrue();
            assertThat(result.getCriteria().get(1).isNested()).isTrue();
        }
    }

    @Nested
    @DisplayName("NotCondition")
    class NotConditionTests {

        @Test
        @DisplayName("not() 应创建 NotCondition")
        void shouldCreateNotCondition() {
            Condition original = Conditions.eq("status", 1);
            Condition notCondition = original.not();

            assertThat(notCondition).isInstanceOf(NotCondition.class);
            assertThat(((NotCondition) notCondition).getOriginal()).isSameAs(original);
        }

        @Test
        @DisplayName("not().not() 应返回原始条件（双重取反）")
        void shouldReturnOriginalForDoubleNegation() {
            Condition original = Conditions.eq("status", 1);
            Condition doubleNot = original.not().not();

            assertThat(doubleNot).isSameAs(original);
        }

        @Test
        @DisplayName("NotCondition 的 getCriteria() 应包含取反嵌套条件")
        void shouldContainNegatedNestedCondition() {
            Condition original = Conditions.eq("status", 1);
            Condition notCondition = original.not();

            assertThat(notCondition.getCriteria()).hasSize(1);
            Criterion criterion = notCondition.getCriteria().get(0);
            assertThat(criterion.isNegated()).isTrue();
            assertThat(criterion.nestedCondition()).isSameAs(original);
        }

        @Test
        @DisplayName("NotCondition 的 isEmpty() 应与原始条件一致")
        void shouldMatchOriginalIsEmpty() {
            Condition original = Conditions.eq("status", 1);
            Condition notCondition = original.not();
            assertThat(notCondition.isEmpty()).isEqualTo(original.isEmpty());

            Condition emptyNot = Condition.empty().not();
            assertThat(emptyNot.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("NotCondition 的 getOperator() 应为 AND")
        void shouldReturnAndOperator() {
            Condition notCondition = Conditions.eq("status", 1).not();
            assertThat(notCondition.getOperator()).isEqualTo(LogicalOperator.AND);
        }
    }

    @Nested
    @DisplayName("DenyAllCondition")
    class DenyAllConditionTests {

        @Test
        @DisplayName("isEmpty() 应为 false")
        void shouldReturnFalseForIsEmpty() {
            Condition denyAll = Conditions.none();
            assertThat(denyAll.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("and(any) 应返回 self（denyAll AND anything = denyAll）")
        void shouldReturnSelfWhenAndWithAny() {
            Condition denyAll = Conditions.none();
            Condition other = Conditions.eq("status", 1);
            Condition result = denyAll.and(other);

            assertThat(result).isSameAs(denyAll);
        }

        @Test
        @DisplayName("or(any) 应返回 other（denyAll OR anything = anything）")
        void shouldReturnOtherWhenOrWithAny() {
            Condition denyAll = Conditions.none();
            Condition other = Conditions.eq("status", 1);
            Condition result = denyAll.or(other);

            assertThat(result).isSameAs(other);
        }

        @Test
        @DisplayName("not() 应返回空条件（NOT(1=0) = 允许全部）")
        void shouldReturnEmptyWhenNot() {
            Condition denyAll = Conditions.none();
            Condition result = denyAll.not();

            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("add(criterion) 应返回 self（denyAll AND anything = denyAll）")
        void shouldReturnSelfWhenAddCriterion() {
            Condition denyAll = Conditions.none();
            Criterion criterion = Criterion.of("status", Operator.EQ, 1);
            Condition result = denyAll.add(criterion);

            assertThat(result).isSameAs(denyAll);
        }

        @Test
        @DisplayName("getCriteria() 应为空列表")
        void shouldReturnEmptyCriteria() {
            Condition denyAll = Conditions.none();
            assertThat(denyAll.getCriteria()).isEmpty();
        }

        @Test
        @DisplayName("getOperator() 应为 AND")
        void shouldReturnAndOperator() {
            Condition denyAll = Conditions.none();
            assertThat(denyAll.getOperator()).isEqualTo(LogicalOperator.AND);
        }

        @Test
        @DisplayName("denyAll.or(denyAll) 应返回 denyAll")
        void shouldReturnSelfWhenOrWithDenyAll() {
            Condition denyAll1 = Conditions.none();
            Condition denyAll2 = Conditions.none();
            Condition result = denyAll1.or(denyAll2);

            assertThat(result).isSameAs(denyAll2);
        }
    }

    @Nested
    @DisplayName("ConditionImpl 构造")
    class ConditionImplConstructionTests {

        @Test
        @DisplayName("应正确存储 operator 和 criteria")
        void shouldStoreOperatorAndCriteria() {
            Criterion c1 = Criterion.of("status", Operator.EQ, 1);
            Criterion c2 = Criterion.of("type", Operator.NE, "X");
            Condition condition = new ConditionImpl(LogicalOperator.AND, List.of(c1, c2));

            assertThat(condition.getOperator()).isEqualTo(LogicalOperator.AND);
            assertThat(condition.getCriteria()).hasSize(2);
            assertThat(condition.getCriteria().get(0)).isSameAs(c1);
            assertThat(condition.getCriteria().get(1)).isSameAs(c2);
        }

        @Test
        @DisplayName("criteria 应为不可变列表")
        void shouldReturnImmutableCriteria() {
            Criterion c1 = Criterion.of("status", Operator.EQ, 1);
            Condition condition = new ConditionImpl(LogicalOperator.AND, List.of(c1));

            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> condition.getCriteria().add(Criterion.of("type", Operator.EQ, "A"))
            ).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("空 criteria 的条件应为 isEmpty")
        void shouldBeEmptyForEmptyCriteria() {
            Condition condition = new ConditionImpl(LogicalOperator.AND, List.of());
            assertThat(condition.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("非空 criteria 的条件应不为 isEmpty")
        void shouldNotBeEmptyForNonEmptyCriteria() {
            Criterion c1 = Criterion.of("status", Operator.EQ, 1);
            Condition condition = new ConditionImpl(LogicalOperator.AND, List.of(c1));
            assertThat(condition.isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("Criterion record")
    class CriterionTests {

        @Test
        @DisplayName("of() 应创建普通条件准则")
        void shouldCreateSimpleCriterion() {
            Criterion c = Criterion.of("status", Operator.EQ, 1);
            assertThat(c.field()).isEqualTo("status");
            assertThat(c.operator()).isEqualTo(Operator.EQ);
            assertThat(c.value()).isEqualTo(1);
            assertThat(c.nextOperator()).isNull();
            assertThat(c.nestedCondition()).isNull();
            assertThat(c.negated()).isFalse();
        }

        @Test
        @DisplayName("of() 带 nextOperator 应创建带逻辑运算符的条件准则")
        void shouldCreateCriterionWithNextOperator() {
            Criterion c = Criterion.of("status", Operator.EQ, 1, LogicalOperator.OR);
            assertThat(c.nextOperator()).isEqualTo(LogicalOperator.OR);
        }

        @Test
        @DisplayName("nested() 应创建嵌套条件准则")
        void shouldCreateNestedCriterion() {
            Condition inner = Conditions.eq("status", 1);
            Criterion c = Criterion.nested(inner, LogicalOperator.AND);
            assertThat(c.isNested()).isTrue();
            assertThat(c.nestedCondition()).isSameAs(inner);
            assertThat(c.nextOperator()).isEqualTo(LogicalOperator.AND);
        }

        @Test
        @DisplayName("notNested() 应创建取反嵌套条件准则")
        void shouldCreateNotNestedCriterion() {
            Condition inner = Conditions.eq("status", 1);
            Criterion c = Criterion.notNested(inner);
            assertThat(c.isNested()).isTrue();
            assertThat(c.isNegated()).isTrue();
            assertThat(c.nestedCondition()).isSameAs(inner);
        }

        @Test
        @DisplayName("isUnary() 应正确判断一元运算符")
        void shouldIdentifyUnaryOperators() {
            assertThat(Criterion.of("x", Operator.IS_NULL, null).isUnary()).isTrue();
            assertThat(Criterion.of("x", Operator.IS_NOT_NULL, null).isUnary()).isTrue();
            assertThat(Criterion.of("x", Operator.EQ, 1).isUnary()).isFalse();
        }

        @Test
        @DisplayName("isRange() 应正确判断范围运算符")
        void shouldIdentifyRangeOperators() {
            assertThat(Criterion.of("x", Operator.BETWEEN, new Comparable<?>[]{1, 10}).isRange()).isTrue();
            assertThat(Criterion.of("x", Operator.NOT_BETWEEN, new Comparable<?>[]{1, 10}).isRange()).isTrue();
            assertThat(Criterion.of("x", Operator.EQ, 1).isRange()).isFalse();
        }

        @Test
        @DisplayName("isCollection() 应正确判断集合运算符")
        void shouldIdentifyCollectionOperators() {
            assertThat(Criterion.of("x", Operator.IN, List.of(1, 2)).isCollection()).isTrue();
            assertThat(Criterion.of("x", Operator.NOT_IN, List.of(1, 2)).isCollection()).isTrue();
            assertThat(Criterion.of("x", Operator.EQ, 1).isCollection()).isFalse();
        }
    }

}
