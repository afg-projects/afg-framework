package io.github.afgprojects.framework.ai.etl.writer;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 文档写入器接口。
 *
 * <p>将文档写入目标存储。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface DocumentWriter {

    /**
     * 将文档写入目标存储。
     *
     * @param documents 文档列表
     */
    void write(@NonNull List<Document> documents);
}