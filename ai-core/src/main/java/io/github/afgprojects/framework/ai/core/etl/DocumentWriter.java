package io.github.afgprojects.framework.ai.core.etl;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 文档写入器接口。
 *
 * <p>负责将文档写入目标存储，如向量数据库、关系数据库、文件等。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface DocumentWriter {

    /**
     * 写入文档。
     *
     * @param documents 要写入的文档列表
     */
    void write(@NonNull List<Document> documents);

    /**
     * 分批写入文档。
     *
     * @param documents 要写入的文档列表
     * @param batchSize 批次大小
     */
    default void write(@NonNull List<Document> documents, int batchSize) {
        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            write(documents.subList(i, end));
        }
    }

    /**
     * 清空已写入的数据（可选实现）。
     */
    default void clear() {
        // 默认不做任何操作
    }
}