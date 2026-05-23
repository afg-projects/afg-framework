package io.github.afgprojects.framework.ai.etl.transformer;

import io.github.afgprojects.framework.ai.core.etl.Document;
import org.jspecify.annotations.NonNull;

import java.util.Map;

/**
 * 元数据提取器接口。
 *
 * <p>从文档中提取元数据信息。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface MetadataExtractor {

    /**
     * 从文档中提取元数据。
     *
     * @param document 文档
     * @return 提取的元数据映射
     */
    @NonNull
    Map<String, Object> extract(@NonNull Document document);

    /**
     * 获取提取器名称。
     *
     * @return 提取器名称
     */
    default @NonNull String getName() {
        return this.getClass().getSimpleName();
    }
}
