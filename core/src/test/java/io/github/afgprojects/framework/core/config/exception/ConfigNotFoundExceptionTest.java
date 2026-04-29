package io.github.afgprojects.framework.core.config.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConfigNotFoundException 测试
 */
@DisplayName("ConfigNotFoundException 测试")
class ConfigNotFoundExceptionTest {

    @Test
    @DisplayName("应该使用前缀创建 ConfigNotFoundException")
    void shouldCreateWithPrefix() {
        ConfigNotFoundException exception = new ConfigNotFoundException("myapp.database");
        assertTrue(exception.getMessage().contains("myapp.database"));
        assertEquals("myapp.database", exception.getPrefix());
    }

    @Test
    @DisplayName("消息应该包含 'Config not found'")
    void messageShouldContainConfigNotFound() {
        ConfigNotFoundException exception = new ConfigNotFoundException("test.prefix");
        assertTrue(exception.getMessage().contains("Config not found"));
    }
}
