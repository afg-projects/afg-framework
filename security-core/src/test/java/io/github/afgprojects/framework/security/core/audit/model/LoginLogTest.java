package io.github.afgprojects.framework.security.core.audit.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * LoginLog 实体测试。
 *
 * @author afg-projects
 * @since 1.0.0
 */
class LoginLogTest {

    @Nested
    @DisplayName("常量测试")
    class ConstantsTests {

        @Test
        @DisplayName("应定义成功常量")
        void shouldDefineSuccessConstant() {
            assertThat(LoginLog.SUCCESS).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("应定义失败常量")
        void shouldDefineFailureConstant() {
            assertThat(LoginLog.FAILURE).isEqualTo("FAILURE");
        }
    }

    @Nested
    @DisplayName("记录字段测试")
    class RecordFieldsTests {

        @Test
        @DisplayName("应包含所有必要字段")
        void shouldContainAllRequiredFields() {
            Instant now = Instant.now();
            LoginLog log = new LoginLog(
                    "log-001",           // id
                    "user-001",          // userId
                    "admin",             // username
                    "tenant-001",        // tenantId
                    "192.168.1.100",     // ip
                    "device-123",        // deviceId
                    "iPhone 15",         // deviceName
                    "Chrome 120",        // browser
                    "macOS 14",          // os
                    "北京市",             // location
                    LoginLog.SUCCESS,    // result
                    null,                // failReason
                    now,                 // loginTime
                    null                 // logoutTime
            );

            assertThat(log.id()).isEqualTo("log-001");
            assertThat(log.userId()).isEqualTo("user-001");
            assertThat(log.username()).isEqualTo("admin");
            assertThat(log.tenantId()).isEqualTo("tenant-001");
            assertThat(log.ip()).isEqualTo("192.168.1.100");
            assertThat(log.deviceId()).isEqualTo("device-123");
            assertThat(log.deviceName()).isEqualTo("iPhone 15");
            assertThat(log.browser()).isEqualTo("Chrome 120");
            assertThat(log.os()).isEqualTo("macOS 14");
            assertThat(log.location()).isEqualTo("北京市");
            assertThat(log.result()).isEqualTo(LoginLog.SUCCESS);
            assertThat(log.failReason()).isNull();
            assertThat(log.loginTime()).isEqualTo(now);
            assertThat(log.logoutTime()).isNull();
        }
    }

    @Nested
    @DisplayName("success 工厂方法测试")
    class SuccessFactoryMethodTests {

        @Test
        @DisplayName("应创建成功登录日志")
        void shouldCreateSuccessLoginLog() {
            Instant before = Instant.now();
            LoginLog log = LoginLog.success(
                    "user-001",
                    "admin",
                    "tenant-001",
                    "192.168.1.100",
                    "device-123",
                    "iPhone 15",
                    "Chrome 120",
                    "macOS 14",
                    "北京市"
            );
            Instant after = Instant.now();

            assertThat(log.id()).isNull(); // ID 由持久层生成
            assertThat(log.userId()).isEqualTo("user-001");
            assertThat(log.username()).isEqualTo("admin");
            assertThat(log.tenantId()).isEqualTo("tenant-001");
            assertThat(log.ip()).isEqualTo("192.168.1.100");
            assertThat(log.deviceId()).isEqualTo("device-123");
            assertThat(log.deviceName()).isEqualTo("iPhone 15");
            assertThat(log.browser()).isEqualTo("Chrome 120");
            assertThat(log.os()).isEqualTo("macOS 14");
            assertThat(log.location()).isEqualTo("北京市");
            assertThat(log.result()).isEqualTo(LoginLog.SUCCESS);
            assertThat(log.failReason()).isNull();
            assertThat(log.loginTime()).isBetween(before, after);
            assertThat(log.logoutTime()).isNull();
        }

        @Test
        @DisplayName("成功日志应支持可选字段为 null")
        void shouldSupportNullOptionalFieldsForSuccess() {
            LoginLog log = LoginLog.success(
                    "user-001",
                    "admin",
                    null,  // tenantId
                    "192.168.1.100",
                    null,  // deviceId
                    null,  // deviceName
                    null,  // browser
                    null,  // os
                    null   // location
            );

            assertThat(log.tenantId()).isNull();
            assertThat(log.deviceId()).isNull();
            assertThat(log.deviceName()).isNull();
            assertThat(log.browser()).isNull();
            assertThat(log.os()).isNull();
            assertThat(log.location()).isNull();
        }
    }

    @Nested
    @DisplayName("failure 工厂方法测试")
    class FailureFactoryMethodTests {

        @Test
        @DisplayName("应创建失败登录日志")
        void shouldCreateFailureLoginLog() {
            Instant before = Instant.now();
            LoginLog log = LoginLog.failure(
                    "admin",
                    "tenant-001",
                    "192.168.1.100",
                    "device-123",
                    "iPhone 15",
                    "Chrome 120",
                    "macOS 14",
                    "北京市",
                    "密码错误"
            );
            Instant after = Instant.now();

            assertThat(log.id()).isNull(); // ID 由持久层生成
            assertThat(log.userId()).isNull(); // 失败时可能没有 userId
            assertThat(log.username()).isEqualTo("admin");
            assertThat(log.tenantId()).isEqualTo("tenant-001");
            assertThat(log.ip()).isEqualTo("192.168.1.100");
            assertThat(log.deviceId()).isEqualTo("device-123");
            assertThat(log.deviceName()).isEqualTo("iPhone 15");
            assertThat(log.browser()).isEqualTo("Chrome 120");
            assertThat(log.os()).isEqualTo("macOS 14");
            assertThat(log.location()).isEqualTo("北京市");
            assertThat(log.result()).isEqualTo(LoginLog.FAILURE);
            assertThat(log.failReason()).isEqualTo("密码错误");
            assertThat(log.loginTime()).isBetween(before, after);
            assertThat(log.logoutTime()).isNull();
        }

        @Test
        @DisplayName("失败日志应支持可选字段为 null")
        void shouldSupportNullOptionalFieldsForFailure() {
            LoginLog log = LoginLog.failure(
                    "admin",
                    null,  // tenantId
                    "192.168.1.100",
                    null,  // deviceId
                    null,  // deviceName
                    null,  // browser
                    null,  // os
                    null,  // location
                    "用户不存在"
            );

            assertThat(log.tenantId()).isNull();
            assertThat(log.deviceId()).isNull();
            assertThat(log.deviceName()).isNull();
            assertThat(log.browser()).isNull();
            assertThat(log.os()).isNull();
            assertThat(log.location()).isNull();
        }
    }

    @Nested
    @DisplayName("空值安全测试")
    class NullSafetyTests {

        @Test
        @DisplayName("应支持 userId 为 null")
        void shouldSupportNullUserId() {
            LoginLog log = LoginLog.failure(
                    "unknown",
                    null,
                    "192.168.1.100",
                    null,
                    null,
                    null,
                    null,
                    null,
                    "用户不存在"
            );

            assertThat(log.userId()).isNull();
        }

        @Test
        @DisplayName("应支持 tenantId 为 null")
        void shouldSupportNullTenantId() {
            LoginLog log = LoginLog.success(
                    "user-001",
                    "admin",
                    null,
                    "192.168.1.100",
                    null,
                    null,
                    null,
                    null,
                    null
            );

            assertThat(log.tenantId()).isNull();
        }

        @Test
        @DisplayName("应支持设备信息为 null")
        void shouldSupportNullDeviceInfo() {
            LoginLog log = LoginLog.success(
                    "user-001",
                    "admin",
                    "tenant-001",
                    "192.168.1.100",
                    null,
                    null,
                    null,
                    null,
                    null
            );

            assertThat(log.deviceId()).isNull();
            assertThat(log.deviceName()).isNull();
            assertThat(log.browser()).isNull();
            assertThat(log.os()).isNull();
        }

        @Test
        @DisplayName("应支持 location 为 null")
        void shouldSupportNullLocation() {
            LoginLog log = LoginLog.success(
                    "user-001",
                    "admin",
                    "tenant-001",
                    "192.168.1.100",
                    null,
                    null,
                    null,
                    null,
                    null
            );

            assertThat(log.location()).isNull();
        }

        @Test
        @DisplayName("应支持 failReason 为 null")
        void shouldSupportNullFailReason() {
            LoginLog log = LoginLog.success(
                    "user-001",
                    "admin",
                    "tenant-001",
                    "192.168.1.100",
                    null,
                    null,
                    null,
                    null,
                    null
            );

            assertThat(log.failReason()).isNull();
        }

        @Test
        @DisplayName("应支持 logoutTime 为 null")
        void shouldSupportNullLogoutTime() {
            LoginLog log = LoginLog.success(
                    "user-001",
                    "admin",
                    "tenant-001",
                    "192.168.1.100",
                    null,
                    null,
                    null,
                    null,
                    null
            );

            assertThat(log.logoutTime()).isNull();
        }
    }
}
