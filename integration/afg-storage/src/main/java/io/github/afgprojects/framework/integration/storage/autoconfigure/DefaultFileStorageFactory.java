package io.github.afgprojects.framework.integration.storage.autoconfigure;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.api.storage.FileStorage;
import io.github.afgprojects.framework.core.api.storage.FileStorageFactory;
import io.github.afgprojects.framework.core.api.storage.model.StorageType;

/**
 * 默认文件存储工厂实现
 */
public class DefaultFileStorageFactory implements FileStorageFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultFileStorageFactory.class);

    private final Map<String, FileStorage> storageMap = new ConcurrentHashMap<>();
    private final String defaultStorageName;

    public DefaultFileStorageFactory() {
        this.defaultStorageName = "default";
    }

    public DefaultFileStorageFactory(@NonNull String defaultStorageName) {
        this.defaultStorageName = defaultStorageName;
    }

    @Override
    @NonNull
    public FileStorage getDefaultStorage() {
        FileStorage storage = storageMap.get(defaultStorageName);
        if (storage == null) {
            throw new IllegalStateException("默认存储未配置: " + defaultStorageName);
        }
        return storage;
    }

    @Override
    @Nullable
    public FileStorage getStorage(@NonNull String name) {
        return storageMap.get(name);
    }

    @Override
    @Nullable
    public FileStorage getStorage(@NonNull StorageType type, @NonNull String name) {
        FileStorage storage = storageMap.get(name);
        if (storage != null && storage.getStorageType() == type) {
            return storage;
        }
        return null;
    }

    @Override
    public void register(@NonNull String name, @NonNull FileStorage storage) {
        storageMap.put(name, storage);
        log.info("Storage registered: name={}, type={}, bucket={}",
                name, storage.getStorageType(), storage.getBucket());
    }

    @Override
    @Nullable
    public FileStorage unregister(@NonNull String name) {
        FileStorage storage = storageMap.remove(name);
        if (storage != null) {
            log.info("Storage unregistered: name={}", name);
        }
        return storage;
    }

    @Override
    public boolean hasStorage(@NonNull String name) {
        return storageMap.containsKey(name);
    }

    /**
     * 获取所有存储实例
     */
    @NonNull
    public Map<String, FileStorage> getAllStorage() {
        return new HashMap<>(storageMap);
    }

    /**
     * 存储实例数量
     */
    public int size() {
        return storageMap.size();
    }
}