package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.api.resilience.CircuitBreaker;
import io.github.afgprojects.framework.ai.core.api.resilience.CircuitBreakerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * CircuitBreakerException 纯单元测试
 */
@DisplayName("CircuitBreakerException")
class CircuitBreakerExceptionTest {

    @Nested
    @DisplayName("构造方法")
    class Constructor {

        @Test
        @DisplayName("应创建带状态的异常")
        void shouldCreateExceptionWithState() {
            var ex = new CircuitBreakerException(CircuitBreaker.State.OPEN);

            assertThat(ex.getState()).isEqualTo(CircuitBreaker.State.OPEN);
            assertThat(ex.getMessage()).contains("OPEN");
            assertThat(ex.getErrorCode()).isEqualTo("CIRCUIT_BREAKER_OPEN");
        }

        @Test
        @DisplayName("应创建带自定义消息的异常")
        void shouldCreateExceptionWithMessage() {
            var ex = new CircuitBreakerException(CircuitBreaker.State.HALF_OPEN, "custom message");

            assertThat(ex.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
            assertThat(ex.getMessage()).isEqualTo("custom message");
        }
    }

    @Nested
    @DisplayName("异常层次")
    class Hierarchy {

        @Test
        @DisplayName("应为 RuntimeException")
        void shouldBeRuntimeException() {
            var ex = new CircuitBreakerException(CircuitBreaker.State.OPEN);

            assertThat(ex).isInstanceOf(RuntimeException.class);
        }
    }
}
