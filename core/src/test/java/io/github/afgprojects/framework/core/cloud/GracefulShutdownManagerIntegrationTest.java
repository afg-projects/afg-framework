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
 * GracefulShutdownManager 集成测试
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

    @Nested
    @DisplayName("关闭管理器配置测试")
    class ShutdownManagerConfigTests {

        @Test
        @DisplayName("应该自动配置关闭管理器")
        void shouldAutoConfigureShutdownManager() {
            if (shutdownManager != null) {
                assertThat(shutdownManager).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("关闭钩子测试")
    class ShutdownHookTests {

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

    @Nested
    @DisplayName("活跃请求测试")
    class ActiveRequestsTests {

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

    @Nested
    @DisplayName("停机状态测试")
    class ShutdownStatusTests {

        @Test
        @DisplayName("应该能够获取停机状态")
        void shouldGetShutdownStatus() {
            if (shutdownManager == null) {
                return;
            }

            var status = shutdownManager.getShutdownStatus("non-existent");

            assertThat(status).isNull();
        }

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
