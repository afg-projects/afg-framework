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
 * GracefulShutdownLifecycle 单元测试
 */
@DisplayName("GracefulShutdownLifecycle 单元测试")
@ExtendWith(MockitoExtension.class)
class GracefulShutdownLifecycleTest extends BaseUnitTest {

    private GracefulShutdownLifecycle lifecycle;

    @Nested
    @DisplayName("基本属性测试")
    class BasicPropertiesTests {

        @Test
        @DisplayName("应该正确获取名称")
        void shouldGetName() {
            // given
            lifecycle = new GracefulShutdownLifecycle("test-lifecycle", 0, 30000, null);

            // when/then
            assertThat(lifecycle.getName()).isEqualTo("test-lifecycle");
        }

        @Test
        @DisplayName("应该正确获取超时时间")
        void shouldGetTimeoutMs() {
            // given
            lifecycle = new GracefulShutdownLifecycle("test", 0, 60000, null);

            // when/then
            assertThat(lifecycle.getTimeoutMs()).isEqualTo(60000);
        }

        @Test
        @DisplayName("应该正确获取 phase")
        void shouldGetPhase() {
            // given
            lifecycle = new GracefulShutdownLifecycle("test", 100, 30000, null);

            // when/then
            assertThat(lifecycle.getPhase()).isEqualTo(100);
        }

        @Test
        @DisplayName("应该自动启动")
        void shouldAutoStartup() {
            // given
            lifecycle = new GracefulShutdownLifecycle("test", 0, 30000, null);

            // when/then
            assertThat(lifecycle.isAutoStartup()).isTrue();
        }
    }

    @Nested
    @DisplayName("生命周期测试")
    class LifecycleTests {

        @BeforeEach
        void setUp() {
            lifecycle = new GracefulShutdownLifecycle("test", 0, 30000, null);
        }

        @Test
        @DisplayName("初始状态应该未运行")
        void shouldNotBeRunningInitially() {
            assertThat(lifecycle.isRunning()).isFalse();
        }

        @Test
        @DisplayName("start 后应该处于运行状态")
        void shouldBeRunningAfterStart() {
            // when
            lifecycle.start();

            // then
            assertThat(lifecycle.isRunning()).isTrue();
        }

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

    @Nested
    @DisplayName("回调测试")
    class CallbackTests {

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
