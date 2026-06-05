package io.github.afgprojects.framework.data.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VersionedEntity 测试
 */
@DisplayName("VersionedEntity 测试")
class VersionedEntityTest {

    @Nested
    @DisplayName("incrementVersion 方法")
    class IncrementVersionTests {

        @Test
        @DisplayName("version=null 时 incrementVersion 应设为 1")
        void shouldSetVersionTo1_whenNull() {
            VersionedEntity entity = new VersionedEntity();
            entity.setVersion(null);
            entity.incrementVersion();
            assertThat(entity.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("version=0 时 incrementVersion 应设为 1")
        void shouldSetVersionTo1_whenZero() {
            VersionedEntity entity = new VersionedEntity();
            entity.setVersion(0);
            entity.incrementVersion();
            assertThat(entity.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("version=5 时 incrementVersion 应设为 6")
        void shouldSetVersionTo6_whenFive() {
            VersionedEntity entity = new VersionedEntity();
            entity.setVersion(5);
            entity.incrementVersion();
            assertThat(entity.getVersion()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("Versioned 接口 default 方法")
    class VersionedDefaultMethodTests {

        @Test
        @DisplayName("直接实现 Versioned 的类应能使用 default 方法")
        void shouldUseDefaultMethods_whenDirectlyImplementing() {
            Versioned entity = new Versioned() {
                private Integer version = null;

                @Override
                public Integer getVersion() {
                    return version;
                }

                @Override
                public void setVersion(Integer version) {
                    this.version = version;
                }
            };

            assertThat(entity.getVersion()).isNull();
            entity.incrementVersion();
            assertThat(entity.getVersion()).isEqualTo(1);
            entity.incrementVersion();
            assertThat(entity.getVersion()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("version 默认值")
    class DefaultValueTests {

        @Test
        @DisplayName("新建实体 version 默认为 0")
        void shouldDefaultToZero_whenNewEntity() {
            VersionedEntity entity = new VersionedEntity();
            assertThat(entity.getVersion()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getter/setter")
    class GetterSetterTests {

        @Test
        @DisplayName("应能设置和获取 version")
        void shouldSetAndGetVersion() {
            VersionedEntity entity = new VersionedEntity();
            entity.setVersion(10);
            assertThat(entity.getVersion()).isEqualTo(10);
        }
    }
}
