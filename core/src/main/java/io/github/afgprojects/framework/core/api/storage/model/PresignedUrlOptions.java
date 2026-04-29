package io.github.afgprojects.framework.core.api.storage.model;

import java.time.Instant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 预签名 URL 选项
 *
 * @param expiration  过期时间
 * @param method      HTTP 方法（GET、PUT 等）
 * @param contentType 内容类型（用于 PUT 请求）
 */
public record PresignedUrlOptions(
        @NonNull Instant expiration,
        @NonNull String method,
        @Nullable String contentType
) {

    /**
     * 默认过期时间：1 小时
     */
    private static final Instant DEFAULT_EXPIRATION = Instant.now().plusSeconds(3600);

    /**
     * 创建 GET 请求的预签名 URL 选项
     */
    @NonNull
    public static PresignedUrlOptions forGet() {
        return new PresignedUrlOptions(DEFAULT_EXPIRATION, "GET", null);
    }

    /**
     * 创建 GET 请求的预签名 URL 选项（自定义过期时间）
     */
    @NonNull
    public static PresignedUrlOptions forGet(@NonNull Instant expiration) {
        return new PresignedUrlOptions(expiration, "GET", null);
    }

    /**
     * 创建 PUT 请求的预签名 URL 选项
     */
    @NonNull
    public static PresignedUrlOptions forPut(@Nullable String contentType) {
        return new PresignedUrlOptions(DEFAULT_EXPIRATION, "PUT", contentType);
    }

    /**
     * 创建 PUT 请求的预签名 URL 选项（自定义过期时间）
     */
    @NonNull
    public static PresignedUrlOptions forPut(@NonNull Instant expiration, @Nullable String contentType) {
        return new PresignedUrlOptions(expiration, "PUT", contentType);
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
        private Instant expiration = DEFAULT_EXPIRATION;
        private String method = "GET";
        private String contentType;

        public Builder expiration(@NonNull Instant expiration) {
            this.expiration = expiration;
            return this;
        }

        public Builder expirationSeconds(long seconds) {
            this.expiration = Instant.now().plusSeconds(seconds);
            return this;
        }

        public Builder method(@NonNull String method) {
            this.method = method;
            return this;
        }

        public Builder contentType(@Nullable String contentType) {
            this.contentType = contentType;
            return this;
        }

        @NonNull
        public PresignedUrlOptions build() {
            return new PresignedUrlOptions(expiration, method, contentType);
        }
    }
}
