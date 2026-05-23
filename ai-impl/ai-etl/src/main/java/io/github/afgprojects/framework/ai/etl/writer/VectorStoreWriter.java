package io.github.afgprojects.framework.ai.etl.writer;

import io.github.afgprojects.framework.ai.core.etl.Document;
import io.github.afgprojects.framework.ai.core.etl.DocumentWriter;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.List;

/**
 * 向量存储写入器。
 *
 * <p>将文档写入 Spring AI 的 VectorStore。VectorStore 会自动生成嵌入向量
 * （如果配置了 EmbeddingModel）。
 *
 * <p>依赖 Spring AI 的 VectorStore 接口。具体的 VectorStore 实现
 * （如 SimpleVectorStore、PgVectorStore 等）需要单独配置。
 *
 * <p><b>注意：</b>此类依赖 Spring AI，需要在项目中添加相关依赖。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class VectorStoreWriter implements DocumentWriter {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreWriter.class);

    private final VectorStore vectorStore;

    /**
     * 创建向量存储写入器。
     *
     * @param vectorStore 向量存储
     */
    public VectorStoreWriter(@NonNull VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void write(@NonNull List<Document> documents) {
        log.debug("Writing {} documents to vector store", documents.size());

        List<org.springframework.ai.document.Document> springAiDocs = new ArrayList<>(documents.size());

        for (Document doc : documents) {
            org.springframework.ai.document.Document springAiDoc = convertToSpringAiDocument(doc);
            springAiDocs.add(springAiDoc);
        }

        // 批量写入，VectorStore 会自动生成嵌入向量
        vectorStore.add(springAiDocs);

        log.debug("Wrote {} documents to vector store", documents.size());
    }

    @Override
    public void write(@NonNull List<Document> documents, int batchSize) {
        log.debug("Writing {} documents in batches of {}", documents.size(), batchSize);

        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            List<Document> batch = documents.subList(i, end);
            write(batch);
            log.trace("Written batch {}/{}", (i / batchSize) + 1, (documents.size() + batchSize - 1) / batchSize);
        }
    }

    /**
     * 将框架文档转换为 Spring AI 文档。
     *
     * <p>Spring AI Document 使用文本内容，嵌入向量由 VectorStore 自动生成。
     */
    private org.springframework.ai.document.Document convertToSpringAiDocument(@NonNull Document doc) {
        // 使用 Builder 创建文档
        return org.springframework.ai.document.Document.builder()
            .id(doc.id())
            .text(doc.content())
            .metadata(doc.metadata())
            .build();
    }

    /**
     * 获取向量存储。
     */
    @NonNull
    public VectorStore getVectorStore() {
        return vectorStore;
    }
}