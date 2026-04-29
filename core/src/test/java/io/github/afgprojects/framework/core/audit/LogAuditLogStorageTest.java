package io.github.afgprojects.framework.core.audit;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * LogAuditLogStorage 单元测试
 */
class LogAuditLogStorageTest {

    private LogAuditLogStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LogAuditLogStorage();
    }

    @Test
    void should_logAuditLog_when_saveCalled() {
        // Given
        AuditLog auditLog = AuditLog.successBuilder()
                .id("test-id")
                .userId(1L)
                .username("admin")
                .operation("创建用户")
                .module("用户管理")
                .timestamp(LocalDateTime.now())
                .durationMs(100)
                .className("UserService")
                .methodName("createUser")
                .build();

        // When & Then - should not throw
        assertThatCode(() -> storage.save(auditLog)).doesNotThrowAnyException();
    }

    @Test
    void should_handleNullFields_when_logging() {
        // Given
        AuditLog auditLog = AuditLog.successBuilder()
                .id("test-id")
                .operation("test")
                .module("test")
                .timestamp(LocalDateTime.now())
                .build();

        // When & Then - should not throw
        assertThatCode(() -> storage.save(auditLog)).doesNotThrowAnyException();
    }
}
