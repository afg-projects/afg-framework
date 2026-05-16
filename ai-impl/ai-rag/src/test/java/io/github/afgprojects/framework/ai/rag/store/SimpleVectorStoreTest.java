package io.github.afgprojects.framework.ai.rag.store;

import io.github.afgprojects.framework.ai.core.rag.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SimpleVectorStore 单元测试
 */
class SimpleVectorStoreTest {

    private SimpleVectorStore store;

    @BeforeEach
    void setUp() {
        store = new SimpleVectorStore();
    }

    @Test
    @DisplayName("添加文档成功")
    void add_success() {
        Document doc = createTestDocument("doc-1", "Test content");

        store.add(List.of(doc));

        assertThat(store.count()).isEqualTo(1);
        assertThat(store.exists("doc-1")).isTrue();
    }

    @Test
    @DisplayName("添加多个文档")
    void add_multipleDocuments() {
        Document doc1 = createTestDocument("doc-1", "Content 1");
        Document doc2 = createTestDocument("doc-2", "Content 2");

        store.add(List.of(doc1, doc2));

        assertThat(store.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("添加带嵌入的文档")
    void add_withEmbedding() {
        List<Double> embedding = List.of(0.1, 0.2, 0.3, 0.4);
        Document doc = new Document("doc-1", "Content", embedding, Map.of());

        store.add(List.of(doc));

        assertThat(store.getById("doc-1"))
                .isNotNull()
                .satisfies(d -> assertThat(d.embedding()).isEqualTo(embedding));
    }

    @Test
    @DisplayName("删除文档成功")
    void delete_success() {
        Document doc = createTestDocument("doc-1", "Content");
        store.add(List.of(doc));

        store.delete(List.of("doc-1"));

        assertThat(store.count()).isZero();
        assertThat(store.exists("doc-1")).isFalse();
    }

    @Test
    @DisplayName("删除多个文档")
    void delete_multipleIds() {
        store.add(List.of(
                createTestDocument("doc-1", "Content 1"),
                createTestDocument("doc-2", "Content 2"),
                createTestDocument("doc-3", "Content 3")
        ));

        store.delete(List.of("doc-1", "doc-2"));

        assertThat(store.count()).isEqualTo(1);
        assertThat(store.exists("doc-3")).isTrue();
    }

    @Test
    @DisplayName("更新文档成功")
    void update_success() {
        Document doc = createTestDocument("doc-1", "Original content");
        store.add(List.of(doc));

        Document updated = new Document("doc-1", "Updated content", null, Map.of());
        store.update(updated);

        assertThat(store.getById("doc-1"))
                .isNotNull()
                .extracting(Document::content)
                .isEqualTo("Updated content");
    }

    @Test
    @DisplayName("更新文档嵌入")
    void update_embedding() {
        Document doc = createTestDocument("doc-1", "Content");
        store.add(List.of(doc));

        List<Double> newEmbedding = List.of(0.5, 0.6, 0.7);
        Document updated = new Document("doc-1", "Content", newEmbedding, Map.of());
        store.update(updated);

        assertThat(store.getById("doc-1").embedding()).isEqualTo(newEmbedding);
    }

    @Test
    @DisplayName("获取不存在的文档返回 null")
    void getById_notFound_returnsNull() {
        assertThat(store.getById("nonexistent")).isNull();
    }

    @Test
    @DisplayName("检查文档是否存在")
    void exists_returnsCorrectResult() {
        Document doc = createTestDocument("doc-1", "Content");
        store.add(List.of(doc));

        assertThat(store.exists("doc-1")).isTrue();
        assertThat(store.exists("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("相似度搜索返回正确数量")
    void similaritySearch_returnsCorrectCount() {
        store.add(List.of(
                createTestDocument("doc-1", "Content 1"),
                createTestDocument("doc-2", "Content 2"),
                createTestDocument("doc-3", "Content 3")
        ));

        List<Document> results = store.similaritySearch("query", 2);

        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("相似度搜索（空存储）")
    void similaritySearch_emptyStore_returnsEmpty() {
        List<Document> results = store.similaritySearch("query", 5);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("嵌入相似度搜索")
    void similaritySearchByEmbedding_returnsOrderedResults() {
        // 创建带嵌入的文档
        List<Double> embedding1 = List.of(1.0, 0.0, 0.0);
        List<Double> embedding2 = List.of(0.0, 1.0, 0.0);
        List<Double> embedding3 = List.of(0.9, 0.1, 0.0); // 与 embedding1 相似

        Document doc1 = new Document("doc-1", "Content 1", embedding1, Map.of());
        Document doc2 = new Document("doc-2", "Content 2", embedding2, Map.of());
        Document doc3 = new Document("doc-3", "Content 3", embedding3, Map.of());

        store.add(List.of(doc1, doc2, doc3));

        // 搜索与 embedding1 相似的文档
        List<Document> results = store.similaritySearchByEmbedding(embedding1, 3);

        assertThat(results).hasSize(3);
        // doc1 应排在最前（相似度最高）
        assertThat(results.get(0).id()).isEqualTo("doc-1");
    }

    @Test
    @DisplayName("嵌入相似度搜索（空存储）")
    void similaritySearchByEmbedding_emptyStore_returnsEmpty() {
        List<Double> queryEmbedding = List.of(0.1, 0.2, 0.3);

        List<Document> results = store.similaritySearchByEmbedding(queryEmbedding, 5);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("嵌入相似度搜索（无嵌入文档）")
    void similaritySearchByEmbedding_noEmbeddings_returnsEmpty() {
        // 添加无嵌入的文档
        store.add(List.of(createTestDocument("doc-1", "Content")));

        List<Double> queryEmbedding = List.of(0.1, 0.2, 0.3);

        List<Document> results = store.similaritySearchByEmbedding(queryEmbedding, 5);

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("计算文档数量")
    void count_returnsCorrectNumber() {
        assertThat(store.count()).isZero();

        store.add(List.of(createTestDocument("doc-1", "Content")));
        assertThat(store.count()).isEqualTo(1);

        store.add(List.of(createTestDocument("doc-2", "Content")));
        assertThat(store.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("清空存储")
    void clear_removesAllDocuments() {
        store.add(List.of(
                createTestDocument("doc-1", "Content 1"),
                createTestDocument("doc-2", "Content 2")
        ));

        store.clear();

        assertThat(store.count()).isZero();
        assertThat(store.exists("doc-1")).isFalse();
        assertThat(store.exists("doc-2")).isFalse();
    }

    private Document createTestDocument(String id, String content) {
        return new Document(id, content, null, Map.of());
    }
}