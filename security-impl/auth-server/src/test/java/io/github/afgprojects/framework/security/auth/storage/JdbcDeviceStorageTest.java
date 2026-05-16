package io.github.afgprojects.framework.security.auth.storage;

import io.github.afgprojects.framework.security.core.security.model.DeviceInfo;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * JdbcDeviceStorage 测试。
 */
@DisplayName("JdbcDeviceStorage 测试")
class JdbcDeviceStorageTest {

    private JdbcTemplate jdbcTemplate;
    private JdbcDeviceStorage storage;

    @BeforeEach
    void setUp() {
        // 创建 H2 数据源
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb_device;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        // 创建 JdbcTemplate
        jdbcTemplate = new JdbcTemplate(dataSource);

        // 先删除表（如果存在）
        jdbcTemplate.execute("DROP TABLE IF EXISTS auth_device");

        // 创建表
        jdbcTemplate.execute("""
                CREATE TABLE auth_device (
                    device_id VARCHAR(128) PRIMARY KEY,
                    user_id VARCHAR(64) NOT NULL,
                    tenant_id VARCHAR(64),
                    device_name VARCHAR(256),
                    device_type VARCHAR(64),
                    last_login_ip VARCHAR(64),
                    last_login_time TIMESTAMP,
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);

        // 创建索引
        jdbcTemplate.execute("CREATE INDEX idx_user_id_device ON auth_device (user_id)");
        jdbcTemplate.execute("CREATE INDEX idx_active_device ON auth_device (active)");

        // 创建存储器
        storage = new JdbcDeviceStorage(jdbcTemplate);
    }

    @AfterEach
    void tearDown() {
        if (jdbcTemplate != null) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS auth_device");
        }
    }

    private DeviceInfo createDevice(String deviceId, String userId) {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceId(deviceId);
        deviceInfo.setUserId(userId);
        return deviceInfo;
    }

    private DeviceInfo createFullDevice(String deviceId, String userId) {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceId(deviceId);
        deviceInfo.setUserId(userId);
        deviceInfo.setTenantId("tenant-001");
        deviceInfo.setDeviceName("Test Device");
        deviceInfo.setDeviceType("PC");
        deviceInfo.setLastLoginIp("192.168.1.1");
        deviceInfo.setLastLoginTime(Instant.now());
        deviceInfo.setActive(true);
        return deviceInfo;
    }

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("应该成功保存设备信息")
        void shouldSaveDevice() {
            // given
            DeviceInfo deviceInfo = createDevice("device-123", "user-123");

            // when
            storage.save(deviceInfo);

            // then
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_device WHERE device_id = ?",
                    Integer.class,
                    "device-123"
            );
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("应该保存所有字段的设备信息")
        void shouldSaveAllFields() {
            // given
            DeviceInfo deviceInfo = createFullDevice("device-complete", "user-complete");

            // when
            storage.save(deviceInfo);

            // then
            var record = jdbcTemplate.queryForObject(
                    "SELECT device_id, user_id, tenant_id, device_name, device_type, last_login_ip, "
                            + "last_login_time, active FROM auth_device WHERE device_id = ?",
                    (rs, rowNum) -> new Object[]{
                            rs.getString("device_id"),
                            rs.getString("user_id"),
                            rs.getString("tenant_id"),
                            rs.getString("device_name"),
                            rs.getString("device_type"),
                            rs.getString("last_login_ip"),
                            rs.getTimestamp("last_login_time"),
                            rs.getBoolean("active")
                    },
                    "device-complete"
            );

            assertThat(record).isNotNull();
            assertThat(record[0]).isEqualTo("device-complete");
            assertThat(record[1]).isEqualTo("user-complete");
            assertThat(record[2]).isEqualTo("tenant-001");
            assertThat(record[3]).isEqualTo("Test Device");
            assertThat(record[4]).isEqualTo("PC");
            assertThat(record[5]).isEqualTo("192.168.1.1");
            assertThat(record[6]).isNotNull();
            assertThat(record[7]).isEqualTo(true);
        }

        @Test
        @DisplayName("应该支持更新已存在的设备信息")
        void shouldUpdateExistingDevice() {
            // given
            DeviceInfo deviceInfo = createDevice("device-update", "user-update");
            storage.save(deviceInfo);

            // when - 更新设备信息
            DeviceInfo updatedInfo = createFullDevice("device-update", "user-update");
            storage.save(updatedInfo);

            // then - 应该只有一条记录，且信息已更新
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_device WHERE device_id = ?",
                    Integer.class,
                    "device-update"
            );
            assertThat(count).isEqualTo(1);

            String deviceName = jdbcTemplate.queryForObject(
                    "SELECT device_name FROM auth_device WHERE device_id = ?",
                    String.class,
                    "device-update"
            );
            assertThat(deviceName).isEqualTo("Test Device");
        }

        @Test
        @DisplayName("应该允许 nullable 字段为 null")
        void shouldAllowNullableFields() {
            // given
            DeviceInfo deviceInfo = new DeviceInfo();
            deviceInfo.setDeviceId("device-null");
            deviceInfo.setUserId("user-null");
            // tenantId, deviceName, deviceType, lastLoginIp, lastLoginTime 都为 null

            // when
            storage.save(deviceInfo);

            // then
            var record = jdbcTemplate.queryForObject(
                    "SELECT tenant_id, device_name, device_type, last_login_ip, last_login_time "
                            + "FROM auth_device WHERE device_id = ?",
                    (rs, rowNum) -> new Object[]{
                            rs.getString("tenant_id"),
                            rs.getString("device_name"),
                            rs.getString("device_type"),
                            rs.getString("last_login_ip"),
                            rs.getTimestamp("last_login_time")
                    },
                    "device-null"
            );

            assertThat(record).isNotNull();
            assertThat(record[0]).isNull();
            assertThat(record[1]).isNull();
            assertThat(record[2]).isNull();
            assertThat(record[3]).isNull();
            assertThat(record[4]).isNull();
        }

        @Test
        @DisplayName("应该正确保存 active 状态")
        void shouldSaveActiveStatus() {
            // given
            DeviceInfo activeDevice = createDevice("device-active", "user-active");
            activeDevice.setActive(true);

            DeviceInfo inactiveDevice = createDevice("device-inactive", "user-inactive");
            inactiveDevice.setActive(false);

            // when
            storage.save(activeDevice);
            storage.save(inactiveDevice);

            // then
            Boolean activeStatus = jdbcTemplate.queryForObject(
                    "SELECT active FROM auth_device WHERE device_id = ?",
                    Boolean.class,
                    "device-active"
            );
            assertThat(activeStatus).isTrue();

            Boolean inactiveStatus = jdbcTemplate.queryForObject(
                    "SELECT active FROM auth_device WHERE device_id = ?",
                    Boolean.class,
                    "device-inactive"
            );
            assertThat(inactiveStatus).isFalse();
        }
    }

    @Nested
    @DisplayName("findById 方法测试")
    class FindByIdTests {

        @Test
        @DisplayName("应该根据设备 ID 查找设备")
        void shouldFindById() {
            // given
            DeviceInfo deviceInfo = createFullDevice("device-find", "user-find");
            storage.save(deviceInfo);

            // when
            Optional<DeviceInfo> result = storage.findById("device-find");

            // then
            assertThat(result).isPresent();
            DeviceInfo found = result.get();
            assertThat(found.getDeviceId()).isEqualTo("device-find");
            assertThat(found.getUserId()).isEqualTo("user-find");
            assertThat(found.getDeviceName()).isEqualTo("Test Device");
        }

        @Test
        @DisplayName("当设备不存在时应该返回空")
        void shouldReturnEmptyWhenNotFound() {
            // when
            Optional<DeviceInfo> result = storage.findById("device-nonexistent");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("应该正确映射所有字段")
        void shouldMapAllFields() {
            // given
            Instant loginTime = Instant.now();
            DeviceInfo deviceInfo = new DeviceInfo();
            deviceInfo.setDeviceId("device-map");
            deviceInfo.setUserId("user-map");
            deviceInfo.setTenantId("tenant-map");
            deviceInfo.setDeviceName("Map Device");
            deviceInfo.setDeviceType("Mobile");
            deviceInfo.setLastLoginIp("10.0.0.1");
            deviceInfo.setLastLoginTime(loginTime);
            deviceInfo.setActive(false);
            storage.save(deviceInfo);

            // when
            Optional<DeviceInfo> result = storage.findById("device-map");

            // then
            assertThat(result).isPresent();
            DeviceInfo found = result.get();
            assertThat(found.getDeviceId()).isEqualTo("device-map");
            assertThat(found.getUserId()).isEqualTo("user-map");
            assertThat(found.getTenantId()).isEqualTo("tenant-map");
            assertThat(found.getDeviceName()).isEqualTo("Map Device");
            assertThat(found.getDeviceType()).isEqualTo("Mobile");
            assertThat(found.getLastLoginIp()).isEqualTo("10.0.0.1");
            assertThat(found.getLastLoginTime()).isNotNull();
            assertThat(found.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("findByUserId 方法测试")
    class FindByUserIdTests {

        @Test
        @DisplayName("应该查找用户的所有设备")
        void shouldFindByUserId() {
            // given
            String userId = "user-multi-device";
            for (int i = 0; i < 3; i++) {
                DeviceInfo deviceInfo = createDevice("device-user-" + i, userId);
                storage.save(deviceInfo);
            }

            // when
            List<DeviceInfo> result = storage.findByUserId(userId);

            // then
            assertThat(result).hasSize(3);
            assertThat(result).allMatch(d -> d.getUserId().equals(userId));
        }

        @Test
        @DisplayName("当用户没有设备时应该返回空列表")
        void shouldReturnEmptyListWhenNoDevices() {
            // when
            List<DeviceInfo> result = storage.findByUserId("user-no-device");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("应该只返回指定用户的设备")
        void shouldReturnOnlySpecifiedUserDevices() {
            // given
            String userId1 = "user-only-1";
            String userId2 = "user-only-2";

            for (int i = 0; i < 2; i++) {
                storage.save(createDevice("device-only-1-" + i, userId1));
            }
            for (int i = 0; i < 3; i++) {
                storage.save(createDevice("device-only-2-" + i, userId2));
            }

            // when
            List<DeviceInfo> result1 = storage.findByUserId(userId1);
            List<DeviceInfo> result2 = storage.findByUserId(userId2);

            // then
            assertThat(result1).hasSize(2);
            assertThat(result2).hasSize(3);
        }
    }

    @Nested
    @DisplayName("countActiveByUserId 方法测试")
    class CountActiveByUserIdTests {

        @Test
        @DisplayName("应该正确统计活跃设备数量")
        void shouldCountActiveDevices() {
            // given
            String userId = "user-count-active";

            DeviceInfo active1 = createDevice("device-active-1", userId);
            active1.setActive(true);
            DeviceInfo active2 = createDevice("device-active-2", userId);
            active2.setActive(true);
            DeviceInfo inactive = createDevice("device-inactive-1", userId);
            inactive.setActive(false);

            storage.save(active1);
            storage.save(active2);
            storage.save(inactive);

            // when
            int count = storage.countActiveByUserId(userId);

            // then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("当用户没有活跃设备时应该返回 0")
        void shouldReturnZeroWhenNoActiveDevices() {
            // given
            String userId = "user-no-active";

            DeviceInfo inactive = createDevice("device-no-active", userId);
            inactive.setActive(false);
            storage.save(inactive);

            // when
            int count = storage.countActiveByUserId(userId);

            // then
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("当用户不存在时应该返回 0")
        void shouldReturnZeroWhenUserNotFound() {
            // when
            int count = storage.countActiveByUserId("user-nonexistent-count");

            // then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("应该成功删除设备")
        void shouldDeleteDevice() {
            // given
            DeviceInfo deviceInfo = createDevice("device-delete", "user-delete");
            storage.save(deviceInfo);

            // when
            storage.delete("device-delete");

            // then
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_device WHERE device_id = ?",
                    Integer.class,
                    "device-delete"
            );
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("删除不存在的设备不应该抛出异常")
        void shouldNotThrowWhenDeletingNonExistentDevice() {
            // when & then
            assertThatCode(() -> storage.delete("device-nonexistent-delete"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("deleteByUserId 方法测试")
    class DeleteByUserIdTests {

        @Test
        @DisplayName("应该删除用户的所有设备")
        void shouldDeleteAllDevicesForUser() {
            // given
            String userId = "user-delete-all";
            for (int i = 0; i < 3; i++) {
                storage.save(createDevice("device-delete-all-" + i, userId));
            }

            // 保存另一个用户的设备
            storage.save(createDevice("device-other-user", "user-other"));

            // when
            storage.deleteByUserId(userId);

            // then
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_device WHERE user_id = ?",
                    Integer.class,
                    userId
            );
            assertThat(count).isZero();

            // 另一个用户的设备应该还在
            Integer otherCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_device WHERE user_id = ?",
                    Integer.class,
                    "user-other"
            );
            assertThat(otherCount).isEqualTo(1);
        }

        @Test
        @DisplayName("删除不存在的用户设备不应该抛出异常")
        void shouldNotThrowWhenDeletingNonExistentUser() {
            // when & then
            assertThatCode(() -> storage.deleteByUserId("user-nonexistent-delete-all"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("updateActiveStatus 方法测试")
    class UpdateActiveStatusTests {

        @Test
        @DisplayName("应该成功更新设备活跃状态")
        void shouldUpdateActiveStatus() {
            // given
            DeviceInfo deviceInfo = createDevice("device-status", "user-status");
            deviceInfo.setActive(true);
            storage.save(deviceInfo);

            // when
            storage.updateActiveStatus("device-status", false);

            // then
            Boolean active = jdbcTemplate.queryForObject(
                    "SELECT active FROM auth_device WHERE device_id = ?",
                    Boolean.class,
                    "device-status"
            );
            assertThat(active).isFalse();
        }

        @Test
        @DisplayName("应该支持从非活跃变为活跃")
        void shouldSupportInactiveToActive() {
            // given
            DeviceInfo deviceInfo = createDevice("device-reactivate", "user-reactivate");
            deviceInfo.setActive(false);
            storage.save(deviceInfo);

            // when
            storage.updateActiveStatus("device-reactivate", true);

            // then
            Boolean active = jdbcTemplate.queryForObject(
                    "SELECT active FROM auth_device WHERE device_id = ?",
                    Boolean.class,
                    "device-reactivate"
            );
            assertThat(active).isTrue();
        }

        @Test
        @DisplayName("更新不存在设备的状态不应该抛出异常")
        void shouldNotThrowWhenUpdatingNonExistentDevice() {
            // when & then
            assertThatCode(() -> storage.updateActiveStatus("device-nonexistent-status", true))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("自定义表名测试")
    class CustomTableNameTests {

        @Test
        @DisplayName("应该支持自定义表名")
        void shouldSupportCustomTableName() {
            // given
            String customTableName = "custom_device";
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + customTableName);
            jdbcTemplate.execute(String.format("""
                    CREATE TABLE %s (
                        device_id VARCHAR(128) PRIMARY KEY,
                        user_id VARCHAR(64) NOT NULL,
                        tenant_id VARCHAR(64),
                        device_name VARCHAR(256),
                        device_type VARCHAR(64),
                        last_login_ip VARCHAR(64),
                        last_login_time TIMESTAMP,
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP NOT NULL
                    )
                    """, customTableName));

            JdbcDeviceStorage customStorage = new JdbcDeviceStorage(jdbcTemplate, customTableName);

            DeviceInfo deviceInfo = createDevice("device-custom-table", "user-custom-table");

            // when
            customStorage.save(deviceInfo);

            // then
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + customTableName + " WHERE device_id = ?",
                    Integer.class,
                    "device-custom-table"
            );
            assertThat(count).isEqualTo(1);

            // cleanup
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + customTableName);
        }
    }

    @Nested
    @DisplayName("并发场景测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("应该支持多设备同时保存")
        void shouldSupportConcurrentSaves() {
            // given
            int deviceCount = 10;

            // when
            for (int i = 0; i < deviceCount; i++) {
                DeviceInfo deviceInfo = createDevice("device-concurrent-" + i, "user-concurrent-" + i);
                storage.save(deviceInfo);
            }

            // then
            Integer totalCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auth_device",
                    Integer.class
            );
            assertThat(totalCount).isEqualTo(deviceCount);
        }
    }

    @Nested
    @DisplayName("完整流程测试")
    class FullWorkflowTests {

        @Test
        @DisplayName("应该支持完整的设备生命周期")
        void shouldSupportFullLifecycle() {
            // given
            String deviceId = "device-lifecycle";
            String userId = "user-lifecycle";

            // when - 保存设备
            DeviceInfo deviceInfo = createFullDevice(deviceId, userId);
            storage.save(deviceInfo);

            // then - 可以找到
            Optional<DeviceInfo> found = storage.findById(deviceId);
            assertThat(found).isPresent();

            // when - 更新活跃状态
            storage.updateActiveStatus(deviceId, false);

            // then - 状态已更新
            found = storage.findById(deviceId);
            assertThat(found).isPresent();
            assertThat(found.get().isActive()).isFalse();

            // when - 删除设备
            storage.delete(deviceId);

            // then - 找不到
            found = storage.findById(deviceId);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("应该支持用户设备管理场景")
        void shouldSupportUserDeviceManagement() {
            // given
            String userId = "user-management";

            // 用户有多个设备
            for (int i = 0; i < 5; i++) {
                DeviceInfo deviceInfo = createDevice("device-mgmt-" + i, userId);
                deviceInfo.setActive(i < 3); // 前 3 个活跃
                storage.save(deviceInfo);
            }

            // when - 查询活跃设备数量
            int activeCount = storage.countActiveByUserId(userId);

            // then
            assertThat(activeCount).isEqualTo(3);

            // when - 查询所有设备
            List<DeviceInfo> allDevices = storage.findByUserId(userId);

            // then
            assertThat(allDevices).hasSize(5);

            // when - 删除所有设备
            storage.deleteByUserId(userId);

            // then
            assertThat(storage.findByUserId(userId)).isEmpty();
        }
    }
}
