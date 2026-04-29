package io.github.afgprojects.framework.data.core.entity;

import io.github.afgprojects.framework.data.core.tenant.TenantAware;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FullEntity 测试
 */
@DisplayName("FullEntity 测试")
class FullEntityTest {

    @Nested
    @DisplayName("初始值测试")
    class InitialValueTests {

        @Test
        @DisplayName("新建实体所有字段应该有正确的初始值")
        void newEntityShouldHaveCorrectInitialValues() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity.getId()).isNull();
            assertThat(entity.getTenantId()).isNull();
            assertThat(entity.isDeleted()).isFalse();
            assertThat(entity.getVersion()).isEqualTo(0L);
            assertThat(entity.getCreateBy()).isNull();
            assertThat(entity.getUpdateBy()).isNull();
        }
    }

    @Nested
    @DisplayName("接口实现测试")
    class InterfaceImplementationTests {

        @Test
        @DisplayName("应该实现 TenantAware 接口")
        void shouldImplementTenantAware() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity).isInstanceOf(TenantAware.class);
        }

        @Test
        @DisplayName("应该实现 SoftDeletable 接口")
        void shouldImplementSoftDeletable() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity).isInstanceOf(SoftDeletable.class);
        }

        @Test
        @DisplayName("应该实现 Versioned 接口")
        void shouldImplementVersioned() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity).isInstanceOf(Versioned.class);
        }

        @Test
        @DisplayName("应该继承 BaseEntity")
        void shouldExtendBaseEntity() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity).isInstanceOf(BaseEntity.class);
        }
    }

    @Nested
    @DisplayName("租户功能测试")
    class TenantFunctionalityTests {

        @Test
        @DisplayName("应该正确设置和获取租户 ID")
        void shouldSetAndGetTenantId() {
            // Given
            TestEntity entity = new TestEntity();
            String tenantId = "tenant-001";

            // When
            entity.setTenantId(tenantId);

            // Then
            assertThat(entity.getTenantId()).isEqualTo(tenantId);
        }

        @Test
        @DisplayName("通过 TenantAware 接口操作应该有效")
        void operationsViaTenantAwareInterfaceShouldWork() {
            // Given
            TenantAware entity = new TestEntity();

            // When
            entity.setTenantId("tenant-002");

            // Then
            assertThat(entity.getTenantId()).isEqualTo("tenant-002");
        }
    }

    @Nested
    @DisplayName("软删除功能测试")
    class SoftDeleteFunctionalityTests {

        @Test
        @DisplayName("markDeleted 应该标记实体为已删除")
        void markDeletedShouldSetDeletedFlag() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.markDeleted();

            // Then
            assertThat(entity.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("restore 应该恢复已删除实体")
        void restoreShouldClearDeletedFlag() {
            // Given
            TestEntity entity = new TestEntity();
            entity.markDeleted();

            // When
            entity.restore();

            // Then
            assertThat(entity.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("通过 SoftDeletable 接口操作应该有效")
        void operationsViaSoftDeletableInterfaceShouldWork() {
            // Given
            SoftDeletable entity = new TestEntity();

            // When
            entity.setDeleted(true);

            // Then
            assertThat(entity.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("删除恢复循环应该正确工作")
        void deleteRestoreCycleShouldWork() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            entity.markDeleted();
            assertThat(entity.isDeleted()).isTrue();

            entity.restore();
            assertThat(entity.isDeleted()).isFalse();

            entity.markDeleted();
            assertThat(entity.isDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("版本号功能测试")
    class VersionFunctionalityTests {

        @Test
        @DisplayName("应该正确设置和获取版本号")
        void shouldSetAndGetVersion() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.setVersion(5L);

            // Then
            assertThat(entity.getVersion()).isEqualTo(5L);
        }

        @Test
        @DisplayName("incrementVersion 应该递增版本号")
        void shouldIncrementVersion() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setVersion(3L);

            // When
            entity.incrementVersion();

            // Then
            assertThat(entity.getVersion()).isEqualTo(4L);
        }

        @Test
        @DisplayName("通过 Versioned 接口操作应该有效")
        void operationsViaVersionedInterfaceShouldWork() {
            // Given
            Versioned entity = new TestEntity();

            // When
            entity.setVersion(10L);
            entity.incrementVersion();

            // Then
            assertThat(entity.getVersion()).isEqualTo(11L);
        }
    }

    @Nested
    @DisplayName("审计字段测试")
    class AuditFieldTests {

        @Test
        @DisplayName("应该正确设置和获取 createBy")
        void shouldSetAndGetCreateBy() {
            // Given
            TestEntity entity = new TestEntity();
            String createBy = "admin";

            // When
            entity.setCreateBy(createBy);

            // Then
            assertThat(entity.getCreateBy()).isEqualTo(createBy);
        }

        @Test
        @DisplayName("应该正确设置和获取 updateBy")
        void shouldSetAndGetUpdateBy() {
            // Given
            TestEntity entity = new TestEntity();
            String updateBy = "modifier";

            // When
            entity.setUpdateBy(updateBy);

            // Then
            assertThat(entity.getUpdateBy()).isEqualTo(updateBy);
        }

        @Test
        @DisplayName("审计字段应该支持 null")
        void shouldSupportNullAuditFields() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setCreateBy("user1");
            entity.setUpdateBy("user2");

            // When
            entity.setCreateBy(null);
            entity.setUpdateBy(null);

            // Then
            assertThat(entity.getCreateBy()).isNull();
            assertThat(entity.getUpdateBy()).isNull();
        }
    }

    @Nested
    @DisplayName("BaseEntity 字段测试")
    class BaseEntityFieldsTests {

        @Test
        @DisplayName("应该正确设置和获取 id")
        void shouldSetAndGetId() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.setId(42L);

            // Then
            assertThat(entity.getId()).isEqualTo(42L);
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

        @Test
        @DisplayName("应该正确设置和获取时间戳")
        void shouldSetAndGetTimestamps() {
            // Given
            TestEntity entity = new TestEntity();
            LocalDateTime now = LocalDateTime.now();

            // When
            entity.setCreateTime(now);
            entity.setUpdateTime(now.plusHours(1));

            // Then
            assertThat(entity.getCreateTime()).isEqualTo(now);
            assertThat(entity.getUpdateTime()).isEqualTo(now.plusHours(1));
        }
    }

    @Nested
    @DisplayName("toString 测试")
    class ToStringTests {

        @Test
        @DisplayName("toString 应该包含所有关键字段")
        void toStringShouldContainAllKeyFields() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(42L);
            entity.setTenantId("tenant-001");
            entity.markDeleted();
            entity.setVersion(3L);

            // When
            String result = entity.toString();

            // Then
            assertThat(result).contains("TestEntity");
            assertThat(result).contains("id=42");
            assertThat(result).contains("tenantId='tenant-001'");
            assertThat(result).contains("deleted=true");
            assertThat(result).contains("version=3");
        }

        @Test
        @DisplayName("toString 格式应该正确")
        void toStringFormatShouldBeCorrect() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setTenantId("t001");
            entity.setVersion(2L);

            // When
            String result = entity.toString();

            // Then
            assertThat(result).isEqualTo("TestEntity{id=1, tenantId='t001', deleted=false, version=2}");
        }

        @Test
        @DisplayName("null 字段应该在 toString 中正确显示")
        void nullFieldsShouldDisplayCorrectlyInToString() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            String result = entity.toString();

            // Then
            assertThat(result).contains("id=null");
            assertThat(result).contains("tenantId='null'");
        }
    }

    @Nested
    @DisplayName("综合业务场景测试")
    class ComprehensiveBusinessScenarioTests {

        @Test
        @DisplayName("完整实体创建流程")
        void completeEntityCreationFlow() {
            // Given
            TestEntity entity = new TestEntity();

            // When - 设置所有字段
            entity.setId(1L);
            entity.setTenantId("tenant-001");
            entity.setCreateBy("admin");
            entity.setCreateTime(LocalDateTime.now());
            entity.setVersion(0L);

            // Then
            assertThat(entity.getId()).isEqualTo(1L);
            assertThat(entity.getTenantId()).isEqualTo("tenant-001");
            assertThat(entity.getCreateBy()).isEqualTo("admin");
            assertThat(entity.isDeleted()).isFalse();
            assertThat(entity.getVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("完整实体更新流程")
        void completeEntityUpdateFlow() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setCreateBy("admin");
            entity.setVersion(0L);

            // When - 更新操作
            entity.setUpdateBy("modifier");
            entity.setUpdateTime(LocalDateTime.now());
            entity.incrementVersion();

            // Then
            assertThat(entity.getCreateBy()).isEqualTo("admin"); // 创建人不变
            assertThat(entity.getUpdateBy()).isEqualTo("modifier"); // 更新人设置
            assertThat(entity.getVersion()).isEqualTo(1L); // 版本递增
        }

        @Test
        @DisplayName("完整软删除流程")
        void completeSoftDeleteFlow() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setTenantId("tenant-001");
            entity.setVersion(5L);

            // When - 软删除
            entity.markDeleted();

            // Then
            assertThat(entity.isDeleted()).isTrue();
            // 其他字段保持不变
            assertThat(entity.getId()).isEqualTo(1L);
            assertThat(entity.getTenantId()).isEqualTo("tenant-001");
            assertThat(entity.getVersion()).isEqualTo(5L);
        }

        @Test
        @DisplayName("完整恢复流程")
        void completeRestoreFlow() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setTenantId("tenant-001");
            entity.setVersion(5L);
            entity.markDeleted();

            // When - 恢复
            entity.restore();
            entity.incrementVersion();

            // Then
            assertThat(entity.isDeleted()).isFalse();
            assertThat(entity.getVersion()).isEqualTo(6L); // 版本递增
        }

        @Test
        @DisplayName("多租户隔离场景")
        void multiTenantIsolationScenario() {
            // Given
            TestEntity entity1 = new TestEntity();
            entity1.setId(1L);
            entity1.setTenantId("tenant-001");
            entity1.setCreateBy("user1");

            TestEntity entity2 = new TestEntity();
            entity2.setId(1L); // 相同 ID
            entity2.setTenantId("tenant-002"); // 不同租户
            entity2.setCreateBy("user2");

            // When & Then
            assertThat(entity1.getId()).isEqualTo(entity2.getId()); // ID 相同
            assertThat(entity1.getTenantId()).isNotEqualTo(entity2.getTenantId()); // 租户不同
            assertThat(entity1.getCreateBy()).isNotEqualTo(entity2.getCreateBy()); // 创建人不同
        }

        @Test
        @DisplayName("乐观锁并发更新模拟")
        void optimisticLockingConcurrencySimulation() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setVersion(0L);

            // When - 模拟多次更新
            entity.incrementVersion(); // 第一次更新 v1
            entity.setUpdateBy("user1");

            entity.incrementVersion(); // 第二次更新 v2
            entity.setUpdateBy("user2");

            entity.incrementVersion(); // 第三次更新 v3
            entity.setUpdateBy("user3");

            // Then
            assertThat(entity.getVersion()).isEqualTo(3L);
            assertThat(entity.getUpdateBy()).isEqualTo("user3"); // 最后更新人
        }
    }

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("所有字段设置最大值应该正确")
        void shouldHandleMaximumValues() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.setId(Long.MAX_VALUE);
            entity.setVersion(Long.MAX_VALUE);
            entity.setTenantId("x".repeat(1000));
            entity.setCreateBy("user-".repeat(100));
            entity.setUpdateBy("modifier-".repeat(100));

            // Then
            assertThat(entity.getId()).isEqualTo(Long.MAX_VALUE);
            assertThat(entity.getVersion()).isEqualTo(Long.MAX_VALUE);
        }

        @Test
        @DisplayName("所有字段设置 null 应该正确")
        void shouldHandleAllNullValues() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setTenantId("tenant");
            entity.setCreateBy("user");
            entity.setUpdateBy("modifier");

            // When
            entity.setId(null);
            entity.setTenantId(null);
            entity.setCreateBy(null);
            entity.setUpdateBy(null);

            // Then
            assertThat(entity.getId()).isNull();
            assertThat(entity.getTenantId()).isNull();
            assertThat(entity.getCreateBy()).isNull();
            assertThat(entity.getUpdateBy()).isNull();
        }

        @Test
        @DisplayName("空字符串审计字段应该正确处理")
        void shouldHandleEmptyStringAuditFields() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.setCreateBy("");
            entity.setUpdateBy("");
            entity.setTenantId("");

            // Then
            assertThat(entity.getCreateBy()).isEmpty();
            assertThat(entity.getUpdateBy()).isEmpty();
            assertThat(entity.getTenantId()).isEmpty();
        }
    }

    /**
     * 测试实体类
     */
    static class TestEntity extends FullEntity<Long> {
        // 用于测试的完整实体类
    }
}
