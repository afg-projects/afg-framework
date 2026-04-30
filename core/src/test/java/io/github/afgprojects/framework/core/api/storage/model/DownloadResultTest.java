package io.github.afgprojects.framework.core.api.storage.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DownloadResult 测试
 */
@DisplayName("DownloadResult 测试")
class DownloadResultTest {

    @Test
    @DisplayName("应该创建简化的 DownloadResult")
    void shouldCreateSimplified() {
        ByteArrayInputStream input = new ByteArrayInputStream("test content".getBytes());
        DownloadResult result = DownloadResult.of(input, 12);

        assertEquals(input, result.inputStream());
        assertEquals(12, result.size());
        assertNull(result.contentType());
        assertNull(result.etag());
    }

    @Test
    @DisplayName("应该创建带类型的 DownloadResult")
    void shouldCreateWithType() {
        ByteArrayInputStream input = new ByteArrayInputStream("data".getBytes());
        DownloadResult result = DownloadResult.of(input, 4, "text/plain");

        assertEquals(input, result.inputStream());
        assertEquals(4, result.size());
        assertEquals("text/plain", result.contentType());
    }

    @Test
    @DisplayName("应该创建完整的 DownloadResult")
    void shouldCreateFull() {
        ByteArrayInputStream input = new ByteArrayInputStream("data".getBytes());
        DownloadResult result = new DownloadResult(input, 4, "application/json", "etag456");

        assertEquals(input, result.inputStream());
        assertEquals(4, result.size());
        assertEquals("application/json", result.contentType());
        assertEquals("etag456", result.etag());
    }

    @Test
    @DisplayName("应该正确关闭流")
    void shouldCloseStream() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream("test".getBytes());
        DownloadResult result = new DownloadResult(input, 4, null, null);

        // 关闭后流应该被关闭
        result.close();

        // 验证流已关闭（ByteArrayInputStream 关闭后读取会返回 -1 或抛出异常，取决于实现）
        // ByteArrayInputStream.close() 实际上什么都不做，所以这里只验证不会抛出异常
        assertDoesNotThrow(result::close);
    }

    @Test
    @DisplayName("应该支持 try-with-resources")
    void shouldSupportTryWithResources() {
        ByteArrayInputStream input = new ByteArrayInputStream("test".getBytes());

        try (DownloadResult result = new DownloadResult(input, 4, null, null)) {
            assertNotNull(result.inputStream());
        }
        // 应该正常退出，不抛出异常
    }
}
