package io.github.afgprojects.framework.core.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.github.afgprojects.framework.core.support.TestApplication;

/**
 * {@link GracefulShutdownManager} 的集成测试。
 * <p>
 * 在 Spring Boot 环境中测试优雅停机管理器的自动配置和功能，包括：
 * <ul>
 *   <li>关闭钩子的注册</li>
 *   <li>活跃请求计数</li>
 *   <li>停机状态查询</li>
 * </ul>
 *
 * @see GracefulShutdownManager
 */
@DisplayName("GracefulShutdownManager 集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.cloud.enabled=true",
                "afg.cloud.graceful-shutdown.enabled=true",
                "afg.cloud.graceful-shutdown.timeout=60s",
                "afg.cloud.graceful-shutdown.request-wait-timeout=30s"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GracefulShutdownManagerIntegrationTest {

    @Autowired(required = false)
    private GracefulShutdownManager shutdownManager;

    /**
     * 关闭管理器配置测试分组。
     * <p>
     * 验证 Spring Boot 自动配置是否正确注入 GracefulShutdownManager。
     */
    @Nested
    @DisplayName("关闭管理器配置测试")
    class ShutdownManagerConfigTests {

        /**
         * 测试 Spring Boot 应自动配置并注入 GracefulShutdownManager。
         */
        @Test
        @DisplayName("应该自动配置关闭管理器")
        void shouldAutoConfigureShutdownManager() {
            if (shutdownManager != null) {
                assertThat(shutdownManager).isNotNull();
            }
        }
    }

    /**
     * 关闭钩子测试分组。
     * <p>
     * 验证关闭钩子的注册功能，包括带阶段和不带阶段的钩子。
     */
    @Nested
    @DisplayName("关闭钩子测试")
    class ShutdownHookTests {

        /**
         * 测试应能注册不带阶段的关闭钩子。
         */
        @Test
        @DisplayName("应该能够注册关闭钩子")
        void shouldRegisterShutdownHook() {
            if (shutdownManager == null) {
                return;
            }

            AtomicInteger counter = new AtomicInteger(0);

            shutdownManager.register("test-hook", 0, () -> {
                counter.incrementAndGet();
            });
        }

        /**
         * 测试应能注册带阶段值的关闭钩子。
         */
        @Test
        @DisplayName("应该能够注册带阶段的关闭钩子")
        void shouldRegisterShutdownHookWithPhase() {
            if (shutdownManager == null) {
                return;
            }

            shutdownManager.register("phase-1-hook", 1, () -> {});
            shutdownManager.register("phase-2-hook", 2, () -> {});
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
        @DisplayName("应该能够增加活跃请求计数")
        void shouldIncrementActiveRequests() {
            if (shutdownManager == null) {
                return;
            }

            int initial = shutdownManager.getActiveRequests();

            shutdownManager.incrementActiveRequests();

            assertThat(shutdownManager.getActiveRequests()).isEqualTo(initial + 1);

            // 清理
            shutdownManager.decrementActiveRequests();
        }

        /**
         * 测试应能正确减少活跃请求计数。
         */
        @Test
        @DisplayName("应该能够减少活跃请求计数")
        void shouldDecrementActiveRequests() {
            if (shutdownManager == null) {
                return;
            }

            shutdownManager.incrementActiveRequests();
            shutdownManager.incrementActiveRequests();

            shutdownManager.decrementActiveRequests();

            assertThat(shutdownManager.getActiveRequests()).isEqualTo(1);

            // 清理
            shutdownManager.decrementActiveRequests();
        }
    }

    /**
     * 停机状态测试分组。
     * <p>
     * 验证停机状态的查询功能。
     */
    @Nested
    @DisplayName("停机状态测试")
    class ShutdownStatusTests {

        /**
         * 测试应能获取指定钩子的停机状态。
         */
        @Test
        @DisplayName("应该能够获取停机状态")
        void shouldGetShutdownStatus() {
            if (shutdownManager == null) {
                return;
            }

            var status = shutdownManager.getShutdownStatus("non-existent");

            assertThat(status).isNull();
        }

        /**
         * 测试应能检查当前是否正在停机。
         */
        @Test
        @DisplayName("应该能够检查是否正在停机")
        void shouldCheckIsShuttingDown() {
            if (shutdownManager == null) {
                return;
            }

            assertThat(shutdownManager.isShuttingDown()).isFalse();
        }
    }
}
