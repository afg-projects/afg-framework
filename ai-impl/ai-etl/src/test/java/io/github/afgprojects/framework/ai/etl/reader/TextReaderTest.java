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
 * TextReader 测试类。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
class TextReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void testSupports() {
        TextReader reader = new TextReader();

        assertTrue(reader.supports(Source.ofFile("document.txt")));
        assertTrue(reader.supports(Source.ofFile("DOCUMENT.TXT")));
        assertTrue(reader.supports(Source.ofFile("notes.text")));
        assertFalse(reader.supports(Source.ofFile("document.pdf")));
        assertFalse(reader.supports(Source.ofFile("document.md")));
    }

    @Test
    void testSupportsWithContentType() {
        TextReader reader = new TextReader();

        // 通过 content type 支持
        Source txtSource = Source.ofBytes(new byte[]{1, 2, 3}, "text/plain");
        assertTrue(reader.supports(txtSource));
    }

    @Test
    void testReadTextFile() throws IOException {
        Path txtFile = tempDir.resolve("test.txt");
        String content = "Hello, World!\nThis is a test file.\nLine 3.";
        Files.writeString(txtFile, content);

        TextReader reader = new TextReader();
        List<Document> documents = reader.read(Source.ofFile(txtFile.toString()));

        assertEquals(1, documents.size());
        Document doc = documents.get(0);
        assertEquals(content, doc.content());
        assertEquals("test.txt", doc.getMetadata("fileName"));
        assertEquals("text/plain", doc.getMetadata("contentType"));
        assertEquals("text/plain", doc.getMetadata("contentType"));
        assertNotNull(doc.getMetadata("size"));
    }

    @Test
    void testReadNonExistentFile() {
        TextReader reader = new TextReader();
        Source source = Source.ofFile("/nonexistent/file.txt");

        assertThrows(RuntimeException.class, () -> reader.read(source));
    }

    @Test
    void testReadEmptyFile() throws IOException {
        Path txtFile = tempDir.resolve("empty.txt");
        Files.writeString(txtFile, "");

        TextReader reader = new TextReader();

        // 空文件会抛出异常，因为 Document 不允许空内容
        assertThrows(RuntimeException.class, () -> reader.read(Source.ofFile(txtFile.toString())));
    }

    @Test
    void testReadChineseText() throws IOException {
        Path txtFile = tempDir.resolve("chinese.txt");
        String content = "这是一段中文内容。\n第二行内容。";
        Files.writeString(txtFile, content);

        TextReader reader = new TextReader();
        List<Document> documents = reader.read(Source.ofFile(txtFile.toString()));

        assertEquals(1, documents.size());
        Document doc = documents.get(0);
        assertTrue(doc.content().contains("中文内容"));
    }
}