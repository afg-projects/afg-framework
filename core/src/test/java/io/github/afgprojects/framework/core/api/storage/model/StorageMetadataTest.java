package io.github.afgprojects.framework.core.api.storage.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StorageMetadata 测试
 */
@DisplayName("StorageMetadata 测试")
class StorageMetadataTest {

    @Test
    @DisplayName("应该创建空的 StorageMetadata")
    void shouldCreateEmpty() {
        StorageMetadata metadata = new StorageMetadata();
        assertTrue(metadata.isEmpty());
    }

    @Test
    @DisplayName("应该从 Map 创建 StorageMetadata")
    void shouldCreateFromMap() {
        StorageMetadata metadata = new StorageMetadata(Map.of("key1", "value1", "key2", "value2"));

        assertEquals("value1", metadata.get("key1"));
        assertEquals("value2", metadata.get("key2"));
        assertFalse(metadata.isEmpty());
    }

    @Test
    @DisplayName("应该正确设置和获取值")
    void shouldPutAndGet() {
        StorageMetadata metadata = new StorageMetadata();
        metadata.put("author", "test");
        metadata.put("version", "1.0");

        assertEquals("test", metadata.get("author"));
        assertEquals("1.0", metadata.get("version"));
        assertNull(metadata.get("nonexistent"));
    }

    @Test
    @DisplayName("应该检查是否包含键")
    void shouldCheckContainsKey() {
        StorageMetadata metadata = new StorageMetadata();
        metadata.put("key", "value");

        assertTrue(metadata.containsKey("key"));
        assertFalse(metadata.containsKey("other"));
    }

    @Test
    @DisplayName("应该返回不可变的 Map")
    void shouldReturnUnmodifiableMap() {
        StorageMetadata metadata = new StorageMetadata();
        metadata.put("key", "value");

        Map<String, String> all = metadata.getAll();
        assertEquals(1, all.size());
        assertThrows(UnsupportedOperationException.class, () -> all.put("new", "value"));
    }

    @Test
    @DisplayName("应该使用 Builder 创建 StorageMetadata")
    void shouldCreateWithBuilder() {
        StorageMetadata metadata = StorageMetadata.builder()
                .put("author", "test")
                .put("date", "2024-01-01")
                .putAll(Map.of("extra1", "val1", "extra2", "val2"))
                .build();

        assertEquals("test", metadata.get("author"));
        assertEquals("2024-01-01", metadata.get("date"));
        assertEquals("val1", metadata.get("extra1"));
        assertEquals("val2", metadata.get("extra2"));
    }
}
