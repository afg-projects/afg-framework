package io.github.afgprojects.framework.data.core.entity;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TimestampSoftDeletable 接口测试
 */
@DisplayName("TimestampSoftDeletable 接口测试")
class TimestampSoftDeletableTest {

    @Nested
    @DisplayName("接口方法测试")
    class InterfaceMethodTests {

        @Test
        @DisplayName("isDeleted 方法应该根据 deletedAt 判断删除状态")
        void isDeletedShouldDetermineByDeletedAt() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();

            // When & Then - deletedAt 为 null 表示未删除
            assertThat(entity.getDeletedAt()).isNull();
            assertThat(entity.isDeleted()).isFalse();

            // When - 设置 deletedAt
            entity.setDeletedAt(LocalDateTime.now());

            // Then
            assertThat(entity.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("getDeletedAt 方法应该返回删除时间")
        void getDeletedAtShouldReturnDeletedTime() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();
            LocalDateTime deletedAt = LocalDateTime.of(2024, 6, 15, 10, 30, 0);

            // When
            entity.setDeletedAt(deletedAt);

            // Then
            assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
        }

        @Test
        @DisplayName("setDeletedAt 方法应该设置删除时间")
        void setDeletedAtShouldSetDeletedTime() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();
            LocalDateTime now = LocalDateTime.now();

            // When
            entity.setDeletedAt(now);

            // Then
            assertThat(entity.getDeletedAt()).isEqualTo(now);
            assertThat(entity.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("setDeletedAt(null) 应该表示未删除")
        void setDeletedAtNullShouldIndicateNotDeleted() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();
            entity.setDeletedAt(LocalDateTime.now());

            // When
            entity.setDeletedAt(null);

            // Then
            assertThat(entity.getDeletedAt()).isNull();
            assertThat(entity.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("与 SoftDeletable 接口对比测试")
    class ComparisonWithSoftDeletableTests {

        @Test
        @DisplayName("TimestampSoftDeletable 使用时间戳而非布尔值")
        void shouldUseTimestampInsteadOfBoolean() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();
            LocalDateTime deleteTime = LocalDateTime.of(2024, 6, 15, 10, 30, 0);

            // When
            entity.setDeletedAt(deleteTime);

            // Then
            assertThat(entity.isDeleted()).isTrue();
            assertThat(entity.getDeletedAt()).isEqualTo(deleteTime); // 可以获取精确删除时间
        }

        @Test
        @DisplayName("时间戳模式可以追溯删除时间")
        void canTraceDeleteTime() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();
            LocalDateTime deleteTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);

            // When - 设置删除时间
            entity.setDeletedAt(deleteTime);

            // Then - 可以知道删除的确切时间
            assertThat(entity.isDeleted()).isTrue();
            assertThat(entity.getDeletedAt()).isEqualTo(deleteTime);
            assertThat(entity.getDeletedAt().getYear()).isEqualTo(2024);
            assertThat(entity.getDeletedAt().getMonthValue()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("时间戳操作测试")
    class TimestampOperationTests {

        @Test
        @DisplayName("删除时间应该支持任意 LocalDateTime")
        void shouldSupportAnyLocalDateTime() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();

            // When - 设置过去的时间
            LocalDateTime pastTime = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
            entity.setDeletedAt(pastTime);

            // Then
            assertThat(entity.getDeletedAt()).isEqualTo(pastTime);
        }

        @Test
        @DisplayName("删除时间应该支持未来时间")
        void shouldSupportFutureTime() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();

            // When - 设置未来的时间
            LocalDateTime futureTime = LocalDateTime.of(2030, 12, 31, 23, 59, 59);
            entity.setDeletedAt(futureTime);

            // Then
            assertThat(entity.getDeletedAt()).isEqualTo(futureTime);
        }

        @Test
        @DisplayName("删除时间应该支持 LocalDateTime.MIN")
        void shouldSupportLocalDateTimeMin() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();

            // When
            entity.setDeletedAt(LocalDateTime.MIN);

            // Then
            assertThat(entity.getDeletedAt()).isEqualTo(LocalDateTime.MIN);
            assertThat(entity.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("删除时间应该支持 LocalDateTime.MAX")
        void shouldSupportLocalDateTimeMax() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();

            // When
            entity.setDeletedAt(LocalDateTime.MAX);

            // Then
            assertThat(entity.getDeletedAt()).isEqualTo(LocalDateTime.MAX);
            assertThat(entity.isDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("恢复操作测试")
    class RestoreOperationTests {

        @Test
        @DisplayName("设置 null 删除时间相当于恢复")
        void setNullDeletedAtShouldRestore() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();
            entity.setDeletedAt(LocalDateTime.now());
            assertThat(entity.isDeleted()).isTrue();

            // When
            entity.setDeletedAt(null);

            // Then
            assertThat(entity.isDeleted()).isFalse();
            assertThat(entity.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("多次恢复应该保持未删除状态")
        void multipleRestoreShouldKeepNotDeleted() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();
            entity.setDeletedAt(LocalDateTime.now());

            // When
            entity.setDeletedAt(null);
            entity.setDeletedAt(null); // 再次恢复

            // Then
            assertThat(entity.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("业务场景测试")
    class BusinessScenarioTests {

        @Test
        @DisplayName("按删除时间范围查询已删除数据")
        void queryDeletedDataByTimeRange() {
            // Given - 创建一组实体，设置不同的删除时间
            TimestampSoftDeletable[] entities = new TimestampSoftDeletable[10];
            LocalDateTime baseTime = LocalDateTime.of(2024, 6, 1, 0, 0, 0);
            for (int i = 0; i < 10; i++) {
                entities[i] = new TestTimestampSoftDeletable();
                if (i < 5) {
                    entities[i].setDeletedAt(baseTime.plusDays(i));
                }
            }

            // When - 查询 6月2日 到 6月4日 删除的数据
            LocalDateTime start = LocalDateTime.of(2024, 6, 2, 0, 0, 0);
            LocalDateTime end = LocalDateTime.of(2024, 6, 5, 0, 0, 0);
            long count = java.util.Arrays.stream(entities)
                    .filter(e -> e.getDeletedAt() != null)
                    .filter(e -> e.getDeletedAt().isAfter(start) || e.getDeletedAt().isEqual(start))
                    .filter(e -> e.getDeletedAt().isBefore(end))
                    .count();

            // Then
            assertThat(count).isEqualTo(3); // 6月2日、3日、4日
        }

        @Test
        @DisplayName("自动清理过期已删除数据")
        void autoCleanupExpiredDeletedData() {
            // Given - 创建一组实体，部分已删除超过 30 天
            TimestampSoftDeletable[] entities = new TimestampSoftDeletable[10];
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < 10; i++) {
                entities[i] = new TestTimestampSoftDeletable();
                if (i < 3) {
                    // 超过 30 天
                    entities[i].setDeletedAt(now.minusDays(31 + i));
                } else if (i < 6) {
                    // 最近删除
                    entities[i].setDeletedAt(now.minusDays(i));
                }
            }

            // When - 查找超过 30 天的已删除数据
            LocalDateTime threshold = now.minusDays(30);
            long expiredCount = java.util.Arrays.stream(entities)
                    .filter(e -> e.getDeletedAt() != null)
                    .filter(e -> e.getDeletedAt().isBefore(threshold))
                    .count();

            // Then
            assertThat(expiredCount).isEqualTo(3);
        }

        @Test
        @DisplayName("区分永久删除和临时删除")
        void distinguishPermanentAndTemporaryDeletion() {
            // Given
            TimestampSoftDeletable permanentDelete = new TestTimestampSoftDeletable();
            TimestampSoftDeletable temporaryDelete = new TestTimestampSoftDeletable();

            // When
            permanentDelete.setDeletedAt(LocalDateTime.of(2020, 1, 1, 0, 0, 0)); // 很久以前删除
            temporaryDelete.setDeletedAt(LocalDateTime.now().minusHours(1)); // 刚删除

            // Then - 可以根据删除时间区分
            assertThat(permanentDelete.getDeletedAt().isBefore(temporaryDelete.getDeletedAt())).isTrue();
        }
    }

    /**
     * 测试 TimestampSoftDeletable 实现
     */
    static class TestTimestampSoftDeletable implements TimestampSoftDeletable {
        private @Nullable LocalDateTime deletedAt;

        @Override
        public boolean isDeleted() {
            return deletedAt != null;
        }

        @Override
        public @Nullable LocalDateTime getDeletedAt() {
            return deletedAt;
        }

        @Override
        public void setDeletedAt(@Nullable LocalDateTime deletedAt) {
            this.deletedAt = deletedAt;
        }
    }
}