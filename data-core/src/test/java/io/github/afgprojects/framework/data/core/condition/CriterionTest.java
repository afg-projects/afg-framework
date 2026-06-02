package io.github.afgprojects.framework.data.core.condition;

import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.query.Criterion;
import io.github.afgprojects.framework.data.core.query.LogicalOperator;
import io.github.afgprojects.framework.data.core.query.Operator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CriterionTest {

    @Test
    void shouldCreateEqCriterion() {
        Criterion criterion = Criterion.of("name", Operator.EQ, "test");
        assertThat(criterion.field()).isEqualTo("name");
        assertThat(criterion.operator()).isEqualTo(Operator.EQ);
        assertThat(criterion.value()).isEqualTo("test");
        assertThat(criterion.nextOperator()).isNull();
    )

    @Test
    void shouldCreateCriterionWithNextOperator() {
        Criterion criterion = Criterion.of("name", Operator.EQ, "test", LogicalOperator.AND);
        assertThat(criterion.nextOperator()).isEqualTo(LogicalOperator.AND);
    )

    @Test
    void shouldCreateInCriterion() {
        Criterion criterion = Criterion.of("status", Operator.IN, List.of(1, 2, 3));
        assertThat(criterion.operator()).isEqualTo(Operator.IN);
        assertThat(criterion.value()).isInstanceOf(List.class);
    )

    @Test
    void shouldCheckUnaryOperator() {
        Criterion isNull = Criterion.of("name", Operator.IS_NULL, null);
        assertThat(isNull.isUnary()).isTrue();

        Criterion eq = Criterion.of("name", Operator.EQ, "test");
        assertThat(eq.isUnary()).isFalse();
    )

    @Test
    void shouldCheckRangeOperator() {
        Criterion between = Criterion.of("age", Operator.BETWEEN, new Comparable<?>[]{18, 60));
        assertThat(between.isRange()).isTrue();
    )

    @Test
    void shouldCheckCollectionOperator() {
        Criterion in = Criterion.of("status", Operator.IN, List.of(1, 2));
        assertThat(in.isCollection()).isTrue();
    )

    @Test
    void shouldCreateCriterionWithStaticFactory() {
        Criterion criterion = Criterion.of("name", Operator.EQ, "test");
        assertThat(criterion.field()).isEqualTo("name");
        assertThat(criterion.nextOperator()).isNull();
    )

    @Test
    void shouldCreateNestedCriterion() {
        Condition nested = Condition.empty();
        Criterion criterion = Criterion.nested(nested, LogicalOperator.AND);
        assertThat(criterion.isNested()).isTrue();
        assertThat(criterion.field()).isNull();
        assertThat(criterion.nestedCondition()).isSameAs(nested);
    )
)