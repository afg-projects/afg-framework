package io.github.afgprojects.framework.data.core.entity;

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
        @DisplayName("isDeleted 方法应该返回删除状态")
        void isDeletedShouldReturnDeletedStatus() {
            // Given
            SoftDeletable entity = new TestSoftDeletable();

            // When & Then
            assertThat(entity.isDeleted()).isFalse();

            entity.setDeleted(true);
            assertThat(entity.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("setDeleted 方法应该设置删除状态")
        void setDeletedShouldSetDeletedStatus() {
            // Given
            SoftDeletable entity = new TestSoftDeletable();

            // When
            entity.setDeleted(true);

            // Then
            assertThat(entity.isDeleted()).isTrue();

            // When
            entity.setDeleted(false);

            // Then
            assertThat(entity.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("软删除状态转换测试")
    class StateTransitionTests {

        @Test
        @DisplayName("未删除 -> 已删除转换应该正确")
        void notDeletedToDeletedTransitionShouldWork() {
            // Given
            SoftDeletable entity = new TestSoftDeletable();
            assertThat(entity.isDeleted()).isFalse();

            // When
            entity.setDeleted(true);

            // Then
            assertThat(entity.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("已删除 -> 未删除转换应该正确")
        void deletedToNotDeletedTransitionShouldWork() {
            // Given
            SoftDeletable entity = new TestSoftDeletable();
            entity.setDeleted(true);
            assertThat(entity.isDeleted()).isTrue();

            // When
            entity.setDeleted(false);

            // Then
            assertThat(entity.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("多次状态转换应该正确")
        void multipleStateTransitionsShouldWork() {
            // Given
            SoftDeletable entity = new TestSoftDeletable();

            // When & Then
            entity.setDeleted(true);
            assertThat(entity.isDeleted()).isTrue();

            entity.setDeleted(false);
            assertThat(entity.isDeleted()).isFalse();

            entity.setDeleted(true);
            assertThat(entity.isDeleted()).isTrue();

            entity.setDeleted(false);
            assertThat(entity.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("业务场景测试")
    class BusinessScenarioTests {

        @Test
        @DisplayName("软删除标记应该用于过滤查询")
        void softDeleteMarkShouldBeUsedForQueryFilter() {
            // Given - 创建一组实体
            SoftDeletable[] entities = new SoftDeletable[10];
            for (int i = 0; i < 10; i++) {
                entities[i] = new TestSoftDeletable();
                // 部分标记为已删除
                if (i % 3 == 0) {
                    entities[i].setDeleted(true);
                }
            }

            // When - 查询未删除的实体
            long notDeletedCount = java.util.Arrays.stream(entities)
                    .filter(e -> !e.isDeleted())
                    .count();

            // Then
            assertThat(notDeletedCount).isEqualTo(6); // 10 - 4 = 6 (i % 3 == 0 for i=0,3,6,9)
        }

        @Test
        @DisplayName("软删除标记应该用于过滤已删除实体")
        void softDeleteMarkShouldBeUsedForDeletedFilter() {
            // Given
            SoftDeletable[] entities = new SoftDeletable[10];
            for (int i = 0; i < 10; i++) {
                entities[i] = new TestSoftDeletable();
                if (i % 2 == 0) {
                    entities[i].setDeleted(true);
                }
            }

            // When - 查询已删除的实体
            long deletedCount = java.util.Arrays.stream(entities)
                    .filter(SoftDeletable::isDeleted)
                    .count();

            // Then
            assertThat(deletedCount).isEqualTo(5);
        }

        @Test
        @DisplayName("批量恢复应该正确")
        void batchRestoreShouldWork() {
            // Given - 所有实体都标记为已删除
            SoftDeletable[] entities = new SoftDeletable[5];
            for (int i = 0; i < 5; i++) {
                entities[i] = new TestSoftDeletable();
                entities[i].setDeleted(true);
            }

            // When - 批量恢复
            for (SoftDeletable entity : entities) {
                entity.setDeleted(false);
            }

            // Then
            for (SoftDeletable entity : entities) {
                assertThat(entity.isDeleted()).isFalse();
            }
        }

        @Test
        @DisplayName("批量删除应该正确")
        void batchDeleteShouldWork() {
            // Given - 所有实体都未删除
            SoftDeletable[] entities = new SoftDeletable[5];
            for (int i = 0; i < 5; i++) {
                entities[i] = new TestSoftDeletable();
                entities[i].setDeleted(false);
            }

            // When - 批量删除
            for (SoftDeletable entity : entities) {
                entity.setDeleted(true);
            }

            // Then
            for (SoftDeletable entity : entities) {
                assertThat(entity.isDeleted()).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("与其他接口组合测试")
    class InterfaceCompositionTests {

        @Test
        @DisplayName("实体可以同时实现多个接口")
        void entityCanImplementMultipleInterfaces() {
            // Given
            MultiInterfaceEntity entity = new MultiInterfaceEntity();

            // When & Then - SoftDeletable 功能
            assertThat(entity.isDeleted()).isFalse();
            entity.setDeleted(true);
            assertThat(entity.isDeleted()).isTrue();

            // Versioned 功能
            assertThat(entity.getVersion()).isEqualTo(0L);
            entity.incrementVersion();
            assertThat(entity.getVersion()).isEqualTo(1L);
        }

        @Test
        @DisplayName("软删除状态应该不影响版本号")
        void softDeleteShouldNotAffectVersion() {
            // Given
            MultiInterfaceEntity entity = new MultiInterfaceEntity();
            entity.setVersion(5L);

            // When - 软删除
            entity.setDeleted(true);

            // Then - 版本号不变
            assertThat(entity.getVersion()).isEqualTo(5L);
        }
    }

    /**
     * 测试 SoftDeletable 实现
     */
    static class TestSoftDeletable implements SoftDeletable {
        private boolean deleted = false;

        @Override
        public boolean isDeleted() {
            return deleted;
        }

        @Override
        public void setDeleted(boolean deleted) {
            this.deleted = deleted;
        }
    }

    /**
     * 多接口实现测试实体
     */
    static class MultiInterfaceEntity implements SoftDeletable, Versioned {
        private boolean deleted = false;
        private long version = 0L;

        @Override
        public boolean isDeleted() {
            return deleted;
        }

        @Override
        public void setDeleted(boolean deleted) {
            this.deleted = deleted;
        }

        @Override
        public long getVersion() {
            return version;
        }

        @Override
        public void setVersion(long version) {
            this.version = version;
        }
    }
}