package io.github.afgprojects.framework.ai.core.etl.transformer;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 文档转换器接口。
 *
 * <p>对文档列表进行转换处理。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface DocumentTransformer {

    /**
     * 转换文档列表。
     *
     * @param documents 输入文档列表
     * @return 转换后的文档列表
     */
    @NonNull
    List<Document> transform(@NonNull List<Document> documents);

    /**
     * 获取转换器名称。
     *
     * @return 转换器名称
     */
    @NonNull
    String getName();

    /**
     * 获取转换器排序值。
     *
     * @return 排序值（越小越先执行）
     */
    int getOrder();
}