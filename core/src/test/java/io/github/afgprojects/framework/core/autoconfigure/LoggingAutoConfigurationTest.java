package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.web.logging.LoggingProperties;

/**
 * LoggingAutoConfiguration 测试
 */
@DisplayName("LoggingAutoConfiguration 测试")
class LoggingAutoConfigurationTest {

    private LoggingAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new LoggingAutoConfiguration();
    }

    @Nested
    @DisplayName("mdcFilter 配置测试")
    class MdcFilterTests {

        @Test
        @DisplayName("应该创建 MDC 过滤器")
        void shouldCreateMdcFilter() {
            LoggingProperties properties = new LoggingProperties();

            var filter = configuration.mdcFilter(properties);

            assertThat(filter).isNotNull();
        }
    }
}
