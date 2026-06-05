package io.github.afgprojects.framework.data.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SoftDeleteEntity 测试
 */
@DisplayName("SoftDeleteEntity 测试")
class SoftDeleteEntityTest {

    @Nested
    @DisplayName("markDeleted 方法")
    class MarkDeletedTests {

        @Test
        @DisplayName("markDeleted() 应设置 deleted=true")
        void shouldSetDeletedTrue_whenMarkDeleted() {
            SoftDeleteEntity entity = new SoftDeleteEntity();
            entity.markDeleted();
            assertThat(entity.getDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("markNotDeleted 方法")
    class MarkNotDeletedTests {

        @Test
        @DisplayName("markNotDeleted() 应设置 deleted=false")
        void shouldSetDeletedFalse_whenMarkNotDeleted() {
            SoftDeleteEntity entity = new SoftDeleteEntity();
            entity.setDeleted(true);
            entity.markNotDeleted();
            assertThat(entity.getDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("isDeleted 方法")
    class IsDeletedTests {

        @Test
        @DisplayName("deleted=true 应返回 true")
        void shouldReturnTrue_whenDeletedIsTrue() {
            SoftDeleteEntity entity = new SoftDeleteEntity();
            entity.setDeleted(true);
            assertThat(entity.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("deleted=false 应返回 false")
        void shouldReturnFalse_whenDeletedIsFalse() {
            SoftDeleteEntity entity = new SoftDeleteEntity();
            entity.setDeleted(false);
            assertThat(entity.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("deleted=null 应返回 false")
        void shouldReturnFalse_whenDeletedIsNull() {
            SoftDeleteEntity entity = new SoftDeleteEntity();
            entity.setDeleted(null);
            assertThat(entity.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("SoftDeletable 接口 default 方法")
    class SoftDeletableDefaultMethodTests {

        @Test
        @DisplayName("直接实现 SoftDeletable 的类应能使用 default 方法")
        void shouldUseDefaultMethods_whenDirectlyImplementing() {
            SoftDeletable entity = new SoftDeletable() {
                private Boolean deleted = false;

                @Override
                public Boolean getDeleted() {
                    return deleted;
                }

                @Override
                public void setDeleted(Boolean deleted) {
                    this.deleted = deleted;
                }
            };

            assertThat(entity.isDeleted()).isFalse();
            entity.markDeleted();
            assertThat(entity.isDeleted()).isTrue();
            entity.markNotDeleted();
            assertThat(entity.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("deleted 默认值")
    class DefaultValueTests {

        @Test
        @DisplayName("新建实体 deleted 默认为 false")
        void shouldDefaultToFalse_whenNewEntity() {
            SoftDeleteEntity entity = new SoftDeleteEntity();
            assertThat(entity.getDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("getter/setter")
    class GetterSetterTests {

        @Test
        @DisplayName("应能设置和获取 deleted")
        void shouldSetAndGetDeleted() {
            SoftDeleteEntity entity = new SoftDeleteEntity();
            entity.setDeleted(true);
            assertThat(entity.getDeleted()).isTrue();
            entity.setDeleted(false);
            assertThat(entity.getDeleted()).isFalse();
            entity.setDeleted(null);
            assertThat(entity.getDeleted()).isNull();
        }
    }
}
