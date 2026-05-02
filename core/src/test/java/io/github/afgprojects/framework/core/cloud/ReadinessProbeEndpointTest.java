package io.github.afgprojects.framework.core.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.cloud.KubernetesProbeProperties.ProbeConfig;

/**
 * ReadinessProbeEndpoint 测试
 */
@DisplayName("ReadinessProbeEndpoint 测试")
class ReadinessProbeEndpointTest {

    private ReadinessProbeEndpoint endpoint;
    private ProbeConfig config;

    @BeforeEach
    void setUp() {
        config = new ProbeConfig();
        config.setPath("/health/readiness");
        endpoint = new ReadinessProbeEndpoint(config);
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
            assertThat(endpoint.getPath()).isEqualTo("/health/readiness");
        }
    }

    @Nested
    @DisplayName("isReady 测试")
    class IsReadyTests {

        @Test
        @DisplayName("默认应该未就绪")
        void shouldNotBeReadyByDefault() {
            assertThat(endpoint.isReady()).isFalse();
        }
    }

    @Nested
    @DisplayName("setReady 测试")
    class SetReadyTests {

        @Test
        @DisplayName("应该设置就绪状态")
        void shouldSetReadyStatus() {
            endpoint.setReady(false);

            assertThat(endpoint.isReady()).isFalse();
        }

        @Test
        @DisplayName("应该可以切换就绪状态")
        void shouldToggleReadyStatus() {
            endpoint.setReady(false);
            assertThat(endpoint.isReady()).isFalse();

            endpoint.setReady(true);
            assertThat(endpoint.isReady()).isTrue();
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
