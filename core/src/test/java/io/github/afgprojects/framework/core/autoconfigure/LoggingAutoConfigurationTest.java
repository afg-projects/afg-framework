package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.web.logging.LoggingProperties;

/**
 * LoggingAutoConfiguration 单元测试。
 * 测试日志自动配置类的 Bean 创建功能。
 *
 * @see LoggingAutoConfiguration
 */
@DisplayName("LoggingAutoConfiguration 测试")
class LoggingAutoConfigurationTest {

    private LoggingAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new LoggingAutoConfiguration();
    }

    /**
     * MDC 过滤器配置测试。
     * 验证 mdcFilter Bean 的创建。
     */
    @Nested
    @DisplayName("mdcFilter 配置测试")
    class MdcFilterTests {

        /**
         * 测试创建 MDC 过滤器。
         */
        @Test
        @DisplayName("应该创建 MDC 过滤器")
        void shouldCreateMdcFilter() {
            LoggingProperties properties = new LoggingProperties();

            var filter = configuration.mdcFilter(properties);

            assertThat(filter).isNotNull();
        }
    }
}
