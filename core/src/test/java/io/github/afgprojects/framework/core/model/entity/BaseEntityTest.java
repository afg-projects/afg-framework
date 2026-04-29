package io.github.afgprojects.framework.core.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BaseEntity 测试
 */
@DisplayName("BaseEntity 测试")
class BaseEntityTest {

    @Test
    @DisplayName("应该正确创建 BaseEntity")
    void shouldCreateBaseEntity() {
        TestEntity entity = new TestEntity();
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    @DisplayName("应该正确设置和获取 ID")
    void shouldSetAndGetId() {
        TestEntity entity = new TestEntity();
        entity.setId("test-id");
        assertEquals("test-id", entity.getId());
    }

    @Test
    @DisplayName("应该正确设置和获取 createdAt")
    void shouldSetAndGetCreatedAt() {
        TestEntity entity = new TestEntity();
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        assertEquals(now, entity.getCreatedAt());
    }

    @Test
    @DisplayName("应该正确设置和获取 updatedAt")
    void shouldSetAndGetUpdatedAt() {
        TestEntity entity = new TestEntity();
        LocalDateTime now = LocalDateTime.now();
        entity.setUpdatedAt(now);
        assertEquals(now, entity.getUpdatedAt());
    }

    @Test
    @DisplayName("createdAt 和 updatedAt 初始化时应该接近当前时间")
    void createdAtAndUpdatedAtShouldBeCloseToNow() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        TestEntity entity = new TestEntity();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertTrue(entity.getCreatedAt().isAfter(before) || entity.getCreatedAt().isEqual(before));
        assertTrue(entity.getCreatedAt().isBefore(after) || entity.getCreatedAt().isEqual(after));
        assertTrue(entity.getUpdatedAt().isAfter(before) || entity.getUpdatedAt().isEqual(before));
        assertTrue(entity.getUpdatedAt().isBefore(after) || entity.getUpdatedAt().isEqual(after));
    }

    /**
     * 测试用具体实体类
     */
    static class TestEntity extends BaseEntity {
        // 简单的测试实现
    }
}
