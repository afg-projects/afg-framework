package io.github.afgprojects.framework.core.statemachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.api.statemachine.DefaultStateMachineInstance;
import io.github.afgprojects.framework.core.api.statemachine.StateMachineInstance;
import io.github.afgprojects.framework.core.statemachine.exception.InvalidTransitionException;

import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DefaultStateMachineInstance")
class DefaultStateMachineInstanceTest {

    enum OrderStatus {
        PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }

    @Data
    static class TestOrder {
        private Long id;
        private OrderStatus status = OrderStatus.PENDING;
    }

    private StateMachineInstance<TestOrder, OrderStatus> strictInstance;
    private StateMachineInstance<TestOrder, OrderStatus> nonStrictInstance;
    private TestOrder order;

    @BeforeEach
    void setUp() {
        var definition = StateMachineDefinition.<OrderStatus>builder()
                .name("OrderStatus")
                .entityType(TestOrder.class)
                .stateType(OrderStatus.class)
                .transition(new TransitionDefinition<>(OrderStatus.PENDING, OrderStatus.CONFIRMED, "confirm", "confirm"))
                .transition(new TransitionDefinition<>(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, "ship", "ship"))
                .transition(new TransitionDefinition<>(OrderStatus.SHIPPED, OrderStatus.DELIVERED, "deliver", "deliver"))
                .transition(new TransitionDefinition<>(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, "cancel", "cancel"))
                .transition(new TransitionDefinition<>(OrderStatus.PENDING, OrderStatus.CANCELLED, "cancel", "cancel"))
                .build();

        strictInstance = new DefaultStateMachineInstance<>(definition, true);
        nonStrictInstance = new DefaultStateMachineInstance<>(definition, false);
        order = new TestOrder();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);
    }

    @Nested
    @DisplayName("getCurrentState")
    class GetCurrentState {

        @Test
        @DisplayName("读取实体当前状态")
        void shouldReadCurrentState() {
            assertThat(strictInstance.getCurrentState(order)).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("状态变更后读取新状态")
        void shouldReadUpdatedState() {
            order.setStatus(OrderStatus.CONFIRMED);
            assertThat(strictInstance.getCurrentState(order)).isEqualTo(OrderStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("transit")
    class Transit {

        @Test
        @DisplayName("合法转换更新实体状态")
        void shouldUpdateState_onValidTransition() {
            strictInstance.transit(order, OrderStatus.CONFIRMED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("连续合法转换")
        void shouldSupportConsecutiveTransitions() {
            strictInstance.transit(order, OrderStatus.CONFIRMED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

            strictInstance.transit(order, OrderStatus.SHIPPED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);

            strictInstance.transit(order, OrderStatus.DELIVERED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        }

        @Test
        @DisplayName("严格模式下非法转换抛出异常")
        void shouldThrowException_onInvalidTransitionInStrictMode() {
            assertThatThrownBy(() -> strictInstance.transit(order, OrderStatus.DELIVERED))
                    .isInstanceOf(InvalidTransitionException.class)
                    .hasMessageContaining("PENDING")
                    .hasMessageContaining("DELIVERED");
        }

        @Test
        @DisplayName("非严格模式下非法转换静默忽略")
        void shouldIgnoreInvalidTransition_inNonStrictMode() {
            nonStrictInstance.transit(order, OrderStatus.DELIVERED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("多源状态转换")
        void shouldSupportMultipleFromStates() {
            // PENDING -> CANCELLED 合法
            strictInstance.transit(order, OrderStatus.CANCELLED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("从 CONFIRMED 状态取消")
        void shouldCancelFromConfirmedState() {
            order.setStatus(OrderStatus.CONFIRMED);
            strictInstance.transit(order, OrderStatus.CANCELLED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("canTransit")
    class CanTransit {

        @Test
        @DisplayName("合法转换返回 true")
        void shouldReturnTrue_forValidTransition() {
            assertThat(strictInstance.canTransit(order, OrderStatus.CONFIRMED)).isTrue();
            assertThat(strictInstance.canTransit(order, OrderStatus.CANCELLED)).isTrue();
        }

        @Test
        @DisplayName("非法转换返回 false")
        void shouldReturnFalse_forInvalidTransition() {
            assertThat(strictInstance.canTransit(order, OrderStatus.DELIVERED)).isFalse();
            assertThat(strictInstance.canTransit(order, OrderStatus.SHIPPED)).isFalse();
        }

        @Test
        @DisplayName("终态无可转换状态")
        void shouldReturnFalse_forTerminalState() {
            order.setStatus(OrderStatus.DELIVERED);
            assertThat(strictInstance.canTransit(order, OrderStatus.PENDING)).isFalse();
            assertThat(strictInstance.canTransit(order, OrderStatus.CONFIRMED)).isFalse();
        }
    }

    @Nested
    @DisplayName("getAvailableTransitions")
    class GetAvailableTransitions {

        @Test
        @DisplayName("PENDING 状态可转换为 CONFIRMED 和 CANCELLED")
        void shouldReturnAvailableTransitions_forPendingState() {
            var available = strictInstance.getAvailableTransitions(order);
            assertThat(available).containsExactlyInAnyOrder(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("CONFIRMED 状态可转换为 SHIPPED 和 CANCELLED")
        void shouldReturnAvailableTransitions_forConfirmedState() {
            order.setStatus(OrderStatus.CONFIRMED);
            var available = strictInstance.getAvailableTransitions(order);
            assertThat(available).containsExactlyInAnyOrder(OrderStatus.SHIPPED, OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("终态无可用转换")
        void shouldReturnEmpty_forTerminalState() {
            order.setStatus(OrderStatus.DELIVERED);
            var available = strictInstance.getAvailableTransitions(order);
            assertThat(available).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDefinition")
    class GetDefinition {

        @Test
        @DisplayName("返回状态机定义")
        void shouldReturnDefinition() {
            var definition = strictInstance.getDefinition();
            assertThat(definition).isNotNull();
            assertThat(definition.getName()).isEqualTo("OrderStatus");
            assertThat(definition.getEntityType()).isEqualTo(TestOrder.class);
            assertThat(definition.getStateType()).isEqualTo(OrderStatus.class);
        }
    }

    @Nested
    @DisplayName("InvalidTransitionException")
    class InvalidTransitionExceptionTest {

        @Test
        @DisplayName("异常包含状态机名称和状态信息")
        void shouldContainStateMachineInfo() {
            assertThatThrownBy(() -> strictInstance.transit(order, OrderStatus.DELIVERED))
                    .isInstanceOf(InvalidTransitionException.class)
                    .satisfies(ex -> {
                        var exception = (InvalidTransitionException) ex;
                        assertThat(exception.getStateMachineName()).isEqualTo("OrderStatus");
                        assertThat(exception.getFromState()).isEqualTo("PENDING");
                        assertThat(exception.getToState()).isEqualTo("DELIVERED");
                    });
        }
    }
}
