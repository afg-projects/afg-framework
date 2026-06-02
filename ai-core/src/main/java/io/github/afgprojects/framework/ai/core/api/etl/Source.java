package io.github.afgprojects.framework.ai.core.api.etl;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * 表示文档来源的抽象接口。
 *
 * <p>Source 提供统一的文档源抽象，支持：
 * <ul>
 *   <li>文件源 - 本地文件</li>
 *   <li>URL 源 - 网络资源</li>
 *   <li>文本源 - 内联文本</li>
 *   <li>字节源 - 原始字节数据</li>
 * </ul>
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface Source {

    /**
     * 获取源路径或标识符。
     *
     * @return 源路径
     */
    @NonNull
    String getPath();

    /**
     * 获取源类型。
     *
     * @return 源类型
     */
    @NonNull
    SourceType getType();

    /**
     * 获取内容类型（MIME 类型）。
     *
     * @return 内容类型，如果未知则返回 null
     */
    @Nullable
    default String getContentType() {
        return null;
    }

    /**
     * 创建文件源。
     *
     * @param path 文件路径
     * @return 文件源
     */
    @NonNull
    static Source ofFile(@NonNull String path) {
        return new FileSource(path);
    }

    /**
     * 创建 URL 源。
     *
     * @param url URL 字符串
     * @return URL 源
     */
    @NonNull
    static Source ofUrl(@NonNull String url) {
        return new UrlSource(url);
    }

    /**
     * 创建文本源。
     *
     * @param content 文本内容
     * @return 文本源
     */
    @NonNull
    static Source ofText(@NonNull String content) {
        return new TextSource(content);
    }

    /**
     * 创建字节源。
     *
     * @param bytes       字节数据
     * @param contentType 内容类型
     * @return 字节源
     */
    @NonNull
    static Source ofBytes(@NonNull byte[] bytes, @NonNull String contentType) {
        return new BytesSource(bytes, contentType);
    }

    /**
     * 源类型枚举。
     */
    enum SourceType {
        FILE,
        URL,
        TEXT,
        BYTES,
        DATABASE,
        API,
        CUSTOM
    }

    /**
     * 文件源实现。
     */
    record FileSource(@NonNull String path) implements Source {

        @Override
        public @NonNull String getPath() {
            return path;
        }

        @Override
        public @NonNull SourceType getType() {
            return SourceType.FILE;
        }

        @Override
        public String getContentType() {
            int dotIndex = path.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < path.length() - 1) {
                String ext = path.substring(dotIndex + 1).toLowerCase();
                return switch (ext) {
                    case "pdf" -> "application/pdf";
                    case "txt" -> "text/plain";
                    case "md", "markdown" -> "text/markdown";
                    case "html", "htm" -> "text/html";
                    case "json" -> "application/json";
                    case "xml" -> "application/xml";
                    case "csv" -> "text/csv";
                    default -> "application/octet-stream";
                };
            }
            return "application/octet-stream";
        }
    }

    /**
     * URL 源实现。
     */
    record UrlSource(@NonNull String path) implements Source {

        @Override
        public @NonNull String getPath() {
            return path;
        }

        @Override
        public @NonNull SourceType getType() {
            return SourceType.URL;
        }
    }

    /**
     * 文本源实现。
     */
    record TextSource(@NonNull String content) implements Source {

        @Override
        public @NonNull String getPath() {
            return "inline://" + Integer.toHexString(content.hashCode());
        }

        @Override
        public @NonNull SourceType getType() {
            return SourceType.TEXT;
        }

        @Override
        public String getContentType() {
            return "text/plain";
        }
    }

    /**
     * 字节源实现。
     */
    record BytesSource(byte[] bytes, @NonNull String contentType) implements Source {

        /**
         * 紧凑构造器，进行防御性复制。
         */
        public BytesSource {
            bytes = bytes.clone(); // 防御性复制
        }

        /**
         * 获取字节数据的副本。
         *
         * @return 字节数组副本
         */
        public byte[] getBytes() {
            return bytes.clone(); // 返回副本，保护内部数据
        }

        @Override
        public @NonNull String getPath() {
            return "bytes://" + Integer.toHexString(Arrays.hashCode(bytes));
        }

        @Override
        public @NonNull SourceType getType() {
            return SourceType.BYTES;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BytesSource that = (BytesSource) o;
            return Arrays.equals(bytes, that.bytes) && contentType.equals(that.contentType);
        }

        @Override
        public int hashCode() {
            int result = contentType.hashCode();
            result = 31 * result + Arrays.hashCode(bytes);
            return result;
        }
    }
}