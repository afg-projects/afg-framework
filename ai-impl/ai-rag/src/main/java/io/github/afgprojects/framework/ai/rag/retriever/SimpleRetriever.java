package io.github.afgprojects.framework.ai.rag.retriever;

import io.github.afgprojects.framework.ai.core.rag.Document;
import io.github.afgprojects.framework.ai.core.rag.EmbeddingModel;
import io.github.afgprojects.framework.ai.core.rag.Retriever;
import io.github.afgprojects.framework.ai.core.rag.VectorStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 简单检索器实现
 *
 * <p>基于向量相似度的文档检索器。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class SimpleRetriever implements Retriever {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final int topK;

    /**
     * 创建检索器
     *
     * @param vectorStore    向量存储
     * @param embeddingModel 嵌入模型
     */
    public SimpleRetriever(@NonNull VectorStore vectorStore, @NonNull EmbeddingModel embeddingModel) {
        this(vectorStore, embeddingModel, 5);
    }

    /**
     * 创建检索器
     *
     * @param vectorStore    向量存储
     * @param embeddingModel 嵌入模型
     * @param topK           返回文档数量
     */
    public SimpleRetriever(@NonNull VectorStore vectorStore, @NonNull EmbeddingModel embeddingModel, int topK) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.topK = topK;
    }

    @Override
    public @NonNull List<Document> retrieve(@NonNull String query) {
        // 生成查询嵌入
        List<Double> queryEmbedding = embeddingModel.embed(query);

        // 搜索向量存储
        return vectorStore.similaritySearchByEmbedding(queryEmbedding, topK);
    }

    @Override
    public @NonNull VectorStore getVectorStore() {
        return vectorStore;
    }

    /**
     * 获取嵌入模型
     */
    @Override
    public @NonNull EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }
}