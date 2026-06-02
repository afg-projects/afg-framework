package io.github.afgprojects.framework.data.core.entity;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

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
            assertThat(entity.isDeleted()).isFalse();
            assertThat(entity.getVersion()).isEqualTo(0);
            assertThat(entity.getCreateBy()).isNull();
            assertThat(entity.getUpdateBy()).isNull();
        )
    )

    @Nested
    @DisplayName("接口实现测试")
    class InterfaceImplementationTests {

        @Test
        @DisplayName("应该实现 SoftDeletable 接口")
        void shouldImplementSoftDeletable() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity).isInstanceOf(SoftDeletable.class);
        )

        @Test
        @DisplayName("应该实现 Versioned 接口")
        void shouldImplementVersioned() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity).isInstanceOf(Versioned.class);
        )

        @Test
        @DisplayName("应该实现 Auditable 接口")
        void shouldImplementAuditable() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity).isInstanceOf(Auditable.class);
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
        )

        @Test
        @DisplayName("markNotDeleted 应该恢复已删除实体")
        void markNotDeletedShouldClearDeletedFlag() {
            // Given
            TestEntity entity = new TestEntity();
            entity.markDeleted();

            // When
            entity.markNotDeleted();

            // Then
            assertThat(entity.isDeleted()).isFalse();
        )

        @Test
        @DisplayName("通过 SoftDeletable 接口操作应该有效")
        void operationsViaSoftDeletableInterfaceShouldWork() {
            // Given
            SoftDeletable entity = new TestEntity();

            // When
            entity.setDeleted(true);

            // Then
            assertThat(entity.isDeleted()).isTrue();
        )

        @Test
        @DisplayName("删除恢复循环应该正确工作")
        void deleteRestoreCycleShouldWork() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            entity.markDeleted();
            assertThat(entity.isDeleted()).isTrue();

            entity.markNotDeleted();
            assertThat(entity.isDeleted()).isFalse();

            entity.markDeleted();
            assertThat(entity.isDeleted()).isTrue();
        )
    )

    @Nested
    @DisplayName("版本号功能测试")
    class VersionFunctionalityTests {

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
        @DisplayName("incrementVersion 应该递增版本号")
        void shouldIncrementVersion() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setVersion(3);

            // When
            entity.incrementVersion();

            // Then
            assertThat(entity.getVersion()).isEqualTo(4);
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
        )

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
        )

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
        )
    )

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
        )

        @Test
        @DisplayName("isNew 方法应该正确工作")
        void isNewShouldWorkCorrectly() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity.getId()).isNull();
            entity.setId(1L);
            assertThat(entity.getId()).isNotNull();
        )

        @Test
        @DisplayName("应该正确设置和获取时间戳")
        void shouldSetAndGetTimestamps() {
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
        @DisplayName("toString 应该包含 id")
        void toStringShouldContainId() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(42L);

            // When
            String result = entity.toString();

            // Then
            assertThat(result).contains("id=42");
        )
    )

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
            entity.setCreateBy("admin");
            entity.setCreatedAt(Instant.now());
            entity.setVersion(0);

            // Then
            assertThat(entity.getId()).isEqualTo(1L);
            assertThat(entity.getCreateBy()).isEqualTo("admin");
            assertThat(entity.isDeleted()).isFalse();
            assertThat(entity.getVersion()).isEqualTo(0);
        )

        @Test
        @DisplayName("完整实体更新流程")
        void completeEntityUpdateFlow() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setCreateBy("admin");
            entity.setVersion(0);

            // When - 更新操作
            entity.setUpdateBy("modifier");
            entity.setUpdatedAt(Instant.now());
            entity.incrementVersion();

            // Then
            assertThat(entity.getCreateBy()).isEqualTo("admin"); // 创建人不变
            assertThat(entity.getUpdateBy()).isEqualTo("modifier"); // 更新人设置
            assertThat(entity.getVersion()).isEqualTo(1); // 版本递增
        )

        @Test
        @DisplayName("完整软删除流程")
        void completeSoftDeleteFlow() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setVersion(5);

            // When - 软删除
            entity.markDeleted();

            // Then
            assertThat(entity.isDeleted()).isTrue();
            assertThat(entity.getId()).isEqualTo(1L);
            assertThat(entity.getVersion()).isEqualTo(5);
        )

        @Test
        @DisplayName("完整恢复流程")
        void completeRestoreFlow() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setVersion(5);
            entity.markDeleted();

            // When - 恢复
            entity.markNotDeleted();
            entity.incrementVersion();

            // Then
            assertThat(entity.isDeleted()).isFalse();
            assertThat(entity.getVersion()).isEqualTo(6); // 版本递增
        )

        @Test
        @DisplayName("乐观锁并发更新模拟")
        void optimisticLockingConcurrencySimulation() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setVersion(0);

            // When - 模拟多次更新
            entity.incrementVersion(); // 第一次更新 v1
            entity.setUpdateBy("user1");

            entity.incrementVersion(); // 第二次更新 v2
            entity.setUpdateBy("user2");

            entity.incrementVersion(); // 第三次更新 v3
            entity.setUpdateBy("user3");

            // Then
            assertThat(entity.getVersion()).isEqualTo(3);
            assertThat(entity.getUpdateBy()).isEqualTo("user3"); // 最后更新人
        )
    )

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
            entity.setVersion(Integer.MAX_VALUE);
            entity.setCreateBy("user-".repeat(100));
            entity.setUpdateBy("modifier-".repeat(100));

            // Then
            assertThat(entity.getId()).isEqualTo(Long.MAX_VALUE);
            assertThat(entity.getVersion()).isEqualTo(Integer.MAX_VALUE);
        )

        @Test
        @DisplayName("所有字段设置 null 应该正确")
        void shouldHandleAllNullValues() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setCreateBy("user");
            entity.setUpdateBy("modifier");

            // When
            entity.setId(null);
            entity.setCreateBy(null);
            entity.setUpdateBy(null);

            // Then
            assertThat(entity.getId()).isNull();
            assertThat(entity.getCreateBy()).isNull();
            assertThat(entity.getUpdateBy()).isNull();
        )

        @Test
        @DisplayName("空字符串审计字段应该正确处理")
        void shouldHandleEmptyStringAuditFields() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.setCreateBy("");
            entity.setUpdateBy("");

            // Then
            assertThat(entity.getCreateBy()).isEmpty();
            assertThat(entity.getUpdateBy()).isEmpty();
        )
    )

    /**
     * 测试实体类
     */
    @Getter
    @Setter
    static class TestEntity extends FullEntity {
        // 用于测试的完整实体类
    )
)