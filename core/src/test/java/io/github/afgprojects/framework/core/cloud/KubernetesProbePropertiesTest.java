package io.github.afgprojects.framework.core.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * KubernetesProbeProperties 测试
 */
@DisplayName("KubernetesProbeProperties 测试")
class KubernetesProbePropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

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

    @Nested
    @DisplayName("ProbeConfig 测试")
    class ProbeConfigTests {

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

    @Nested
    @DisplayName("设置属性测试")
    class SetPropertiesTests {

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
