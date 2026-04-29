package io.github.afgprojects.framework.integration.storage.local;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.api.storage.FileStorage;
import io.github.afgprojects.framework.core.api.storage.model.DownloadResult;
import io.github.afgprojects.framework.core.api.storage.model.ListOptions;
import io.github.afgprojects.framework.core.api.storage.model.ListResult;
import io.github.afgprojects.framework.core.api.storage.model.PresignedUrlOptions;
import io.github.afgprojects.framework.core.api.storage.model.StorageMetadata;
import io.github.afgprojects.framework.core.api.storage.model.StorageObject;
import io.github.afgprojects.framework.core.api.storage.model.StorageType;
import io.github.afgprojects.framework.core.api.storage.model.UploadRequest;
import io.github.afgprojects.framework.integration.storage.model.StorageErrorCode;
import io.github.afgprojects.framework.integration.storage.model.StorageException;

/**
 * 本地文件存储实现
 * <p>
 * 将文件存储在本地文件系统中。
 * 适用于开发、测试或小规模应用场景。
 */
public class LocalFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

    private final String bucket;
    private final Path rootPath;
    private final String baseUrl;

    /**
     * 构造本地文件存储
     *
     * @param bucket  存储桶名称（用作根目录名）
     * @param rootDir 根目录路径
     * @param baseUrl 基础 URL（用于生成访问 URL）
     */
    public LocalFileStorage(@NonNull String bucket, @NonNull String rootDir, @Nullable String baseUrl) {
        this.bucket = bucket;
        this.rootPath = Paths.get(rootDir, bucket).toAbsolutePath().normalize();
        this.baseUrl = baseUrl != null ? baseUrl : "";

        // 确保目录存在
        try {
            Files.createDirectories(rootPath);
            log.info("Local storage initialized: bucket={}, path={}", bucket, rootPath);
        } catch (IOException e) {
            throw StorageException.connectionFailed(
                    "无法创建存储目录: " + rootPath, e);
        }
    }

    @Override
    @NonNull
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }

    @Override
    @NonNull
    public String getBucket() {
        return bucket;
    }

    @Override
    @NonNull
    public StorageObject upload(@NonNull UploadRequest request) {
        Path filePath = resolvePath(request.key());

        // 创建父目录
        try {
            Files.createDirectories(filePath.getParent());
        } catch (IOException e) {
            throw StorageException.uploadFailed(request.key(), e);
        }

        // 写入文件
        try (InputStream is = request.inputStream()) {
            Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);

            // 设置内容类型（存储为 .meta 文件）
            if (request.contentType() != null) {
                Path metaPath = getMetaPath(filePath);
                Files.writeString(metaPath, request.contentType());
            }

            log.debug("File uploaded: {}", request.key());
            return getStorageObject(request.key(), filePath);
        } catch (IOException e) {
            throw StorageException.uploadFailed(request.key(), e);
        }
    }

    @Override
    @NonNull
    public DownloadResult download(@NonNull String key) {
        Path filePath = resolvePath(key);

        if (!Files.exists(filePath)) {
            throw StorageException.fileNotFound(key);
        }

        try {
            InputStream inputStream = Files.newInputStream(filePath, StandardOpenOption.READ);
            long size = Files.size(filePath);
            String contentType = readContentType(filePath);

            return new DownloadResult(inputStream, size, contentType, null);
        } catch (IOException e) {
            throw StorageException.downloadFailed(key, e);
        }
    }

    @Override
    public boolean delete(@NonNull String key) {
        Path filePath = resolvePath(key);

        if (!Files.exists(filePath)) {
            return false;
        }

        try {
            Files.delete(filePath);
            // 删除元数据文件
            Path metaPath = getMetaPath(filePath);
            Files.deleteIfExists(metaPath);
            log.debug("File deleted: {}", key);
            return true;
        } catch (IOException e) {
            throw StorageException.deleteFailed(key, e);
        }
    }

    @Override
    public int deleteBatch(@NonNull Iterable<String> keys) {
        int count = 0;
        for (String key : keys) {
            if (delete(key)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean exists(@NonNull String key) {
        Path filePath = resolvePath(key);
        return Files.exists(filePath);
    }

    @Override
    @Nullable
    public StorageObject get(@NonNull String key) {
        Path filePath = resolvePath(key);

        if (!Files.exists(filePath)) {
            return null;
        }

        return getStorageObject(key, filePath);
    }

    @Override
    @NonNull
    public ListResult list(@NonNull ListOptions options) {
        List<StorageObject> objects = new ArrayList<>();
        List<String> commonPrefixes = new ArrayList<>();
        String nextMarker = null;

        Path basePath = options.prefix() != null
                ? resolvePath(options.prefix())
                : rootPath;

        if (!Files.exists(basePath)) {
            return ListResult.empty();
        }

        try (Stream<Path> stream = Files.walk(basePath)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.toString().endsWith(".meta"))
                    .limit(options.maxKeys() + 1)
                    .toList();

            for (int i = 0; i < Math.min(files.size(), options.maxKeys()); i++) {
                Path file = files.get(i);
                String key = rootPath.relativize(file).toString().replace('\\', '/');
                objects.add(getStorageObject(key, file));
            }

            if (files.size() > options.maxKeys()) {
                nextMarker = rootPath.relativize(files.get(options.maxKeys())).toString().replace('\\', '/');
            }
        } catch (IOException e) {
            log.error("Failed to list files: {}", options.prefix(), e);
        }

        return new ListResult(objects, commonPrefixes, nextMarker != null, nextMarker);
    }

    @Override
    @NonNull
    public String getUrl(@NonNull String key) {
        if (!exists(key)) {
            throw StorageException.fileNotFound(key);
        }
        // 本地存储返回相对路径 URL
        return baseUrl + "/" + bucket + "/" + key;
    }

    @Override
    @NonNull
    public String getPresignedUrl(@NonNull String key, @NonNull PresignedUrlOptions options) {
        // 本地存储不支持预签名 URL，返回普通 URL
        log.warn("Local storage does not support presigned URLs, returning normal URL");
        return getUrl(key);
    }

    @Override
    @NonNull
    public StorageObject updateMetadata(@NonNull String key, @NonNull StorageMetadata metadata) {
        // 本地存储不支持自定义元数据
        Path filePath = resolvePath(key);
        if (!Files.exists(filePath)) {
            throw StorageException.fileNotFound(key);
        }
        return getStorageObject(key, filePath);
    }

    @Override
    @NonNull
    public StorageObject copy(@NonNull String sourceKey, @NonNull String targetKey) {
        Path sourcePath = resolvePath(sourceKey);
        Path targetPath = resolvePath(targetKey);

        if (!Files.exists(sourcePath)) {
            throw StorageException.fileNotFound(sourceKey);
        }

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 复制元数据文件
            Path sourceMeta = getMetaPath(sourcePath);
            if (Files.exists(sourceMeta)) {
                Files.copy(sourceMeta, getMetaPath(targetPath), StandardCopyOption.REPLACE_EXISTING);
            }

            log.debug("File copied: {} -> {}", sourceKey, targetKey);
            return getStorageObject(targetKey, targetPath);
        } catch (IOException e) {
            throw new StorageException(StorageErrorCode.FILE_COPY_FAILED,
                    "文件复制失败: " + sourceKey + " -> " + targetKey, e);
        }
    }

    /**
     * 解析文件路径
     */
    private Path resolvePath(String key) {
        // 安全检查：防止路径遍历攻击
        Path resolved = rootPath.resolve(key).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new StorageException(StorageErrorCode.FILE_PATH_INVALID,
                    "非法文件路径: " + key);
        }
        return resolved;
    }

    /**
     * 获取元数据文件路径
     */
    private Path getMetaPath(Path filePath) {
        return filePath.resolveSibling(filePath.getFileName() + ".meta");
    }

    /**
     * 读取内容类型
     */
    @Nullable
    private String readContentType(Path filePath) {
        Path metaPath = getMetaPath(filePath);
        if (Files.exists(metaPath)) {
            try {
                return Files.readString(metaPath).trim();
            } catch (IOException e) {
                log.warn("Failed to read content type: {}", metaPath);
            }
        }
        // 尝试从文件名推断
        return guessContentType(filePath.getFileName().toString());
    }

    /**
     * 获取存储对象
     */
    private StorageObject getStorageObject(String key, Path filePath) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            String contentType = readContentType(filePath);
            FileTime lastModified = attrs.lastModifiedTime();

            return new StorageObject(
                    key,
                    attrs.size(),
                    contentType,
                    null,
                    lastModified != null ? lastModified.toInstant() : Instant.now(),
                    null
            );
        } catch (IOException e) {
            log.error("Failed to read file attributes: {}", filePath, e);
            return StorageObject.of(key, 0, null);
        }
    }

    /**
     * 根据文件名猜测内容类型
     */
    @Nullable
    private String guessContentType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return null;
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "pdf" -> "application/pdf";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "txt" -> "text/plain";
            case "zip" -> "application/zip";
            case "mp3" -> "audio/mpeg";
            case "mp4" -> "video/mp4";
            default -> null;
        };
    }
}
