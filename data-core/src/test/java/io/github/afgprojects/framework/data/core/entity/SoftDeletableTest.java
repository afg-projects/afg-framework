package io.github.afgprojects.framework.data.core.entity;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SoftDeletable 接口测试
 */
@DisplayName("SoftDeletable 接口测试")
class SoftDeletableTest {

    @Nested
    @DisplayName("接口方法测试")
    class InterfaceMethodTests {

        @Test
        @DisplayName("isDeleted 方法应该根据 getDeleted 判断")
        void isDeletedShouldDetermineByGetDeleted() {
            // Given
            SoftDeletable entity = new TestSoftDeletable();

            // When & Then - getDeleted 返回 null 或 false 时未删除
            assertThat(entity.getDeleted()).isNull();
            assertThat(entity.isDeleted()).isFalse();

            // When - 设置 deleted
            entity.setDeleted(true);

            // Then
            assertThat(entity.isDeleted()).isTrue();
        )

        @Test
        @DisplayName("getDeleted 方法应该返回删除标记")
        void getDeletedShouldReturnDeletedFlag() {
            // Given
            SoftDeletable entity = new TestSoftDeletable();

            // When
            entity.setDeleted(true);

            // Then
            assertThat(entity.getDeleted()).isTrue();
        )

        @Test
        @DisplayName("setDeleted 方法应该设置删除标记")
        void setDeletedShouldSetDeletedFlag() {
            // Given
            SoftDeletable entity = new TestSoftDeletable();

            // When
            entity.setDeleted(true);

            // Then
            assertThat(entity.getDeleted()).isTrue();
            assertThat(entity.isDeleted()).isTrue();
        )

        @Test
        @DisplayName("setDeleted(false) 应该表示未删除")
        void setDeletedFalseShouldIndicateNotDeleted() {
            // Given
            SoftDeletable entity = new TestSoftDeletable();
            entity.setDeleted(true);

            // When
            entity.setDeleted(false);

            // Then
            assertThat(entity.getDeleted()).isFalse();
            assertThat(entity.isDeleted()).isFalse();
        )

        @Test
        @DisplayName("setDeleted(null) 应该表示未删除")
        void setDeletedNullShouldIndicateNotDeleted() {
            // Given
            SoftDeletable entity = new TestSoftDeletable();
            entity.setDeleted(true);

            // When
            entity.setDeleted(null);

            // Then
            assertThat(entity.getDeleted()).isNull();
            assertThat(entity.isDeleted()).isFalse(); // null 被视为未删除
        )
    )

    @Nested
    @DisplayName("isDeleted 默认方法行为测试")
    class IsDeletedDefaultMethodTests {

        @Test
        @DisplayName("getDeleted 为 true 时 isDeleted 应该返回 true")
        void isDeletedShouldReturnTrueWhenGetDeletedIsTrue() {
            // Given
            SoftDeletable entity = new TestSoftDeletable();
            entity.setDeleted(true);

            // When & Then
            assertThat(entity.isDeleted()).isTrue();
        )

        @Test
        @DisplayName("getDeleted 为 false 时 isDeleted 应该返回 false")
        void isDeletedShouldReturnFalseWhenGetDeletedIsFalse() {
            // Given
            SoftDeletable entity = new TestSoftDeletable();
            entity.setDeleted(false);

            // When & Then
            assertThat(entity.isDeleted()).isFalse();
        )

        @Test
        @DisplayName("getDeleted 为 null 时 isDeleted 应该返回 false")
        void isDeletedShouldReturnFalseWhenGetDeletedIsNull() {
            // Given
            SoftDeletable entity = new TestSoftDeletable();

            // When & Then
            assertThat(entity.getDeleted()).isNull();
            assertThat(entity.isDeleted()).isFalse(); // null 安全处理
        )
    )

    @Nested
    @DisplayName("删除/恢复循环测试")
    class DeleteRestoreCycleTests {

        @Test
        @DisplayName("删除后恢复应该正确工作")
        void deleteAndRestoreShouldWork() {
            // Given
            SoftDeletable entity = new TestSoftDeletable();

            // When - 删除
            entity.setDeleted(true);
            assertThat(entity.isDeleted()).isTrue();

            // 恢复
            entity.setDeleted(false);
            assertThat(entity.isDeleted()).isFalse();

            // 再次删除
            entity.setDeleted(true);

            // Then
            assertThat(entity.isDeleted()).isTrue();
        )

        @Test
        @DisplayName("重复设置删除状态应该无副作用")
        void repeatedDeleteShouldHaveNoSideEffect() {
            // Given
            SoftDeletable entity = new TestSoftDeletable();

            // When - 重复设置删除
            entity.setDeleted(true);
            entity.setDeleted(true);
            entity.setDeleted(true);

            // Then
            assertThat(entity.isDeleted()).isTrue();
        )

        @Test
        @DisplayName("重复设置恢复状态应该无副作用")
        void repeatedRestoreShouldHaveNoSideEffect() {
            // Given
            SoftDeletable entity = new TestSoftDeletable();
            entity.setDeleted(true);

            // When - 重复设置恢复
            entity.setDeleted(false);
            entity.setDeleted(false);

            // Then
            assertThat(entity.isDeleted()).isFalse();
        )
    )

    @Nested
    @DisplayName("与 SoftDeleteEntity 集成测试")
    class IntegrationWithSoftDeleteEntityTests {

        @Test
        @DisplayName("SoftDeleteEntity 应该实现 SoftDeletable 接口")
        void softDeleteEntityShouldImplementSoftDeletable() {
            // Given
            SoftDeleteEntity entity = new SoftDeleteEntity();

            // When & Then
            assertThat(entity).isInstanceOf(SoftDeletable.class);
        )

        @Test
        @DisplayName("SoftDeleteEntity 的 isDeleted 应该正确工作")
        void softDeleteEntityIsDeletedShouldWork() {
            // Given
            SoftDeleteEntity entity = new SoftDeleteEntity();

            // When & Then
            assertThat(entity.isDeleted()).isFalse();
            entity.setDeleted(true);
            assertThat(entity.isDeleted()).isTrue();
        )

        @Test
        @DisplayName("通过 SoftDeletable 接口操作 SoftDeleteEntity 应该有效")
        void operateSoftDeleteEntityViaInterfaceShouldWork() {
            // Given
            SoftDeletable entity = new SoftDeleteEntity();

            // When
            entity.setDeleted(true);

            // Then
            assertThat(entity.isDeleted()).isTrue();
        )
    )

    @Nested
    @DisplayName("业务场景测试")
    class BusinessScenarioTests {

        @Test
        @DisplayName("批量软删除操作")
        void batchSoftDeleteOperation() {
            // Given
            SoftDeletable[] entities = new SoftDeletable[10];
            for (int i = 0; i < 10; i++) {
                entities[i] = new TestSoftDeletable();
            )

            // When - 软删除前 5 个
            for (int i = 0; i < 5; i++) {
                entities[i].setDeleted(true);
            )

            // Then
            long deletedCount = java.util.Arrays.stream(entities)
                    .filter(SoftDeletable::isDeleted)
                    .count();
            long activeCount = java.util.Arrays.stream(entities)
                    .filter(e -> !e.isDeleted())
                    .count();

            assertThat(deletedCount).isEqualTo(5);
            assertThat(activeCount).isEqualTo(5);
        )

        @Test
        @DisplayName("条件查询过滤已删除数据")
        void conditionalQueryShouldFilterDeletedData() {
            // Given
            SoftDeletable[] entities = new SoftDeletable[5];
            for (int i = 0; i < 5; i++) {
                entities[i] = new TestSoftDeletable();
                if (i % 2 == 0) {
                    entities[i].setDeleted(true);
                )
            )

            // When - 只查询未删除的数据
            long activeCount = java.util.Arrays.stream(entities)
                    .filter(e -> !e.isDeleted())
                    .count();

            // Then
            assertThat(activeCount).isEqualTo(2); // 索引 1, 3 未删除
        )
    )

    /**
     * 测试 SoftDeletable 实现
     */
    static class TestSoftDeletable implements SoftDeletable {
        private @Nullable Boolean deleted;

        @Override
        public @Nullable Boolean getDeleted() {
            return deleted;
        )

        @Override
        public void setDeleted(@Nullable Boolean deleted) {
            this.deleted = deleted;
        )
    )
)