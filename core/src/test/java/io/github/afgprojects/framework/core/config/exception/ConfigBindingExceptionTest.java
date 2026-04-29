package io.github.afgprojects.framework.core.config.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConfigBindingException 测试
 */
@DisplayName("ConfigBindingException 测试")
class ConfigBindingExceptionTest {

    @Test
    @DisplayName("应该使用前缀和消息创建 ConfigBindingException")
    void shouldCreateWithPrefixAndMessage() {
        ConfigBindingException exception = new ConfigBindingException("myapp", "Invalid type");
        assertTrue(exception.getMessage().contains("myapp"));
        assertTrue(exception.getMessage().contains("Invalid type"));
        assertTrue(exception.getMessage().contains("Config binding failed"));
        assertEquals("myapp", exception.getPrefix());
    }

    @Test
    @DisplayName("应该使用前缀、消息和原因创建 ConfigBindingException")
    void shouldCreateWithPrefixMessageAndCause() {
        Throwable cause = new RuntimeException("Type mismatch");
        ConfigBindingException exception = new ConfigBindingException("db.config", "Cannot bind", cause);
        assertTrue(exception.getMessage().contains("db.config"));
        assertTrue(exception.getMessage().contains("Cannot bind"));
        assertTrue(exception.getMessage().contains("Config binding failed"));
        assertEquals("db.config", exception.getPrefix());
        assertEquals(cause, exception.getCause());
    }
}
