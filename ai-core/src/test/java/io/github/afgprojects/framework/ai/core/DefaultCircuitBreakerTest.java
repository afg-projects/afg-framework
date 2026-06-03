package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.api.resilience.CircuitBreaker;
import io.github.afgprojects.framework.ai.core.api.resilience.CircuitBreakerException;
import io.github.afgprojects.framework.ai.core.resilience.DefaultCircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * DefaultCircuitBreaker 纯单元测试
 */
@DisplayName("DefaultCircuitBreaker")
class DefaultCircuitBreakerTest {

    @Nested
    @DisplayName("初始状态")
    class InitialState {

        @Test
        @DisplayName("新建熔断器应为 CLOSED 状态")
        void shouldBeClosed_whenNew() {
            var cb = new DefaultCircuitBreaker();

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("CLOSED 状态应允许请求")
        void shouldAllowRequest_whenClosed() {
            var cb = new DefaultCircuitBreaker();

            assertThat(cb.allowRequest()).isTrue();
        }
    }

    @Nested
    @DisplayName("CLOSED -> OPEN 转换")
    class ClosedToOpen {

        @Test
        @DisplayName("失败率超过阈值应转为 OPEN")
        void shouldOpen_whenFailureRateExceedsThreshold() {
            // windowSize=4, failureRateThreshold=0.5 → 3 failures out of 4 = 75% > 50%
            var cb = new DefaultCircuitBreaker("test", 4, 0.5, 10, 30000);

            cb.recordSuccess();
            cb.recordFailure(new RuntimeException("fail1"));
            cb.recordFailure(new RuntimeException("fail2"));
            cb.recordFailure(new RuntimeException("fail3")); // 3/4 = 75% >= 50%

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }

        @Test
        @DisplayName("OPEN 状态应拒绝请求")
        void shouldRejectRequest_whenOpen() {
            var cb = new DefaultCircuitBreaker("test", 2, 0.5, 10, 60000); // long timeout
            cb.forceOpen();

            assertThat(cb.allowRequest()).isFalse();
        }
    }

    @Nested
    @DisplayName("OPEN -> HALF_OPEN 转换")
    class OpenToHalfOpen {

        @Test
        @DisplayName("超时后应转为 HALF_OPEN")
        void shouldTransitionToHalfOpen_whenTimeout() {
            // openStateTimeoutMs=1 → 几乎立即允许探测
            var cb = new DefaultCircuitBreaker("test", 2, 0.5, 10, 1);
            cb.forceOpen();

            // 等待超时
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}

            assertThat(cb.allowRequest()).isTrue();
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        }
    }

    @Nested
    @DisplayName("HALF_OPEN -> CLOSED 转换")
    class HalfOpenToClosed {

        @Test
        @DisplayName("半开状态成功应转为 CLOSED")
        void shouldTransitionToClosed_whenSuccessInHalfOpen() {
            var cb = new DefaultCircuitBreaker("test", 2, 0.5, 10, 1);
            cb.forceOpen();

            // 等待超时进入 HALF_OPEN
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            cb.allowRequest(); // 触发 OPEN -> HALF_OPEN

            cb.recordSuccess(); // HALF_OPEN 成功 -> CLOSED

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }
    }

    @Nested
    @DisplayName("HALF_OPEN -> OPEN 转换")
    class HalfOpenToOpen {

        @Test
        @DisplayName("半开状态失败应转回 OPEN")
        void shouldTransitionToOpen_whenFailureInHalfOpen() {
            var cb = new DefaultCircuitBreaker("test", 2, 0.5, 10, 1);
            cb.forceOpen();

            // 等待超时进入 HALF_OPEN
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            cb.allowRequest(); // 触发 OPEN -> HALF_OPEN

            cb.recordFailure(new RuntimeException("probe failed")); // HALF_OPEN 失败 -> OPEN

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }
    }

    @Nested
    @DisplayName("forceOpen / forceClose")
    class ForceOperations {

        @Test
        @DisplayName("forceOpen 应强制打开")
        void shouldForceOpen() {
            var cb = new DefaultCircuitBreaker();
            cb.forceOpen();

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
            assertThat(cb.allowRequest()).isFalse();
        }

        @Test
        @DisplayName("forceClose 应强制关闭")
        void shouldForceClose() {
            var cb = new DefaultCircuitBreaker();
            cb.forceOpen();
            cb.forceClose();

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(cb.allowRequest()).isTrue();
        }
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("CLOSED 状态应正常执行操作")
        void shouldExecute_whenClosed() throws Exception {
            var cb = new DefaultCircuitBreaker();

            String result = cb.execute(() -> "success");

            assertThat(result).isEqualTo("success");
        }

        @Test
        @DisplayName("OPEN 状态应抛 CircuitBreakerException")
        void shouldThrowCircuitBreakerException_whenOpen() {
            var cb = new DefaultCircuitBreaker("test", 2, 0.5, 10, 60000);
            cb.forceOpen();

            assertThatThrownBy(() -> cb.execute(() -> "fail"))
                    .isInstanceOf(CircuitBreakerException.class);
        }

        @Test
        @DisplayName("操作失败应传播异常")
        void shouldPropagateException_whenOperationFails() {
            var cb = new DefaultCircuitBreaker();

            assertThatThrownBy(() -> cb.execute(() -> {
                throw new RuntimeException("operation error");
            })).isInstanceOf(RuntimeException.class)
                    .hasMessage("operation error");
        }
    }

    @Nested
    @DisplayName("getStats")
    class Stats {

        @Test
        @DisplayName("应正确统计成功和失败次数")
        void shouldCountSuccessAndFailure() {
            var cb = new DefaultCircuitBreaker();

            cb.recordSuccess();
            cb.recordSuccess();
            cb.recordFailure(new RuntimeException("fail"));

            var stats = cb.getStats();

            assertThat(stats.getSuccessCount()).isEqualTo(2);
            assertThat(stats.getFailureCount()).isEqualTo(1);
            assertThat(stats.getTotalCount()).isEqualTo(3);
            assertThat(stats.getFailureRate()).isCloseTo(1.0 / 3.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("Builder 应创建自定义熔断器")
        void shouldCreateCustomCircuitBreaker() {
            var cb = DefaultCircuitBreaker.builder()
                    .name("custom")
                    .windowSize(50)
                    .failureRateThreshold(0.8)
                    .halfOpenMaxCalls(5)
                    .openStateTimeoutMs(10000)
                    .build();

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(cb.allowRequest()).isTrue();
        }
    }
}
