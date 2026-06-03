package io.github.afgprojects.framework.ai.core.properties.rag;

import lombok.Data;

/**
 * RAG 配置。
 */
@Data
public class RagConfig {

    /**
     * 是否启用 RAG。
     */
    private boolean enabled = true;

    /**
     * 向量维度。
     */
    private int embeddingDimensions = 1536;

    /**
     * 搜索模式：VECTOR、KEYWORD、BLEND。
     */
    private SearchMode searchMode = SearchMode.BLEND;

    /**
     * 相似度阈值（0.0 ~ 1.0）。
     */
    private double similarityThreshold = 0.7;

    /**
     * 返回结果数量。
     */
    private int topN = 5;
}
