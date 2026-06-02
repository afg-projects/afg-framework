package io.github.afgprojects.framework.ai.etl.transformer;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DeduplicationTransformer 测试。
 */
class DeduplicationTransformerTest {

    @Test
    void testDeduplicateByContent() {
        Document doc1 = Document.of("Same content");
        Document doc2 = Document.of("Same content"); // 重复
        Document doc3 = Document.of("Different content");

        DeduplicationTransformer transformer = new DeduplicationTransformer(
            DeduplicationStrategy.contentBased()
        );

        List<Document> result = transformer.transform(List.of(doc1, doc2, doc3));

        assertEquals(2, result.size());
    }

    @Test
    void testDeduplicateById() {
        Document doc1 = Document.of("Content 1").withId("doc-1");
        Document doc2 = Document.of("Content 2").withId("doc-1"); // 相同 ID
        Document doc3 = Document.of("Content 3").withId("doc-2");

        DeduplicationTransformer transformer = new DeduplicationTransformer(
            DeduplicationStrategy.idBased()
        );

        List<Document> result = transformer.transform(List.of(doc1, doc2, doc3));

        assertEquals(2, result.size());
    }

    @Test
    void testNoDuplicates() {
        Document doc1 = Document.of("Content 1");
        Document doc2 = Document.of("Content 2");
        Document doc3 = Document.of("Content 3");

        DeduplicationTransformer transformer = new DeduplicationTransformer();

        List<Document> result = transformer.transform(List.of(doc1, doc2, doc3));

        assertEquals(3, result.size());
    }

    @Test
    void testAllDuplicates() {
        Document doc1 = Document.of("Same content");
        Document doc2 = Document.of("Same content");
        Document doc3 = Document.of("Same content");

        DeduplicationTransformer transformer = new DeduplicationTransformer();

        List<Document> result = transformer.transform(List.of(doc1, doc2, doc3));

        assertEquals(1, result.size());
    }

    @Test
    void testEmptyList() {
        DeduplicationTransformer transformer = new DeduplicationTransformer();

        List<Document> result = transformer.transform(List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void testPreservesMetadata() {
        Document doc1 = Document.of("Same content")
            .withMetadata("source", "file1.txt");
        Document doc2 = Document.of("Same content")
            .withMetadata("source", "file2.txt");

        DeduplicationTransformer transformer = new DeduplicationTransformer();

        List<Document> result = transformer.transform(List.of(doc1, doc2));

        assertEquals(1, result.size());
        // 保留第一个文档的元数据
        assertEquals("file1.txt", result.get(0).getMetadata("source"));
    }

    @Test
    void testGetOrder() {
        DeduplicationTransformer transformer = new DeduplicationTransformer();
        assertEquals(20, transformer.getOrder());
    }

    @Test
    void testGetName() {
        DeduplicationTransformer transformer = new DeduplicationTransformer();
        assertEquals("DeduplicationTransformer", transformer.getName());
    }
}