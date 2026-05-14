package io.github.afgprojects.framework.core.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.cloud.CloudNativeProperties.GracefulShutdownConfig;

/**
 * {@link GracefulShutdownManager} 的单元测试。
 * <p>
 * 测试优雅停机管理器的核心功能，包括：
 * <ul>
 *   <li>关闭钩子的注册与执行</li>
 *   <li>活跃请求计数管理</li>
 *   <li>停机状态跟踪</li>
 *   <li>阶段顺序执行</li>
 * </ul>
 *
 * @see GracefulShutdownManager
 * @see GracefulShutdownConfig
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

    /**
     * 钩子注册测试分组。
     * <p>
     * 验证关闭钩子的注册功能。
     */
    @Nested
    @DisplayName("注册测试")
    class RegisterTests {

        /**
         * 测试应能注册不带阶段的停机回调。
         */
        @Test
        @DisplayName("应该注册停机回调")
        void shouldRegisterShutdownHook() {
            manager.register("test-hook", () -> {});

            assertThat(manager.getShutdownStatus("test-hook")).isNull();
        }

        /**
         * 测试应能注册带阶段值的停机回调。
         */
        @Test
        @DisplayName("应该注册带阶段的停机回调")
        void shouldRegisterShutdownHookWithPhase() {
            manager.register("test-hook", 10, () -> {});

            assertThat(manager.getShutdownStatus("test-hook")).isNull();
        }
    }

    /**
     * 活跃请求测试分组。
     * <p>
     * 验证活跃请求计数的增减操作。
     */
    @Nested
    @DisplayName("活跃请求测试")
    class ActiveRequestsTests {

        /**
         * 测试应能正确增加活跃请求计数。
         */
        @Test
        @DisplayName("应该正确增加活跃请求计数")
        void shouldIncrementActiveRequests() {
            manager.incrementActiveRequests();

            assertThat(manager.getActiveRequests()).isEqualTo(1);
        }

        /**
         * 测试应能正确减少活跃请求计数。
         */
        @Test
        @DisplayName("应该正确减少活跃请求计数")
        void shouldDecrementActiveRequests() {
            manager.incrementActiveRequests();
            manager.incrementActiveRequests();
            manager.decrementActiveRequests();

            assertThat(manager.getActiveRequests()).isEqualTo(1);
        }
    }

    /**
     * 停机状态测试分组。
     * <p>
     * 验证停机状态的初始值和状态转换。
     */
    @Nested
    @DisplayName("停机状态测试")
    class ShutdownStatusTests {

        /**
         * 测试初始状态应不在停机中。
         */
        @Test
        @DisplayName("初始状态应该不在停机中")
        void shouldNotBeShuttingDownInitially() {
            assertThat(manager.isShuttingDown()).isFalse();
        }

        /**
         * 测试调用 shutdown 后应处于停机状态。
         */
        @Test
        @DisplayName("停机后应该处于停机状态")
        void shouldBeShuttingDownAfterShutdown() {
            manager.register("test", () -> {});
            manager.shutdown();

            assertThat(manager.isShuttingDown()).isTrue();
        }
    }

    /**
     * 停机执行测试分组。
     * <p>
     * 验证停机回调的执行行为，包括执行顺序、状态记录和重复调用处理。
     */
    @Nested
    @DisplayName("停机执行测试")
    class ShutdownExecutionTests {

        /**
         * 测试停机时应执行已注册的回调。
         */
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

        /**
         * 测试停机回调应按阶段值从小到大顺序执行。
         */
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

        /**
         * 测试回调执行失败时应记录 FAILED 状态。
         */
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

        /**
         * 测试重复调用 shutdown 应被忽略，回调只执行一次。
         */
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

    /**
     * ShutdownStatus 枚举测试分组。
     * <p>
     * 验证 ShutdownStatus 枚举包含所有预期的状态值。
     */
    @Nested
    @DisplayName("ShutdownStatus 枚举测试")
    class ShutdownStatusEnumTests {

        /**
         * 测试枚举应包含 PENDING、RUNNING、COMPLETED、FAILED 四种状态。
         */
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

    /**
     * ShutdownHook 记录测试分组。
     * <p>
     * 验证 ShutdownHook 记录类的创建和属性访问。
     */
    @Nested
    @DisplayName("ShutdownHook 记录测试")
    class ShutdownHookTests {

        /**
         * 测试应能正确创建 ShutdownHook 记录并访问其属性。
         */
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
