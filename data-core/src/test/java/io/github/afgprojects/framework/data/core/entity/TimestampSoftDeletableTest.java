package io.github.afgprojects.framework.data.core.entity;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

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
            entity.setDeletedAt(Instant.now());

            // Then
            assertThat(entity.isDeleted()).isTrue();
        )

        @Test
        @DisplayName("getDeletedAt 方法应该返回删除时间")
        void getDeletedAtShouldReturnDeletedTime() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();
            Instant deletedAt = Instant.parse("2024-06-15T10:30:00Z");

            // When
            entity.setDeletedAt(deletedAt);

            // Then
            assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
        )

        @Test
        @DisplayName("setDeletedAt 方法应该设置删除时间")
        void setDeletedAtShouldSetDeletedTime() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();
            Instant now = Instant.now();

            // When
            entity.setDeletedAt(now);

            // Then
            assertThat(entity.getDeletedAt()).isEqualTo(now);
            assertThat(entity.isDeleted()).isTrue();
        )

        @Test
        @DisplayName("setDeletedAt(null) 应该表示未删除")
        void setDeletedAtNullShouldIndicateNotDeleted() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();
            entity.setDeletedAt(Instant.now());

            // When
            entity.setDeletedAt(null);

            // Then
            assertThat(entity.getDeletedAt()).isNull();
            assertThat(entity.isDeleted()).isFalse();
        )
    )

    @Nested
    @DisplayName("与 SoftDeletable 接口对比测试")
    class ComparisonWithSoftDeletableTests {

        @Test
        @DisplayName("TimestampSoftDeletable 使用时间戳而非布尔值")
        void shouldUseTimestampInsteadOfBoolean() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();
            Instant deleteTime = Instant.parse("2024-06-15T10:30:00Z");

            // When
            entity.setDeletedAt(deleteTime);

            // Then
            assertThat(entity.isDeleted()).isTrue();
            assertThat(entity.getDeletedAt()).isEqualTo(deleteTime); // 可以获取精确删除时间
        )

        @Test
        @DisplayName("时间戳模式可以追溯删除时间")
        void canTraceDeleteTime() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();
            Instant deleteTime = Instant.parse("2024-01-01T00:00:00Z");

            // When - 设置删除时间
            entity.setDeletedAt(deleteTime);

            // Then - 可以知道删除的确切时间
            assertThat(entity.isDeleted()).isTrue();
            assertThat(entity.getDeletedAt()).isEqualTo(deleteTime);
        )
    )

    @Nested
    @DisplayName("时间戳操作测试")
    class TimestampOperationTests {

        @Test
        @DisplayName("删除时间应该支持任意 Instant")
        void shouldSupportAnyInstant() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();

            // When - 设置过去的时间
            Instant pastTime = Instant.parse("2020-01-01T00:00:00Z");
            entity.setDeletedAt(pastTime);

            // Then
            assertThat(entity.getDeletedAt()).isEqualTo(pastTime);
        )

        @Test
        @DisplayName("删除时间应该支持未来时间")
        void shouldSupportFutureTime() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();

            // When - 设置未来的时间
            Instant futureTime = Instant.parse("2030-12-31T23:59:59Z");
            entity.setDeletedAt(futureTime);

            // Then
            assertThat(entity.getDeletedAt()).isEqualTo(futureTime);
        )

        @Test
        @DisplayName("删除时间应该支持 Instant.MIN")
        void shouldSupportInstantMin() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();

            // When
            entity.setDeletedAt(Instant.MIN);

            // Then
            assertThat(entity.getDeletedAt()).isEqualTo(Instant.MIN);
            assertThat(entity.isDeleted()).isTrue();
        )

        @Test
        @DisplayName("删除时间应该支持 Instant.MAX")
        void shouldSupportInstantMax() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();

            // When
            entity.setDeletedAt(Instant.MAX);

            // Then
            assertThat(entity.getDeletedAt()).isEqualTo(Instant.MAX);
            assertThat(entity.isDeleted()).isTrue();
        )
    )

    @Nested
    @DisplayName("恢复操作测试")
    class RestoreOperationTests {

        @Test
        @DisplayName("设置 null 删除时间相当于恢复")
        void setNullDeletedAtShouldRestore() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();
            entity.setDeletedAt(Instant.now());
            assertThat(entity.isDeleted()).isTrue();

            // When
            entity.setDeletedAt(null);

            // Then
            assertThat(entity.isDeleted()).isFalse();
            assertThat(entity.getDeletedAt()).isNull();
        )

        @Test
        @DisplayName("多次恢复应该保持未删除状态")
        void multipleRestoreShouldKeepNotDeleted() {
            // Given
            TimestampSoftDeletable entity = new TestTimestampSoftDeletable();
            entity.setDeletedAt(Instant.now());

            // When
            entity.setDeletedAt(null);
            entity.setDeletedAt(null); // 再次恢复

            // Then
            assertThat(entity.isDeleted()).isFalse();
        )
    )

    @Nested
    @DisplayName("业务场景测试")
    class BusinessScenarioTests {

        @Test
        @DisplayName("按删除时间范围查询已删除数据")
        void queryDeletedDataByTimeRange() {
            // Given - 创建一组实体，设置不同的删除时间
            TimestampSoftDeletable[] entities = new TimestampSoftDeletable[10];
            Instant baseTime = Instant.parse("2024-06-01T00:00:00Z");
            for (int i = 0; i < 10; i++) {
                entities[i] = new TestTimestampSoftDeletable();
                if (i < 5) {
                    entities[i].setDeletedAt(baseTime.plusSeconds(i * 86400L));
                )
            )

            // When - 查询 6月2日 到 6月4日 删除的数据
            Instant start = Instant.parse("2024-06-02T00:00:00Z");
            Instant end = Instant.parse("2024-06-05T00:00:00Z");
            long count = java.util.Arrays.stream(entities)
                    .filter(e -> e.getDeletedAt() != null)
                    .filter(e -> !e.getDeletedAt().isBefore(start))
                    .filter(e -> e.getDeletedAt().isBefore(end))
                    .count();

            // Then
            assertThat(count).isEqualTo(3); // 6月2日、3日、4日
        )

        @Test
        @DisplayName("自动清理过期已删除数据")
        void autoCleanupExpiredDeletedData() {
            // Given - 创建一组实体，部分已删除超过 30 天
            TimestampSoftDeletable[] entities = new TimestampSoftDeletable[10];
            Instant now = Instant.now();
            for (int i = 0; i < 10; i++) {
                entities[i] = new TestTimestampSoftDeletable();
                if (i < 3) {
                    // 超过 30 天
                    entities[i].setDeletedAt(now.minusSeconds((31L + i) * 86400L));
                ) else if (i < 6) {
                    // 最近删除
                    entities[i].setDeletedAt(now.minusSeconds(i * 86400L));
                )
            )

            // When - 查找超过 30 天的已删除数据
            Instant threshold = now.minusSeconds(30L * 86400L);
            long expiredCount = java.util.Arrays.stream(entities)
                    .filter(e -> e.getDeletedAt() != null)
                    .filter(e -> e.getDeletedAt().isBefore(threshold))
                    .count();

            // Then
            assertThat(expiredCount).isEqualTo(3);
        )

        @Test
        @DisplayName("区分永久删除和临时删除")
        void distinguishPermanentAndTemporaryDeletion() {
            // Given
            TimestampSoftDeletable permanentDelete = new TestTimestampSoftDeletable();
            TimestampSoftDeletable temporaryDelete = new TestTimestampSoftDeletable();

            // When
            permanentDelete.setDeletedAt(Instant.parse("2020-01-01T00:00:00Z")); // 很久以前删除
            temporaryDelete.setDeletedAt(Instant.now().minusSeconds(3600)); // 刚删除

            // Then - 可以根据删除时间区分
            assertThat(permanentDelete.getDeletedAt().isBefore(temporaryDelete.getDeletedAt())).isTrue();
        )
    )

    /**
     * 测试 TimestampSoftDeletable 实现
     */
    static class TestTimestampSoftDeletable implements TimestampSoftDeletable {
        private @Nullable Instant deletedAt;

        @Override
        public boolean isDeleted() {
            return deletedAt != null;
        )

        @Override
        public @Nullable Instant getDeletedAt() {
            return deletedAt;
        )

        @Override
        public void setDeletedAt(@Nullable Instant deletedAt) {
            this.deletedAt = deletedAt;
        )
    )
)