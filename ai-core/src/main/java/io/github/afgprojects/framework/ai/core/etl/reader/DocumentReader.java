package io.github.afgprojects.framework.ai.core.etl.reader;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import io.github.afgprojects.framework.ai.core.api.etl.Source;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 文档读取器接口。
 *
 * <p>从数据源读取文档内容。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface DocumentReader {

    /**
     * 从数据源读取文档。
     *
     * @param source 数据源
     * @return 读取的文档列表
     */
    @NonNull
    List<Document> read(@NonNull Source source);

    /**
     * 判断是否支持指定数据源。
     *
     * @param source 数据源
     * @return 是否支持
     */
    boolean supports(@NonNull Source source);
}