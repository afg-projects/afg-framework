package io.github.afgprojects.framework.data.core.condition;

import io.github.afgprojects.framework.data.core.query.Operator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorTest {

    @Test
    void shouldHaveAllOperators() {
        assertThat(Operator.values()).containsExactlyInAnyOrder(
            Operator.EQ, Operator.NE, Operator.GT, Operator.GE, Operator.LT, Operator.LE,
            Operator.LIKE, Operator.LIKE_LEFT, Operator.LIKE_RIGHT, Operator.NOT_LIKE,
            Operator.IN, Operator.NOT_IN,
            Operator.IS_NULL, Operator.IS_NOT_NULL,
            Operator.BETWEEN, Operator.NOT_BETWEEN
        );
    }

    @Test
    void shouldReturnSymbol() {
        assertThat(Operator.EQ.getSymbol()).isEqualTo("=");
        assertThat(Operator.LIKE.getSymbol()).isEqualTo("LIKE");
    }

    @Test
    void shouldCheckRequiresValue() {
        assertThat(Operator.EQ.requiresValue()).isTrue();
        assertThat(Operator.IS_NULL.requiresValue()).isFalse();
        assertThat(Operator.IS_NOT_NULL.requiresValue()).isFalse();
    }
}
