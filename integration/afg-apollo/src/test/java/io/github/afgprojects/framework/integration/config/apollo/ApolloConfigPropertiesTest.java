package io.github.afgprojects.framework.integration.config.apollo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApolloConfigProperties 单元测试
 */
@DisplayName("ApolloConfigProperties 测试")
class ApolloConfigPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            ApolloConfigProperties properties = new ApolloConfigProperties();

            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getNamespace()).isEqualTo("application");
            assertThat(properties.isCacheEnabled()).isTrue();
            assertThat(properties.getCacheDir()).isNull();
            assertThat(properties.getCluster()).isNull();
        }
    }

    @Nested
    @DisplayName("属性设置测试")
    class PropertySetterTests {

        @Test
        @DisplayName("应该正确设置 enabled 属性")
        void shouldSetEnabledProperty() {
            ApolloConfigProperties properties = new ApolloConfigProperties();
            properties.setEnabled(false);

            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("应该正确设置 namespace 属性")
        void shouldSetNamespaceProperty() {
            ApolloConfigProperties properties = new ApolloConfigProperties();
            properties.setNamespace("custom-namespace");

            assertThat(properties.getNamespace()).isEqualTo("custom-namespace");
        }

        @Test
        @DisplayName("应该正确设置 cacheEnabled 属性")
        void shouldSetCacheEnabledProperty() {
            ApolloConfigProperties properties = new ApolloConfigProperties();
            properties.setCacheEnabled(false);

            assertThat(properties.isCacheEnabled()).isFalse();
        }

        @Test
        @DisplayName("应该正确设置 cacheDir 属性")
        void shouldSetCacheDirProperty() {
            ApolloConfigProperties properties = new ApolloConfigProperties();
            properties.setCacheDir("/tmp/apollo-cache");

            assertThat(properties.getCacheDir()).isEqualTo("/tmp/apollo-cache");
        }

        @Test
        @DisplayName("应该正确设置 cluster 属性")
        void shouldSetClusterProperty() {
            ApolloConfigProperties properties = new ApolloConfigProperties();
            properties.setCluster("prod-cluster");

            assertThat(properties.getCluster()).isEqualTo("prod-cluster");
        }
    }

    @Nested
    @DisplayName("多实例测试")
    class MultipleInstanceTests {

        @Test
        @DisplayName("不同的实例应该有独立的属性值")
        void shouldHaveIndependentPropertyValues() {
            ApolloConfigProperties properties1 = new ApolloConfigProperties();
            ApolloConfigProperties properties2 = new ApolloConfigProperties();

            properties1.setNamespace("namespace-1");
            properties2.setNamespace("namespace-2");

            assertThat(properties1.getNamespace()).isEqualTo("namespace-1");
            assertThat(properties2.getNamespace()).isEqualTo("namespace-2");
        }
    }
}
