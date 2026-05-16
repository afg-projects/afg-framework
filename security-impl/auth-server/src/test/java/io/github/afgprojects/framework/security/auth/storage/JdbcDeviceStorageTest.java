package io.github.afgprojects.framework.security.auth.storage;

import io.github.afgprojects.framework.data.jdbc.JdbcDataManager;
import io.github.afgprojects.framework.security.auth.entity.AuthUserDevice;
import io.github.afgprojects.framework.security.core.security.model.DeviceInfo;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

    private JdbcDataManager dataManager;
    private JdbcDeviceStorage storage;
    private JdbcDataSource dataSource;

    @BeforeEach
    void setUp() {
        // 创建 H2 数据源
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb_device;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        // 创建 DataManager
        dataManager = new JdbcDataManager(dataSource);

        // 创建表
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS auth_user_device");
            stmt.execute("""
                CREATE TABLE auth_user_device (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    device_id VARCHAR(128) NOT NULL UNIQUE,
                    user_id VARCHAR(64) NOT NULL,
                    tenant_id VARCHAR(64),
                    device_name VARCHAR(256),
                    device_type VARCHAR(64),
                    device_os VARCHAR(64),
                    browser VARCHAR(64),
                    last_login_ip VARCHAR(64),
                    last_login_time TIMESTAMP,
                    first_login_time TIMESTAMP,
                    is_active BOOLEAN NOT NULL DEFAULT TRUE,
                    is_trusted BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP
                )
                """);
            stmt.execute("CREATE INDEX idx_user_id_device ON auth_user_device (user_id)");
            stmt.execute("CREATE INDEX idx_active_device ON auth_user_device (is_active)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 创建存储器
        storage = new JdbcDeviceStorage(dataManager);
    }

    @AfterEach
    void tearDown() {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS auth_user_device");
        } catch (Exception e) {
            // ignore
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
            var entity = dataManager.entity(AuthUserDevice.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("device_id", "device-123")
                            .build())
                    .one();
            assertThat(entity).isPresent();
        }

        @Test
        @DisplayName("应该保存所有字段的设备信息")
        void shouldSaveAllFields() {
            // given
            DeviceInfo deviceInfo = createFullDevice("device-complete", "user-complete");

            // when
            storage.save(deviceInfo);

            // then
            var entity = dataManager.entity(AuthUserDevice.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("device_id", "device-complete")
                            .build())
                    .one();

            assertThat(entity).isPresent();
            assertThat(entity.get().getUserId()).isEqualTo("user-complete");
            assertThat(entity.get().getTenantId()).isEqualTo("tenant-001");
            assertThat(entity.get().getDeviceName()).isEqualTo("Test Device");
            assertThat(entity.get().getDeviceType()).isEqualTo("PC");
            assertThat(entity.get().getLastLoginIp()).isEqualTo("192.168.1.1");
            assertThat(entity.get().isActive()).isTrue();
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
            var entities = dataManager.entity(AuthUserDevice.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("device_id", "device-update")
                            .build())
                    .list();
            assertThat(entities).hasSize(1);
            assertThat(entities.get(0).getDeviceName()).isEqualTo("Test Device");
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
            var entity = dataManager.entity(AuthUserDevice.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("device_id", "device-delete")
                            .build())
                    .one();
            assertThat(entity).isEmpty();
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
            var entities = dataManager.entity(AuthUserDevice.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("user_id", userId)
                            .build())
                    .list();
            assertThat(entities).isEmpty();

            // 另一个用户的设备应该还在
            var otherEntities = dataManager.entity(AuthUserDevice.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("user_id", "user-other")
                            .build())
                    .list();
            assertThat(otherEntities).hasSize(1);
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
            var entity = dataManager.entity(AuthUserDevice.class)
                    .query()
                    .where(io.github.afgprojects.framework.data.core.condition.Conditions.builder()
                            .eq("device_id", "device-status")
                            .build())
                    .one();
            assertThat(entity).isPresent();
            assertThat(entity.get().isActive()).isFalse();
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
    }
}