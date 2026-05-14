package io.github.afgprojects.framework.core.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CircuitBreaker 单元测试。
 * <p>
 * 测试熔断器的状态转换、失败阈值、半开状态恢复等功能。
 *
 * @see CircuitBreaker
 */
@DisplayName("CircuitBreaker 测试")
class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = new CircuitBreaker(
                "test-breaker",
                3, // failureThreshold
                Duration.ofMillis(100), // openDuration
                2, // halfOpenMaxCalls
                2 // successThreshold
                );
    }

    /**
     * 测试熔断器初始状态。
     */
    @Nested
    @DisplayName("初始状态测试")
    class InitialStateTests {

        /**
         * 测试初始状态为 CLOSED。
         */
        @Test
        @DisplayName("初始状态应该是 CLOSED")
        void shouldBeClosedInitially() {
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        /**
         * 测试 CLOSED 状态允许请求通过。
         */
        @Test
        @DisplayName("CLOSED 状态应该允许请求")
        void shouldAllowRequestWhenClosed() {
            assertThat(circuitBreaker.allowRequest()).isTrue();
        }

        /**
         * 测试返回熔断器名称。
         */
        @Test
        @DisplayName("应该返回熔断器名称")
        void shouldReturnName() {
            assertThat(circuitBreaker.getName()).isEqualTo("test-breaker");
        }

        /**
         * 测试初始失败计数为 0。
         */
        @Test
        @DisplayName("初始失败计数应该为 0")
        void shouldHaveZeroFailureCountInitially() {
            assertThat(circuitBreaker.getFailureCount()).isZero();
        }
    }

    /**
     * 测试 CLOSED 状态下的行为。
     */
    @Nested
    @DisplayName("CLOSED 状态测试")
    class ClosedStateTests {

        /**
         * 测试记录成功后重置失败计数。
         */
        @Test
        @DisplayName("记录成功应该重置失败计数")
        void shouldResetFailureCountOnSuccess() {
            // given
            circuitBreaker.recordFailure();
            circuitBreaker.recordFailure();

            // when
            circuitBreaker.recordSuccess();

            // then
            assertThat(circuitBreaker.getFailureCount()).isZero();
        }

        /**
         * 测试达到失败阈值后转换为 OPEN 状态。
         */
        @Test
        @DisplayName("达到失败阈值应该转换为 OPEN 状态")
        void shouldTransitionToOpenWhenThresholdReached() {
            // when
            for (int i = 0; i < 3; i++) {
                circuitBreaker.recordFailure();
            }

            // then
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }

        /**
         * 测试 OPEN 状态拒绝请求。
         */
        @Test
        @DisplayName("OPEN 状态应该拒绝请求")
        void shouldRejectRequestWhenOpen() {
            // given
            for (int i = 0; i < 3; i++) {
                circuitBreaker.recordFailure();
            }

            // then
            assertThat(circuitBreaker.allowRequest()).isFalse();
        }
    }

    /**
     * 测试 OPEN 状态下的行为。
     */
    @Nested
    @DisplayName("OPEN 状态测试")
    class OpenStateTests {

        @BeforeEach
        void openCircuitBreaker() {
            for (int i = 0; i < 3; i++) {
                circuitBreaker.recordFailure();
            }
        }

        /**
         * 测试 OPEN 状态经过 openDuration 后转换为 HALF_OPEN。
         */
        @Test
        @DisplayName("OPEN 状态经过 openDuration 后应该转换为 HALF_OPEN")
        void shouldTransitionToHalfOpenAfterDuration() throws InterruptedException {
            // when
            Thread.sleep(150); // 等待超过 openDuration

            // then
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        }

        /**
         * 测试 OPEN 状态未经过 openDuration 时保持 OPEN。
         */
        @Test
        @DisplayName("OPEN 状态未经过 openDuration 应该保持 OPEN")
        void shouldStayOpenBeforeDurationExpires() {
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }
    }

    /**
     * 测试 HALF_OPEN 状态下的行为。
     */
    @Nested
    @DisplayName("HALF_OPEN 状态测试")
    class HalfOpenStateTests {

        @BeforeEach
        void setUpHalfOpenState() throws InterruptedException {
            // 打开熔断器
            for (int i = 0; i < 3; i++) {
                circuitBreaker.recordFailure();
            }
            // 等待转换为 HALF_OPEN
            Thread.sleep(200);
        }

        /**
         * 测试 HALF_OPEN 状态允许有限数量的请求。
         */
        @Test
        @DisplayName("HALF_OPEN 状态应该允许有限数量的请求")
        void shouldAllowLimitedRequestsInHalfOpen() {
            // 首先验证处于 HALF_OPEN 状态
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
            assertThat(circuitBreaker.allowRequest()).isTrue();
            assertThat(circuitBreaker.allowRequest()).isTrue();
            assertThat(circuitBreaker.allowRequest()).isFalse(); // 超过 halfOpenMaxCalls
        }

        /**
         * 测试 HALF_OPEN 状态下记录成功达到阈值后转换为 CLOSED。
         */
        @Test
        @DisplayName("HALF_OPEN 状态下记录成功达到阈值应该转换为 CLOSED")
        void shouldTransitionToClosedOnSuccessThreshold() {
            // 首先验证处于 HALF_OPEN 状态
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

            // when
            circuitBreaker.recordSuccess();
            circuitBreaker.recordSuccess();

            // then
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        /**
         * 测试 HALF_OPEN 状态下记录失败后转换为 OPEN。
         */
        @Test
        @DisplayName("HALF_OPEN 状态下记录失败应该转换为 OPEN")
        void shouldTransitionToOpenOnFailure() {
            // when
            circuitBreaker.recordFailure();

            // then
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }

        /**
         * 测试 HALF_OPEN 状态下成功次数未达到阈值时保持 HALF_OPEN。
         */
        @Test
        @DisplayName("HALF_OPEN 状态下成功次数未达到阈值应该保持 HALF_OPEN")
        void shouldStayHalfOpenOnPartialSuccess() {
            // when
            circuitBreaker.recordSuccess();

            // then
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        }
    }

    /**
     * 测试 createDefault 工厂方法。
     */
    @Nested
    @DisplayName("createDefault 测试")
    class CreateDefaultTests {

        /**
         * 测试创建默认配置的熔断器。
         */
        @Test
        @DisplayName("应该创建默认配置的熔断器")
        void shouldCreateDefaultCircuitBreaker() {
            // when
            CircuitBreaker defaultBreaker = CircuitBreaker.createDefault("default");

            // then
            assertThat(defaultBreaker.getName()).isEqualTo("default");
            assertThat(defaultBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(defaultBreaker.allowRequest()).isTrue();
        }
    }

    /**
     * 测试并发场景。
     */
    @Nested
    @DisplayName("并发测试")
    class ConcurrencyTests {

        /**
         * 测试安全处理并发记录失败。
         */
        @Test
        @DisplayName("应该安全处理并发记录失败")
        void shouldHandleConcurrentFailureRecording() throws InterruptedException {
            // given
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];

            // when
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 10; j++) {
                        circuitBreaker.recordFailure();
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }

            // then
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }
    }

    /**
     * 测试边界条件。
     */
    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        /**
         * 测试失败阈值恰好达到时打开熔断器。
         */
        @Test
        @DisplayName("失败阈值恰好达到时应该打开熔断器")
        void shouldOpenWhenExactlyAtThreshold() {
            // when
            circuitBreaker.recordFailure();
            circuitBreaker.recordFailure();
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

            circuitBreaker.recordFailure();
            // then
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }

        /**
         * 测试 CLOSED 状态下记录成功不改变状态。
         */
        @Test
        @DisplayName("CLOSED 状态下记录成功不应该改变状态")
        void shouldNotChangeStateOnSuccessInClosedState() {
            // when
            circuitBreaker.recordSuccess();
            circuitBreaker.recordSuccess();

            // then
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(circuitBreaker.allowRequest()).isTrue();
        }
    }
}
