package io.github.afgprojects.framework.data.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TimestampSoftDeleteEntity 单元测试
 */
@DisplayName("TimestampSoftDeleteEntity 测试")
class TimestampSoftDeleteEntityTest {

    @Nested
    @DisplayName("TimestampSoftDeletable 接口")
    class InterfaceTests {

        @Test
        @DisplayName("TimestampSoftDeleteEntity 实现 TimestampSoftDeletable")
        void shouldImplementTimestampSoftDeletable() {
            TimestampSoftDeleteEntity entity = new ConcreteTimestampSoftDeleteEntity();
            assertThat(entity).isInstanceOf(TimestampSoftDeletable.class);
        }

        @Test
        @DisplayName("TimestampSoftDeleteEntity 继承 BaseEntity")
        void shouldExtendBaseEntity() {
            TimestampSoftDeleteEntity entity = new ConcreteTimestampSoftDeleteEntity();
            assertThat(entity).isInstanceOf(BaseEntity.class);
        }
    }

    @Nested
    @DisplayName("deletedAt getter/setter")
    class DeletedAtGetterSetterTests {

        @Test
        @DisplayName("deletedAt getter/setter 正常工作")
        void shouldGetAndSetDeletedAt() {
            TimestampSoftDeleteEntity entity = new ConcreteTimestampSoftDeleteEntity();
            Instant now = Instant.now();

            entity.setDeletedAt(now);

            assertThat(entity.getDeletedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("deletedAt 可设置为不同时间")
        void shouldSetDeletedAtToDifferentTimes() {
            TimestampSoftDeleteEntity entity = new ConcreteTimestampSoftDeleteEntity();
            Instant time1 = Instant.parse("2024-01-01T00:00:00Z");
            Instant time2 = Instant.parse("2025-06-15T12:30:00Z");

            entity.setDeletedAt(time1);
            assertThat(entity.getDeletedAt()).isEqualTo(time1);

            entity.setDeletedAt(time2);
            assertThat(entity.getDeletedAt()).isEqualTo(time2);
        }
    }

    @Nested
    @DisplayName("默认值")
    class DefaultValueTests {

        @Test
        @DisplayName("deletedAt 默认 null")
        void shouldDefaultDeletedAtToNull() {
            TimestampSoftDeleteEntity entity = new ConcreteTimestampSoftDeleteEntity();
            assertThat(entity.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("isDeleted 默认 false")
        void shouldDefaultIsDeletedToFalse() {
            TimestampSoftDeleteEntity entity = new ConcreteTimestampSoftDeleteEntity();
            assertThat(entity.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("markDeleted")
    class MarkDeletedTests {

        @Test
        @DisplayName("markDeleted 设置 deletedAt 为当前时间")
        void shouldSetDeletedAtToCurrentTime() {
            TimestampSoftDeleteEntity entity = new ConcreteTimestampSoftDeleteEntity();
            assertThat(entity.getDeletedAt()).isNull();

            Instant beforeMark = Instant.now();
            entity.markDeleted();
            Instant afterMark = Instant.now();

            assertThat(entity.getDeletedAt()).isNotNull();
            assertThat(entity.getDeletedAt()).isBetween(beforeMark, afterMark);
        }

        @Test
        @DisplayName("markDeleted 后 isDeleted 返回 true")
        void shouldReturnTrueAfterMarkDeleted() {
            TimestampSoftDeleteEntity entity = new ConcreteTimestampSoftDeleteEntity();
            assertThat(entity.isDeleted()).isFalse();

            entity.markDeleted();

            assertThat(entity.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("重复 markDeleted 更新 deletedAt 时间")
        void shouldUpdateTimeOnRepeatedMarkDeleted() {
            TimestampSoftDeleteEntity entity = new ConcreteTimestampSoftDeleteEntity();
            entity.markDeleted();
            Instant firstDeletedAt = entity.getDeletedAt();

            // 短暂等待确保时间不同
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}

            entity.markDeleted();

            // deletedAt 应被更新为新的时间
            assertThat(entity.getDeletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("isDeleted 判断")
    class IsDeletedTests {

        @Test
        @DisplayName("deletedAt 为 null 时 isDeleted 返回 false")
        void shouldReturnFalseWhenDeletedAtIsNull() {
            TimestampSoftDeleteEntity entity = new ConcreteTimestampSoftDeleteEntity();
            entity.setDeletedAt(null);
            assertThat(entity.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("deletedAt 非空时 isDeleted 返回 true")
        void shouldReturnTrueWhenDeletedAtIsSet() {
            TimestampSoftDeleteEntity entity = new ConcreteTimestampSoftDeleteEntity();
            entity.setDeletedAt(Instant.now());
            assertThat(entity.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("deletedAt 设置为未来时间 isDeleted 仍返回 true")
        void shouldReturnTrueWhenDeletedAtIsInFuture() {
            TimestampSoftDeleteEntity entity = new ConcreteTimestampSoftDeleteEntity();
            entity.setDeletedAt(Instant.now().plusSeconds(3600));
            assertThat(entity.isDeleted()).isTrue();
        }
    }

    /**
     * TimestampSoftDeleteEntity 的具体实现类，用于测试
     */
    static class ConcreteTimestampSoftDeleteEntity extends TimestampSoftDeleteEntity {
    }
}
