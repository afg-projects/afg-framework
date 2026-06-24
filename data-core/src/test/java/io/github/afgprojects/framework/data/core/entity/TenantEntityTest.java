package io.github.afgprojects.framework.data.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantEntity 测试
 */
@DisplayName("TenantEntity 测试")
class TenantEntityTest {

    @Nested
    @DisplayName("tenantId getter/setter")
    class TenantIdGetterSetterTests {

        @Test
        @DisplayName("应能设置和获取 tenantId")
        void shouldSetAndGetTenantId() {
            TenantEntity entity = new TenantEntity();
            entity.setTenantId("tenant-001");
            assertThat(entity.getTenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("tenantId 默认为 null")
        void shouldDefaultToNull_whenNewEntity() {
            TenantEntity entity = new TenantEntity();
            assertThat(entity.getTenantId()).isNull();
        }
    }

    @Nested
    @DisplayName("继承 BaseEntity 的字段")
    class InheritedFieldsTests {

        @Test
        @DisplayName("应能设置和获取 id")
        void shouldSetAndGetId() {
            TenantEntity entity = new TenantEntity();
            entity.setId("1");
            assertThat(entity.getId()).isEqualTo("1");
        }

        @Test
        @DisplayName("应能设置和获取 createdAt")
        void shouldSetAndGetCreatedAt() {
            TenantEntity entity = new TenantEntity();
            Instant now = Instant.now();
            entity.setCreatedAt(now);
            assertThat(entity.getCreatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("应能设置和获取 updatedAt")
        void shouldSetAndGetUpdatedAt() {
            TenantEntity entity = new TenantEntity();
            Instant now = Instant.now();
            entity.setUpdatedAt(now);
            assertThat(entity.getUpdatedAt()).isEqualTo(now);
        }
    }
}
