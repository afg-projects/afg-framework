package io.github.afgprojects.framework.data.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SoftDeleteEntity 测试
 */
@DisplayName("SoftDeleteEntity 测试")
class SoftDeleteEntityTest {

    @Nested
    @DisplayName("删除状态初始值测试")
    class InitialDeletedStateTests {

        @Test
        @DisplayName("新建实体应该未删除")
        void newEntityShouldNotBeDeleted() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("删除标记初始值应该为 false")
        void deletedFlagShouldBeFalseInitially() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity.deleted).isFalse();
        }
    }

    @Nested
    @DisplayName("删除操作测试")
    class DeleteOperationTests {

        @Test
        @DisplayName("markDeleted 应该将实体标记为已删除")
        void markDeletedShouldSetDeletedFlag() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.markDeleted();

            // Then
            assertThat(entity.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("setDeleted(true) 应该标记为已删除")
        void setDeletedTrueShouldMarkAsDeleted() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.setDeleted(true);

            // Then
            assertThat(entity.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("setDeleted(false) 应该保持未删除状态")
        void setDeletedFalseShouldKeepNotDeleted() {
            // Given
            TestEntity entity = new TestEntity();
            entity.markDeleted();

            // When
            entity.setDeleted(false);

            // Then
            assertThat(entity.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("恢复操作测试")
    class RestoreOperationTests {

        @Test
        @DisplayName("restore 应该将已删除实体恢复")
        void restoreShouldClearDeletedFlag() {
            // Given
            TestEntity entity = new TestEntity();
            entity.markDeleted();
            assertThat(entity.isDeleted()).isTrue();

            // When
            entity.restore();

            // Then
            assertThat(entity.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("对未删除实体调用 restore 应该无副作用")
        void restoreOnNotDeletedEntityShouldHaveNoEffect() {
            // Given
            TestEntity entity = new TestEntity();
            assertThat(entity.isDeleted()).isFalse();

            // When
            entity.restore();

            // Then
            assertThat(entity.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("多次恢复应该保持未删除状态")
        void multipleRestoreShouldKeepNotDeleted() {
            // Given
            TestEntity entity = new TestEntity();
            entity.markDeleted();

            // When
            entity.restore();
            entity.restore();
            entity.restore();

            // Then
            assertThat(entity.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("删除/恢复循环测试")
    class DeleteRestoreCycleTests {

        @Test
        @DisplayName("删除后恢复再删除应该正确工作")
        void deleteRestoreDeleteCycle() {
            // Given
            TestEntity entity = new TestEntity();

            // When - 删除
            entity.markDeleted();
            assertThat(entity.isDeleted()).isTrue();

            // 恢复
            entity.restore();
            assertThat(entity.isDeleted()).isFalse();

            // 再次删除
            entity.markDeleted();

            // Then
            assertThat(entity.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("多次删除应该保持已删除状态")
        void multipleDeleteShouldKeepDeleted() {
            // Given
            TestEntity entity = new TestEntity();

            // When
            entity.markDeleted();
            entity.markDeleted();
            entity.markDeleted();

            // Then
            assertThat(entity.isDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("SoftDeletable 接口测试")
    class SoftDeletableInterfaceTests {

        @Test
        @DisplayName("应该实现 SoftDeletable 接口")
        void shouldImplementSoftDeletableInterface() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity).isInstanceOf(SoftDeletable.class);
        }

        @Test
        @DisplayName("通过接口操作应该有效")
        void operationsViaInterfaceShouldWork() {
            // Given
            SoftDeletable entity = new TestEntity();

            // When
            entity.setDeleted(true);

            // Then
            assertThat(entity.isDeleted()).isTrue();

            // 恢复
            entity.setDeleted(false);
            assertThat(entity.isDeleted()).isFalse();
        }
    }

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
        }

        @Test
        @DisplayName("应该继承 id 字段")
        void shouldInheritIdField() {
            // Given
            TestEntity entity = new TestEntity();
            Long id = 456L;

            // When
            entity.setId(id);

            // Then
            assertThat(entity.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("应该继承时间戳字段")
        void shouldInheritTimestampFields() {
            // Given
            TestEntity entity = new TestEntity();
            LocalDateTime now = LocalDateTime.now();

            // When
            entity.setCreateTime(now);
            entity.setUpdateTime(now.plusHours(2));

            // Then
            assertThat(entity.getCreateTime()).isEqualTo(now);
            assertThat(entity.getUpdateTime()).isEqualTo(now.plusHours(2));
        }

        @Test
        @DisplayName("isNew 方法应该正确工作")
        void isNewShouldWorkCorrectly() {
            // Given
            TestEntity entity = new TestEntity();

            // When & Then
            assertThat(entity.isNew()).isTrue();
            entity.setId(1L);
            assertThat(entity.isNew()).isFalse();
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
        @DisplayName("toString 应该包含 id 和 deleted")
        void toStringShouldContainIdAndDeleted() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(42L);
            entity.markDeleted();

            // When
            String result = entity.toString();

            // Then
            assertThat(result).contains("id=42");
            assertThat(result).contains("deleted=true");
        }

        @Test
        @DisplayName("未删除实体 toString 应该显示 deleted=false")
        void toStringShouldShowDeletedFalse() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);

            // When
            String result = entity.toString();

            // Then
            assertThat(result).contains("deleted=false");
        }

        @Test
        @DisplayName("toString 格式应该正确")
        void toStringFormatShouldBeCorrect() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.markDeleted();

            // When
            String result = entity.toString();

            // Then
            assertThat(result).isEqualTo("TestEntity{id=1, deleted=true}");
        }
    }

    @Nested
    @DisplayName("业务场景测试")
    class BusinessScenarioTests {

        @Test
        @DisplayName("软删除不应该影响实体其他属性")
        void softDeleteShouldNotAffectOtherProperties() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(100L);
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());

            // When
            entity.markDeleted();

            // Then - 其他属性应该保持不变
            assertThat(entity.getId()).isEqualTo(100L);
            assertThat(entity.getCreateTime()).isNotNull();
            assertThat(entity.getUpdateTime()).isNotNull();
            assertThat(entity.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("查询已删除实体应该正确判断")
        void queryDeletedEntitiesShouldJudgeCorrectly() {
            // Given
            TestEntity deleted1 = new TestEntity();
            deleted1.markDeleted();

            TestEntity deleted2 = new TestEntity();
            deleted2.setDeleted(true);

            TestEntity notDeleted = new TestEntity();

            // When & Then
            assertThat(deleted1.isDeleted()).isTrue();
            assertThat(deleted2.isDeleted()).isTrue();
            assertThat(notDeleted.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("批量恢复操作应该正确")
        void batchRestoreShouldWork() {
            // Given
            TestEntity[] entities = new TestEntity[5];
            for (int i = 0; i < 5; i++) {
                entities[i] = new TestEntity();
                entities[i].setId((long) i);
                entities[i].markDeleted();
            }

            // When
            for (TestEntity entity : entities) {
                entity.restore();
            }

            // Then
            for (TestEntity entity : entities) {
                assertThat(entity.isDeleted()).isFalse();
            }
        }
    }

    /**
     * 测试实体类
     */
    static class TestEntity extends SoftDeleteEntity<Long> {
        // 用于测试的简单实体类
    }
}