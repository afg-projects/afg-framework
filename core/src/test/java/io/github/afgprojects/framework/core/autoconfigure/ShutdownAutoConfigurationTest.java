package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * ShutdownAutoConfiguration 单元测试。
 * 测试关闭钩子自动配置类的 Bean 创建功能。
 *
 * @see ShutdownAutoConfiguration
 */
@DisplayName("ShutdownAutoConfiguration 测试")
class ShutdownAutoConfigurationTest {

    private ShutdownAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new ShutdownAutoConfiguration();
    }

    /**
     * 关闭钩子配置测试。
     * 验证 shutdownHook Bean 的创建。
     */
    @Nested
    @DisplayName("shutdownHook 配置测试")
    class ShutdownHookTests {

        /**
         * 测试创建关闭钩子。
         */
        @Test
        @DisplayName("应该创建关闭钩子")
        void shouldCreateShutdownHook() {
            AfgCoreProperties properties = new AfgCoreProperties();

            var hook = configuration.shutdownHook(properties);

            assertThat(hook).isNotNull();
        }
    }
}
