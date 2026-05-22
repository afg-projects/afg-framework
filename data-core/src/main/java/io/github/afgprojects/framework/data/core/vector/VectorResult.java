package io.github.afgprojects.framework.data.core.vector;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 向量搜索结果
 *
 * @param id         文档ID
 * @param content    文档内容
 * @param embedding  向量嵌入（可能为null，取决于查询）
 * @param metadata   元数据
 * @param similarity 相似度分数
 * @param tenantId   租户ID
 * @param userId     用户ID
 * @author afg-projects
 * @since 1.0.0
 */
public record VectorResult(
    @NonNull String id,
    @NonNull String content,
    @Nullable List<Double> embedding,
    @NonNull Map<String, Object> metadata,
    double similarity,
    @Nullable String tenantId,
    @Nullable String userId
) {

    /**
     * 创建结果
     */
    public VectorResult {
        if (metadata == null) {
            metadata = Map.of();
        }
    }

    /**
     * 获取元数据值
     */
    @Nullable
    public Object getMetadata(@NonNull String key) {
        return metadata.get(key);
    }

    /**
     * 获取元数据值（带默认值）
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public <T> T getMetadata(@NonNull String key, @NonNull T defaultValue) {
        Object value = metadata.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 是否高相似度
     */
    public boolean isHighSimilarity(double threshold) {
        return similarity >= threshold;
    }
}
