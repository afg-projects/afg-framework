package io.github.afgprojects.framework.data.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FullEntity 单元测试
 */
@DisplayName("FullEntity 测试")
class FullEntityTest {

    @Nested
    @DisplayName("同时实现 SoftDeletable/Versioned/Auditable")
    class InterfaceImplementationTests {

        @Test
        @DisplayName("FullEntity 是 SoftDeletable")
        void shouldBeSoftDeletable() {
            FullEntity entity = new ConcreteFullEntity();
            assertThat(entity).isInstanceOf(SoftDeletable.class);
        }

        @Test
        @DisplayName("FullEntity 是 Versioned")
        void shouldBeVersioned() {
            FullEntity entity = new ConcreteFullEntity();
            assertThat(entity).isInstanceOf(Versioned.class);
        }

        @Test
        @DisplayName("FullEntity 是 Auditable")
        void shouldBeAuditable() {
            FullEntity entity = new ConcreteFullEntity();
            assertThat(entity).isInstanceOf(Auditable.class);
        }

        @Test
        @DisplayName("FullEntity 继承 BaseEntity")
        void shouldExtendBaseEntity() {
            FullEntity entity = new ConcreteFullEntity();
            assertThat(entity).isInstanceOf(BaseEntity.class);
        }
    }

    @Nested
    @DisplayName("getter/setter")
    class GetterSetterTests {

        @Test
        @DisplayName("deleted getter/setter")
        void shouldGetAndSetDeleted() {
            FullEntity entity = new ConcreteFullEntity();
            entity.setDeleted(true);
            assertThat(entity.getDeleted()).isTrue();
        }

        @Test
        @DisplayName("version getter/setter")
        void shouldGetAndSetVersion() {
            FullEntity entity = new ConcreteFullEntity();
            entity.setVersion(5);
            assertThat(entity.getVersion()).isEqualTo(5);
        }

        @Test
        @DisplayName("createBy getter/setter")
        void shouldGetAndSetCreateBy() {
            FullEntity entity = new ConcreteFullEntity();
            entity.setCreateBy("admin");
            assertThat(entity.getCreateBy()).isEqualTo("admin");
        }

        @Test
        @DisplayName("updateBy getter/setter")
        void shouldGetAndSetUpdateBy() {
            FullEntity entity = new ConcreteFullEntity();
            entity.setUpdateBy("operator");
            assertThat(entity.getUpdateBy()).isEqualTo("operator");
        }

        @Test
        @DisplayName("id 继承自 BaseEntity")
        void shouldGetAndSetId() {
            FullEntity entity = new ConcreteFullEntity();
            entity.setId(42L);
            assertThat(entity.getId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("createdAt 继承自 BaseEntity")
        void shouldGetAndSetCreatedAt() {
            FullEntity entity = new ConcreteFullEntity();
            Instant now = Instant.now();
            entity.setCreatedAt(now);
            assertThat(entity.getCreatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("updatedAt 继承自 BaseEntity")
        void shouldGetAndSetUpdatedAt() {
            FullEntity entity = new ConcreteFullEntity();
            Instant now = Instant.now();
            entity.setUpdatedAt(now);
            assertThat(entity.getUpdatedAt()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("默认值")
    class DefaultValueTests {

        @Test
        @DisplayName("deleted 默认 false")
        void shouldDefaultDeletedToFalse() {
            FullEntity entity = new ConcreteFullEntity();
            assertThat(entity.getDeleted()).isFalse();
        }

        @Test
        @DisplayName("version 默认 0")
        void shouldDefaultVersionToZero() {
            FullEntity entity = new ConcreteFullEntity();
            assertThat(entity.getVersion()).isEqualTo(0);
        }

        @Test
        @DisplayName("createBy 默认 null")
        void shouldDefaultCreateByToNull() {
            FullEntity entity = new ConcreteFullEntity();
            assertThat(entity.getCreateBy()).isNull();
        }

        @Test
        @DisplayName("updateBy 默认 null")
        void shouldDefaultUpdateByToNull() {
            FullEntity entity = new ConcreteFullEntity();
            assertThat(entity.getUpdateBy()).isNull();
        }
    }

    @Nested
    @DisplayName("markDeleted")
    class MarkDeletedTests {

        @Test
        @DisplayName("markDeleted 设置 deleted 为 true")
        void shouldSetDeletedToTrue() {
            FullEntity entity = new ConcreteFullEntity();
            assertThat(entity.getDeleted()).isFalse();

            entity.markDeleted();

            assertThat(entity.getDeleted()).isTrue();
        }

        @Test
        @DisplayName("markDeleted 使 isDeleted 返回 true")
        void shouldMakeIsDeletedReturnTrue() {
            FullEntity entity = new ConcreteFullEntity();
            assertThat(entity.isDeleted()).isFalse();

            entity.markDeleted();

            assertThat(entity.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("重复 markDeleted 仍然为 true")
        void shouldRemainDeletedAfterDoubleMark() {
            FullEntity entity = new ConcreteFullEntity();
            entity.markDeleted();
            entity.markDeleted();

            assertThat(entity.getDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("incrementVersion")
    class IncrementVersionTests {

        @Test
        @DisplayName("incrementVersion 递增版本号")
        void shouldIncrementVersion() {
            FullEntity entity = new ConcreteFullEntity();
            assertThat(entity.getVersion()).isEqualTo(0);

            entity.incrementVersion();

            assertThat(entity.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("多次 incrementVersion 持续递增")
        void shouldIncrementMultipleTimes() {
            FullEntity entity = new ConcreteFullEntity();

            entity.incrementVersion();
            entity.incrementVersion();
            entity.incrementVersion();

            assertThat(entity.getVersion()).isEqualTo(3);
        }

        @Test
        @DisplayName("incrementVersion 从非零版本递增")
        void shouldIncrementFromNonZeroVersion() {
            FullEntity entity = new ConcreteFullEntity();
            entity.setVersion(5);

            entity.incrementVersion();

            assertThat(entity.getVersion()).isEqualTo(6);
        }
    }

    /**
     * FullEntity 的具体实现类，用于测试
     */
    static class ConcreteFullEntity extends FullEntity {
    }
}
