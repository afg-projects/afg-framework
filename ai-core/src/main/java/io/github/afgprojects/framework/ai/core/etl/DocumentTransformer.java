package io.github.afgprojects.framework.ai.core.etl;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 文档转换器接口。
 *
 * <p>负责对文档进行转换处理，如分割、清洗、摘要等。
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
     * 获取转换器名称（用于日志和监控）。
     *
     * @return 转换器名称
     */
    default @NonNull String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取执行顺序（数字越小越先执行）。
     *
     * @return 执行顺序
     */
    default int getOrder() {
        return 0;
    }
}