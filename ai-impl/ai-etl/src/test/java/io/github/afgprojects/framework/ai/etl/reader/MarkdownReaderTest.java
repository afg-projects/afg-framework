package io.github.afgprojects.framework.ai.etl.reader;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import io.github.afgprojects.framework.ai.core.api.etl.Source;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MarkdownReader 测试类。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
class MarkdownReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void testSupports() {
        MarkdownReader reader = new MarkdownReader();

        assertTrue(reader.supports(Source.ofFile("readme.md")));
        assertTrue(reader.supports(Source.ofFile("README.MD")));
        assertTrue(reader.supports(Source.ofFile("doc.markdown")));
        assertTrue(reader.supports(Source.ofFile("doc.MARKDOWN")));
        assertFalse(reader.supports(Source.ofFile("doc.txt")));
        assertFalse(reader.supports(Source.ofFile("doc.pdf")));
    }

    @Test
    void testSupportsWithContentType() {
        MarkdownReader reader = new MarkdownReader();

        // 通过 content type 支持
        Source mdSource = Source.ofBytes(new byte[]{1, 2, 3}, "text/markdown");
        assertTrue(reader.supports(mdSource));
    }

    @Test
    void testReadMarkdown() throws IOException {
        Path mdFile = tempDir.resolve("test.md");
        String content = "# Test Title\n\nThis is a test document.\n\n## Section\n\nMore content.";
        Files.writeString(mdFile, content);

        MarkdownReader reader = new MarkdownReader();
        List<Document> documents = reader.read(Source.ofFile(mdFile.toString()));

        assertEquals(1, documents.size());
        Document doc = documents.get(0);
        assertTrue(doc.content().contains("Test Title"));
        assertEquals("Test Title", doc.getMetadata("title"));
        assertEquals("test.md", doc.getMetadata("fileName"));
        assertEquals("text/markdown", doc.getMetadata("contentType"));
    }

    @Test
    void testReadMarkdownWithoutH1() throws IOException {
        Path mdFile = tempDir.resolve("no-title.md");
        String content = "This is a document without a title.\n\n## Section\n\nMore content.";
        Files.writeString(mdFile, content);

        MarkdownReader reader = new MarkdownReader();
        List<Document> documents = reader.read(Source.ofFile(mdFile.toString()));

        assertEquals(1, documents.size());
        Document doc = documents.get(0);
        assertTrue(doc.content().contains("This is a document"));
        // 没有 H1 标题时不应该有 title 元数据
        assertNull(doc.getMetadata("title"));
    }

    @Test
    void testReadNonExistentFile() {
        MarkdownReader reader = new MarkdownReader();
        Source source = Source.ofFile("/nonexistent/file.md");

        assertThrows(RuntimeException.class, () -> reader.read(source));
    }

    @Test
    void testDefaultOptions() {
        MarkdownReader.MarkdownReadOptions defaults = MarkdownReader.MarkdownReadOptions.defaults();

        assertTrue(defaults.extractTitle());
    }

    @Test
    void testReadMarkdownWithCodeBlock() throws IOException {
        Path mdFile = tempDir.resolve("code.md");
        String content = "# Code Example\n\n```java\npublic class Test {\n    public static void main(String[] args) {}\n}\n```\n\nSome text after code.";
        Files.writeString(mdFile, content);

        MarkdownReader reader = new MarkdownReader();
        List<Document> documents = reader.read(Source.ofFile(mdFile.toString()));

        assertEquals(1, documents.size());
        Document doc = documents.get(0);
        assertTrue(doc.content().contains("```java"));
        assertTrue(doc.content().contains("public class Test"));
        assertEquals("Code Example", doc.getMetadata("title"));
    }

    @Test
    void testReadMarkdownWithChinese() throws IOException {
        Path mdFile = tempDir.resolve("chinese.md");
        String content = "# 中文标题\n\n这是一段中文内容。\n\n## 第二节\n\n更多内容。";
        Files.writeString(mdFile, content);

        MarkdownReader reader = new MarkdownReader();
        List<Document> documents = reader.read(Source.ofFile(mdFile.toString()));

        assertEquals(1, documents.size());
        Document doc = documents.get(0);
        assertTrue(doc.content().contains("中文标题"));
        assertTrue(doc.content().contains("中文内容"));
        assertEquals("中文标题", doc.getMetadata("title"));
    }
}