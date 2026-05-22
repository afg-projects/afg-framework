package io.github.afgprojects.framework.data.core.vector;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 向量文档
 *
 * @param id        文档ID
 * @param content   文档内容
 * @param embedding 向量嵌入
 * @param metadata  元数据
 * @param tenantId  租户ID
 * @param userId    用户ID
 * @author afg-projects
 * @since 1.0.0
 */
public record VectorDocument(
    @NonNull String id,
    @NonNull String content,
    @NonNull List<Double> embedding,
    @NonNull Map<String, Object> metadata,
    @Nullable String tenantId,
    @Nullable String userId
) {

    /**
     * 创建文档（自动生成ID）
     */
    public VectorDocument(@NonNull String content, @NonNull List<Double> embedding) {
        this(UUID.randomUUID().toString(), content, embedding, new HashMap<>(), null, null);
    }

    /**
     * 创建文档（带元数据，无嵌入向量）
     */
    public static VectorDocument withoutEmbedding(@NonNull String id, @NonNull String content,
                                                   @NonNull Map<String, Object> metadata) {
        return new VectorDocument(id, content, List.of(), metadata, null, null);
    }

    /**
     * 创建文档（带元数据）
     */
    public VectorDocument(@NonNull String content, @NonNull List<Double> embedding,
                          @NonNull Map<String, Object> metadata) {
        this(UUID.randomUUID().toString(), content, embedding, metadata, null, null);
    }

    /**
     * 创建文档（带租户和用户）
     */
    public VectorDocument(@NonNull String content, @NonNull List<Double> embedding,
                          @Nullable String tenantId, @Nullable String userId) {
        this(UUID.randomUUID().toString(), content, embedding, new HashMap<>(), tenantId, userId);
    }

    /**
     * 创建文档（完整参数）
     */
    public VectorDocument {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
    }

    /**
     * 创建带新ID的文档
     */
    @NonNull
    public VectorDocument withId(@NonNull String newId) {
        return new VectorDocument(newId, content, embedding, metadata, tenantId, userId);
    }

    /**
     * 创建带新内容的文档
     */
    @NonNull
    public VectorDocument withContent(@NonNull String newContent) {
        return new VectorDocument(id, newContent, embedding, metadata, tenantId, userId);
    }

    /**
     * 创建带新向量的文档
     */
    @NonNull
    public VectorDocument withEmbedding(@NonNull List<Double> newEmbedding) {
        return new VectorDocument(id, content, newEmbedding, metadata, tenantId, userId);
    }

    /**
     * 创建带新元数据的文档
     */
    @NonNull
    public VectorDocument withMetadata(@NonNull Map<String, Object> newMetadata) {
        return new VectorDocument(id, content, embedding, newMetadata, tenantId, userId);
    }

    /**
     * 添加元数据
     */
    @NonNull
    public VectorDocument withMetadata(@NonNull String key, @Nullable Object value) {
        Map<String, Object> newMetadata = new HashMap<>(metadata);
        newMetadata.put(key, value);
        return new VectorDocument(id, content, embedding, newMetadata, tenantId, userId);
    }

    /**
     * 创建带租户的文档
     */
    @NonNull
    public VectorDocument withTenant(@Nullable String newTenantId) {
        return new VectorDocument(id, content, embedding, metadata, newTenantId, userId);
    }

    /**
     * 创建带用户的文档
     */
    @NonNull
    public VectorDocument withUser(@Nullable String newUserId) {
        return new VectorDocument(id, content, embedding, metadata, tenantId, newUserId);
    }
}
