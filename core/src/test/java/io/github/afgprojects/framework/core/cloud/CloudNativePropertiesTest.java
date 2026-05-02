package io.github.afgprojects.framework.core.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CloudNativeProperties 测试
 */
@DisplayName("CloudNativeProperties 测试")
class CloudNativePropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            CloudNativeProperties props = new CloudNativeProperties();

            assertThat(props.getKubernetes()).isNotNull();
            assertThat(props.getGracefulShutdown()).isNotNull();
            assertThat(props.getConfigExternalization()).isNotNull();
        }
    }

    @Nested
    @DisplayName("KubernetesConfig 测试")
    class KubernetesConfigTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            CloudNativeProperties.KubernetesConfig config = new CloudNativeProperties.KubernetesConfig();

            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getNamespace()).isNull();
            assertThat(config.getServiceAccount()).isNull();
            assertThat(config.getPodName()).isNull();
            assertThat(config.getPodIp()).isNull();
            assertThat(config.getNodeName()).isNull();
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            CloudNativeProperties.KubernetesConfig config = new CloudNativeProperties.KubernetesConfig();
            config.setEnabled(true);
            config.setNamespace("default");
            config.setServiceAccount("default-service");
            config.setPodName("my-pod");
            config.setPodIp("10.0.0.1");
            config.setNodeName("node-1");

            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getNamespace()).isEqualTo("default");
            assertThat(config.getServiceAccount()).isEqualTo("default-service");
            assertThat(config.getPodName()).isEqualTo("my-pod");
            assertThat(config.getPodIp()).isEqualTo("10.0.0.1");
            assertThat(config.getNodeName()).isEqualTo("node-1");
        }
    }

    @Nested
    @DisplayName("GracefulShutdownConfig 测试")
    class GracefulShutdownConfigTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            CloudNativeProperties.GracefulShutdownConfig config = new CloudNativeProperties.GracefulShutdownConfig();

            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(30));
            assertThat(config.isWaitForRequests()).isTrue();
            assertThat(config.getRequestWaitTimeout()).isEqualTo(Duration.ofSeconds(10));
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            CloudNativeProperties.GracefulShutdownConfig config = new CloudNativeProperties.GracefulShutdownConfig();
            config.setEnabled(false);
            config.setTimeout(Duration.ofSeconds(60));
            config.setWaitForRequests(false);
            config.setRequestWaitTimeout(Duration.ofSeconds(20));

            assertThat(config.isEnabled()).isFalse();
            assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(60));
            assertThat(config.isWaitForRequests()).isFalse();
            assertThat(config.getRequestWaitTimeout()).isEqualTo(Duration.ofSeconds(20));
        }
    }

    @Nested
    @DisplayName("ConfigExternalizationConfig 测试")
    class ConfigExternalizationConfigTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            CloudNativeProperties.ConfigExternalizationConfig config = new CloudNativeProperties.ConfigExternalizationConfig();

            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getConfigMap()).isNull();
            assertThat(config.getSecret()).isNull();
            assertThat(config.isAutoRefresh()).isTrue();
            assertThat(config.getRefreshInterval()).isEqualTo(Duration.ofMinutes(1));
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            CloudNativeProperties.ConfigExternalizationConfig config = new CloudNativeProperties.ConfigExternalizationConfig();
            config.setEnabled(false);
            config.setConfigMap("my-configmap");
            config.setSecret("my-secret");
            config.setAutoRefresh(false);
            config.setRefreshInterval(Duration.ofMinutes(5));

            assertThat(config.isEnabled()).isFalse();
            assertThat(config.getConfigMap()).isEqualTo("my-configmap");
            assertThat(config.getSecret()).isEqualTo("my-secret");
            assertThat(config.isAutoRefresh()).isFalse();
            assertThat(config.getRefreshInterval()).isEqualTo(Duration.ofMinutes(5));
        }
    }
}
