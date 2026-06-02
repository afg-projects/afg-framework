package io.github.afgprojects.framework.data.core.entity;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

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
            assertThat(entity.getId()).isNull();
        )

        @Test
        @DisplayName("有 id 的实体不应该是新实体")
        void entityWithIdShouldNotBeNew() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);

            // When & Then
            assertThat(entity.getId()).isNotNull();
        )

        @Test
        @DisplayName("设置 id 后再设为 null 应该重新变成新实体")
        void setIdToNullShouldMakeEntityNewAgain() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setId(null);

            // When & Then
            assertThat(entity.getId()).isNull();
        )
    )

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
        )

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
        )
    )

    @Nested
    @DisplayName("时间戳操作测试")
    class TimestampOperationTests {

        @Test
        @DisplayName("createdAt 应该正确设置和获取")
        void shouldSetAndGetCreatedAt() {
            // Given
            TestEntity entity = new TestEntity();
            Instant createdAt = Instant.parse("2024-06-15T10:30:00Z");

            // When
            entity.setCreatedAt(createdAt);

            // Then
            assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        )

        @Test
        @DisplayName("updatedAt 应该正确设置和获取")
        void shouldSetAndGetUpdatedAt() {
            // Given
            TestEntity entity = new TestEntity();
            Instant updatedAt = Instant.parse("2024-06-15T11:30:00Z");

            // When
            entity.setUpdatedAt(updatedAt);

            // Then
            assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        )

        @Test
        @DisplayName("时间戳应该支持 null")
        void shouldSupportNullTimestamps() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());

            // When
            entity.setCreatedAt(null);
            entity.setUpdatedAt(null);

            // Then
            assertThat(entity.getCreatedAt()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
        )

        @Test
        @DisplayName("createdAt 和 updatedAt 可以不同")
        void createdAtAndUpdatedAtCanBeDifferent() {
            // Given
            TestEntity entity = new TestEntity();
            Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
            Instant updatedAt = Instant.parse("2024-12-31T23:59:59Z");

            // When
            entity.setCreatedAt(createdAt);
            entity.setUpdatedAt(updatedAt);

            // Then
            assertThat(entity.getCreatedAt()).isBefore(entity.getUpdatedAt());
        )
    )

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
        )

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
        )

        @Test
        @DisplayName("id 为 null 时 toString 应该显示 null")
        void toStringShouldShowNullId() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            String result = entity.toString();

            // Then
            assertThat(result).contains("id=null");
        )

        @Test
        @DisplayName("toString 格式应该正确")
        void toStringFormatShouldBeCorrect() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);

            // When
            String result = entity.toString();

            // Then
            assertThat(result).isEqualTo("TestEntity(1)");
        )
    )

    @Nested
    @DisplayName("继承测试")
    class InheritanceTests {

        @Test
        @DisplayName("子类应该继承所有字段")
        void subclassShouldInheritAllFields() {
            // Given
            TestEntity entity = new TestEntity();
            Long id = 100L;
            Instant createdAt = Instant.now();
            Instant updatedAt = Instant.now().plusSeconds(3600);

            // When
            entity.setId(id);
            entity.setCreatedAt(createdAt);
            entity.setUpdatedAt(updatedAt);

            // Then
            assertThat(entity.getId()).isEqualTo(id);
            assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
            assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        )
    )

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
            assertThat(entity.getId()).isNotNull(); // 0 is not null
        )

        @Test
        @DisplayName("ID 应该支持负数")
        void shouldSupportNegativeId() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.setId(-1L);

            // Then
            assertThat(entity.getId()).isEqualTo(-1L);
        )

        @Test
        @DisplayName("ID 应该支持 Long.MAX_VALUE")
        void shouldSupportMaxLongId() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.setId(Long.MAX_VALUE);

            // Then
            assertThat(entity.getId()).isEqualTo(Long.MAX_VALUE);
        )

        @Test
        @DisplayName("时间戳应该支持最小值")
        void shouldSupportMinTimestamp() {
            // Given
            TestEntity entity = new TestEntity();
            Instant minTime = Instant.MIN;

            // When
            entity.setCreatedAt(minTime);
            entity.setUpdatedAt(minTime);

            // Then
            assertThat(entity.getCreatedAt()).isEqualTo(minTime);
            assertThat(entity.getUpdatedAt()).isEqualTo(minTime);
        )

        @Test
        @DisplayName("时间戳应该支持最大值")
        void shouldSupportMaxTimestamp() {
            // Given
            TestEntity entity = new TestEntity();
            Instant maxTime = Instant.MAX;

            // When
            entity.setCreatedAt(maxTime);
            entity.setUpdatedAt(maxTime);

            // Then
            assertThat(entity.getCreatedAt()).isEqualTo(maxTime);
            assertThat(entity.getUpdatedAt()).isEqualTo(maxTime);
        )
    )

    /**
     * 测试实体类
     */
    @Getter
    @Setter
    static class TestEntity extends BaseEntity {
        // 用于测试的简单实体类
    )
)
