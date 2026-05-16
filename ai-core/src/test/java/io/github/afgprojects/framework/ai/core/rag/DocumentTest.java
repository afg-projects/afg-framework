package io.github.afgprojects.framework.ai.core.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Document 单元测试
 */
class DocumentTest {

    @Test
    @DisplayName("创建文档（仅内容）")
    void create_withContentOnly() {
        Document doc = new Document("Test content");

        assertThat(doc.content()).isEqualTo("Test content");
        assertThat(doc.id()).isNotBlank();
        assertThat(doc.embedding()).isNull();
        assertThat(doc.metadata()).isEmpty();
    }

    @Test
    @DisplayName("创建文档（内容和元数据）")
    void create_withContentAndMetadata() {
        Map<String, Object> metadata = Map.of("source", "web");
        Document doc = new Document("Test content", metadata);

        assertThat(doc.content()).isEqualTo("Test content");
        assertThat(doc.id()).isNotBlank();
        assertThat(doc.metadata()).containsEntry("source", "web");
    }

    @Test
    @DisplayName("创建文档（完整参数）")
    void create_withAllParams() {
        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        Map<String, Object> metadata = Map.of("key", "value");
        Document doc = new Document("doc-1", "Content", embedding, metadata);

        assertThat(doc.id()).isEqualTo("doc-1");
        assertThat(doc.content()).isEqualTo("Content");
        assertThat(doc.embedding()).isEqualTo(embedding);
        assertThat(doc.metadata()).containsEntry("key", "value");
    }

    @Test
    @DisplayName("使用静态方法创建")
    void staticFactory_success() {
        Document doc1 = Document.of("Content");
        assertThat(doc1.content()).isEqualTo("Content");

        Document doc2 = Document.of("Content", Map.of("key", "value"));
        assertThat(doc2.metadata()).containsEntry("key", "value");

        Document doc3 = Document.of("id", "Content", List.of(0.1), Map.of());
        assertThat(doc3.id()).isEqualTo("id");
    }

    @Test
    @DisplayName("空白 ID 抛出异常")
    void blankId_throwsException() {
        assertThatThrownBy(() -> new Document("", "Content", null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id cannot be null");
    }

    @Test
    @DisplayName("空白内容抛出异常")
    void blankContent_throwsException() {
        assertThatThrownBy(() -> new Document("id", "  ", null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("content cannot be null");
    }

    @Test
    @DisplayName("空元数据默认为空 Map")
    void nullMetadata_defaultsToEmptyMap() {
        Document doc = new Document("id", "Content", null, null);

        assertThat(doc.metadata()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("检查是否有嵌入")
    void hasEmbedding_returnsCorrectResult() {
        Document docWithEmbedding = new Document("id", "Content", List.of(0.1, 0.2), Map.of());
        assertThat(docWithEmbedding.hasEmbedding()).isTrue();

        Document docWithoutEmbedding = new Document("id", "Content", null, Map.of());
        assertThat(docWithoutEmbedding.hasEmbedding()).isFalse();

        Document docWithEmptyEmbedding = new Document("id", "Content", List.of(), Map.of());
        assertThat(docWithEmptyEmbedding.hasEmbedding()).isFalse();
    }

    @Test
    @DisplayName("获取元数据值")
    void getMetadata_success() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", "value");
        metadata.put("number", 42);
        Document doc = new Document("id", "Content", null, metadata);

        assertThat(doc.getMetadata("key")).isEqualTo("value");
        assertThat(doc.getMetadata("nonexistent")).isNull();
        assertThat(doc.getMetadata("number", 0)).isEqualTo(42);
        assertThat(doc.getMetadata("nonexistent", "default")).isEqualTo("default");
    }

    @Test
    @DisplayName("创建带新 ID 的文档")
    void withId_success() {
        Document doc = new Document("id1", "Content", null, Map.of());
        Document newDoc = doc.withId("id2");

        assertThat(newDoc.id()).isEqualTo("id2");
        assertThat(newDoc.content()).isEqualTo("Content");
    }

    @Test
    @DisplayName("创建带新内容的文档")
    void withContent_success() {
        Document doc = new Document("id", "Content1", null, Map.of());
        Document newDoc = doc.withContent("Content2");

        assertThat(newDoc.content()).isEqualTo("Content2");
        assertThat(newDoc.id()).isEqualTo("id");
    }

    @Test
    @DisplayName("创建带嵌入的文档")
    void withEmbedding_success() {
        Document doc = new Document("id", "Content", null, Map.of());
        List<Double> embedding = List.of(0.1, 0.2);
        Document newDoc = doc.withEmbedding(embedding);

        assertThat(newDoc.embedding()).isEqualTo(embedding);
    }

    @Test
    @DisplayName("创建带额外元数据的文档")
    void withMetadata_success() {
        Document doc = new Document("id", "Content", null, Map.of("key1", "value1"));
        Document newDoc = doc.withMetadata("key2", "value2");

        assertThat(newDoc.metadata()).containsEntry("key1", "value1");
        assertThat(newDoc.metadata()).containsEntry("key2", "value2");
    }
}