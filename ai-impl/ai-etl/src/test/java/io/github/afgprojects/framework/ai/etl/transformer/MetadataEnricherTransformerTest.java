package io.github.afgprojects.framework.ai.etl.transformer;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import io.github.afgprojects.framework.ai.etl.extractor.ContentMetadataExtractor;
import io.github.afgprojects.framework.ai.etl.extractor.FileMetadataExtractor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MetadataEnricherTransformer 测试。
 */
class MetadataEnricherTransformerTest {

    @Test
    void testEnrichWithFileMetadata() {
        Document doc = Document.of("Test content")
            .withMetadata("source", "/path/to/test.txt");

        MetadataEnricherTransformer transformer = new MetadataEnricherTransformer()
            .addExtractor(new FileMetadataExtractor());

        List<Document> result = transformer.transform(List.of(doc));

        assertEquals(1, result.size());
        Document enriched = result.get(0);
        assertEquals("test.txt", enriched.getMetadata("fileName"));
        assertEquals("txt", enriched.getMetadata("fileExtension"));
        assertEquals("/path/to", enriched.getMetadata("directory"));
    }

    @Test
    void testEnrichWithContentMetadata() {
        String content = "Hello world\nThis is line 2\nThis is line 3";
        Document doc = Document.of(content);

        MetadataEnricherTransformer transformer = new MetadataEnricherTransformer()
            .addExtractor(new ContentMetadataExtractor());

        List<Document> result = transformer.transform(List.of(doc));

        assertEquals(1, result.size());
        Document enriched = result.get(0);
        assertEquals(content.length(), enriched.getMetadata("charCount"));
        assertEquals(3, enriched.getMetadata("lineCount"));
        assertNotNull(enriched.getMetadata("wordCount"));
        assertNotNull(enriched.getMetadata("contentHash"));
    }

    @Test
    void testEnrichWithMultipleExtractors() {
        Document doc = Document.of("Test content")
            .withMetadata("source", "/path/to/test.txt");

        MetadataEnricherTransformer transformer = new MetadataEnricherTransformer()
            .addExtractor(new FileMetadataExtractor())
            .addExtractor(new ContentMetadataExtractor());

        List<Document> result = transformer.transform(List.of(doc));

        assertEquals(1, result.size());
        Document enriched = result.get(0);
        // FileMetadataExtractor
        assertEquals("test.txt", enriched.getMetadata("fileName"));
        // ContentMetadataExtractor
        assertNotNull(enriched.getMetadata("charCount"));
        assertNotNull(enriched.getMetadata("lineCount"));
    }

    @Test
    void testNoExtractors() {
        Document doc = Document.of("Test content");

        MetadataEnricherTransformer transformer = new MetadataEnricherTransformer();

        List<Document> result = transformer.transform(List.of(doc));

        assertEquals(1, result.size());
        assertEquals(doc, result.get(0));
    }

    @Test
    void testPreservesExistingMetadata() {
        Document doc = Document.of("Test content")
            .withMetadata("custom", "value")
            .withMetadata("source", "/path/to/test.txt");

        MetadataEnricherTransformer transformer = new MetadataEnricherTransformer()
            .addExtractor(new FileMetadataExtractor());

        List<Document> result = transformer.transform(List.of(doc));

        assertEquals(1, result.size());
        Document enriched = result.get(0);
        assertEquals("value", enriched.getMetadata("custom"));
        assertEquals("test.txt", enriched.getMetadata("fileName"));
    }

    @Test
    void testGetOrder() {
        MetadataEnricherTransformer transformer = new MetadataEnricherTransformer();
        assertEquals(50, transformer.getOrder());
    }

    @Test
    void testGetName() {
        MetadataEnricherTransformer transformer = new MetadataEnricherTransformer();
        assertEquals("MetadataEnricherTransformer", transformer.getName());
    }
}