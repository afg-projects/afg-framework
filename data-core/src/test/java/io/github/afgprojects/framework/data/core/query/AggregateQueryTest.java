package io.github.afgprojects.framework.data.core.query;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 聚合查询相关类单元测试
 * <p>
 * 验证 AggregateResult、AggregateFunction、AggregateReference 的行为。
 */
class AggregateQueryTest {

    // ========== AggregateResult ==========

    @Nested
    @DisplayName("AggregateResult")
    class AggregateResultTest {

        @Test
        @DisplayName("should return values from map when get called")
        void shouldReturnValuesFromMap_whenGetCalled() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("userCount", 42L);
            data.put("avgAge", 28.5);
            AggregateResult result = new AggregateResult(data);

            assertThat(result.get("userCount")).isEqualTo(42L);
            assertThat(result.get("avgAge")).isEqualTo(28.5);
        }

        @Test
        @DisplayName("should return null when key not found")
        void shouldReturnNull_whenKeyNotFound() {
            AggregateResult result = new AggregateResult(Map.of("userCount", 42L));

            assertThat(result.get("nonExistent")).isNull();
        }

        @Test
        @DisplayName("should return Long value when getLong called with Long input")
        void shouldReturnLongValue_whenGetLongCalledWithLongInput() {
            AggregateResult result = new AggregateResult(Map.of("userCount", 42L));

            assertThat(result.getLong("userCount")).isEqualTo(42L);
        }

        @Test
        @DisplayName("should return Long value when getLong called with Integer input")
        void shouldReturnLongValue_whenGetLongCalledWithIntegerInput() {
            AggregateResult result = new AggregateResult(Map.of("userCount", 42));

            assertThat(result.getLong("userCount")).isEqualTo(42L);
        }

        @Test
        @DisplayName("should return null when getLong called with null value")
        void shouldReturnNull_whenGetLongCalledWithNullValue() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("userCount", null);
            AggregateResult result = new AggregateResult(data);

            assertThat(result.getLong("userCount")).isNull();
        }

        @Test
        @DisplayName("should return Long from string when getLong called with string input")
        void shouldReturnLongFromString_whenGetLongCalledWithStringInput() {
            AggregateResult result = new AggregateResult(Map.of("userCount", "42"));

            assertThat(result.getLong("userCount")).isEqualTo(42L);
        }

        @Test
        @DisplayName("should return BigDecimal value when getBigDecimal called with BigDecimal input")
        void shouldReturnBigDecimalValue_whenGetBigDecimalCalledWithBigDecimalInput() {
            AggregateResult result = new AggregateResult(Map.of("totalSalary", new BigDecimal("50000.50")));

            assertThat(result.getBigDecimal("totalSalary")).isEqualByComparingTo(new BigDecimal("50000.50"));
        }

        @Test
        @DisplayName("should return BigDecimal from number when getBigDecimal called with Number input")
        void shouldReturnBigDecimalFromNumber_whenGetBigDecimalCalledWithNumberInput() {
            AggregateResult result = new AggregateResult(Map.of("totalSalary", 50000.50));

            assertThat(result.getBigDecimal("totalSalary")).isNotNull();
        }

        @Test
        @DisplayName("should return null when getBigDecimal called with null value")
        void shouldReturnNull_whenGetBigDecimalCalledWithNullValue() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("totalSalary", null);
            AggregateResult result = new AggregateResult(data);

            assertThat(result.getBigDecimal("totalSalary")).isNull();
        }

        @Test
        @DisplayName("should return Double value when getDouble called with Double input")
        void shouldReturnDoubleValue_whenGetDoubleCalledWithDoubleInput() {
            AggregateResult result = new AggregateResult(Map.of("avgAge", 28.5));

            assertThat(result.getDouble("avgAge")).isEqualTo(28.5);
        }

        @Test
        @DisplayName("should return Double from number when getDouble called with Long input")
        void shouldReturnDoubleFromNumber_whenGetDoubleCalledWithLongInput() {
            AggregateResult result = new AggregateResult(Map.of("avgAge", 28L));

            assertThat(result.getDouble("avgAge")).isEqualTo(28.0);
        }

        @Test
        @DisplayName("should return null when getDouble called with null value")
        void shouldReturnNull_whenGetDoubleCalledWithNullValue() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("avgAge", null);
            AggregateResult result = new AggregateResult(data);

            assertThat(result.getDouble("avgAge")).isNull();
        }

        @Test
        @DisplayName("should return String value when getString called")
        void shouldReturnStringValue_whenGetStringCalled() {
            AggregateResult result = new AggregateResult(Map.of("deptName", "Engineering"));

            assertThat(result.getString("deptName")).isEqualTo("Engineering");
        }

        @Test
        @DisplayName("should return null when getString called with null value")
        void shouldReturnNull_whenGetStringCalledWithNullValue() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("deptName", null);
            AggregateResult result = new AggregateResult(data);

            assertThat(result.getString("deptName")).isNull();
        }

        @Test
        @DisplayName("should return Integer value when getInteger called with Integer input")
        void shouldReturnIntegerValue_whenGetIntegerCalledWithIntegerInput() {
            AggregateResult result = new AggregateResult(Map.of("count", 42));

            assertThat(result.getInteger("count")).isEqualTo(42);
        }

        @Test
        @DisplayName("should return Integer from Long when getInteger called with Long input")
        void shouldReturnIntegerFromLong_whenGetIntegerCalledWithLongInput() {
            AggregateResult result = new AggregateResult(Map.of("count", 42L));

            assertThat(result.getInteger("count")).isEqualTo(42);
        }

        @Test
        @DisplayName("should return null when getInteger called with null value")
        void shouldReturnNull_whenGetIntegerCalledWithNullValue() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("count", null);
            AggregateResult result = new AggregateResult(data);

            assertThat(result.getInteger("count")).isNull();
        }

        @Test
        @DisplayName("should return immutable map when asMap called")
        void shouldReturnImmutableMap_whenAsMapCalled() {
            AggregateResult result = new AggregateResult(new LinkedHashMap<>(Map.of("userCount", 42L)));

            assertThatThrownBy(() -> result.asMap().put("newKey", "newValue"))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should implement equals and hashCode correctly")
        void shouldImplementEqualsAndHashCodeCorrectly() {
            AggregateResult result1 = new AggregateResult(Map.of("userCount", 42L));
            AggregateResult result2 = new AggregateResult(Map.of("userCount", 42L));

            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("should implement toString correctly")
        void shouldImplementToStringCorrectly() {
            AggregateResult result = new AggregateResult(Map.of("userCount", 42L));

            assertThat(result.toString()).contains("AggregateResult").contains("userCount");
        }
    }

    // ========== AggregateFunction ==========

    @Nested
    @DisplayName("AggregateFunction")
    class AggregateFunctionTest {

        @Test
        @DisplayName("should return COUNT keyword for COUNT and COUNT_DISTINCT")
        void shouldReturnCountKeyword_forCountAndCountDistinct() {
            assertThat(AggregateFunction.COUNT.getSqlKeyword()).isEqualTo("COUNT");
            assertThat(AggregateFunction.COUNT_DISTINCT.getSqlKeyword()).isEqualTo("COUNT");
        }

        @Test
        @DisplayName("should return SUM keyword for SUM")
        void shouldReturnSumKeyword_forSum() {
            assertThat(AggregateFunction.SUM.getSqlKeyword()).isEqualTo("SUM");
        }

        @Test
        @DisplayName("should return AVG keyword for AVG")
        void shouldReturnAvgKeyword_forAvg() {
            assertThat(AggregateFunction.AVG.getSqlKeyword()).isEqualTo("AVG");
        }

        @Test
        @DisplayName("should return MAX keyword for MAX")
        void shouldReturnMaxKeyword_forMax() {
            assertThat(AggregateFunction.MAX.getSqlKeyword()).isEqualTo("MAX");
        }

        @Test
        @DisplayName("should return MIN keyword for MIN")
        void shouldReturnMinKeyword_forMin() {
            assertThat(AggregateFunction.MIN.getSqlKeyword()).isEqualTo("MIN");
        }
    }

    // ========== AggregateReference ==========

    @Nested
    @DisplayName("AggregateReference")
    class AggregateReferenceTest {

        @Test
        @DisplayName("should create COUNT(*) reference when countAll called")
        void shouldCreateCountAllReference_whenCountAllCalled() {
            AggregateReference ref = AggregateReference.countAll("totalCount");

            assertThat(ref.function()).isEqualTo(AggregateFunction.COUNT);
            assertThat(ref.field()).isNull();
            assertThat(ref.alias()).isEqualTo("totalCount");
            assertThat(ref.toSqlExpression()).isEqualTo("COUNT(*)");
        }

        @Test
        @DisplayName("should create COUNT DISTINCT reference when of called with COUNT_DISTINCT")
        void shouldCreateCountDistinctReference_whenOfCalledWithCountDistinct() {
            AggregateReference ref = AggregateReference.of(AggregateFunction.COUNT_DISTINCT, "status", "distinctCount");

            assertThat(ref.toSqlExpression()).isEqualTo("COUNT(DISTINCT status)");
        }

        @Test
        @DisplayName("should create SUM reference when of called with SUM")
        void shouldCreateSumReference_whenOfCalledWithSum() {
            AggregateReference ref = AggregateReference.of(AggregateFunction.SUM, "salary", "totalSalary");

            assertThat(ref.toSqlExpression()).isEqualTo("SUM(salary)");
        }

        @Test
        @DisplayName("should create AVG reference when of called with AVG")
        void shouldCreateAvgReference_whenOfCalledWithAvg() {
            AggregateReference ref = AggregateReference.of(AggregateFunction.AVG, "age", "avgAge");

            assertThat(ref.toSqlExpression()).isEqualTo("AVG(age)");
        }

        @Test
        @DisplayName("should create MAX reference when of called with MAX")
        void shouldCreateMaxReference_whenOfCalledWithMax() {
            AggregateReference ref = AggregateReference.of(AggregateFunction.MAX, "salary", "maxSalary");

            assertThat(ref.toSqlExpression()).isEqualTo("MAX(salary)");
        }

        @Test
        @DisplayName("should create MIN reference when of called with MIN")
        void shouldCreateMinReference_whenOfCalledWithMin() {
            AggregateReference ref = AggregateReference.of(AggregateFunction.MIN, "age", "minAge");

            assertThat(ref.toSqlExpression()).isEqualTo("MIN(age)");
        }
    }

    // ========== NotCondition ==========

    @Nested
    @DisplayName("NotCondition")
    class NotConditionTest {

        @Test
        @DisplayName("should wrap original condition when NotCondition created")
        void shouldWrapOriginalCondition_whenNotConditionCreated() {
            Condition original = Conditions.builder().eq("status", 1).build();
            NotCondition not = new NotCondition(original);

            assertThat(not.getOriginal()).isSameAs(original);
        }

        @Test
        @DisplayName("should return NOT criteria when getCriteria called")
        void shouldReturnNotCriteria_whenGetCriteriaCalled() {
            Condition original = Conditions.builder().eq("status", 1).build();
            NotCondition not = new NotCondition(original);

            assertThat(not.getCriteria()).hasSize(1);
            assertThat(not.getCriteria().get(0).isNegated()).isTrue();
        }

        @Test
        @DisplayName("should return AND logical operator when getOperator called")
        void shouldReturnAndOperator_whenGetOperatorCalled() {
            NotCondition not = new NotCondition(Conditions.builder().eq("status", 1).build());

            assertThat(not.getOperator()).isEqualTo(LogicalOperator.AND);
        }

        @Test
        @DisplayName("should reflect original condition emptiness when isEmpty called")
        void shouldReflectOriginalEmptiness_whenIsEmptyCalled() {
            NotCondition notEmpty = new NotCondition(Conditions.builder().eq("status", 1).build());

            assertThat(notEmpty.isEmpty()).isFalse();
            assertThat(Condition.empty().isEmpty()).isTrue();
        }

        @Test
        @DisplayName("should return original condition when not called on NotCondition (double negation)")
        void shouldReturnOriginalCondition_whenNotCalledOnNotCondition() {
            Condition original = Conditions.builder().eq("status", 1).build();
            NotCondition not = new NotCondition(original);

            Condition doubleNot = not.not();

            assertThat(doubleNot).isSameAs(original);
        }

        @Test
        @DisplayName("should return other condition when and called with empty this")
        void shouldReturnOtherCondition_whenAndCalledWithEmptyThis() {
            NotCondition emptyNot = new NotCondition(Condition.empty());
            Condition other = Conditions.builder().eq("status", 1).build();

            Condition result = emptyNot.and(other);

            assertThat(result).isSameAs(other);
        }

        @Test
        @DisplayName("should return this condition when and called with empty other")
        void shouldReturnThisCondition_whenAndCalledWithEmptyOther() {
            NotCondition not = new NotCondition(Conditions.builder().eq("status", 1).build());
            Condition empty = Condition.empty();

            Condition result = not.and(empty);

            assertThat(result).isSameAs(not);
        }

        @Test
        @DisplayName("should combine conditions when and called with two non-empty conditions")
        void shouldCombineConditions_whenAndCalledWithTwoNonEmptyConditions() {
            NotCondition not = new NotCondition(Conditions.builder().eq("status", 1).build());
            Condition other = Conditions.builder().eq("name", "test").build();

            Condition result = not.and(other);

            assertThat(result).isNotSameAs(not);
            assertThat(result).isNotSameAs(other);
            assertThat(result.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("should combine conditions when or called with two non-empty conditions")
        void shouldCombineConditions_whenOrCalledWithTwoNonEmptyConditions() {
            NotCondition not = new NotCondition(Conditions.builder().eq("status", 1).build());
            Condition other = Conditions.builder().eq("name", "test").build();

            Condition result = not.or(other);

            assertThat(result).isNotSameAs(not);
            assertThat(result).isNotSameAs(other);
            assertThat(result.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("should return other condition when or called with empty this")
        void shouldReturnOtherCondition_whenOrCalledWithEmptyThis() {
            NotCondition emptyNot = new NotCondition(Condition.empty());
            Condition other = Conditions.builder().eq("status", 1).build();

            Condition result = emptyNot.or(other);

            assertThat(result).isSameAs(other);
        }

        @Test
        @DisplayName("should return toString representation")
        void shouldReturnToStringRepresentation() {
            NotCondition not = new NotCondition(Conditions.builder().eq("status", 1).build());

            assertThat(not.toString()).contains("NOT(");
        }
    }
}