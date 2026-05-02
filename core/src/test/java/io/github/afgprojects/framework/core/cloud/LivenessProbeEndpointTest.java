package io.github.afgprojects.framework.core.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.cloud.KubernetesProbeProperties.ProbeConfig;

/**
 * LivenessProbeEndpoint 测试
 */
@DisplayName("LivenessProbeEndpoint 测试")
class LivenessProbeEndpointTest {

    private LivenessProbeEndpoint endpoint;
    private ProbeConfig config;

    @BeforeEach
    void setUp() {
        config = new ProbeConfig();
        config.setPath("/health/liveness");
        endpoint = new LivenessProbeEndpoint(config);
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该正确初始化")
        void shouldInitialize() {
            assertThat(endpoint).isNotNull();
        }
    }

    @Nested
    @DisplayName("getPath 测试")
    class GetPathTests {

        @Test
        @DisplayName("应该返回探针路径")
        void shouldReturnPath() {
            assertThat(endpoint.getPath()).isEqualTo("/health/liveness");
        }
    }

    @Nested
    @DisplayName("isHealthy 测试")
    class IsHealthyTests {

        @Test
        @DisplayName("默认应该健康")
        void shouldBeHealthyByDefault() {
            assertThat(endpoint.isHealthy()).isTrue();
        }
    }

    @Nested
    @DisplayName("setHealthy 测试")
    class SetHealthyTests {

        @Test
        @DisplayName("应该设置健康状态")
        void shouldSetHealthyStatus() {
            endpoint.setHealthy(false);

            assertThat(endpoint.isHealthy()).isFalse();
        }

        @Test
        @DisplayName("应该可以切换健康状态")
        void shouldToggleHealthyStatus() {
            endpoint.setHealthy(false);
            assertThat(endpoint.isHealthy()).isFalse();

            endpoint.setHealthy(true);
            assertThat(endpoint.isHealthy()).isTrue();
        }
    }

    @Nested
    @DisplayName("getConfig 测试")
    class GetConfigTests {

        @Test
        @DisplayName("应该返回探针配置")
        void shouldReturnConfig() {
            assertThat(endpoint.getConfig()).isSameAs(config);
        }
    }
}
