package io.github.afgprojects.framework.data.core.entity;

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

    /**
     * 测试用具体子类，因为 BaseEntity 是 abstract
     */
    static class TestEntity extends BaseEntity {
    }

    @Nested
    @DisplayName("equals 方法")
    class EqualsTests {

        @Test
        @DisplayName("同一对象应返回 true")
        void shouldReturnTrue_whenSameObject() {
            TestEntity entity = new TestEntity();
            assertThat(entity.equals(entity)).isTrue();
        }

        @Test
        @DisplayName("null 应返回 false")
        void shouldReturnFalse_whenNull() {
            TestEntity entity = new TestEntity();
            assertThat(entity.equals(null)).isFalse();
        }

        @Test
        @DisplayName("不同类应返回 false")
        void shouldReturnFalse_whenDifferentClass() {
            TestEntity entity = new TestEntity();
            assertThat(entity.equals("string")).isFalse();
        }

        @Test
        @DisplayName("双方 id=null 应返回 false（新建实体使用对象身份）")
        void shouldReturnFalse_whenBothIdNull() {
            TestEntity entity1 = new TestEntity();
            TestEntity entity2 = new TestEntity();
            assertThat(entity1.equals(entity2)).isFalse();
        }

        @Test
        @DisplayName("一方 id=null 另一方 id!=null 应返回 false")
        void shouldReturnFalse_whenOneIdNull() {
            TestEntity entity1 = new TestEntity();
            TestEntity entity2 = new TestEntity();
            entity2.setId(1L);
            assertThat(entity1.equals(entity2)).isFalse();
            assertThat(entity2.equals(entity1)).isFalse();
        }

        @Test
        @DisplayName("双方 id 相同应返回 true")
        void shouldReturnTrue_whenSameId() {
            TestEntity entity1 = new TestEntity();
            TestEntity entity2 = new TestEntity();
            entity1.setId(1L);
            entity2.setId(1L);
            assertThat(entity1.equals(entity2)).isTrue();
        }

        @Test
        @DisplayName("双方 id 不同应返回 false")
        void shouldReturnFalse_whenDifferentId() {
            TestEntity entity1 = new TestEntity();
            TestEntity entity2 = new TestEntity();
            entity1.setId(1L);
            entity2.setId(2L);
            assertThat(entity1.equals(entity2)).isFalse();
        }
    }

    @Nested
    @DisplayName("hashCode 方法")
    class HashCodeTests {

        @Test
        @DisplayName("id=null 应返回 System.identityHashCode")
        void shouldReturnIdentityHashCode_whenIdNull() {
            TestEntity entity = new TestEntity();
            assertThat(entity.hashCode()).isEqualTo(System.identityHashCode(entity));
        }

        @Test
        @DisplayName("id!=null 应返回 id.hashCode")
        void shouldReturnIdHashCode_whenIdNotNull() {
            TestEntity entity = new TestEntity();
            entity.setId(42L);
            assertThat(entity.hashCode()).isEqualTo(Long.hashCode(42L));
        }
    }

    @Nested
    @DisplayName("toString 方法")
    class ToStringTests {

        @Test
        @DisplayName("id!=null 应返回 SimpleClassName(id=X)")
        void shouldReturnSimpleClassNameWithId_whenIdNotNull() {
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            assertThat(entity.toString()).isEqualTo("TestEntity(id=1)");
        }

        @Test
        @DisplayName("id=null 应返回 SimpleClassName(id=null)")
        void shouldReturnSimpleClassNameWithNull_whenIdNull() {
            TestEntity entity = new TestEntity();
            assertThat(entity.toString()).isEqualTo("TestEntity(id=null)");
        }
    }

    @Nested
    @DisplayName("getter/setter 方法")
    class GetterSetterTests {

        @Test
        @DisplayName("id getter/setter")
        void shouldSetAndGetId() {
            TestEntity entity = new TestEntity();
            entity.setId(100L);
            assertThat(entity.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("createdAt getter/setter")
        void shouldSetAndGetCreatedAt() {
            TestEntity entity = new TestEntity();
            Instant now = Instant.now();
            entity.setCreatedAt(now);
            assertThat(entity.getCreatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("updatedAt getter/setter")
        void shouldSetAndGetUpdatedAt() {
            TestEntity entity = new TestEntity();
            Instant now = Instant.now();
            entity.setUpdatedAt(now);
            assertThat(entity.getUpdatedAt()).isEqualTo(now);
        }
    }
}
