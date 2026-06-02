package io.github.afgprojects.framework.data.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

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
        void newEntityVersionShouldBeZero() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity.getVersion()).isEqualTo(0);
        )
    )

    @Nested
    @DisplayName("版本号设置和获取测试")
    class VersionSetterGetterTests {

        @Test
        @DisplayName("应该正确设置和获取版本号")
        void shouldSetAndGetVersion() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.setVersion(5);

            // Then
            assertThat(entity.getVersion()).isEqualTo(5);
        )

        @Test
        @DisplayName("版本号应该支持 Integer 最大值")
        void versionShouldSupportMaxValue() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.setVersion(Integer.MAX_VALUE);

            // Then
            assertThat(entity.getVersion()).isEqualTo(Integer.MAX_VALUE);
        )

        @Test
        @DisplayName("版本号应该支持 0 值")
        void versionShouldSupportZero() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setVersion(10);

            // When
            entity.setVersion(0);

            // Then
            assertThat(entity.getVersion()).isEqualTo(0);
        )
    )

    @Nested
    @DisplayName("版本号递增测试")
    class VersionIncrementTests {

        @Test
        @DisplayName("incrementVersion 应该递增版本号")
        void shouldIncrementVersion() {
            // Given
            TestEntity entity = new TestEntity();
            assertThat(entity.getVersion()).isEqualTo(0);

            // When
            entity.incrementVersion();

            // Then
            assertThat(entity.getVersion()).isEqualTo(1);
        )

        @Test
        @DisplayName("多次递增版本号应该正确累加")
        void shouldIncrementMultipleTimes() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            for (int i = 0; i < 10; i++) {
                entity.incrementVersion();
            )

            // Then
            assertThat(entity.getVersion()).isEqualTo(10);
        )

        @Test
        @DisplayName("递增后版本号应该比之前大 1")
        void incrementShouldIncreaseByOne() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setVersion(5);

            // When
            entity.incrementVersion();

            // Then
            assertThat(entity.getVersion()).isEqualTo(6);
        )
    )

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
        )

        @Test
        @DisplayName("通过 Versioned 接口操作应该有效")
        void operationsViaVersionedInterfaceShouldWork() {
            // Given
            Versioned entity = new TestEntity();

            // When
            entity.setVersion(10);
            entity.incrementVersion();

            // Then
            assertThat(entity.getVersion()).isEqualTo(11);
        )
    )

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
        )

        @Test
        @DisplayName("应该继承 id 字段")
        void shouldInheritIdField() {
            // Given
            TestEntity entity = new TestEntity();
            Long id = 999L;

            // When
            entity.setId(id);

            // Then
            assertThat(entity.getId()).isEqualTo(id);
        )

        @Test
        @DisplayName("应该继承时间戳字段")
        void shouldInheritTimestampFields() {
            // Given
            TestEntity entity = new TestEntity();
            Instant now = Instant.now();

            // When
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now.plusSeconds(3600));

            // Then
            assertThat(entity.getCreatedAt()).isEqualTo(now);
            assertThat(entity.getUpdatedAt()).isEqualTo(now.plusSeconds(3600));
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
        @DisplayName("toString 应该包含 id 和 version")
        void toStringShouldContainIdAndVersion() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(42L);
            entity.setVersion(7);

            // When
            String result = entity.toString();

            // Then
            assertThat(result).contains("id=42");
            assertThat(result).contains("version=7");
        )
    )

    @Nested
    @DisplayName("乐观锁业务场景测试")
    class OptimisticLockingBusinessTests {

        @Test
        @DisplayName("模拟乐观锁并发更新")
        void simulateOptimisticLocking() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            assertThat(entity.getVersion()).isEqualTo(0);

            // When - 第一次更新
            entity.incrementVersion();
            assertThat(entity.getVersion()).isEqualTo(1);

            // 第二次更新
            entity.incrementVersion();
            assertThat(entity.getVersion()).isEqualTo(2);

            // 第三次更新
            entity.incrementVersion();

            // Then
            assertThat(entity.getVersion()).isEqualTo(3);
        )

        @Test
        @DisplayName("更新失败后重试应该递增版本号")
        void retryAfterFailureShouldIncrementVersion() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setVersion(5);

            // When - 模拟更新操作
            int currentVersion = entity.getVersion();
            entity.incrementVersion();

            // Then - 版本号应该递增
            assertThat(entity.getVersion()).isEqualTo(currentVersion + 1);
        )

        @Test
        @DisplayName("新建实体首次更新应该从版本 0 到 1")
        void firstUpdateShouldIncrementFromZeroToOne() {
            // Given
            TestEntity entity = new TestEntity();
            assertThat(entity.getVersion()).isEqualTo(0);

            // When
            entity.incrementVersion();

            // Then
            assertThat(entity.getVersion()).isEqualTo(1);
        )
    )

    /**
     * 测试实体类
     */
    static class TestEntity extends VersionedEntity {
        // 用于测试的简单实体类
    )
)