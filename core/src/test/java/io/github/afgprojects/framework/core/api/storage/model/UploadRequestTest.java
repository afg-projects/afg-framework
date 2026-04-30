package io.github.afgprojects.framework.core.api.storage.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UploadRequest 测试
 */
@DisplayName("UploadRequest 测试")
class UploadRequestTest {

    @Test
    @DisplayName("应该使用简化方法创建 UploadRequest")
    void shouldCreateWithSimplifiedMethod() {
        ByteArrayInputStream input = new ByteArrayInputStream("test".getBytes());
        UploadRequest request = UploadRequest.of("test.txt", input);

        assertEquals("test.txt", request.key());
        assertEquals(input, request.inputStream());
        assertEquals(-1, request.size());
        assertNull(request.contentType());
        assertNull(request.metadata());
    }

    @Test
    @DisplayName("应该使用完整参数创建 UploadRequest")
    void shouldCreateWithFullParameters() {
        ByteArrayInputStream input = new ByteArrayInputStream("test content".getBytes());
        UploadRequest request = UploadRequest.of("doc.pdf", input, 12, "application/pdf");

        assertEquals("doc.pdf", request.key());
        assertEquals(input, request.inputStream());
        assertEquals(12, request.size());
        assertEquals("application/pdf", request.contentType());
        assertNull(request.metadata());
    }

    @Test
    @DisplayName("应该使用 Builder 创建 UploadRequest")
    void shouldCreateWithBuilder() {
        ByteArrayInputStream input = new ByteArrayInputStream("data".getBytes());
        StorageMetadata metadata = StorageMetadata.builder().put("author", "test").build();

        UploadRequest request = UploadRequest.builder()
                .key("image.png")
                .inputStream(input)
                .size(4)
                .contentType("image/png")
                .metadata(metadata)
                .build();

        assertEquals("image.png", request.key());
        assertEquals(input, request.inputStream());
        assertEquals(4, request.size());
        assertEquals("image/png", request.contentType());
        assertNotNull(request.metadata());
        assertEquals("test", request.metadata().get("author"));
    }

    @Test
    @DisplayName("Builder 应该在没有 key 时抛出异常")
    void builderShouldThrowWithoutKey() {
        ByteArrayInputStream input = new ByteArrayInputStream("test".getBytes());
        assertThrows(IllegalStateException.class, () ->
            UploadRequest.builder().inputStream(input).build()
        );
    }

    @Test
    @DisplayName("Builder 应该在没有 inputStream 时抛出异常")
    void builderShouldThrowWithoutInputStream() {
        assertThrows(IllegalStateException.class, () ->
            UploadRequest.builder().key("test.txt").build()
        );
    }
}
