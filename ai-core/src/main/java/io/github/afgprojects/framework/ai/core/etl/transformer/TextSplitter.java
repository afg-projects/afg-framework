package io.github.afgprojects.framework.ai.core.etl.transformer;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分割器接口。
 *
 * <p>定义文本分割的核心方法，支持将长文本分割为多个块。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface TextSplitter {

    /**
     * 分割文本。
     *
     * @param text 待分割的文本
     * @return 分割后的文本块列表
     */
    @NonNull
    List<String> split(@NonNull String text);

    /**
     * 分割文档。
     *
     * <p>将文档内容分割为多个块，每个块保持相同的元数据。
     *
     * @param document 待分割的文档
     * @return 分割后的文档列表
     */
    default @NonNull List<Document> splitDocument(@NonNull Document document) {
        List<String> chunks = split(document.content());
        List<Document> result = new ArrayList<>(chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            Document chunkDoc = new Document(
                document.id() + "-chunk-" + i,
                chunk,
                null,
                document.metadata()
            );
            result.add(chunkDoc);
        }

        return result;
    }

    /**
     * 获取目标块大小。
     *
     * @return 块大小（字符数）
     */
    int getChunkSize();

    /**
     * 获取块重叠大小。
     *
     * @return 重叠大小（字符数）
     */
    int getChunkOverlap();
}
