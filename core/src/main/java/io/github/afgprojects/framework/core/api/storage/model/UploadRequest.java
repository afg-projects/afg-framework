package io.github.afgprojects.framework.core.api.storage.model;

import java.io.InputStream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 文件上传请求
 *
 * @param key         文件唯一标识（路径），如 "images/2024/avatar.jpg"
 * @param inputStream 文件输入流
 * @param size        文件大小（字节），-1 表示未知
 * @param contentType 内容类型（MIME type），如 "image/jpeg"
 * @param metadata    自定义元数据
 */
public record UploadRequest(
        @NonNull String key,
        @NonNull InputStream inputStream,
        long size,
        @Nullable String contentType,
        @Nullable StorageMetadata metadata
) {

    /**
     * 创建上传请求（简化构造）
     */
    @NonNull
    public static UploadRequest of(@NonNull String key, @NonNull InputStream inputStream) {
        return new UploadRequest(key, inputStream, -1, null, null);
    }

    /**
     * 创建上传请求（带大小和类型）
     */
    @NonNull
    public static UploadRequest of(@NonNull String key, @NonNull InputStream inputStream,
                                   long size, @Nullable String contentType) {
        return new UploadRequest(key, inputStream, size, contentType, null);
    }

    /**
     * 创建 Builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder 类
     */
    public static class Builder {
        private String key;
        private InputStream inputStream;
        private long size = -1;
        private String contentType;
        private StorageMetadata metadata;

        public Builder key(@NonNull String key) {
            this.key = key;
            return this;
        }

        public Builder inputStream(@NonNull InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Builder contentType(@Nullable String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder metadata(@Nullable StorageMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        @NonNull
        public UploadRequest build() {
            if (key == null || inputStream == null) {
                throw new IllegalStateException("key and inputStream are required");
            }
            return new UploadRequest(key, inputStream, size, contentType, metadata);
        }
    }
}
