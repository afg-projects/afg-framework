package io.github.afgprojects.framework.security.core.storage;

import io.github.afgprojects.framework.security.core.security.model.DeviceInfo;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * 设备存储接口。
 *
 * <p>定义用户设备信息的存储、查询和管理操作。
 *
 * <p>实现类可以基于内存、Redis、数据库等存储介质。
 *
 * <p>典型使用场景：
 * <ul>
 *   <li>记录用户登录设备</li>
 *   <li>实现设备管理功能</li>
 *   <li>限制设备登录数量</li>
 *   <li>设备信任管理</li>
 *   <li>安全审计</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface AfgDeviceStorage {

    /**
     * 保存设备信息。
     *
     * <p>新增或更新设备信息。如果设备 ID 已存在，则更新设备信息。
     *
     * @param deviceInfo 设备信息，永不为 null
     */
    void save(@NonNull DeviceInfo deviceInfo);

    /**
     * 根据设备 ID 查找设备。
     *
     * @param deviceId 设备唯一标识，永不为 null
     * @return 设备信息，如果不存在则返回空
     */
    @NonNull
    Optional<DeviceInfo> findById(@NonNull String deviceId);

    /**
     * 查找用户所有设备。
     *
     * <p>返回指定用户注册的所有设备，包括活跃和非活跃设备。
     *
     * @param userId 用户 ID，永不为 null
     * @return 设备列表，永不为 null
     */
    @NonNull
    List<DeviceInfo> findByUserId(@NonNull String userId);

    /**
     * 查找用户活跃设备数量。
     *
     * <p>返回指定用户的活跃设备数量，用于设备数量限制检查。
     *
     * @param userId 用户 ID，永不为 null
     * @return 活跃设备数量，永不为负数
     */
    int countActiveByUserId(@NonNull String userId);

    /**
     * 删除设备。
     *
     * <p>根据设备 ID 删除设备记录。
     *
     * @param deviceId 设备唯一标识，永不为 null
     */
    void delete(@NonNull String deviceId);

    /**
     * 删除用户所有设备。
     *
     * <p>删除指定用户的所有设备记录，用于强制下线或账户注销场景。
     *
     * @param userId 用户 ID，永不为 null
     */
    void deleteByUserId(@NonNull String userId);

    /**
     * 更新设备活跃状态。
     *
     * <p>更新设备的活跃状态，用于设备管理功能。
     *
     * @param deviceId 设备唯一标识，永不为 null
     * @param active   活跃状态，true 表示活跃，false 表示非活跃
     */
    void updateActiveStatus(@NonNull String deviceId, boolean active);
}
