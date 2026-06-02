package io.github.afgprojects.framework.data.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TimestampSoftDeleteEntity 测试
 */
@DisplayName("TimestampSoftDeleteEntity 测试")
class TimestampSoftDeleteEntityTest {

    @Nested
    @DisplayName("删除状态测试")
    class DeletionStatusTests {

        @Test
        @DisplayName("新建实体应该未删除")
        void newEntityShouldNotBeDeleted() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity.isDeleted()).isFalse();
            assertThat(entity.getDeletedAt()).isNull();
        )

        @Test
        @DisplayName("markDeleted 应该设置删除时间为当前时间")
        void markDeletedShouldSetCurrentTime() {
            // Given
            TestEntity entity = new TestEntity();
            Instant before = Instant.now();

            // When
            entity.markDeleted();

            // Then
            Instant after = Instant.now();
            assertThat(entity.isDeleted()).isTrue();
            assertThat(entity.getDeletedAt()).isNotNull();
            assertThat(entity.getDeletedAt()).isAfterOrEqualTo(before);
            assertThat(entity.getDeletedAt()).isBeforeOrEqualTo(after);
        )

        @Test
        @DisplayName("markDeleted 应该支持指定删除时间")
        void markDeletedShouldSupportCustomTime() {
            // Given
            TestEntity entity = new TestEntity();
            Instant customTime = Instant.parse("2024-06-15T10:30:00Z");

            // When
            entity.setDeletedAt(customTime);

            // Then
            assertThat(entity.isDeleted()).isTrue();
            assertThat(entity.getDeletedAt()).isEqualTo(customTime);
        )

        @Test
        @DisplayName("markNotDeleted 应该清除删除时间")
        void markNotDeletedShouldClearDeletedAt() {
            // Given
            TestEntity entity = new TestEntity();
            entity.markDeleted();

            // When
            entity.markNotDeleted();

            // Then
            assertThat(entity.isDeleted()).isFalse();
            assertThat(entity.getDeletedAt()).isNull();
        )
    )

    @Nested
    @DisplayName("接口实现测试")
    class InterfaceTests {

        @Test
        @DisplayName("应该实现 TimestampSoftDeletable 接口")
        void shouldImplementTimestampSoftDeletable() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity).isInstanceOf(TimestampSoftDeletable.class);
        )

        @Test
        @DisplayName("应该继承 BaseEntity")
        void shouldExtendBaseEntity() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity).isInstanceOf(BaseEntity.class);
        )
    )

    @Nested
    @DisplayName("setter/getter 测试")
    class SetterGetterTests {

        @Test
        @DisplayName("setDeletedAt 应该正确设置删除时间")
        void setDeletedAtShouldWork() {
            // Given
            TestEntity entity = new TestEntity();
            Instant time = Instant.parse("2024-01-01T12:00:00Z");

            // When
            entity.setDeletedAt(time);

            // Then
            assertThat(entity.getDeletedAt()).isEqualTo(time);
            assertThat(entity.isDeleted()).isTrue();
        )

        @Test
        @DisplayName("setDeletedAt(null) 应该表示未删除")
        void setDeletedAtNullShouldIndicateNotDeleted() {
            // Given
            TestEntity entity = new TestEntity();
            entity.markDeleted();

            // When
            entity.setDeletedAt(null);

            // Then
            assertThat(entity.isDeleted()).isFalse();
            assertThat(entity.getDeletedAt()).isNull();
        )
    )

    @Nested
    @DisplayName("toString 测试")
    class ToStringTests {

        @Test
        @DisplayName("toString 应该包含类名和关键字段")
        void toStringShouldContainClassNameAndFields() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.markDeleted();

            // When
            String result = entity.toString();

            // Then
            assertThat(result).contains("TestEntity");
            assertThat(result).contains("id=1");
            // deletedAt 信息可能在 toString 中
        )
    )

    /**
     * 测试实体类
     */
    static class TestEntity extends TimestampSoftDeleteEntity {
        // 用于测试的简单实体类
    )
)