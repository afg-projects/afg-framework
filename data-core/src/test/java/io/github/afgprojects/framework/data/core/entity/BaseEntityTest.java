package io.github.afgprojects.framework.data.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseEntity 测试
 */
@DisplayName("BaseEntity 测试")
class BaseEntityTest {

    @Nested
    @DisplayName("isNew 测试")
    class IsNewTests {

        @Test
        @DisplayName("新建实体（id 为 null）应该是新实体")
        void newEntityWithNullIdShouldBeNew() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity.isNew()).isTrue();
        }

        @Test
        @DisplayName("有 id 的实体不应该是新实体")
        void entityWithIdShouldNotBeNew() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);

            // When & Then
            assertThat(entity.isNew()).isFalse();
        }

        @Test
        @DisplayName("设置 id 后再设为 null 应该重新变成新实体")
        void setIdToNullShouldMakeEntityNewAgain() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setId(null);

            // When & Then
            assertThat(entity.isNew()).isTrue();
        }
    }

    @Nested
    @DisplayName("ID 操作测试")
    class IdOperationTests {

        @Test
        @DisplayName("应该支持 Long 类型 ID")
        void shouldSupportLongId() {
            // Given
            TestEntity entity = new TestEntity();
            Long id = 12345L;

            // When
            entity.setId(id);

            // Then
            assertThat(entity.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("应该支持 String 类型 ID")
        void shouldSupportStringId() {
            // Given
            StringIdEntity entity = new StringIdEntity();
            String id = "uuid-12345";

            // When
            entity.setId(id);

            // Then
            assertThat(entity.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("应该支持 Integer 类型 ID")
        void shouldSupportIntegerId() {
            // Given
            IntEntity entity = new IntEntity();
            Integer id = 999;

            // When
            entity.setId(id);

            // Then
            assertThat(entity.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("应该支持 null ID")
        void shouldSupportNullId() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);

            // When
            entity.setId(null);

            // Then
            assertThat(entity.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("时间戳操作测试")
    class TimestampOperationTests {

        @Test
        @DisplayName("createTime 应该正确设置和获取")
        void shouldSetAndGetCreateTime() {
            // Given
            TestEntity entity = new TestEntity();
            LocalDateTime createTime = LocalDateTime.of(2024, 6, 15, 10, 30, 0);

            // When
            entity.setCreateTime(createTime);

            // Then
            assertThat(entity.getCreateTime()).isEqualTo(createTime);
        }

        @Test
        @DisplayName("updateTime 应该正确设置和获取")
        void shouldSetAndGetUpdateTime() {
            // Given
            TestEntity entity = new TestEntity();
            LocalDateTime updateTime = LocalDateTime.of(2024, 6, 15, 11, 30, 0);

            // When
            entity.setUpdateTime(updateTime);

            // Then
            assertThat(entity.getUpdateTime()).isEqualTo(updateTime);
        }

        @Test
        @DisplayName("时间戳应该支持 null")
        void shouldSupportNullTimestamps() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());

            // When
            entity.setCreateTime(null);
            entity.setUpdateTime(null);

            // Then
            assertThat(entity.getCreateTime()).isNull();
            assertThat(entity.getUpdateTime()).isNull();
        }

        @Test
        @DisplayName("createTime 和 updateTime 可以不同")
        void createTimeAndUpdateTimeCanBeDifferent() {
            // Given
            TestEntity entity = new TestEntity();
            LocalDateTime createTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
            LocalDateTime updateTime = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

            // When
            entity.setCreateTime(createTime);
            entity.setUpdateTime(updateTime);

            // Then
            assertThat(entity.getCreateTime()).isBefore(entity.getUpdateTime());
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
        @DisplayName("toString 应该包含 id 字段")
        void toStringShouldContainId() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(42L);

            // When
            String result = entity.toString();

            // Then
            assertThat(result).contains("id=42");
        }

        @Test
        @DisplayName("id 为 null 时 toString 应该显示 null")
        void toStringShouldShowNullId() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            String result = entity.toString();

            // Then
            assertThat(result).contains("id=null");
        }

        @Test
        @DisplayName("toString 格式应该正确")
        void toStringFormatShouldBeCorrect() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);

            // When
            String result = entity.toString();

            // Then
            assertThat(result).isEqualTo("TestEntity{id=1}");
        }
    }

    @Nested
    @DisplayName("继承测试")
    class InheritanceTests {

        @Test
        @DisplayName("子类应该继承所有字段")
        void subclassShouldInheritAllFields() {
            // Given
            TestEntity entity = new TestEntity();
            Long id = 100L;
            LocalDateTime createTime = LocalDateTime.now();
            LocalDateTime updateTime = LocalDateTime.now().plusHours(1);

            // When
            entity.setId(id);
            entity.setCreateTime(createTime);
            entity.setUpdateTime(updateTime);

            // Then
            assertThat(entity.getId()).isEqualTo(id);
            assertThat(entity.getCreateTime()).isEqualTo(createTime);
            assertThat(entity.getUpdateTime()).isEqualTo(updateTime);
        }
    }

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("ID 应该支持 0 值")
        void shouldSupportZeroId() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.setId(0L);

            // Then
            assertThat(entity.getId()).isEqualTo(0L);
            assertThat(entity.isNew()).isFalse(); // 0 is not null
        }

        @Test
        @DisplayName("ID 应该支持负数")
        void shouldSupportNegativeId() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.setId(-1L);

            // Then
            assertThat(entity.getId()).isEqualTo(-1L);
        }

        @Test
        @DisplayName("ID 应该支持 Long.MAX_VALUE")
        void shouldSupportMaxLongId() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.setId(Long.MAX_VALUE);

            // Then
            assertThat(entity.getId()).isEqualTo(Long.MAX_VALUE);
        }

        @Test
        @DisplayName("时间戳应该支持最小值")
        void shouldSupportMinTimestamp() {
            // Given
            TestEntity entity = new TestEntity();
            LocalDateTime minTime = LocalDateTime.MIN;

            // When
            entity.setCreateTime(minTime);
            entity.setUpdateTime(minTime);

            // Then
            assertThat(entity.getCreateTime()).isEqualTo(minTime);
            assertThat(entity.getUpdateTime()).isEqualTo(minTime);
        }

        @Test
        @DisplayName("时间戳应该支持最大值")
        void shouldSupportMaxTimestamp() {
            // Given
            TestEntity entity = new TestEntity();
            LocalDateTime maxTime = LocalDateTime.MAX;

            // When
            entity.setCreateTime(maxTime);
            entity.setUpdateTime(maxTime);

            // Then
            assertThat(entity.getCreateTime()).isEqualTo(maxTime);
            assertThat(entity.getUpdateTime()).isEqualTo(maxTime);
        }

        @Test
        @DisplayName("空字符串 ID 应该有效")
        void shouldSupportEmptyStringId() {
            // Given
            StringIdEntity entity = new StringIdEntity();

            // When
            entity.setId("");

            // Then
            assertThat(entity.getId()).isEmpty();
            assertThat(entity.isNew()).isFalse(); // empty string is not null
        }
    }

    /**
     * 测试实体类
     */
    static class TestEntity extends BaseEntity<Long> {
        // 用于测试的简单实体类
    }

    /**
     * String ID 测试实体类
     */
    static class StringIdEntity extends BaseEntity<String> {
        // 用于测试 String 类型 ID
    }

    /**
     * Integer ID 测试实体类
     */
    static class IntEntity extends BaseEntity<Integer> {
        // 用于测试 Integer 类型 ID
    }
}
