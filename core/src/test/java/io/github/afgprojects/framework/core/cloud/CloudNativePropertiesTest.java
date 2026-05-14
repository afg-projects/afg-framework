package io.github.afgprojects.framework.core.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link CloudNativeProperties} 的单元测试。
 * <p>
 * 测试云原生配置属性类的默认值设置和属性读写功能，包括：
 * <ul>
 *   <li>Kubernetes 配置（命名空间、Pod 信息等）</li>
 *   <li>优雅停机配置（超时时间、请求等待等）</li>
 *   <li>配置外部化（ConfigMap、Secret 等）</li>
 * </ul>
 *
 * @see CloudNativeProperties
 * @see CloudNativeProperties.KubernetesConfig
 * @see CloudNativeProperties.GracefulShutdownConfig
 * @see CloudNativeProperties.ConfigExternalizationConfig
 */
@DisplayName("CloudNativeProperties 测试")
class CloudNativePropertiesTest {

    /**
     * 默认值测试分组。
     * <p>
     * 验证 CloudNativeProperties 实例化后各嵌套配置对象的默认初始化状态。
     */
    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        /**
         * 测试 CloudNativeProperties 实例化后应包含非空的嵌套配置对象。
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            CloudNativeProperties props = new CloudNativeProperties();

            assertThat(props.getKubernetes()).isNotNull();
            assertThat(props.getGracefulShutdown()).isNotNull();
            assertThat(props.getConfigExternalization()).isNotNull();
        }
    }

    /**
     * KubernetesConfig 配置测试分组。
     * <p>
     * 验证 Kubernetes 环境配置的默认值和属性设置功能。
     */
    @Nested
    @DisplayName("KubernetesConfig 测试")
    class KubernetesConfigTests {

        /**
         * 测试 KubernetesConfig 的默认属性值。
         */
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

        /**
         * 测试 KubernetesConfig 属性的 setter 和 getter 方法。
         */
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

    /**
     * GracefulShutdownConfig 配置测试分组。
     * <p>
     * 验证优雅停机配置的默认值和属性设置功能。
     */
    @Nested
    @DisplayName("GracefulShutdownConfig 测试")
    class GracefulShutdownConfigTests {

        /**
         * 测试 GracefulShutdownConfig 的默认属性值。
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            CloudNativeProperties.GracefulShutdownConfig config = new CloudNativeProperties.GracefulShutdownConfig();

            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(30));
            assertThat(config.isWaitForRequests()).isTrue();
            assertThat(config.getRequestWaitTimeout()).isEqualTo(Duration.ofSeconds(10));
        }

        /**
         * 测试 GracefulShutdownConfig 属性的 setter 和 getter 方法。
         */
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

    /**
     * ConfigExternalizationConfig 配置测试分组。
     * <p>
     * 验证配置外部化（ConfigMap、Secret）的默认值和属性设置功能。
     */
    @Nested
    @DisplayName("ConfigExternalizationConfig 测试")
    class ConfigExternalizationConfigTests {

        /**
         * 测试 ConfigExternalizationConfig 的默认属性值。
         */
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

        /**
         * 测试 ConfigExternalizationConfig 属性的 setter 和 getter 方法。
         */
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
