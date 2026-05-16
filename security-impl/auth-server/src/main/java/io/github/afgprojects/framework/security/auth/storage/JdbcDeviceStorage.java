package io.github.afgprojects.framework.security.auth.storage;

import lombok.extern.slf4j.Slf4j;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.security.auth.entity.AuthUserDevice;
import io.github.afgprojects.framework.security.core.security.model.DeviceInfo;
import io.github.afgprojects.framework.security.core.storage.AfgDeviceStorage;

import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static io.github.afgprojects.framework.data.core.condition.Conditions.*;

/**
 * 基于 DataManager 的设备存储。
 *
 * <p>将设备信息持久化到关系型数据库，支持：
 * <ul>
 *   <li>设备信息保存与查询</li>
 *   <li>设备删除</li>
 *   <li>设备活跃状态管理</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
public class JdbcDeviceStorage implements AfgDeviceStorage {

    private final DataManager dataManager;

    /**
     * 构造函数。
     *
     * @param dataManager 数据管理器
     */
    public JdbcDeviceStorage(@NonNull DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void save(@NonNull DeviceInfo deviceInfo) {
        var existing = dataManager.findOneByField(AuthUserDevice.class,
                AuthUserDevice::getDeviceId, deviceInfo.getDeviceId());

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        AuthUserDevice entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setUserId(deviceInfo.getUserId());
            entity.setTenantId(deviceInfo.getTenantId());
            entity.setDeviceName(deviceInfo.getDeviceName());
            entity.setDeviceType(deviceInfo.getDeviceType());
            entity.setLastLoginIp(deviceInfo.getLastLoginIp());
            if (deviceInfo.getLastLoginTime() != null) {
                entity.setLastLoginTime(java.time.LocalDateTime.ofInstant(
                        deviceInfo.getLastLoginTime(), java.time.ZoneId.systemDefault()));
            }
            entity.setActive(deviceInfo.isActive());
            entity.setUpdatedAt(now);
        } else {
            entity = new AuthUserDevice();
            entity.setDeviceId(deviceInfo.getDeviceId());
            entity.setUserId(deviceInfo.getUserId());
            entity.setTenantId(deviceInfo.getTenantId());
            entity.setDeviceName(deviceInfo.getDeviceName());
            entity.setDeviceType(deviceInfo.getDeviceType());
            entity.setLastLoginIp(deviceInfo.getLastLoginIp());
            if (deviceInfo.getLastLoginTime() != null) {
                entity.setLastLoginTime(java.time.LocalDateTime.ofInstant(
                        deviceInfo.getLastLoginTime(), java.time.ZoneId.systemDefault()));
            }
            entity.setActive(deviceInfo.isActive());
            entity.setFirstLoginTime(now);
            entity.setCreatedAt(now);
        }
        dataManager.save(AuthUserDevice.class, entity);
        log.debug("Saved device: deviceId={}, userId={}", deviceInfo.getDeviceId(), deviceInfo.getUserId());
    }

    @Override
    @NonNull
    public Optional<DeviceInfo> findById(@NonNull String deviceId) {
        return dataManager.findOneByField(AuthUserDevice.class,
                AuthUserDevice::getDeviceId, deviceId)
                .map(this::toDeviceInfo);
    }

    @Override
    @NonNull
    public List<DeviceInfo> findByUserId(@NonNull String userId) {
        return dataManager.findAllByField(AuthUserDevice.class,
                AuthUserDevice::getUserId, userId)
                .stream()
                .map(this::toDeviceInfo)
                .toList();
    }

    @Override
    public int countActiveByUserId(@NonNull String userId) {
        return (int) dataManager.entity(AuthUserDevice.class)
                .query()
                .where(builder(AuthUserDevice.class)
                        .eq(AuthUserDevice::getUserId, userId)
                        .eq(AuthUserDevice::isActive, true)
                        .build())
                .count();
    }

    @Override
    public void delete(@NonNull String deviceId) {
        dataManager.findOneByField(AuthUserDevice.class,
                AuthUserDevice::getDeviceId, deviceId)
                .ifPresent(entity -> {
                    dataManager.deleteById(AuthUserDevice.class, entity.getId());
                    log.debug("Deleted device: deviceId={}", deviceId);
                });
    }

    @Override
    public void deleteByUserId(@NonNull String userId) {
        var entities = dataManager.findList(AuthUserDevice.class,
                builder(AuthUserDevice.class)
                        .eq(AuthUserDevice::getUserId, userId)
                        .build());

        for (var entity : entities) {
            dataManager.deleteById(AuthUserDevice.class, entity.getId());
        }
        log.debug("Deleted all devices for user: userId={}, count={}", userId, entities.size());
    }

    @Override
    public void updateActiveStatus(@NonNull String deviceId, boolean active) {
        dataManager.findOneByField(AuthUserDevice.class,
                AuthUserDevice::getDeviceId, deviceId)
                .ifPresent(entity -> {
                    entity.setActive(active);
                    dataManager.save(AuthUserDevice.class, entity);
                    log.debug("Updated device active status: deviceId={}, active={}", deviceId, active);
                });
    }

    /**
     * 将 AuthUserDevice 转换为 DeviceInfo。
     */
    private DeviceInfo toDeviceInfo(AuthUserDevice entity) {
        DeviceInfo info = new DeviceInfo();
        info.setDeviceId(entity.getDeviceId());
        info.setUserId(entity.getUserId());
        info.setTenantId(entity.getTenantId());
        info.setDeviceName(entity.getDeviceName());
        info.setDeviceType(entity.getDeviceType());
        info.setLastLoginIp(entity.getLastLoginIp());
        if (entity.getLastLoginTime() != null) {
            info.setLastLoginTime(entity.getLastLoginTime()
                    .atZone(java.time.ZoneId.systemDefault()).toInstant());
        }
        info.setActive(entity.isActive());
        return info;
    }
}