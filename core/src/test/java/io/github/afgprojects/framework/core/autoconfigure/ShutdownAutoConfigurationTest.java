package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.web.shutdown.ShutdownProperties;

/**
 * ShutdownAutoConfiguration 测试
 */
@DisplayName("ShutdownAutoConfiguration 测试")
class ShutdownAutoConfigurationTest {

    private ShutdownAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new ShutdownAutoConfiguration();
    }

    @Nested
    @DisplayName("shutdownHook 配置测试")
    class ShutdownHookTests {

        @Test
        @DisplayName("应该创建关闭钩子")
        void shouldCreateShutdownHook() {
            ShutdownProperties properties = new ShutdownProperties();

            var hook = configuration.shutdownHook(properties);

            assertThat(hook).isNotNull();
        }
    }
}
