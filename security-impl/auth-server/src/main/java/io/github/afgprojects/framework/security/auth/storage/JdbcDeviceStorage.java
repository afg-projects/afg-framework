package io.github.afgprojects.framework.security.auth.storage;

import io.github.afgprojects.framework.security.core.security.model.DeviceInfo;
import io.github.afgprojects.framework.security.core.storage.AfgDeviceStorage;
import lombok.extern.slf4j.Slf4j;
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
 * <p>将用户设备信息持久化到关系型数据库，支持：
 * <ul>
 *   <li>设备信息保存与查询</li>
 *   <li>按用户查询设备列表</li>
 *   <li>统计用户活跃设备数量</li>
 *   <li>设备删除与状态更新</li>
 * </ul>
 *
 * <h3>表结构</h3>
 * <pre>
 * CREATE TABLE auth_user_device (
 *     device_id VARCHAR(100) NOT NULL UNIQUE,
 *     user_id VARCHAR(64) NOT NULL,
 *     tenant_id VARCHAR(64),
 *     device_name VARCHAR(200),
 *     device_type VARCHAR(50),
 *     last_login_ip VARCHAR(50),
 *     last_login_time TIMESTAMP,
 *     active BOOLEAN DEFAULT TRUE,
 *     created_at TIMESTAMP NOT NULL,
 *     updated_at TIMESTAMP NOT NULL,
 *     INDEX idx_user_id (user_id)
 * );
 * </pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class JdbcDeviceStorage implements AfgDeviceStorage {

    /** 默认表名 */
    private static final String DEFAULT_TABLE_NAME = "auth_user_device";

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

        String sql = String.format(
                "INSERT INTO %s (device_id, user_id, tenant_id, device_name, device_type, "
                        + "last_login_ip, last_login_time, active, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE "
                        + "user_id = VALUES(user_id), "
                        + "tenant_id = VALUES(tenant_id), "
                        + "device_name = VALUES(device_name), "
                        + "device_type = VALUES(device_type), "
                        + "last_login_ip = VALUES(last_login_ip), "
                        + "last_login_time = VALUES(last_login_time), "
                        + "active = VALUES(active), "
                        + "updated_at = VALUES(updated_at)",
                tableName
        );

        try {
            jdbcTemplate.update(
                    sql,
                    deviceInfo.getDeviceId(),
                    deviceInfo.getUserId(),
                    deviceInfo.getTenantId(),
                    deviceInfo.getDeviceName(),
                    deviceInfo.getDeviceType(),
                    deviceInfo.getLastLoginIp(),
                    deviceInfo.getLastLoginTime() != null
                            ? Timestamp.from(deviceInfo.getLastLoginTime())
                            : null,
                    deviceInfo.isActive(),
                    Timestamp.from(now),
                    Timestamp.from(now)
            );
            log.debug("Saved device: deviceId={}, userId={}", deviceInfo.getDeviceId(), deviceInfo.getUserId());
        } catch (DataAccessException e) {
            log.error("Failed to save device: deviceId={}, userId={}",
                    deviceInfo.getDeviceId(), deviceInfo.getUserId(), e);
            throw e;
        }
    }

    @Override
    @NonNull
    public Optional<DeviceInfo> findById(@NonNull String deviceId) {
        String sql = String.format(
                "SELECT device_id, user_id, tenant_id, device_name, device_type, "
                        + "last_login_ip, last_login_time, active, created_at, updated_at "
                        + "FROM %s WHERE device_id = ?",
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
                "SELECT device_id, user_id, tenant_id, device_name, device_type, "
                        + "last_login_ip, last_login_time, active, created_at, updated_at "
                        + "FROM %s WHERE user_id = ? ORDER BY last_login_time DESC",
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
            log.error("Failed to count active devices by user: userId={}", userId, e);
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
            log.error("Failed to update device active status: deviceId={}, active={}", deviceId, active, e);
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
