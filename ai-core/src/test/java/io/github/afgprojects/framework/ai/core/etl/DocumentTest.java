package io.github.afgprojects.framework.ai.core.etl;

import io.github.afgprojects.framework.ai.core.rag.Document;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTest {

    @Test
    void testCreateWithContent() {
        Document doc = Document.of("Hello, World!");

        assertNotNull(doc.id());
        assertEquals("Hello, World!", doc.content());
        assertFalse(doc.hasEmbedding());
        assertTrue(doc.metadata().isEmpty());
    }

    @Test
    void testCreateWithContentAndMetadata() {
        Map<String, Object> metadata = Map.of("source", "test", "author", "John");
        Document doc = Document.of("Content", metadata);

        assertNotNull(doc.id());
        assertEquals("Content", doc.content());
        assertEquals("test", doc.getMetadata("source"));
        assertEquals("John", doc.getMetadata("author"));
    }

    @Test
    void testWithMethods() {
        Document doc = Document.of("Original");

        Document withId = doc.withId("custom-id");
        assertEquals("custom-id", withId.id());
        assertEquals("Original", withId.content());

        Document withContent = doc.withContent("New Content");
        assertEquals("New Content", withContent.content());

        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        Document withEmbedding = doc.withEmbedding(embedding);
        assertEquals(embedding, withEmbedding.embedding());
        assertTrue(withEmbedding.hasEmbedding());

        Document withMeta = doc.withMetadata("key", "value");
        assertEquals("value", withMeta.getMetadata("key"));
    }

    @Test
    void testGetMetadataWithDefault() {
        Document doc = Document.of("Content");

        assertEquals("default", doc.getMetadata("nonexistent", "default"));
    }

    @Test
    void testInvalidDocument() {
        assertThrows(IllegalArgumentException.class, () -> Document.of(""));
        assertThrows(IllegalArgumentException.class, () -> Document.of(null));
        assertThrows(IllegalArgumentException.class, () -> new Document("", "content", null, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new Document("id", "", null, Map.of()));
    }
}
