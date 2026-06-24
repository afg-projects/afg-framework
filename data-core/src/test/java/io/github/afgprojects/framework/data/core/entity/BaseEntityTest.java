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
    @DisplayName("getter/setter 方法")
    class GetterSetterTests {

        @Test
        @DisplayName("id getter/setter")
        void shouldSetAndGetId() {
            TestEntity entity = new TestEntity();
            entity.setId("100");
            assertThat(entity.getId()).isEqualTo("100");
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
