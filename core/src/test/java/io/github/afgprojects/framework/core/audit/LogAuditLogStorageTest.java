package io.github.afgprojects.framework.core.audit;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * LogAuditLogStorage 单元测试。
 * <p>
 * 测试基于日志输出的审计日志存储实现。
 *
 * @see LogAuditLogStorage
 */
class LogAuditLogStorageTest {

    private LogAuditLogStorage storage;

    /**
     * 初始化测试用的存储实例。
     */
    @BeforeEach
    void setUp() {
        storage = new LogAuditLogStorage();
    }

    /**
     * 测试调用 save 方法时是否正常输出日志。
     */
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

    /**
     * 测试处理包含 null 字段的审计日志时是否正常输出。
     */
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
