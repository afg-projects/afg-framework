package io.github.afgprojects.framework.core.statemachine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.statemachine.exception.InvalidTransitionException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InvalidTransitionException")
class InvalidTransitionExceptionTest {

    enum TestStatus {
        PENDING, CONFIRMED, CANCELLED
    }

    @Nested
    @DisplayName("构造函数")
    class Constructor {

        @Test
        @DisplayName("默认构造包含状态机名称和状态信息")
        void shouldContainStateMachineAndStateInfo() {
            var exception = new InvalidTransitionException("OrderStatus", TestStatus.PENDING, TestStatus.CANCELLED);

            assertThat(exception.getStateMachineName()).isEqualTo("OrderStatus");
            assertThat(exception.getFromState()).isEqualTo("PENDING");
            assertThat(exception.getToState()).isEqualTo("CANCELLED");
            assertThat(exception.getMessage()).contains("OrderStatus");
            assertThat(exception.getMessage()).contains("PENDING");
            assertThat(exception.getMessage()).contains("CANCELLED");
        }

        @Test
        @DisplayName("自定义消息构造")
        void shouldSupportCustomMessage() {
            var exception = new InvalidTransitionException("OrderStatus", TestStatus.PENDING, TestStatus.CANCELLED,
                    "自定义错误消息");

            assertThat(exception.getMessage()).isEqualTo("自定义错误消息");
            assertThat(exception.getStateMachineName()).isEqualTo("OrderStatus");
            assertThat(exception.getFromState()).isEqualTo("PENDING");
            assertThat(exception.getToState()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("null 状态不抛异常")
        void shouldHandleNullStates() {
            var exception = new InvalidTransitionException("Test", null, null);

            assertThat(exception.getFromState()).isNull();
            assertThat(exception.getToState()).isNull();
        }

        @Test
        @DisplayName("继承自 BusinessException")
        void shouldBeBusinessException() {
            var exception = new InvalidTransitionException("OrderStatus", TestStatus.PENDING, TestStatus.CANCELLED);

            assertThat(exception).isInstanceOf(io.github.afgprojects.framework.commons.exception.BusinessException.class);
        }
    }
}
