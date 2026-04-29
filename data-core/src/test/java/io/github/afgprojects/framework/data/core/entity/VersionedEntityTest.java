package io.github.afgprojects.framework.data.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VersionedEntity 测试
 */
@DisplayName("VersionedEntity 测试")
class VersionedEntityTest {

    @Nested
    @DisplayName("版本号初始值测试")
    class InitialVersionTests {

        @Test
        @DisplayName("新建实体版本号应该为 0")
        void newEntityShouldHaveZeroVersion() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity.getVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("新建实体不应该有负版本号")
        void newEntityShouldNotHaveNegativeVersion() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity.getVersion()).isGreaterThanOrEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("版本号操作测试")
    class VersionOperationTests {

        @Test
        @DisplayName("setVersion 应该正确设置版本号")
        void shouldSetVersionCorrectly() {
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
        @DisplayName("多次递增版本号应该正确累计")
        void shouldIncrementMultipleTimes() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            for (int i = 0; i < 10; i++) {
                entity.incrementVersion();
            }

            // Then
            assertThat(entity.getVersion()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("Versioned 接口测试")
    class VersionedInterfaceTests {

        @Test
        @DisplayName("应该实现 Versioned 接口")
        void shouldImplementVersionedInterface() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity).isInstanceOf(Versioned.class);
        }

        @Test
        @DisplayName("通过接口调用 incrementVersion 应该有效")
        void incrementVersionViaInterfaceShouldWork() {
            // Given
            Versioned entity = new TestEntity();
            entity.setVersion(1L);

            // When
            entity.incrementVersion();

            // Then
            assertThat(entity.getVersion()).isEqualTo(2L);
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
            Long id = 123L;

            // When
            entity.setId(id);

            // Then
            assertThat(entity.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("应该继承 createTime 和 updateTime 字段")
        void shouldInheritTimestampFields() {
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

        @Test
        @DisplayName("isNew 方法应该继承自 BaseEntity")
        void isNewShouldBeInherited() {
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
        @DisplayName("toString 应该包含 id 和 version")
        void toStringShouldContainIdAndVersion() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(42L);
            entity.setVersion(7L);

            // When
            String result = entity.toString();

            // Then
            assertThat(result).contains("id=42");
            assertThat(result).contains("version=7");
        }

        @Test
        @DisplayName("toString 格式应该正确")
        void toStringFormatShouldBeCorrect() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setVersion(2L);

            // When
            String result = entity.toString();

            // Then
            assertThat(result).isEqualTo("TestEntity{id=1, version=2}");
        }

        @Test
        @DisplayName("id 为 null 时 toString 应该显示 null")
        void toStringShouldShowNullId() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setVersion(1L);

            // When
            String result = entity.toString();

            // Then
            assertThat(result).contains("id=null");
            assertThat(result).contains("version=1");
        }
    }

    @Nested
    @DisplayName("乐观锁场景测试")
    class OptimisticLockingTests {

        @Test
        @DisplayName("模拟更新操作：版本号应该递增")
        void simulateUpdateOperation() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            assertThat(entity.getVersion()).isEqualTo(0L);

            // When - 第一次更新
            entity.incrementVersion();
            // 第二次更新
            entity.incrementVersion();
            // 第三次更新
            entity.incrementVersion();

            // Then
            assertThat(entity.getVersion()).isEqualTo(3L);
        }

        @Test
        @DisplayName("版本号冲突检测：相同版本号表示无冲突")
        void noConflictWithSameVersion() {
            // Given
            TestEntity entity1 = new TestEntity();
            TestEntity entity2 = new TestEntity();
            entity1.setVersion(5L);
            entity2.setVersion(5L);

            // When & Then
            assertThat(entity1.getVersion()).isEqualTo(entity2.getVersion());
        }

        @Test
        @DisplayName("版本号冲突检测：不同版本号表示有冲突")
        void conflictWithDifferentVersion() {
            // Given
            TestEntity entity1 = new TestEntity();
            TestEntity entity2 = new TestEntity();
            entity1.setVersion(5L);
            entity2.setVersion(6L);

            // When & Then
            assertThat(entity1.getVersion()).isNotEqualTo(entity2.getVersion());
        }
    }

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("版本号应该支持 Long.MAX_VALUE")
        void shouldSupportMaxLongVersion() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.setVersion(Long.MAX_VALUE);

            // Then
            assertThat(entity.getVersion()).isEqualTo(Long.MAX_VALUE);
        }

        @Test
        @DisplayName("版本号应该支持 0 值")
        void shouldSupportZeroVersion() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setVersion(100L);

            // When
            entity.setVersion(0L);

            // Then
            assertThat(entity.getVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("版本号应该支持负数（虽然不推荐）")
        void shouldSupportNegativeVersion() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.setVersion(-1L);

            // Then
            assertThat(entity.getVersion()).isEqualTo(-1L);
        }

        @Test
        @DisplayName("从最大值递增会溢出")
        void incrementFromMaxValueWillOverflow() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setVersion(Long.MAX_VALUE);

            // When
            entity.incrementVersion();

            // Then - 溢出变成负数
            assertThat(entity.getVersion()).isNegative();
        }
    }

    /**
     * 测试实体类
     */
    static class TestEntity extends VersionedEntity<Long> {
        // 用于测试的简单实体类
    }
}
