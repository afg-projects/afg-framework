package io.github.afgprojects.framework.data.core.entity;

import io.github.afgprojects.framework.data.core.tenant.TenantAware;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantEntity 测试
 */
@DisplayName("TenantEntity 测试")
class TenantEntityTest {

    @Nested
    @DisplayName("租户 ID 初始值测试")
    class InitialTenantIdTests {

        @Test
        @DisplayName("新建实体租户 ID 应该为 null")
        void newEntityShouldHaveNullTenantId() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity.getTenantId()).isNull();
        }
    }

    @Nested
    @DisplayName("租户 ID 操作测试")
    class TenantIdOperationTests {

        @Test
        @DisplayName("setTenantId 应该正确设置租户 ID")
        void shouldSetTenantIdCorrectly() {
            // Given
            TestEntity entity = new TestEntity();
            String tenantId = "tenant-001";

            // When
            entity.setTenantId(tenantId);

            // Then
            assertThat(entity.getTenantId()).isEqualTo(tenantId);
        }

        @Test
        @DisplayName("应该支持修改租户 ID")
        void shouldSupportChangingTenantId() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setTenantId("tenant-001");

            // When
            entity.setTenantId("tenant-002");

            // Then
            assertThat(entity.getTenantId()).isEqualTo("tenant-002");
        }

        @Test
        @DisplayName("应该支持设置为 null")
        void shouldSupportNullTenantId() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setTenantId("tenant-001");

            // When
            entity.setTenantId(null);

            // Then
            assertThat(entity.getTenantId()).isNull();
        }
    }

    @Nested
    @DisplayName("TenantAware 接口测试")
    class TenantAwareInterfaceTests {

        @Test
        @DisplayName("应该实现 TenantAware 接口")
        void shouldImplementTenantAwareInterface() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity).isInstanceOf(TenantAware.class);
        }

        @Test
        @DisplayName("通过接口操作应该有效")
        void operationsViaInterfaceShouldWork() {
            // Given
            TenantAware entity = new TestEntity();
            String tenantId = "tenant-003";

            // When
            entity.setTenantId(tenantId);

            // Then
            assertThat(entity.getTenantId()).isEqualTo(tenantId);
        }
    }

    @Nested
    @DisplayName("BaseEntity 继承测试")
    class BaseEntityInheritanceTests {

        @Test
        @DisplayName("应该继承 BaseEntity")
        void shouldExtendBaseEntity() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity).isInstanceOf(BaseEntity.class);
        }

        @Test
        @DisplayName("应该继承 id 字段")
        void shouldInheritIdField() {
            // Given
            TestEntity entity = new TestEntity();
            Long id = 789L;

            // When
            entity.setId(id);

            // Then
            assertThat(entity.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("应该继承时间戳字段")
        void shouldInheritTimestampFields() {
            // Given
            TestEntity entity = new TestEntity();
            LocalDateTime now = LocalDateTime.now();

            // When
            entity.setCreateTime(now);
            entity.setUpdateTime(now.plusHours(3));

            // Then
            assertThat(entity.getCreateTime()).isEqualTo(now);
            assertThat(entity.getUpdateTime()).isEqualTo(now.plusHours(3));
        }

        @Test
        @DisplayName("isNew 方法应该正确工作")
        void isNewShouldWorkCorrectly() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity.isNew()).isTrue();
            entity.setId(1L);
            assertThat(entity.isNew()).isFalse();
        }
    }

    @Nested
    @DisplayName("toString 测试")
    class ToStringTests {

        @Test
        @DisplayName("toString 应该包含类名")
        void toStringShouldContainClassName() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            String result = entity.toString();

            // Then
            assertThat(result).contains("TestEntity");
        }

        @Test
        @DisplayName("toString 应该包含 id 和 tenantId")
        void toStringShouldContainIdAndTenantId() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(42L);
            entity.setTenantId("tenant-abc");

            // When
            String result = entity.toString();

            // Then
            assertThat(result).contains("id=42");
            assertThat(result).contains("tenantId='tenant-abc'");
        }

        @Test
        @DisplayName("tenantId 为 null 时 toString 应该显示 null")
        void toStringShouldShowNullTenantId() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);

            // When
            String result = entity.toString();

            // Then
            assertThat(result).contains("tenantId='null'");
        }

        @Test
        @DisplayName("toString 格式应该正确")
        void toStringFormatShouldBeCorrect() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setTenantId("t001");

            // When
            String result = entity.toString();

            // Then
            assertThat(result).isEqualTo("TestEntity{id=1, tenantId='t001'}");
        }
    }

    @Nested
    @DisplayName("多租户场景测试")
    class MultiTenantScenarioTests {

        @Test
        @DisplayName("不同租户的实体应该有不同租户 ID")
        void entitiesFromDifferentTenantsShouldHaveDifferentTenantIds() {
            // Given
            TestEntity entity1 = new TestEntity();
            entity1.setTenantId("tenant-001");
            entity1.setId(1L);

            TestEntity entity2 = new TestEntity();
            entity2.setTenantId("tenant-002");
            entity2.setId(1L); // 相同 ID，不同租户

            // When & Then
            assertThat(entity1.getTenantId()).isNotEqualTo(entity2.getTenantId());
            assertThat(entity1.getId()).isEqualTo(entity2.getId()); // ID 相同但租户不同
        }

        @Test
        @DisplayName("租户隔离：查询时应该按租户过滤")
        void tenantIsolationShouldFilterByTenant() {
            // Given
            TestEntity[] entities = new TestEntity[5];
            for (int i = 0; i < 5; i++) {
                entities[i] = new TestEntity();
                entities[i].setId((long) i);
                entities[i].setTenantId(i < 3 ? "tenant-001" : "tenant-002");
            }

            // When
            long tenant001Count = java.util.Arrays.stream(entities)
                    .filter(e -> "tenant-001".equals(e.getTenantId()))
                    .count();
            long tenant002Count = java.util.Arrays.stream(entities)
                    .filter(e -> "tenant-002".equals(e.getTenantId()))
                    .count();

            // Then
            assertThat(tenant001Count).isEqualTo(3);
            assertThat(tenant002Count).isEqualTo(2);
        }

        @Test
        @DisplayName("租户 ID 应该不影响实体其他属性")
        void tenantIdShouldNotAffectOtherProperties() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(100L);
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());

            // When
            entity.setTenantId("tenant-xyz");

            // Then - 其他属性应该保持不变
            assertThat(entity.getId()).isEqualTo(100L);
            assertThat(entity.getCreateTime()).isNotNull();
            assertThat(entity.getUpdateTime()).isNotNull();
            assertThat(entity.getTenantId()).isEqualTo("tenant-xyz");
        }
    }

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("租户 ID 应该支持空字符串")
        void shouldSupportEmptyStringTenantId() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.setTenantId("");

            // Then
            assertThat(entity.getTenantId()).isEmpty();
        }

        @Test
        @DisplayName("租户 ID 应该支持长字符串")
        void shouldSupportLongTenantId() {
            // Given
            TestEntity entity = new TestEntity();
            String longTenantId = "tenant-" + "x".repeat(100);

            // When
            entity.setTenantId(longTenantId);

            // Then
            assertThat(entity.getTenantId()).isEqualTo(longTenantId);
            assertThat(entity.getTenantId()).hasSize(107);
        }

        @Test
        @DisplayName("租户 ID 应该支持特殊字符")
        void shouldSupportSpecialCharactersInTenantId() {
            // Given
            TestEntity entity = new TestEntity();
            String specialTenantId = "tenant-001_测试@特殊!字符#";

            // When
            entity.setTenantId(specialTenantId);

            // Then
            assertThat(entity.getTenantId()).isEqualTo(specialTenantId);
        }

        @Test
        @DisplayName("租户 ID 应该支持 UUID 格式")
        void shouldSupportUuidFormatTenantId() {
            // Given
            TestEntity entity = new TestEntity();
            String uuidTenantId = "550e8400-e29b-41d4-a716-446655440000";

            // When
            entity.setTenantId(uuidTenantId);

            // Then
            assertThat(entity.getTenantId()).isEqualTo(uuidTenantId);
        }
    }

    /**
     * 测试实体类
     */
    static class TestEntity extends TenantEntity<Long> {
        // 用于测试的简单实体类
    }
}