package io.github.afgprojects.framework.core.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TenantEntity 测试
 */
@DisplayName("TenantEntity 测试")
class TenantEntityTest {

    @Test
    @DisplayName("应该正确创建 TenantEntity")
    void shouldCreateTenantEntity() {
        TestTenantEntity entity = new TestTenantEntity();
        // 继承自 BaseEntity，createdAt 和 updatedAt 应该被初始化
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    @DisplayName("应该正确设置和获取 tenantId")
    void shouldSetAndGetTenantId() {
        TestTenantEntity entity = new TestTenantEntity();
        entity.setTenantId("tenant-123");
        assertEquals("tenant-123", entity.getTenantId());
    }

    @Test
    @DisplayName("应该正确设置和获取 createdBy")
    void shouldSetAndGetCreatedBy() {
        TestTenantEntity entity = new TestTenantEntity();
        entity.setCreatedBy("user-001");
        assertEquals("user-001", entity.getCreatedBy());
    }

    @Test
    @DisplayName("应该正确设置和获取 updatedBy")
    void shouldSetAndGetUpdatedBy() {
        TestTenantEntity entity = new TestTenantEntity();
        entity.setUpdatedBy("user-002");
        assertEquals("user-002", entity.getUpdatedBy());
    }

    @Test
    @DisplayName("应该继承 BaseEntity 的属性")
    void shouldInheritBaseEntityProperties() {
        TestTenantEntity entity = new TestTenantEntity();
        entity.setId("entity-id");
        entity.setTenantId("tenant-123");
        entity.setCreatedBy("creator");
        entity.setUpdatedBy("updater");

        assertEquals("entity-id", entity.getId());
        assertEquals("tenant-123", entity.getTenantId());
        assertEquals("creator", entity.getCreatedBy());
        assertEquals("updater", entity.getUpdatedBy());
    }

    /**
     * 测试用具体租户实体类
     */
    static class TestTenantEntity extends TenantEntity {
        // 简单的测试实现
    }
}
