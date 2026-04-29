package io.github.afgprojects.framework.data.core.condition;

import io.github.afgprojects.framework.data.core.query.Criterion;
import io.github.afgprojects.framework.data.core.query.LogicalOperator;
import io.github.afgprojects.framework.data.core.query.Operator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CriterionTest {

    @Test
    void shouldCreateEqCriterion() {
        Criterion criterion = new Criterion("name", Operator.EQ, "test", null);
        assertThat(criterion.field()).isEqualTo("name");
        assertThat(criterion.operator()).isEqualTo(Operator.EQ);
        assertThat(criterion.value()).isEqualTo("test");
        assertThat(criterion.nextOperator()).isNull();
    }

    @Test
    void shouldCreateCriterionWithNextOperator() {
        Criterion criterion = new Criterion("name", Operator.EQ, "test", LogicalOperator.AND);
        assertThat(criterion.nextOperator()).isEqualTo(LogicalOperator.AND);
    }

    @Test
    void shouldCreateInCriterion() {
        Criterion criterion = new Criterion("status", Operator.IN, List.of(1, 2, 3), null);
        assertThat(criterion.operator()).isEqualTo(Operator.IN);
        assertThat(criterion.value()).isInstanceOf(List.class);
    }

    @Test
    void shouldCheckUnaryOperator() {
        Criterion isNull = new Criterion("name", Operator.IS_NULL, null, null);
        assertThat(isNull.isUnary()).isTrue();

        Criterion eq = new Criterion("name", Operator.EQ, "test", null);
        assertThat(eq.isUnary()).isFalse();
    }

    @Test
    void shouldCheckRangeOperator() {
        Criterion between = new Criterion("age", Operator.BETWEEN, new Comparable<?>[]{18, 60}, null);
        assertThat(between.isRange()).isTrue();
    }

    @Test
    void shouldCheckCollectionOperator() {
        Criterion in = new Criterion("status", Operator.IN, List.of(1, 2), null);
        assertThat(in.isCollection()).isTrue();
    }

    @Test
    void shouldCreateCriterionWithStaticFactory() {
        Criterion criterion = Criterion.of("name", Operator.EQ, "test");
        assertThat(criterion.field()).isEqualTo("name");
        assertThat(criterion.nextOperator()).isNull();
    }
}