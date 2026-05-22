package io.github.afgprojects.framework.core.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * {@link AfgCoreProperties.CloudNativeConfig.ProbeConfig} 的单元测试。
 * <p>
 * 测试 Kubernetes 探针配置属性类的默认值设置和属性读写功能，包括：
 * <ul>
 *   <li>存活探针（Liveness）配置</li>
 *   <li>就绪探针（Readiness）配置</li>
 *   <li>启动探针（Startup）配置</li>
 * </ul>
 *
 * @see AfgCoreProperties.CloudNativeConfig.ProbeConfig
 * @see AfgCoreProperties.CloudNativeConfig.ProbeDetailConfig
 */
@DisplayName("KubernetesProbeProperties 测试")
class KubernetesProbePropertiesTest {

    /**
     * 默认值测试分组。
     * <p>
     * 验证 ProbeConfig 实例化后各嵌套配置对象的默认初始化状态。
     */
    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        /**
         * 测试 ProbeConfig 实例化后应包含非空的探针配置对象。
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            AfgCoreProperties.CloudNativeConfig.ProbeConfig props = new AfgCoreProperties.CloudNativeConfig.ProbeConfig();

            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getLiveness()).isNotNull();
            assertThat(props.getReadiness()).isNotNull();
            assertThat(props.getStartup()).isNotNull();
        }
    }

    /**
     * ProbeDetailConfig 配置测试分组。
     * <p>
     * 验证探针配置的默认值和属性设置功能。
     */
    @Nested
    @DisplayName("ProbeDetailConfig 测试")
    class ProbeDetailConfigTests {

        /**
         * 测试 ProbeDetailConfig 的默认属性值。
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            AfgCoreProperties.CloudNativeConfig.ProbeDetailConfig config = new AfgCoreProperties.CloudNativeConfig.ProbeDetailConfig();

            assertThat(config.getPath()).isEqualTo("/health/default");
            assertThat(config.getInitialDelay()).isEqualTo(Duration.ofSeconds(10));
            assertThat(config.getPeriod()).isEqualTo(Duration.ofSeconds(10));
            assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(5));
            assertThat(config.getSuccessThreshold()).isEqualTo(1);
            assertThat(config.getFailureThreshold()).isEqualTo(3);
        }

        /**
         * 测试 ProbeDetailConfig 属性的 setter 和 getter 方法。
         */
        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            AfgCoreProperties.CloudNativeConfig.ProbeDetailConfig config = new AfgCoreProperties.CloudNativeConfig.ProbeDetailConfig();
            config.setPath("/health/custom");
            config.setInitialDelay(Duration.ofSeconds(20));
            config.setPeriod(Duration.ofSeconds(5));
            config.setTimeout(Duration.ofSeconds(3));
            config.setSuccessThreshold(2);
            config.setFailureThreshold(5);

            assertThat(config.getPath()).isEqualTo("/health/custom");
            assertThat(config.getInitialDelay()).isEqualTo(Duration.ofSeconds(20));
            assertThat(config.getPeriod()).isEqualTo(Duration.ofSeconds(5));
            assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(3));
            assertThat(config.getSuccessThreshold()).isEqualTo(2);
            assertThat(config.getFailureThreshold()).isEqualTo(5);
        }
    }

    /**
     * 设置属性测试分组。
     * <p>
     * 验证 ProbeConfig 各探针配置的设置功能。
     */
    @Nested
    @DisplayName("设置属性测试")
    class SetPropertiesTests {

        /**
         * 测试应能正确设置存活、就绪、启动探针的配置。
         */
        @Test
        @DisplayName("应该正确设置探针配置")
        void shouldSetProbeConfigs() {
            AfgCoreProperties.CloudNativeConfig.ProbeConfig props = new AfgCoreProperties.CloudNativeConfig.ProbeConfig();
            props.setEnabled(false);

            AfgCoreProperties.CloudNativeConfig.ProbeDetailConfig liveness = new AfgCoreProperties.CloudNativeConfig.ProbeDetailConfig();
            liveness.setPath("/liveness");
            props.setLiveness(liveness);

            AfgCoreProperties.CloudNativeConfig.ProbeDetailConfig readiness = new AfgCoreProperties.CloudNativeConfig.ProbeDetailConfig();
            readiness.setPath("/readiness");
            props.setReadiness(readiness);

            AfgCoreProperties.CloudNativeConfig.ProbeDetailConfig startup = new AfgCoreProperties.CloudNativeConfig.ProbeDetailConfig();
            startup.setPath("/startup");
            props.setStartup(startup);

            assertThat(props.isEnabled()).isFalse();
            assertThat(props.getLiveness().getPath()).isEqualTo("/liveness");
            assertThat(props.getReadiness().getPath()).isEqualTo("/readiness");
            assertThat(props.getStartup().getPath()).isEqualTo("/startup");
        }
    }
}
