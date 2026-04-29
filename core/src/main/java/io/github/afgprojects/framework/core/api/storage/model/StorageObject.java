package io.github.afgprojects.framework.core.api.storage.model;

import java.time.Instant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 文件存储信息
 *
 * @param key         文件唯一标识（路径）
 * @param size        文件大小（字节）
 * @param contentType 内容类型（MIME type）
 * @param etag        实体标签（用于缓存校验）
 * @param lastModified 最后修改时间
 * @param metadata    自定义元数据
 */
public record StorageObject(
        @NonNull String key,
        long size,
        @Nullable String contentType,
        @Nullable String etag,
        @Nullable Instant lastModified,
        @Nullable StorageMetadata metadata
) {

    /**
     * 创建存储对象（简化构造）
     */
    public static StorageObject of(@NonNull String key, long size, @Nullable String contentType) {
        return new StorageObject(key, size, contentType, null, Instant.now(), null);
    }

    /**
     * 创建存储对象（带元数据）
     */
    public static StorageObject of(@NonNull String key, long size, @Nullable String contentType,
                                   @Nullable StorageMetadata metadata) {
        return new StorageObject(key, size, contentType, null, Instant.now(), metadata);
    }

    /**
     * 获取文件名（从 key 中提取）
     */
    @NonNull
    public String getFileName() {
        int lastSlash = key.lastIndexOf('/');
        return lastSlash >= 0 ? key.substring(lastSlash + 1) : key;
    }

    /**
     * 获取文件扩展名
     */
    @Nullable
    public String getExtension() {
        String fileName = getFileName();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : null;
    }
}
