package io.github.afgprojects.framework.core.cloud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * {@link GracefulShutdownLifecycle} 的单元测试。
 * <p>
 * 测试优雅停机生命周期组件的基本属性、生命周期状态转换和回调执行功能。
 *
 * @see GracefulShutdownLifecycle
 */
@DisplayName("GracefulShutdownLifecycle 单元测试")
@ExtendWith(MockitoExtension.class)
class GracefulShutdownLifecycleTest extends BaseUnitTest {

    private GracefulShutdownLifecycle lifecycle;

    /**
     * 基本属性测试分组。
     * <p>
     * 验证生命周期组件的名称、超时时间、phase 等基本属性。
     */
    @Nested
    @DisplayName("基本属性测试")
    class BasicPropertiesTests {

        /**
         * 测试 getName 方法应返回构造时指定的名称。
         */
        @Test
        @DisplayName("应该正确获取名称")
        void shouldGetName() {
            // given
            lifecycle = new GracefulShutdownLifecycle("test-lifecycle", 0, 30000, null);

            // when/then
            assertThat(lifecycle.getName()).isEqualTo("test-lifecycle");
        }

        /**
         * 测试 getTimeoutMs 方法应返回构造时指定的超时时间。
         */
        @Test
        @DisplayName("应该正确获取超时时间")
        void shouldGetTimeoutMs() {
            // given
            lifecycle = new GracefulShutdownLifecycle("test", 0, 60000, null);

            // when/then
            assertThat(lifecycle.getTimeoutMs()).isEqualTo(60000);
        }

        /**
         * 测试 getPhase 方法应返回构造时指定的阶段值。
         */
        @Test
        @DisplayName("应该正确获取 phase")
        void shouldGetPhase() {
            // given
            lifecycle = new GracefulShutdownLifecycle("test", 100, 30000, null);

            // when/then
            assertThat(lifecycle.getPhase()).isEqualTo(100);
        }

        /**
         * 测试 isAutoStartup 方法应返回 true，表示自动启动。
         */
        @Test
        @DisplayName("应该自动启动")
        void shouldAutoStartup() {
            // given
            lifecycle = new GracefulShutdownLifecycle("test", 0, 30000, null);

            // when/then
            assertThat(lifecycle.isAutoStartup()).isTrue();
        }
    }

    /**
     * 生命周期状态测试分组。
     * <p>
     * 验证 start/stop 操作后的运行状态转换。
     */
    @Nested
    @DisplayName("生命周期测试")
    class LifecycleTests {

        @BeforeEach
        void setUp() {
            lifecycle = new GracefulShutdownLifecycle("test", 0, 30000, null);
        }

        /**
         * 测试初始状态应为未运行。
         */
        @Test
        @DisplayName("初始状态应该未运行")
        void shouldNotBeRunningInitially() {
            assertThat(lifecycle.isRunning()).isFalse();
        }

        /**
         * 测试调用 start 方法后应处于运行状态。
         */
        @Test
        @DisplayName("start 后应该处于运行状态")
        void shouldBeRunningAfterStart() {
            // when
            lifecycle.start();

            // then
            assertThat(lifecycle.isRunning()).isTrue();
        }

        /**
         * 测试调用 stop 方法后应处于未运行状态。
         */
        @Test
        @DisplayName("stop 后应该处于未运行状态")
        void shouldNotBeRunningAfterStop() {
            // given
            lifecycle.start();

            // when
            lifecycle.stop();

            // then
            assertThat(lifecycle.isRunning()).isFalse();
        }
    }

    /**
     * 回调执行测试分组。
     * <p>
     * 验证 stop 操作时回调的执行行为，包括正常执行、异常处理等场景。
     */
    @Nested
    @DisplayName("回调测试")
    class CallbackTests {

        /**
         * 测试 stop 时应执行构造时传入的回调。
         */
        @Test
        @DisplayName("stop 时应该执行回调")
        void shouldExecuteCallbackOnStop() {
            // given
            Runnable callback = mock(Runnable.class);
            lifecycle = new GracefulShutdownLifecycle("test", 0, 30000, callback);
            lifecycle.start();

            // when
            lifecycle.stop();

            // then
            verify(callback).run();
        }

        /**
         * 测试 stop(callback) 方法应同时执行构造回调和传入的回调。
         */
        @Test
        @DisplayName("stop(callback) 时应该执行回调和传入的回调")
        void shouldExecuteBothCallbacks() {
            // given
            Runnable shutdownCallback = mock(Runnable.class);
            Runnable stopCallback = mock(Runnable.class);
            lifecycle = new GracefulShutdownLifecycle("test", 0, 30000, shutdownCallback);
            lifecycle.start();

            // when
            lifecycle.stop(stopCallback);

            // then
            verify(shutdownCallback).run();
            verify(stopCallback).run();
        }

        /**
         * 测试回调为 null 时 stop 不应抛出异常。
         */
        @Test
        @DisplayName("回调为 null 时不应该抛出异常")
        void shouldNotThrowWhenCallbackIsNull() {
            // given
            lifecycle = new GracefulShutdownLifecycle("test", 0, 30000, null);
            lifecycle.start();

            // when/then
            lifecycle.stop(); // 不应该抛出异常
            assertThat(lifecycle.isRunning()).isFalse();
        }

        /**
         * 测试回调抛出异常时 stop 应继续执行并完成状态转换。
         */
        @Test
        @DisplayName("回调抛出异常时应该继续执行")
        void shouldContinueWhenCallbackThrows() {
            // given
            Runnable callback = () -> { throw new RuntimeException("test error"); };
            lifecycle = new GracefulShutdownLifecycle("test", 0, 30000, callback);
            lifecycle.start();

            // when
            lifecycle.stop();

            // then
            assertThat(lifecycle.isRunning()).isFalse();
        }
    }
}
