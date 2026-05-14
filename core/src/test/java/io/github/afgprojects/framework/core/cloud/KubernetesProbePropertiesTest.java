package io.github.afgprojects.framework.core.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link KubernetesProbeProperties} 的单元测试。
 * <p>
 * 测试 Kubernetes 探针配置属性类的默认值设置和属性读写功能，包括：
 * <ul>
 *   <li>存活探针（Liveness）配置</li>
 *   <li>就绪探针（Readiness）配置</li>
 *   <li>启动探针（Startup）配置</li>
 * </ul>
 *
 * @see KubernetesProbeProperties
 * @see KubernetesProbeProperties.ProbeConfig
 */
@DisplayName("KubernetesProbeProperties 测试")
class KubernetesProbePropertiesTest {

    /**
     * 默认值测试分组。
     * <p>
     * 验证 KubernetesProbeProperties 实例化后各嵌套配置对象的默认初始化状态。
     */
    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        /**
         * 测试 KubernetesProbeProperties 实例化后应包含非空的探针配置对象。
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            KubernetesProbeProperties props = new KubernetesProbeProperties();

            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getLiveness()).isNotNull();
            assertThat(props.getReadiness()).isNotNull();
            assertThat(props.getStartup()).isNotNull();
        }
    }

    /**
     * ProbeConfig 配置测试分组。
     * <p>
     * 验证探针配置的默认值和属性设置功能。
     */
    @Nested
    @DisplayName("ProbeConfig 测试")
    class ProbeConfigTests {

        /**
         * 测试 ProbeConfig 的默认属性值。
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            KubernetesProbeProperties.ProbeConfig config = new KubernetesProbeProperties.ProbeConfig();

            assertThat(config.getPath()).isEqualTo("/health/default");
            assertThat(config.getInitialDelay()).isEqualTo(Duration.ofSeconds(10));
            assertThat(config.getPeriod()).isEqualTo(Duration.ofSeconds(10));
            assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(5));
            assertThat(config.getSuccessThreshold()).isEqualTo(1);
            assertThat(config.getFailureThreshold()).isEqualTo(3);
        }

        /**
         * 测试 ProbeConfig 属性的 setter 和 getter 方法。
         */
        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            KubernetesProbeProperties.ProbeConfig config = new KubernetesProbeProperties.ProbeConfig();
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
     * 验证 KubernetesProbeProperties 各探针配置的设置功能。
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
            KubernetesProbeProperties props = new KubernetesProbeProperties();
            props.setEnabled(false);

            KubernetesProbeProperties.ProbeConfig liveness = new KubernetesProbeProperties.ProbeConfig();
            liveness.setPath("/liveness");
            props.setLiveness(liveness);

            KubernetesProbeProperties.ProbeConfig readiness = new KubernetesProbeProperties.ProbeConfig();
            readiness.setPath("/readiness");
            props.setReadiness(readiness);

            KubernetesProbeProperties.ProbeConfig startup = new KubernetesProbeProperties.ProbeConfig();
            startup.setPath("/startup");
            props.setStartup(startup);

            assertThat(props.isEnabled()).isFalse();
            assertThat(props.getLiveness().getPath()).isEqualTo("/liveness");
            assertThat(props.getReadiness().getPath()).isEqualTo("/readiness");
            assertThat(props.getStartup().getPath()).isEqualTo("/startup");
        }
    }
}
