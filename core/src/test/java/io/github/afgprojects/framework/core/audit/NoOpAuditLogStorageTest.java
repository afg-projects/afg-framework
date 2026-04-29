package io.github.afgprojects.framework.core.audit;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * NoOpAuditLogStorage 单元测试
 */
class NoOpAuditLogStorageTest {

    private NoOpAuditLogStorage storage;

    @BeforeEach
    void setUp() {
        storage = new NoOpAuditLogStorage();
    }

    @Test
    void should_doNothing_when_saveCalled() {
        // 准备测试数据
        AuditLog auditLog = AuditLog.successBuilder()
                .id("test-id")
                .operation("创建用户")
                .module("用户管理")
                .timestamp(LocalDateTime.now())
                .build();

        // 执行并验证 - 不应抛出异常，不执行任何操作
        assertThatCode(() -> storage.save(auditLog)).doesNotThrowAnyException();
    }
}
