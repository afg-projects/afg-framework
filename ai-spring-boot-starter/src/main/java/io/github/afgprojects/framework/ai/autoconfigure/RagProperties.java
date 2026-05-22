package io.github.afgprojects.framework.ai.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 配置属性
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.ai.rag")
public class RagProperties {

    private boolean enabled = true;
    private SplitterConfig splitter = new SplitterConfig();
    private EmbeddingConfig embedding = new EmbeddingConfig();
    private StoreConfig store = new StoreConfig();
    private RetrieverConfig retriever = new RetrieverConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public SplitterConfig getSplitter() {
        return splitter;
    }

    public void setSplitter(SplitterConfig splitter) {
        this.splitter = splitter;
    }

    public EmbeddingConfig getEmbedding() {
        return embedding;
    }

    public void setEmbedding(EmbeddingConfig embedding) {
        this.embedding = embedding;
    }

    public StoreConfig getStore() {
        return store;
    }

    public void setStore(StoreConfig store) {
        this.store = store;
    }

    public RetrieverConfig getRetriever() {
        return retriever;
    }

    public void setRetriever(RetrieverConfig retriever) {
        this.retriever = retriever;
    }

    /**
     * 文本切片器配置
     */
    public static class SplitterConfig {
        private SplitterType type = SplitterType.RECURSIVE;
        private int chunkSize = 500;
        private int chunkOverlap = 50;

        public SplitterType getType() {
            return type;
        }

        public void setType(SplitterType type) {
            this.type = type;
        }

        public int getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        public int getChunkOverlap() {
            return chunkOverlap;
        }

        public void setChunkOverlap(int chunkOverlap) {
            this.chunkOverlap = chunkOverlap;
        }
    }

    /**
     * 切片器类型
     */
    public enum SplitterType {
        RECURSIVE,
        MARKDOWN,
        TOKEN
    }

    /**
     * 嵌入模型配置
     */
    public static class EmbeddingConfig {
        private String provider = "ollama";
        private String baseUrl = "http://localhost:11434";
        private String model = "nomic-embed-text";
        private int dimension = 768;
        private int maxBatchSize = 100;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getDimension() {
            return dimension;
        }

        public void setDimension(int dimension) {
            this.dimension = dimension;
        }

        public int getMaxBatchSize() {
            return maxBatchSize;
        }

        public void setMaxBatchSize(int maxBatchSize) {
            this.maxBatchSize = maxBatchSize;
        }
    }

    /**
     * 向量存储配置
     */
    public static class StoreConfig {
        private String type = "simple";
        private String tableName = "ai_vectors";
        private int dimension = 768;
        private String indexType = "hnsw";

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public int getDimension() {
            return dimension;
        }

        public void setDimension(int dimension) {
            this.dimension = dimension;
        }

        public String getIndexType() {
            return indexType;
        }

        public void setIndexType(String indexType) {
            this.indexType = indexType;
        }
    }

    /**
     * 检索器配置
     */
    public static class RetrieverConfig {
        private int topK = 5;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }
    }
}
