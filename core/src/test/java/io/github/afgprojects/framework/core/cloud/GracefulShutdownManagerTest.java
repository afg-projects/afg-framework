package io.github.afgprojects.framework.core.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.cloud.CloudNativeProperties.GracefulShutdownConfig;

/**
 * GracefulShutdownManager 测试
 */
@DisplayName("GracefulShutdownManager 测试")
class GracefulShutdownManagerTest {

    private GracefulShutdownManager manager;
    private GracefulShutdownConfig config;

    @BeforeEach
    void setUp() {
        config = new GracefulShutdownConfig();
        config.setTimeout(Duration.ofSeconds(5));
        config.setWaitForRequests(true);
        config.setRequestWaitTimeout(Duration.ofSeconds(2));
        manager = new GracefulShutdownManager(config);
    }

    @Nested
    @DisplayName("注册测试")
    class RegisterTests {

        @Test
        @DisplayName("应该注册停机回调")
        void shouldRegisterShutdownHook() {
            manager.register("test-hook", () -> {});

            assertThat(manager.getShutdownStatus("test-hook")).isNull();
        }

        @Test
        @DisplayName("应该注册带阶段的停机回调")
        void shouldRegisterShutdownHookWithPhase() {
            manager.register("test-hook", 10, () -> {});

            assertThat(manager.getShutdownStatus("test-hook")).isNull();
        }
    }

    @Nested
    @DisplayName("活跃请求测试")
    class ActiveRequestsTests {

        @Test
        @DisplayName("应该正确增加活跃请求计数")
        void shouldIncrementActiveRequests() {
            manager.incrementActiveRequests();

            assertThat(manager.getActiveRequests()).isEqualTo(1);
        }

        @Test
        @DisplayName("应该正确减少活跃请求计数")
        void shouldDecrementActiveRequests() {
            manager.incrementActiveRequests();
            manager.incrementActiveRequests();
            manager.decrementActiveRequests();

            assertThat(manager.getActiveRequests()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("停机状态测试")
    class ShutdownStatusTests {

        @Test
        @DisplayName("初始状态应该不在停机中")
        void shouldNotBeShuttingDownInitially() {
            assertThat(manager.isShuttingDown()).isFalse();
        }

        @Test
        @DisplayName("停机后应该处于停机状态")
        void shouldBeShuttingDownAfterShutdown() {
            manager.register("test", () -> {});
            manager.shutdown();

            assertThat(manager.isShuttingDown()).isTrue();
        }
    }

    @Nested
    @DisplayName("停机执行测试")
    class ShutdownExecutionTests {

        @Test
        @DisplayName("应该执行停机回调")
        void shouldExecuteShutdownHooks() {
            boolean[] executed = {false};
            manager.register("test-hook", () -> executed[0] = true);

            manager.shutdown();

            assertThat(executed[0]).isTrue();
            assertThat(manager.getShutdownStatus("test-hook")).isEqualTo(
                    GracefulShutdownManager.ShutdownStatus.COMPLETED);
        }

        @Test
        @DisplayName("应该按阶段顺序执行")
        void shouldExecuteHooksInPhaseOrder() {
            StringBuilder order = new StringBuilder();
            manager.register("hook-3", 30, () -> order.append("3"));
            manager.register("hook-1", 10, () -> order.append("1"));
            manager.register("hook-2", 20, () -> order.append("2"));

            manager.shutdown();

            assertThat(order.toString()).isEqualTo("123");
        }

        @Test
        @DisplayName("应该记录失败状态")
        void shouldRecordFailedStatus() {
            manager.register("failing-hook", () -> {
                throw new RuntimeException("Test failure");
            });

            manager.shutdown();

            assertThat(manager.getShutdownStatus("failing-hook")).isEqualTo(
                    GracefulShutdownManager.ShutdownStatus.FAILED);
        }

        @Test
        @DisplayName("重复停机应该被忽略")
        void shouldIgnoreDuplicateShutdown() {
            int[] count = {0};
            manager.register("test", () -> count[0]++);

            manager.shutdown();
            manager.shutdown();

            assertThat(count[0]).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("ShutdownStatus 枚举测试")
    class ShutdownStatusEnumTests {

        @Test
        @DisplayName("应该包含所有状态")
        void shouldContainAllStatuses() {
            GracefulShutdownManager.ShutdownStatus[] statuses =
                    GracefulShutdownManager.ShutdownStatus.values();

            assertThat(statuses).hasSize(4);
            assertThat(statuses).contains(
                    GracefulShutdownManager.ShutdownStatus.PENDING,
                    GracefulShutdownManager.ShutdownStatus.RUNNING,
                    GracefulShutdownManager.ShutdownStatus.COMPLETED,
                    GracefulShutdownManager.ShutdownStatus.FAILED
            );
        }
    }

    @Nested
    @DisplayName("ShutdownHook 记录测试")
    class ShutdownHookTests {

        @Test
        @DisplayName("应该正确创建 ShutdownHook")
        void shouldCreateShutdownHook() {
            Runnable callback = () -> {};
            GracefulShutdownManager.ShutdownHook hook =
                    new GracefulShutdownManager.ShutdownHook("test", 10, callback);

            assertThat(hook.name()).isEqualTo("test");
            assertThat(hook.phase()).isEqualTo(10);
            assertThat(hook.callback()).isSameAs(callback);
        }
    }
}
