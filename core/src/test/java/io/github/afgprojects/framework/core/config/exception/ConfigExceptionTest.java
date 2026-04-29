package io.github.afgprojects.framework.core.config.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConfigException 测试
 */
@DisplayName("ConfigException 测试")
class ConfigExceptionTest {

    @Test
    @DisplayName("应该使用错误码和消息创建 ConfigException")
    void shouldCreateWithCodeAndMessage() {
        ConfigException exception = new ConfigException(1001, "Config error");
        assertEquals(1001, exception.getCode());
        assertEquals("Config error", exception.getMessage());
        assertNull(exception.getPrefix());
    }

    @Test
    @DisplayName("应该使用错误码、消息和前缀创建 ConfigException")
    void shouldCreateWithCodeMessageAndPrefix() {
        ConfigException exception = new ConfigException(1002, "Config error", "myapp");
        assertEquals(1002, exception.getCode());
        assertEquals("Config error", exception.getMessage());
        assertEquals("myapp", exception.getPrefix());
    }

    @Test
    @DisplayName("应该使用错误码、消息和原因创建 ConfigException")
    void shouldCreateWithCodeMessageAndCause() {
        Throwable cause = new RuntimeException("IO error");
        ConfigException exception = new ConfigException(1003, "Config error", cause);
        assertEquals(1003, exception.getCode());
        assertEquals("Config error", exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNull(exception.getPrefix());
    }

    @Test
    @DisplayName("应该使用所有参数创建 ConfigException")
    void shouldCreateWithAllParameters() {
        Throwable cause = new RuntimeException("IO error");
        ConfigException exception = new ConfigException(1004, "Config error", "db", cause);
        assertEquals(1004, exception.getCode());
        assertEquals("Config error", exception.getMessage());
        assertEquals("db", exception.getPrefix());
        assertEquals(cause, exception.getCause());
    }
}
