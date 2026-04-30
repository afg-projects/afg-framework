package io.github.afgprojects.framework.core.api.storage.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ListOptions 测试
 */
@DisplayName("ListOptions 测试")
class ListOptionsTest {

    @Test
    @DisplayName("应该创建默认选项")
    void shouldCreateDefaults() {
        ListOptions options = ListOptions.defaults();

        assertNull(options.prefix());
        assertNull(options.delimiter());
        assertEquals(1000, options.maxKeys());
        assertNull(options.marker());
    }

    @Test
    @DisplayName("应该创建带前缀的选项")
    void shouldCreateWithPrefix() {
        ListOptions options = ListOptions.withPrefix("images/");

        assertEquals("images/", options.prefix());
        assertNull(options.delimiter());
        assertEquals(1000, options.maxKeys());
    }

    @Test
    @DisplayName("应该创建带前缀和分隔符的选项")
    void shouldCreateWithPrefixAndDelimiter() {
        ListOptions options = ListOptions.withPrefixAndDelimiter("docs/", "/");

        assertEquals("docs/", options.prefix());
        assertEquals("/", options.delimiter());
    }

    @Test
    @DisplayName("应该使用 Builder 创建选项")
    void shouldCreateWithBuilder() {
        ListOptions options = ListOptions.builder()
                .prefix("files/")
                .delimiter("/")
                .maxKeys(100)
                .marker("marker123")
                .build();

        assertEquals("files/", options.prefix());
        assertEquals("/", options.delimiter());
        assertEquals(100, options.maxKeys());
        assertEquals("marker123", options.marker());
    }

    @Test
    @DisplayName("Builder 应该使用默认 maxKeys")
    void builderShouldUseDefaultMaxKeys() {
        ListOptions options = ListOptions.builder()
                .prefix("test/")
                .build();

        assertEquals(1000, options.maxKeys());
    }
}
