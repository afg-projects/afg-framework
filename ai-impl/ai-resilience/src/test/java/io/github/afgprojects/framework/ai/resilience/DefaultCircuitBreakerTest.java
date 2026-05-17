package io.github.afgprojects.framework.ai.resilience;

import io.github.afgprojects.framework.ai.core.resilience.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DefaultCircuitBreaker 单元测试
 */
class DefaultCircuitBreakerTest {

    private DefaultCircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = DefaultCircuitBreaker.builder()
                .name("test")
                .windowSize(10)
                .failureRateThreshold(0.5)
                .halfOpenMaxCalls(3)
                .openStateTimeoutMs(1000)
                .build();
    }

    @Test
    @DisplayName("初始状态为 CLOSED")
    void initialState_isClosed() {
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.allowRequest()).isTrue();
    }

    @Test
    @DisplayName("记录成功请求")
    void recordSuccess_increasesSuccessCount() {
        circuitBreaker.recordSuccess();
        circuitBreaker.recordSuccess();

        var stats = circuitBreaker.getStats();
        assertThat(stats.getSuccessCount()).isEqualTo(2);
        assertThat(stats.getFailureCount()).isZero();
    }

    @Test
    @DisplayName("记录失败请求")
    void recordFailure_increasesFailureCount() {
        circuitBreaker.recordFailure(new RuntimeException("test"));

        var stats = circuitBreaker.getStats();
        assertThat(stats.getFailureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("失败率达到阈值时打开熔断器")
    void failureRateThreshold_opensCircuit() {
        // 记录足够多的失败以达到窗口大小
        for (int i = 0; i < 10; i++) {
            circuitBreaker.recordFailure(new RuntimeException("test"));
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.allowRequest()).isFalse();
    }

    @Test
    @DisplayName("OPEN 状态超时后进入 HALF_OPEN")
    void openTimeout_transitionsToHalfOpen() throws InterruptedException {
        // 打开熔断器
        circuitBreaker.forceOpen();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // 等待超时
        Thread.sleep(1100);

        // 允许请求，状态变为 HALF_OPEN
        assertThat(circuitBreaker.allowRequest()).isTrue();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }

    @Test
    @DisplayName("HALF_OPEN 状态成功后关闭熔断器")
    void halfOpenSuccess_closesCircuit() throws InterruptedException {
        circuitBreaker.forceOpen();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // 等待超时
        Thread.sleep(1100);

        // 允许请求，状态变为 HALF_OPEN
        assertThat(circuitBreaker.allowRequest()).isTrue();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        circuitBreaker.recordSuccess();

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("HALF_OPEN 状态失败后重新打开熔断器")
    void halfOpenFailure_reopensCircuit() throws InterruptedException {
        circuitBreaker.forceOpen();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // 等待超时
        Thread.sleep(1100);

        // 允许请求，状态变为 HALF_OPEN
        assertThat(circuitBreaker.allowRequest()).isTrue();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        circuitBreaker.recordFailure(new RuntimeException("test"));

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("强制打开熔断器")
    void forceOpen_setsStateToOpen() {
        circuitBreaker.forceOpen();

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.allowRequest()).isFalse();
    }

    @Test
    @DisplayName("强制关闭熔断器")
    void forceClose_setsStateToClosed() {
        circuitBreaker.forceOpen();
        circuitBreaker.forceClose();

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.allowRequest()).isTrue();
    }

    @Test
    @DisplayName("执行成功操作")
    void execute_success() throws Exception {
        String result = circuitBreaker.execute(() -> "success");

        assertThat(result).isEqualTo("success");
        assertThat(circuitBreaker.getStats().getSuccessCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("OPEN 状态执行抛出异常")
    void execute_openState_throwsException() {
        circuitBreaker.forceOpen();

        assertThatThrownBy(() -> circuitBreaker.execute(() -> "success"))
                .isInstanceOf(io.github.afgprojects.framework.ai.core.resilience.CircuitBreakerException.class);
    }

    @Test
    @DisplayName("统计信息计算正确")
    void stats_calculatedCorrectly() {
        circuitBreaker.recordSuccess();
        circuitBreaker.recordSuccess();
        circuitBreaker.recordFailure(new RuntimeException("test"));

        var stats = circuitBreaker.getStats();

        assertThat(stats.getSuccessCount()).isEqualTo(2);
        assertThat(stats.getFailureCount()).isEqualTo(1);
        assertThat(stats.getTotalCount()).isEqualTo(3);
        assertThat(stats.getFailureRate()).isCloseTo(1.0 / 3, org.assertj.core.data.Offset.offset(0.01));
    }
}