package io.github.afgprojects.framework.ai.core.api.chat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * AI 多媒体内容 - 与具体 AI 框架解耦的媒体表示
 *
 * @param mimeType MIME 类型
 * @param url      媒体 URL（与 data 二选一）
 * @param data     媒体二进制数据（与 url 二选一）
 * @author afg-projects
 * @since 1.0.0
 */
public record AiMedia(
    @NonNull String mimeType,
    @Nullable String url,
    @Nullable byte[] data
) {

    public AiMedia {
        if (url == null && data == null) {
            throw new IllegalArgumentException("Either url or data must be provided");
        }
        if (data != null) {
            data = data.clone(); // 防御性拷贝
        }
    }

    public static AiMedia imageUrl(@NonNull String url) {
        return new AiMedia("image/png", url, null);
    }

    public static AiMedia imageUrl(@NonNull String mimeType, @NonNull String url) {
        return new AiMedia(mimeType, url, null);
    }

    public static AiMedia imageBytes(@NonNull String mimeType, @NonNull byte[] data) {
        return new AiMedia(mimeType, null, data);
    }

    public static AiMedia audioUrl(@NonNull String mimeType, @NonNull String url) {
        return new AiMedia(mimeType, url, null);
    }

    public static AiMedia audioBytes(@NonNull String mimeType, @NonNull byte[] data) {
        return new AiMedia(mimeType, null, data);
    }

    public byte[] getData() {
        return data != null ? data.clone() : null;
    }

    public boolean isUrlBased() {
        return url != null;
    }

    public boolean isDataBased() {
        return data != null;
    }
}