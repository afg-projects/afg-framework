package io.github.afgprojects.framework.core.api.storage.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ListResult 测试
 */
@DisplayName("ListResult 测试")
class ListResultTest {

    @Test
    @DisplayName("应该创建空结果")
    void shouldCreateEmpty() {
        ListResult result = ListResult.empty();

        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
        assertTrue(result.objects().isEmpty());
        assertTrue(result.commonPrefixes().isEmpty());
        assertFalse(result.isTruncated());
        assertNull(result.nextMarker());
    }

    @Test
    @DisplayName("应该创建带对象列表的结果")
    void shouldCreateWithObjects() {
        StorageObject obj1 = StorageObject.of("file1.txt", 100, "text/plain");
        StorageObject obj2 = StorageObject.of("file2.txt", 200, "text/plain");
        ListResult result = ListResult.of(List.of(obj1, obj2));

        assertEquals(2, result.size());
        assertFalse(result.isEmpty());
        assertEquals(2, result.objects().size());
    }

    @Test
    @DisplayName("应该创建带公共前缀的结果")
    void shouldCreateWithCommonPrefixes() {
        StorageObject obj = StorageObject.of("docs/readme.txt", 100, "text/plain");
        ListResult result = ListResult.of(List.of(obj), List.of("images/", "videos/"));

        assertEquals(1, result.objects().size());
        assertEquals(2, result.commonPrefixes().size());
        assertTrue(result.commonPrefixes().contains("images/"));
    }

    @Test
    @DisplayName("应该创建带分页的结果")
    void shouldCreateWithPagination() {
        StorageObject obj = StorageObject.of("file.txt", 100, "text/plain");
        ListResult result = ListResult.of(
                List.of(obj),
                Collections.emptyList(),
                true,
                "next-marker-123"
        );

        assertTrue(result.isTruncated());
        assertEquals("next-marker-123", result.nextMarker());
    }

    @Test
    @DisplayName("isEmpty 应该在只有 commonPrefixes 时返回 false")
    void isEmptyShouldReturnFalseWhenOnlyCommonPrefixes() {
        ListResult result = ListResult.of(Collections.emptyList(), List.of("prefix/"));

        assertFalse(result.isEmpty());
        assertEquals(0, result.size());
    }
}
