package io.github.afgprojects.framework.ai.core.api.etl.

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SourceTest {

    @Test
    void testOfFile() {
        Source source = Source.ofFile("/path/to/document.pdf");

        assertEquals("/path/to/document.pdf", source.getPath());
        assertEquals(Source.SourceType.FILE, source.getType());
        assertEquals("application/pdf", source.getContentType());
    }

    @Test
    void testOfUrl() {
        Source source = Source.ofUrl("https://example.com/article.html");

        assertEquals("https://example.com/article.html", source.getPath());
        assertEquals(Source.SourceType.URL, source.getType());
    }

    @Test
    void testOfText() {
        Source source = Source.ofText("Hello, World!");

        assertEquals(Source.SourceType.TEXT, source.getType());
        assertEquals("text/plain", source.getContentType());
    }

    @Test
    void testOfBytes() {
        byte[] data = new byte[]{1, 2, 3};
        Source source = Source.ofBytes(data, "application/octet-stream");

        assertEquals(Source.SourceType.BYTES, source.getType());
        assertEquals("application/octet-stream", source.getContentType());
    }

    @Test
    void testFileSourceContentTypeInference() {
        assertEquals("application/pdf", Source.ofFile("doc.pdf").getContentType());
        assertEquals("text/markdown", Source.ofFile("readme.md").getContentType());
        assertEquals("text/plain", Source.ofFile("notes.txt").getContentType());
        assertEquals("text/html", Source.ofFile("page.html").getContentType());
        assertEquals("application/json", Source.ofFile("data.json").getContentType());
        assertEquals("application/xml", Source.ofFile("config.xml").getContentType());
        assertEquals("text/csv", Source.ofFile("data.csv").getContentType());
        assertEquals("application/octet-stream", Source.ofFile("unknown.xyz").getContentType());
    }
}
