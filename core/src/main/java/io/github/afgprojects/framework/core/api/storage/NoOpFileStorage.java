package io.github.afgprojects.framework.core.api.storage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.api.storage.model.DownloadResult;
import io.github.afgprojects.framework.core.api.storage.model.ListOptions;
import io.github.afgprojects.framework.core.api.storage.model.ListResult;
import io.github.afgprojects.framework.core.api.storage.model.PresignedUrlOptions;
import io.github.afgprojects.framework.core.api.storage.model.StorageMetadata;
import io.github.afgprojects.framework.core.api.storage.model.StorageObject;
import io.github.afgprojects.framework.core.api.storage.model.StorageType;
import io.github.afgprojects.framework.core.api.storage.model.UploadRequest;

/**
 * NoOp 文件存储实现
 * <p>
 * 空操作降级实现，写操作抛出 {@link UnsupportedOperationException} 提示需要配置存储后端，
 * 读操作返回空/不存在结果。
 * 适用于未配置任何文件存储后端的场景。
 * <p>
 * 不由 AutoConfiguration 自动注册，仅在需要手动降级时使用。
 * 文件存储由 integration/afg-storage 模块提供。
 *
 * @since 1.0.0
 */
public class NoOpFileStorage implements FileStorage {

    private static final String NOT_CONFIGURED_MESSAGE =
            "FileStorage is not configured. Please add afg-storage module dependency and configure a storage backend.";

    @Override
    @NonNull
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }

    @Override
    @NonNull
    public String getBucket() {
        return "noop";
    }

    @Override
    @NonNull
    public StorageObject upload(@NonNull UploadRequest request) {
        throw new UnsupportedOperationException(NOT_CONFIGURED_MESSAGE);
    }

    @Override
    @NonNull
    public DownloadResult download(@NonNull String key) {
        throw new UnsupportedOperationException(NOT_CONFIGURED_MESSAGE);
    }

    @Override
    public boolean delete(@NonNull String key) {
        return false;
    }

    @Override
    public int deleteBatch(@NonNull Iterable<String> keys) {
        return 0;
    }

    @Override
    public boolean exists(@NonNull String key) {
        return false;
    }

    @Override
    @Nullable
    public StorageObject get(@NonNull String key) {
        return null;
    }

    @Override
    @NonNull
    public ListResult list(@NonNull ListOptions options) {
        return ListResult.empty();
    }

    @Override
    @NonNull
    public String getUrl(@NonNull String key) {
        throw new UnsupportedOperationException(NOT_CONFIGURED_MESSAGE);
    }

    @Override
    @NonNull
    public String getPresignedUrl(@NonNull String key, @NonNull PresignedUrlOptions options) {
        throw new UnsupportedOperationException(NOT_CONFIGURED_MESSAGE);
    }

    @Override
    @NonNull
    public StorageObject updateMetadata(@NonNull String key, @NonNull StorageMetadata metadata) {
        throw new UnsupportedOperationException(NOT_CONFIGURED_MESSAGE);
    }

    @Override
    @NonNull
    public StorageObject copy(@NonNull String sourceKey, @NonNull String targetKey) {
        throw new UnsupportedOperationException(NOT_CONFIGURED_MESSAGE);
    }
}
