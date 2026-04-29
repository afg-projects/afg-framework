package io.github.afgprojects.framework.integration.storage.local;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.api.storage.FileStorage;

/**
 * 本地存储工具类
 */
public final class LocalStorageUtils {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageUtils.class);

    private LocalStorageUtils() {
    }

    /**
     * 创建本地存储
     *
     * @param bucket  存储桶名称
     * @param rootDir 根目录
     * @return 文件存储实例
     */
    @NonNull
    public static FileStorage create(@NonNull String bucket, @NonNull String rootDir) {
        return new LocalFileStorage(bucket, rootDir, null);
    }

    /**
     * 创建本地存储
     *
     * @param bucket  存储桶名称
     * @param rootDir 根目录
     * @param baseUrl 基础 URL
     * @return 文件存储实例
     */
    @NonNull
    public static FileStorage create(@NonNull String bucket, @NonNull String rootDir, @Nullable String baseUrl) {
        return new LocalFileStorage(bucket, rootDir, baseUrl);
    }

    /**
     * 写入文件内容
     *
     * @param storage 存储实例
     * @param key     文件键
     * @param content 文件内容
     * @param contentType 内容类型
     * @return 写入的字节数
     */
    public static long writeString(@NonNull FileStorage storage, @NonNull String key,
                                   @NonNull String content, @Nullable String contentType) {
        try {
            Path tempFile = Files.createTempFile("upload-", ".tmp");
            Files.writeString(tempFile, content);
            try (InputStream is = Files.newInputStream(tempFile)) {
                long size = Files.size(tempFile);
                storage.upload(key, is, size, contentType);
                return size;
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write string to storage", e);
        }
    }

    /**
     * 读取文件内容
     *
     * @param storage 存储实例
     * @param key     文件键
     * @return 文件内容
     */
    @NonNull
    public static String readString(@NonNull FileStorage storage, @NonNull String key) {
        try (var result = storage.download(key)) {
            return new String(result.inputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read string from storage", e);
        }
    }

    /**
     * 写入字节数组
     *
     * @param storage 存储实例
     * @param key     文件键
     * @param bytes   字节数组
     * @param contentType 内容类型
     */
    public static void writeBytes(@NonNull FileStorage storage, @NonNull String key,
                                  @NonNull byte[] bytes, @Nullable String contentType) {
        try {
            Path tempFile = Files.createTempFile("upload-", ".tmp");
            Files.write(tempFile, bytes);
            try (InputStream is = Files.newInputStream(tempFile)) {
                storage.upload(key, is, bytes.length, contentType);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write bytes to storage", e);
        }
    }

    /**
     * 读取字节数组
     *
     * @param storage 存储实例
     * @param key     文件键
     * @return 字节数组
     */
    @NonNull
    public static byte[] readBytes(@NonNull FileStorage storage, @NonNull String key) {
        try (var result = storage.download(key)) {
            return result.inputStream().readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read bytes from storage", e);
        }
    }
}
