package io.github.afgprojects.framework.core.api.storage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.api.storage.model.StorageType;

/**
 * NoOp 文件存储工厂降级实现
 * <p>
 * 当没有配置任何存储后端时，提供本地降级。
 * 所有注册操作静默丢弃，查询返回 null/false。
 */
public class NoOpFileStorageFactory implements FileStorageFactory {

    private static final NoOpFileStorage NO_OP_STORAGE = new NoOpFileStorage();

    @Override
    @NonNull
    public FileStorage getDefaultStorage() {
        return NO_OP_STORAGE;
    }

    @Override
    @Nullable
    public FileStorage getStorage(@NonNull String name) {
        return null;
    }

    @Override
    @Nullable
    public FileStorage getStorage(@NonNull StorageType type, @NonNull String name) {
        return null;
    }

    @Override
    public void register(@NonNull String name, @NonNull FileStorage storage) {
        // no-op: 静默丢弃
    }

    @Override
    @Nullable
    public FileStorage unregister(@NonNull String name) {
        return null;
    }

    @Override
    public boolean hasStorage(@NonNull String name) {
        return false;
    }
}
