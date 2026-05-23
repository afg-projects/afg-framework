package io.github.afgprojects.framework.ai.etl.transformer;

import io.github.afgprojects.framework.ai.core.etl.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SplitterTransformer 测试。
 */
class SplitterTransformerTest {

    @Test
    void testSplitDocument() {
        String longText = "This is a long text that needs to be split into multiple chunks. ".repeat(10);
        Document doc = Document.of(longText);

        TextSplitter splitter = new RecursiveCharacterTextSplitter(100, 20);
        SplitterTransformer transformer = new SplitterTransformer(splitter);

        List<Document> result = transformer.transform(List.of(doc));

        assertTrue(result.size() > 1);
        for (Document chunk : result) {
            assertTrue(chunk.content().length() <= 120, "Chunk length should be within limit");
            assertTrue(chunk.id().contains("-chunk-"), "Chunk ID should contain '-chunk-'");
        }
    }

    @Test
    void testPreservesMetadata() {
        Document doc = Document.of("Test content")
            .withMetadata("source", "test.txt")
            .withMetadata("author", "test");

        TextSplitter splitter = new RecursiveCharacterTextSplitter(5, 0);
        SplitterTransformer transformer = new SplitterTransformer(splitter);

        List<Document> result = transformer.transform(List.of(doc));

        assertTrue(result.size() > 0);
        for (Document chunk : result) {
            assertEquals("test.txt", chunk.getMetadata("source"));
            assertEquals("test", chunk.getMetadata("author"));
        }
    }

    @Test
    void testEmptyDocument() {
        Document doc = Document.of("Short text");

        TextSplitter splitter = new RecursiveCharacterTextSplitter(100, 20);
        SplitterTransformer transformer = new SplitterTransformer(splitter);

        List<Document> result = transformer.transform(List.of(doc));

        assertEquals(1, result.size());
        assertEquals("Short text", result.get(0).content());
    }

    @Test
    void testMultipleDocuments() {
        Document doc1 = Document.of("First document content that is quite long and needs splitting. ".repeat(5));
        Document doc2 = Document.of("Second document content that is also quite long and needs splitting. ".repeat(5));

        TextSplitter splitter = new RecursiveCharacterTextSplitter(100, 20);
        SplitterTransformer transformer = new SplitterTransformer(splitter);

        List<Document> result = transformer.transform(List.of(doc1, doc2));

        assertTrue(result.size() > 2);
    }

    @Test
    void testChineseTextSplit() {
        String chineseText = "这是一个中文文本。它需要被分割成多个块。每个块应该保持语义完整性。" +
            "递归字符分割器会优先按中文句号分割。" +
            "然后再按中文逗号分割。" +
            "最后按字符分割。";

        Document doc = Document.of(chineseText);

        TextSplitter splitter = new RecursiveCharacterTextSplitter(50, 10);
        SplitterTransformer transformer = new SplitterTransformer(splitter);

        List<Document> result = transformer.transform(List.of(doc));

        assertTrue(result.size() > 1);
        for (Document chunk : result) {
            assertTrue(chunk.content().length() <= 60, "Chinese chunk length should be within limit");
        }
    }

    @Test
    void testGetOrder() {
        TextSplitter splitter = new RecursiveCharacterTextSplitter(100, 20);
        SplitterTransformer transformer = new SplitterTransformer(splitter);

        assertEquals(100, transformer.getOrder());
    }

    @Test
    void testGetName() {
        TextSplitter splitter = new RecursiveCharacterTextSplitter(100, 20);
        SplitterTransformer transformer = new SplitterTransformer(splitter);

        assertEquals("SplitterTransformer", transformer.getName());
    }
}