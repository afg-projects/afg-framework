package io.github.afgprojects.framework.core.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * AuditLog 单元测试
 */
class AuditLogTest {

    @Test
    void should_createAuditLog_when_usingBuilder() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        AuditLog auditLog = AuditLog.successBuilder()
                .id("test-id")
                .userId(1L)
                .username("admin")
                .tenantId(100L)
                .operation("创建用户")
                .module("用户管理")
                .target("user-123")
                .args("{\"name\":\"张三\"}")
                .newValue("{\"id\":123,\"name\":\"张三\"}")
                .timestamp(now)
                .durationMs(100)
                .traceId("trace-123")
                .requestId("request-456")
                .clientIp("192.168.1.1")
                .className("UserService")
                .methodName("createUser")
                .build();

        // Then
        assertThat(auditLog.id()).isEqualTo("test-id");
        assertThat(auditLog.userId()).isEqualTo(1L);
        assertThat(auditLog.username()).isEqualTo("admin");
        assertThat(auditLog.tenantId()).isEqualTo(100L);
        assertThat(auditLog.operation()).isEqualTo("创建用户");
        assertThat(auditLog.module()).isEqualTo("用户管理");
        assertThat(auditLog.target()).isEqualTo("user-123");
        assertThat(auditLog.args()).isEqualTo("{\"name\":\"张三\"}");
        assertThat(auditLog.newValue()).isEqualTo("{\"id\":123,\"name\":\"张三\"}");
        assertThat(auditLog.result()).isEqualTo(AuditLog.Result.SUCCESS);
        assertThat(auditLog.errorMessage()).isNull();
        assertThat(auditLog.timestamp()).isEqualTo(now);
        assertThat(auditLog.durationMs()).isEqualTo(100);
        assertThat(auditLog.traceId()).isEqualTo("trace-123");
        assertThat(auditLog.requestId()).isEqualTo("request-456");
        assertThat(auditLog.clientIp()).isEqualTo("192.168.1.1");
        assertThat(auditLog.className()).isEqualTo("UserService");
        assertThat(auditLog.methodName()).isEqualTo("createUser");
    }

    @Test
    void should_createFailureLog_when_usingFailureBuilder() {
        // When
        AuditLog auditLog = AuditLog.failureBuilder()
                .id("test-id")
                .operation("删除用户")
                .module("用户管理")
                .errorMessage("用户不存在")
                .build();

        // Then
        assertThat(auditLog.result()).isEqualTo(AuditLog.Result.FAILURE);
        assertThat(auditLog.errorMessage()).isEqualTo("用户不存在");
    }

    @Test
    void should_useCurrentTimestamp_when_notProvided() {
        // When
        AuditLog auditLog = AuditLog.successBuilder()
                .id("test-id")
                .operation("test")
                .module("test")
                .build();

        // Then
        assertThat(auditLog.timestamp()).isNotNull();
        assertThat(auditLog.timestamp()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void should_handleNullableFields() {
        // When
        AuditLog auditLog = AuditLog.successBuilder()
                .id("test-id")
                .operation("test")
                .module("test")
                .build();

        // Then
        assertThat(auditLog.userId()).isNull();
        assertThat(auditLog.username()).isNull();
        assertThat(auditLog.tenantId()).isNull();
        assertThat(auditLog.target()).isNull();
        assertThat(auditLog.args()).isNull();
        assertThat(auditLog.oldValue()).isNull();
        assertThat(auditLog.newValue()).isNull();
        assertThat(auditLog.errorMessage()).isNull();
        assertThat(auditLog.traceId()).isNull();
        assertThat(auditLog.requestId()).isNull();
        assertThat(auditLog.clientIp()).isNull();
    }
}
