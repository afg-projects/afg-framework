package io.github.afgprojects.framework.core.api.storage.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StorageObject 测试
 */
@DisplayName("StorageObject 测试")
class StorageObjectTest {

    @Test
    @DisplayName("应该创建简化的 StorageObject")
    void shouldCreateSimplified() {
        StorageObject obj = StorageObject.of("test.txt", 1024, "text/plain");

        assertEquals("test.txt", obj.key());
        assertEquals(1024, obj.size());
        assertEquals("text/plain", obj.contentType());
        assertNotNull(obj.lastModified());
        assertNull(obj.etag());
        assertNull(obj.metadata());
    }

    @Test
    @DisplayName("应该创建带元数据的 StorageObject")
    void shouldCreateWithMetadata() {
        StorageMetadata metadata = StorageMetadata.builder().put("author", "test").build();
        StorageObject obj = StorageObject.of("doc.pdf", 2048, "application/pdf", metadata);

        assertEquals("doc.pdf", obj.key());
        assertEquals(2048, obj.size());
        assertEquals("application/pdf", obj.contentType());
        assertNotNull(obj.metadata());
        assertEquals("test", obj.metadata().get("author"));
    }

    @Test
    @DisplayName("应该正确提取文件名")
    void shouldExtractFileName() {
        StorageObject obj1 = StorageObject.of("path/to/file.txt", 100, "text/plain");
        assertEquals("file.txt", obj1.getFileName());

        StorageObject obj2 = StorageObject.of("simple.txt", 100, "text/plain");
        assertEquals("simple.txt", obj2.getFileName());

        StorageObject obj3 = StorageObject.of("noextension", 100, "text/plain");
        assertEquals("noextension", obj3.getFileName());
    }

    @Test
    @DisplayName("应该正确提取扩展名")
    void shouldExtractExtension() {
        StorageObject obj1 = StorageObject.of("path/to/file.txt", 100, "text/plain");
        assertEquals("txt", obj1.getExtension());

        StorageObject obj2 = StorageObject.of("archive.tar.gz", 100, "application/gzip");
        assertEquals("gz", obj2.getExtension());

        StorageObject obj3 = StorageObject.of("noextension", 100, "text/plain");
        assertNull(obj3.getExtension());

        StorageObject obj4 = StorageObject.of(".hidden", 100, "text/plain");
        assertNull(obj4.getExtension());
    }

    @Test
    @DisplayName("应该创建完整的 StorageObject")
    void shouldCreateFullObject() {
        Instant now = Instant.now();
        StorageMetadata metadata = new StorageMetadata();
        StorageObject obj = new StorageObject("test.json", 500, "application/json", "etag123", now, metadata);

        assertEquals("test.json", obj.key());
        assertEquals(500, obj.size());
        assertEquals("application/json", obj.contentType());
        assertEquals("etag123", obj.etag());
        assertEquals(now, obj.lastModified());
        assertEquals(metadata, obj.metadata());
    }
}
