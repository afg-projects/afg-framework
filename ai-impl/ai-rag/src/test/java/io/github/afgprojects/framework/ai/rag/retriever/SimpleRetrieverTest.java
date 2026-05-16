package io.github.afgprojects.framework.ai.rag.retriever;

import io.github.afgprojects.framework.ai.core.rag.Document;
import io.github.afgprojects.framework.ai.core.rag.EmbeddingModel;
import io.github.afgprojects.framework.ai.core.rag.VectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SimpleRetriever 单元测试
 */
class SimpleRetrieverTest {

    private VectorStore vectorStore;
    private EmbeddingModel embeddingModel;
    private SimpleRetriever retriever;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        embeddingModel = mock(EmbeddingModel.class);
        retriever = new SimpleRetriever(vectorStore, embeddingModel, 5);
    }

    @Test
    @DisplayName("创建检索器（默认 topK）")
    void create_withDefaultTopK() {
        SimpleRetriever defaultRetriever = new SimpleRetriever(vectorStore, embeddingModel);

        assertThat(defaultRetriever.getVectorStore()).isEqualTo(vectorStore);
        assertThat(defaultRetriever.getEmbeddingModel()).isEqualTo(embeddingModel);
    }

    @Test
    @DisplayName("创建检索器（自定义 topK）")
    void create_withCustomTopK() {
        SimpleRetriever customRetriever = new SimpleRetriever(vectorStore, embeddingModel, 10);

        assertThat(customRetriever.getVectorStore()).isEqualTo(vectorStore);
        assertThat(customRetriever.getEmbeddingModel()).isEqualTo(embeddingModel);
    }

    @Test
    @DisplayName("检索文档成功")
    void retrieve_success() {
        // 准备测试数据
        List<Double> queryEmbedding = List.of(0.1, 0.2, 0.3);
        List<Document> expectedDocs = List.of(
                new Document("doc-1", "Content 1", null, java.util.Map.of()),
                new Document("doc-2", "Content 2", null, java.util.Map.of())
        );

        // 配置 mock
        when(embeddingModel.embed(anyString())).thenReturn(queryEmbedding);
        when(vectorStore.similaritySearchByEmbedding(queryEmbedding, 5)).thenReturn(expectedDocs);

        // 执行检索
        List<Document> results = retriever.retrieve("test query");

        assertThat(results).isEqualTo(expectedDocs);
    }

    @Test
    @DisplayName("检索返回空结果")
    void retrieve_emptyResults() {
        List<Double> queryEmbedding = List.of(0.1, 0.2, 0.3);

        when(embeddingModel.embed(anyString())).thenReturn(queryEmbedding);
        when(vectorStore.similaritySearchByEmbedding(queryEmbedding, 5)).thenReturn(List.of());

        List<Document> results = retriever.retrieve("test query");

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("获取向量存储")
    void getVectorStore_returnsCorrectStore() {
        assertThat(retriever.getVectorStore()).isEqualTo(vectorStore);
    }

    @Test
    @DisplayName("获取嵌入模型")
    void getEmbeddingModel_returnsCorrectModel() {
        assertThat(retriever.getEmbeddingModel()).isEqualTo(embeddingModel);
    }
}