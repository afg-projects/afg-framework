package io.github.afgprojects.framework.ai.langchain4j.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.github.afgprojects.framework.ai.core.api.rag.Document;
import io.github.afgprojects.framework.ai.core.api.rag.KnowledgeBaseService;

import java.util.List;
import java.util.Objects;

/**
 * 将 AFG KnowledgeBaseService 适配为 LangChain4j ContentRetriever
 *
 * <p>LangChain4j 的 {@link ContentRetriever} 接口通过此适配器桥接到
 * AFG 的 {@link KnowledgeBaseService}，使得 LangChain4j 的 RAG 流程
 * 可以使用 AFG 的知识库服务。
 *
 * <p>此适配器支持两种模式：
 * <ul>
 *   <li>基于 KnowledgeBaseService：使用知识库 ID 进行搜索</li>
 *   <li>基于 EmbeddingStore + EmbeddingModel：直接使用向量搜索</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class Lc4jContentRetrieverAdapter implements ContentRetriever {

    private final KnowledgeBaseService knowledgeBaseService;
    private final String knowledgeBaseId;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final int maxResults;
    private final double minScore;

    /**
     * 基于知识库服务的适配器
     *
     * @param knowledgeBaseService AFG 知识库服务
     * @param knowledgeBaseId      知识库 ID
     * @param maxResults           最大返回结果数
     * @param minScore             最低相似度分数
     */
    public Lc4jContentRetrieverAdapter(KnowledgeBaseService knowledgeBaseService,
                                       String knowledgeBaseId,
                                       int maxResults,
                                       double minScore) {
        this.knowledgeBaseService = Objects.requireNonNull(knowledgeBaseService, "knowledgeBaseService must not be null");
        this.knowledgeBaseId = Objects.requireNonNull(knowledgeBaseId, "knowledgeBaseId must not be null");
        this.embeddingStore = null;
        this.embeddingModel = null;
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    /**
     * 基于向量存储的适配器
     *
     * @param embeddingStore  LangChain4j EmbeddingStore
     * @param embeddingModel  LangChain4j EmbeddingModel
     * @param maxResults      最大返回结果数
     * @param minScore        最低相似度分数
     */
    public Lc4jContentRetrieverAdapter(EmbeddingStore<TextSegment> embeddingStore,
                                       EmbeddingModel embeddingModel,
                                       int maxResults,
                                       double minScore) {
        this.knowledgeBaseService = null;
        this.knowledgeBaseId = null;
        this.embeddingStore = Objects.requireNonNull(embeddingStore, "embeddingStore must not be null");
        this.embeddingModel = Objects.requireNonNull(embeddingModel, "embeddingModel must not be null");
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    @Override
    public List<Content> retrieve(Query query) {
        String queryText = query.text();

        if (knowledgeBaseService != null) {
            return retrieveFromKnowledgeBase(queryText);
        }

        if (embeddingStore != null && embeddingModel != null) {
            return retrieveFromEmbeddingStore(queryText);
        }

        return List.of();
    }

    private List<Content> retrieveFromKnowledgeBase(String queryText) {
        List<Document> documents = knowledgeBaseService.search(
            knowledgeBaseId, queryText, maxResults, minScore);

        return documents.stream()
            .map(doc -> Content.from(doc.content()))
            .toList();
    }

    private List<Content> retrieveFromEmbeddingStore(String queryText) {
        Embedding queryEmbedding = embeddingModel.embed(queryText).content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(maxResults)
            .minScore(minScore)
            .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

        return searchResult.matches().stream()
            .map(match -> {
                TextSegment segment = match.embedded();
                return Content.from(segment.text());
            })
            .toList();
    }
}
