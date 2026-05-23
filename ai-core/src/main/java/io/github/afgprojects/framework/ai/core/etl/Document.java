package io.github.afgprojects.framework.ai.core.etl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ETL 和 RAG 系统中的核心文档数据结构。
 *
 * @param id        唯一标识符
 * @param content   文本内容
 * @param embedding 向量嵌入（可为 null）
 * @param metadata  元数据映射
 * @author AFG Projects
 * @since 1.0.0
 */
public record Document(
    @NonNull String id,
    @NonNull String content,
    @Nullable List<Double> embedding,
    @NonNull Map<String, Object> metadata
) {

    /**
     * 创建带自动生成 ID 和空元数据的文档。
     */
    public Document(@NonNull String content) {
        this(UUID.randomUUID().toString(), content, null, new HashMap<>());
    }

    /**
     * 创建带自动生成 ID 和指定元数据的文档。
     */
    public Document(@NonNull String content, @NonNull Map<String, Object> metadata) {
        this(UUID.randomUUID().toString(), content, null, metadata);
    }

    /**
     * 紧凑构造器，进行参数校验和防御性复制。
     */
    public Document {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id cannot be null or blank");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content cannot be null or blank");
        }
        // 防御性复制：确保 metadata 不可被外部修改
        metadata = new HashMap<>(metadata);
    }

    /**
     * 工厂方法：创建带内容的文档。
     */
    @NonNull
    public static Document of(@NonNull String content) {
        return new Document(content);
    }

    /**
     * 工厂方法：创建带内容和元数据的文档。
     */
    @NonNull
    public static Document of(@NonNull String content, @NonNull Map<String, Object> metadata) {
        return new Document(content, metadata);
    }

    /**
     * 工厂方法：创建完整参数的文档。
     */
    @NonNull
    public static Document of(
        @NonNull String id,
        @NonNull String content,
        @Nullable List<Double> embedding,
        @NonNull Map<String, Object> metadata
    ) {
        return new Document(id, content, embedding, metadata);
    }

    /**
     * 返回带新 ID 的文档副本。
     */
    @NonNull
    public Document withId(@NonNull String id) {
        return new Document(id, this.content, this.embedding, this.metadata);
    }

    /**
     * 返回带新内容的文档副本。
     */
    @NonNull
    public Document withContent(@NonNull String content) {
        return new Document(this.id, content, this.embedding, this.metadata);
    }

    /**
     * 返回带新嵌入的文档副本。
     */
    @NonNull
    public Document withEmbedding(@Nullable List<Double> embedding) {
        return new Document(this.id, this.content, embedding, this.metadata);
    }

    /**
     * 返回添加了新元数据的文档副本。
     */
    @NonNull
    public Document withMetadata(@NonNull String key, @Nullable Object value) {
        Map<String, Object> newMetadata = new HashMap<>(this.metadata);
        newMetadata.put(key, value);
        return new Document(this.id, this.content, this.embedding, newMetadata);
    }

    /**
     * 检查是否有嵌入。
     */
    public boolean hasEmbedding() {
        return embedding != null && !embedding.isEmpty();
    }

    /**
     * 获取元数据值。
     */
    @Nullable
    public Object getMetadata(@NonNull String key) {
        return metadata.get(key);
    }

    /**
     * 获取元数据值，带默认值。
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public <T> T getMetadata(@NonNull String key, @NonNull T defaultValue) {
        Object value = metadata.get(key);
        return value != null ? (T) value : defaultValue;
    }
}