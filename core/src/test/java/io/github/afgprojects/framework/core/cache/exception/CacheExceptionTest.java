package io.github.afgprojects.framework.core.cache.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CacheException 测试
 */
@DisplayName("CacheException 测试")
class CacheExceptionTest {

    @Test
    @DisplayName("应该使用消息创建 CacheException")
    void shouldCreateWithMessage() {
        CacheException exception = new CacheException("Cache miss");
        assertEquals(17000, exception.getCode());
        assertEquals("Cache miss", exception.getMessage());
    }

    @Test
    @DisplayName("应该使用消息和原因创建 CacheException")
    void shouldCreateWithMessageAndCause() {
        Throwable cause = new RuntimeException("Connection failed");
        CacheException exception = new CacheException("Cache error", cause);
        assertEquals(17000, exception.getCode());
        assertEquals("Cache error", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("应该使用自定义错误码和消息创建 CacheException")
    void shouldCreateWithCodeAndMessage() {
        CacheException exception = new CacheException(17001, "Cache timeout");
        assertEquals(17001, exception.getCode());
        assertEquals("Cache timeout", exception.getMessage());
    }

    @Test
    @DisplayName("应该使用自定义错误码、消息和原因创建 CacheException")
    void shouldCreateWithCodeMessageAndCause() {
        Throwable cause = new RuntimeException("Redis down");
        CacheException exception = new CacheException(17002, "Cache unavailable", cause);
        assertEquals(17002, exception.getCode());
        assertEquals("Cache unavailable", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
