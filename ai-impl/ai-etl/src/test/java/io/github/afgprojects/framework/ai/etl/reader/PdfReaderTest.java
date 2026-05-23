package io.github.afgprojects.framework.ai.etl.reader;

import io.github.afgprojects.framework.ai.core.etl.Document;
import io.github.afgprojects.framework.ai.core.etl.Source;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PdfReader 测试类。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
class PdfReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void testSupports() {
        PdfReader reader = new PdfReader();

        assertTrue(reader.supports(Source.ofFile("document.pdf")));
        assertTrue(reader.supports(Source.ofFile("DOCUMENT.PDF")));
        assertFalse(reader.supports(Source.ofFile("document.txt")));
        assertFalse(reader.supports(Source.ofFile("document.md")));
    }

    @Test
    void testSupportsWithContentType() {
        PdfReader reader = new PdfReader();

        // 通过 content type 支持
        Source pdfSource = Source.ofBytes(new byte[]{1, 2, 3}, "application/pdf");
        assertTrue(reader.supports(pdfSource));
    }

    @Test
    void testReadNonExistentFile() {
        PdfReader reader = new PdfReader();
        Source source = Source.ofFile("/nonexistent/file.pdf");

        assertThrows(RuntimeException.class, () -> reader.read(source));
    }

    @Test
    void testDefaultOptions() {
        PdfReader.PdfReadOptions defaults = PdfReader.PdfReadOptions.defaults();

        assertFalse(defaults.splitByPage());
        assertEquals(1, defaults.startPage());
        assertEquals(Integer.MAX_VALUE, defaults.endPage());
        assertTrue(defaults.extractMetadata());
    }

    @Test
    void testByPageOptions() {
        PdfReader.PdfReadOptions byPage = PdfReader.PdfReadOptions.byPage();

        assertTrue(byPage.splitByPage());
        assertTrue(byPage.extractMetadata());
    }

    @Test
    void testPagesOptions() {
        PdfReader.PdfReadOptions pages = PdfReader.PdfReadOptions.pages(1, 10);

        assertTrue(pages.splitByPage());
        assertEquals(1, pages.startPage());
        assertEquals(10, pages.endPage());
        assertTrue(pages.extractMetadata());
    }
}