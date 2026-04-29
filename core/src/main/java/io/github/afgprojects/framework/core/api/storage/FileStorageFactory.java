package io.github.afgprojects.framework.core.api.storage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.api.storage.model.StorageType;

/**
 * 文件存储工厂接口
 * <p>
 * 用于创建和管理多个存储实例。
 *
 * <p>具体实现由 integration/afg-storage 模块提供。
 */
public interface FileStorageFactory {

    /**
     * 获取默认存储实例
     *
     * @return 默认存储实例
     */
    @NonNull
    FileStorage getDefaultStorage();

    /**
     * 获取指定存储实例
     *
     * @param name 存储名称
     * @return 存储实例，不存在时返回 null
     */
    @Nullable
    FileStorage getStorage(@NonNull String name);

    /**
     * 获取指定类型和名称的存储实例
     *
     * @param type 存储类型
     * @param name 存储名称
     * @return 存储实例，不存在时返回 null
     */
    @Nullable
    FileStorage getStorage(@NonNull StorageType type, @NonNull String name);

    /**
     * 注册存储实例
     *
     * @param name    存储名称
     * @param storage 存储实例
     */
    void register(@NonNull String name, @NonNull FileStorage storage);

    /**
     * 注销存储实例
     *
     * @param name 存储名称
     * @return 被注销的存储实例，不存在时返回 null
     */
    @Nullable
    FileStorage unregister(@NonNull String name);

    /**
     * 检查存储实例是否存在
     *
     * @param name 存储名称
     * @return 是否存在
     */
    boolean hasStorage(@NonNull String name);
}
