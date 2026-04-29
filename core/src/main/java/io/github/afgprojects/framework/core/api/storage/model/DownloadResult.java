package io.github.afgprojects.framework.core.api.storage.model;

import java.io.IOException;
import java.io.InputStream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 文件下载结果
 * <p>
 * 实现 {@link AutoCloseable} 以支持 try-with-resources 语法。
 *
 * @param inputStream 文件输入流
 * @param size        文件大小（字节）
 * @param contentType 内容类型（MIME type）
 * @param etag        实体标签（用于缓存校验）
 */
public record DownloadResult(
        @NonNull InputStream inputStream,
        long size,
        @Nullable String contentType,
        @Nullable String etag
) implements AutoCloseable {

    /**
     * 创建下载结果（简化构造）
     */
    @NonNull
    public static DownloadResult of(@NonNull InputStream inputStream, long size) {
        return new DownloadResult(inputStream, size, null, null);
    }

    /**
     * 创建下载结果（带类型）
     */
    @NonNull
    public static DownloadResult of(@NonNull InputStream inputStream, long size,
                                    @Nullable String contentType) {
        return new DownloadResult(inputStream, size, contentType, null);
    }

    @Override
    public void close() {
        try {
            inputStream.close();
        } catch (IOException e) {
            // 忽略关闭异常
        }
    }
}
