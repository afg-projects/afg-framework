package io.github.afgprojects.framework.security.auth.storage;

import lombok.extern.slf4j.Slf4j;
import io.github.afgprojects.framework.security.core.security.model.DeviceInfo;
import io.github.afgprojects.framework.security.core.storage.AfgDeviceStorage;

import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 基于 JDBC 的设备存储。
 *
 * <p>将设备信息持久化到关系型数据库，支持：
 * <ul>
 *   <li>设备信息保存与查询</li>
 *   <li>设备删除</li>
 *   <li>设备活跃状态管理</li>
 * </ul>
 *
 * <h3>表结构</h3>
 * <pre>
 * CREATE TABLE auth_device (
 *     device_id VARCHAR(128) PRIMARY KEY,
 *     user_id VARCHAR(64) NOT NULL,
 *     tenant_id VARCHAR(64),
 *     device_name VARCHAR(256),
 *     device_type VARCHAR(64),
 *     last_login_ip VARCHAR(64),
 *     last_login_time TIMESTAMP,
 *     active BOOLEAN NOT NULL DEFAULT TRUE,
 *     created_at TIMESTAMP NOT NULL,
 *     updated_at TIMESTAMP NOT NULL,
 *     INDEX idx_user_id (user_id),
 *     INDEX idx_active (active)
 * );
 * </pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class JdbcDeviceStorage implements AfgDeviceStorage {

    /** 默认表名 */
    private static final String DEFAULT_TABLE_NAME = "auth_device";

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;

    /** RowMapper 用于将 ResultSet 映射为 DeviceInfo */
    private final RowMapper<DeviceInfo> rowMapper = new DeviceInfoRowMapper();

    /**
     * 构造函数，使用默认表名。
     *
     * @param jdbcTemplate JDBC 模板
     */
    public JdbcDeviceStorage(@NonNull JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, DEFAULT_TABLE_NAME);
    }

    /**
     * 构造函数，使用自定义表名。
     *
     * @param jdbcTemplate JDBC 模板
     * @param tableName    表名
     */
    public JdbcDeviceStorage(@NonNull JdbcTemplate jdbcTemplate, @NonNull String tableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
    }

    @Override
    public void save(@NonNull DeviceInfo deviceInfo) {
        Instant now = Instant.now();
        Timestamp lastLoginTime = deviceInfo.getLastLoginTime() != null
                ? Timestamp.from(deviceInfo.getLastLoginTime())
                : null;

        // 先检查是否存在记录
        String checkSql = String.format("SELECT COUNT(*) FROM %s WHERE device_id = ?", tableName);
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, deviceInfo.getDeviceId());
        boolean exists = count != null && count > 0;

        if (exists) {
            // 更新现有记录
            String updateSql = String.format(
                    "UPDATE %s SET user_id = ?, tenant_id = ?, device_name = ?, device_type = ?, "
                            + "last_login_ip = ?, last_login_time = ?, active = ?, updated_at = ? "
                            + "WHERE device_id = ?",
                    tableName
            );
            jdbcTemplate.update(
                    updateSql,
                    deviceInfo.getUserId(),
                    deviceInfo.getTenantId(),
                    deviceInfo.getDeviceName(),
                    deviceInfo.getDeviceType(),
                    deviceInfo.getLastLoginIp(),
                    lastLoginTime,
                    deviceInfo.isActive(),
                    Timestamp.from(now),
                    deviceInfo.getDeviceId()
            );
            log.debug("Updated device: deviceId={}, userId={}", deviceInfo.getDeviceId(), deviceInfo.getUserId());
        } else {
            // 插入新记录
            String insertSql = String.format(
                    "INSERT INTO %s (device_id, user_id, tenant_id, device_name, device_type, last_login_ip, "
                            + "last_login_time, active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    tableName
            );
            jdbcTemplate.update(
                    insertSql,
                    deviceInfo.getDeviceId(),
                    deviceInfo.getUserId(),
                    deviceInfo.getTenantId(),
                    deviceInfo.getDeviceName(),
                    deviceInfo.getDeviceType(),
                    deviceInfo.getLastLoginIp(),
                    lastLoginTime,
                    deviceInfo.isActive(),
                    Timestamp.from(now),
                    Timestamp.from(now)
            );
            log.debug("Saved device: deviceId={}, userId={}", deviceInfo.getDeviceId(), deviceInfo.getUserId());
        }
    }

    @Override
    @NonNull
    public Optional<DeviceInfo> findById(@NonNull String deviceId) {
        String sql = String.format(
                "SELECT device_id, user_id, tenant_id, device_name, device_type, last_login_ip, "
                        + "last_login_time, active FROM %s WHERE device_id = ?",
                tableName
        );

        try {
            DeviceInfo deviceInfo = jdbcTemplate.queryForObject(sql, rowMapper, deviceId);
            return Optional.ofNullable(deviceInfo);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            log.error("Failed to find device by id: deviceId={}", deviceId, e);
            throw e;
        }
    }

    @Override
    @NonNull
    public List<DeviceInfo> findByUserId(@NonNull String userId) {
        String sql = String.format(
                "SELECT device_id, user_id, tenant_id, device_name, device_type, last_login_ip, "
                        + "last_login_time, active FROM %s WHERE user_id = ? ORDER BY updated_at DESC",
                tableName
        );

        try {
            return jdbcTemplate.query(sql, rowMapper, userId);
        } catch (DataAccessException e) {
            log.error("Failed to find devices by user: userId={}", userId, e);
            throw e;
        }
    }

    @Override
    public int countActiveByUserId(@NonNull String userId) {
        String sql = String.format(
                "SELECT COUNT(*) FROM %s WHERE user_id = ? AND active = TRUE",
                tableName
        );

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            log.error("Failed to count active devices: userId={}", userId, e);
            throw e;
        }
    }

    @Override
    public void delete(@NonNull String deviceId) {
        String sql = String.format("DELETE FROM %s WHERE device_id = ?", tableName);

        try {
            int rows = jdbcTemplate.update(sql, deviceId);
            if (rows > 0) {
                log.debug("Deleted device: deviceId={}", deviceId);
            }
        } catch (DataAccessException e) {
            log.error("Failed to delete device: deviceId={}", deviceId, e);
            throw e;
        }
    }

    @Override
    public void deleteByUserId(@NonNull String userId) {
        String sql = String.format("DELETE FROM %s WHERE user_id = ?", tableName);

        try {
            int rows = jdbcTemplate.update(sql, userId);
            log.debug("Deleted all devices for user: userId={}, count={}", userId, rows);
        } catch (DataAccessException e) {
            log.error("Failed to delete devices by user: userId={}", userId, e);
            throw e;
        }
    }

    @Override
    public void updateActiveStatus(@NonNull String deviceId, boolean active) {
        String sql = String.format(
                "UPDATE %s SET active = ?, updated_at = ? WHERE device_id = ?",
                tableName
        );

        try {
            int rows = jdbcTemplate.update(sql, active, Timestamp.from(Instant.now()), deviceId);
            if (rows > 0) {
                log.debug("Updated device active status: deviceId={}, active={}", deviceId, active);
            }
        } catch (DataAccessException e) {
            log.error("Failed to update device active status: deviceId={}", deviceId, e);
            throw e;
        }
    }

    /**
     * DeviceInfo RowMapper 实现。
     */
    private static class DeviceInfoRowMapper implements RowMapper<DeviceInfo> {
        @Override
        public DeviceInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            DeviceInfo deviceInfo = new DeviceInfo();
            deviceInfo.setDeviceId(rs.getString("device_id"));
            deviceInfo.setUserId(rs.getString("user_id"));
            deviceInfo.setTenantId(rs.getString("tenant_id"));
            deviceInfo.setDeviceName(rs.getString("device_name"));
            deviceInfo.setDeviceType(rs.getString("device_type"));
            deviceInfo.setLastLoginIp(rs.getString("last_login_ip"));

            Timestamp lastLoginTime = rs.getTimestamp("last_login_time");
            if (lastLoginTime != null) {
                deviceInfo.setLastLoginTime(lastLoginTime.toInstant());
            }

            deviceInfo.setActive(rs.getBoolean("active"));

            return deviceInfo;
        }
    }
}
