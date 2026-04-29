package io.github.afgprojects.framework.integration.redis.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;

import io.github.afgprojects.framework.core.audit.AuditLog;
import io.github.afgprojects.framework.core.audit.AuditLogProperties;

/**
 * RedisAuditLogStorage 单元测试
 */
@ExtendWith(MockitoExtension.class)
class RedisAuditLogStorageTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RList<String> rList;

    private AuditLogProperties properties;
    private RedisAuditLogStorage storage;

    @BeforeEach
    void setUp() {
        properties = new AuditLogProperties();
        properties.setMaxSize(1000);
        properties.setTtl(Duration.ofDays(7));
        properties.setMultiTenant(true);
    }

    @Test
    void should_saveToTenantKey_when_multiTenantEnabled() {
        // Given
        when(redissonClient.<String>getList(anyString())).thenReturn(rList);
        when(rList.size()).thenReturn(0);

        storage = new RedisAuditLogStorage(redissonClient, properties);

        AuditLog auditLog = createTestAuditLog(100L);

        // When
        storage.save(auditLog);

        // Then
        verify(redissonClient).getList("audit:log:100");
        verify(rList).addFirst(anyString());
    }

    @Test
    void should_saveToGlobalKey_when_multiTenantDisabled() {
        // Given
        properties.setMultiTenant(false);
        when(redissonClient.<String>getList(anyString())).thenReturn(rList);
        when(rList.size()).thenReturn(0);

        storage = new RedisAuditLogStorage(redissonClient, properties);

        AuditLog auditLog = createTestAuditLog(100L);

        // When
        storage.save(auditLog);

        // Then
        verify(redissonClient).getList("audit:log:global");
    }

    @Test
    void should_saveToGlobalKey_when_tenantIdIsNull() {
        // Given
        when(redissonClient.<String>getList(anyString())).thenReturn(rList);
        when(rList.size()).thenReturn(0);

        storage = new RedisAuditLogStorage(redissonClient, properties);

        AuditLog auditLog = createTestAuditLog(null);

        // When
        storage.save(auditLog);

        // Then
        verify(redissonClient).getList("audit:log:global");
    }

    @Test
    void should_removeOldest_when_exceedsMaxSize() {
        // Given
        properties.setMaxSize(2);
        when(redissonClient.<String>getList(anyString())).thenReturn(rList);
        when(rList.size()).thenReturn(3, 2);

        storage = new RedisAuditLogStorage(redissonClient, properties);

        AuditLog auditLog = createTestAuditLog(null);

        // When
        storage.save(auditLog);

        // Then
        verify(rList).removeLast();
    }

    @Test
    void should_setTtl_when_ttlConfigured() {
        // Given
        when(redissonClient.<String>getList(anyString())).thenReturn(rList);
        when(rList.size()).thenReturn(0);

        storage = new RedisAuditLogStorage(redissonClient, properties);

        AuditLog auditLog = createTestAuditLog(null);

        // When
        storage.save(auditLog);

        // Then
        verify(rList).expire(any(Duration.class));
    }

    @Test
    void should_notThrowException_when_saveFails() {
        // Given
        when(redissonClient.<String>getList(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        storage = new RedisAuditLogStorage(redissonClient, properties);

        AuditLog auditLog = createTestAuditLog(null);

        // When & Then - should not throw
        storage.save(auditLog);
    }

    private AuditLog createTestAuditLog(Long tenantId) {
        return AuditLog.successBuilder()
                .id("test-id")
                .userId(1L)
                .username("admin")
                .tenantId(tenantId)
                .operation("创建用户")
                .module("用户管理")
                .timestamp(LocalDateTime.now())
                .durationMs(100)
                .className("UserService")
                .methodName("createUser")
                .build();
    }
}
