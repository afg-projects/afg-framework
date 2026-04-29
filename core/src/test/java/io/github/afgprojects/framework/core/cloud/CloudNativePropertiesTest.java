package io.github.afgprojects.framework.core.cloud;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CloudNativeProperties 测试
 */
@DisplayName("CloudNativeProperties 测试")
class CloudNativePropertiesTest {

    @Test
    @DisplayName("应该使用默认值创建 CloudNativeProperties")
    void shouldCreateWithDefaults() {
        CloudNativeProperties properties = new CloudNativeProperties();
        assertNotNull(properties.getKubernetes());
        assertNotNull(properties.getGracefulShutdown());
        assertNotNull(properties.getConfigExternalization());
    }

    @Test
    @DisplayName("KubernetesConfig 应该有默认值")
    void kubernetesConfigShouldHaveDefaults() {
        CloudNativeProperties.KubernetesConfig config = new CloudNativeProperties.KubernetesConfig();
        assertTrue(config.isEnabled());
        assertNull(config.getNamespace());
        assertNull(config.getServiceAccount());
        assertNull(config.getPodName());
        assertNull(config.getPodIp());
        assertNull(config.getNodeName());
    }

    @Test
    @DisplayName("KubernetesConfig 应该正确设置属性")
    void kubernetesConfigShouldSetProperties() {
        CloudNativeProperties.KubernetesConfig config = new CloudNativeProperties.KubernetesConfig();
        config.setEnabled(false);
        config.setNamespace("default");
        config.setServiceAccount("my-sa");
        config.setPodName("my-pod");
        config.setPodIp("10.0.0.1");
        config.setNodeName("node-1");

        assertFalse(config.isEnabled());
        assertEquals("default", config.getNamespace());
        assertEquals("my-sa", config.getServiceAccount());
        assertEquals("my-pod", config.getPodName());
        assertEquals("10.0.0.1", config.getPodIp());
        assertEquals("node-1", config.getNodeName());
    }

    @Test
    @DisplayName("GracefulShutdownConfig 应该有默认值")
    void gracefulShutdownConfigShouldHaveDefaults() {
        CloudNativeProperties.GracefulShutdownConfig config = new CloudNativeProperties.GracefulShutdownConfig();
        assertTrue(config.isEnabled());
        assertEquals(Duration.ofSeconds(30), config.getTimeout());
        assertTrue(config.isWaitForRequests());
        assertEquals(Duration.ofSeconds(10), config.getRequestWaitTimeout());
    }

    @Test
    @DisplayName("GracefulShutdownConfig 应该正确设置属性")
    void gracefulShutdownConfigShouldSetProperties() {
        CloudNativeProperties.GracefulShutdownConfig config = new CloudNativeProperties.GracefulShutdownConfig();
        config.setEnabled(false);
        config.setTimeout(Duration.ofSeconds(60));
        config.setWaitForRequests(false);
        config.setRequestWaitTimeout(Duration.ofSeconds(20));

        assertFalse(config.isEnabled());
        assertEquals(Duration.ofSeconds(60), config.getTimeout());
        assertFalse(config.isWaitForRequests());
        assertEquals(Duration.ofSeconds(20), config.getRequestWaitTimeout());
    }

    @Test
    @DisplayName("ConfigExternalizationConfig 应该有默认值")
    void configExternalizationConfigShouldHaveDefaults() {
        CloudNativeProperties.ConfigExternalizationConfig config = new CloudNativeProperties.ConfigExternalizationConfig();
        assertTrue(config.isEnabled());
        assertNull(config.getConfigMap());
        assertNull(config.getSecret());
        assertTrue(config.isAutoRefresh());
        assertEquals(Duration.ofMinutes(1), config.getRefreshInterval());
    }

    @Test
    @DisplayName("ConfigExternalizationConfig 应该正确设置属性")
    void configExternalizationConfigShouldSetProperties() {
        CloudNativeProperties.ConfigExternalizationConfig config = new CloudNativeProperties.ConfigExternalizationConfig();
        config.setEnabled(false);
        config.setConfigMap("my-config");
        config.setSecret("my-secret");
        config.setAutoRefresh(false);
        config.setRefreshInterval(Duration.ofMinutes(5));

        assertFalse(config.isEnabled());
        assertEquals("my-config", config.getConfigMap());
        assertEquals("my-secret", config.getSecret());
        assertFalse(config.isAutoRefresh());
        assertEquals(Duration.ofMinutes(5), config.getRefreshInterval());
    }
}
