package io.github.afgprojects.framework.security.core.storage;

import io.github.afgprojects.framework.security.core.security.model.DeviceInfo;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * NoOp 设备存储降级实现。
 * <p>
 * 存储操作为空操作，查询返回空/不存在。
 *
 * @since 1.0.0
 */
public class NoOpDeviceStorage implements AfgDeviceStorage {

    @Override
    public void save(@NonNull DeviceInfo deviceInfo) {
        // no-op
    }

    @Override
    public @NonNull Optional<DeviceInfo> findById(@NonNull String deviceId) {
        return Optional.empty();
    }

    @Override
    public @NonNull List<DeviceInfo> findByUserId(@NonNull String userId) {
        return List.of();
    }

    @Override
    public int countActiveByUserId(@NonNull String userId) {
        return 0;
    }

    @Override
    public void delete(@NonNull String deviceId) {
        // no-op
    }

    @Override
    public void deleteByUserId(@NonNull String userId) {
        // no-op
    }

    @Override
    public void updateActiveStatus(@NonNull String deviceId, boolean active) {
        // no-op
    }
}
