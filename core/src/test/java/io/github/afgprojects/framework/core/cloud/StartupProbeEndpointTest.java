package io.github.afgprojects.framework.core.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.cloud.KubernetesProbeProperties.ProbeConfig;

/**
 * StartupProbeEndpoint 测试
 */
@DisplayName("StartupProbeEndpoint 测试")
class StartupProbeEndpointTest {

    private StartupProbeEndpoint endpoint;
    private ProbeConfig config;

    @BeforeEach
    void setUp() {
        config = new ProbeConfig();
        config.setPath("/health/startup");
        endpoint = new StartupProbeEndpoint(config);
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
            assertThat(endpoint.getPath()).isEqualTo("/health/startup");
        }
    }

    @Nested
    @DisplayName("isStarted 测试")
    class IsStartedTests {

        @Test
        @DisplayName("默认应该未启动")
        void shouldNotBeStartedByDefault() {
            assertThat(endpoint.isStarted()).isFalse();
        }
    }

    @Nested
    @DisplayName("setStarted 测试")
    class SetStartedTests {

        @Test
        @DisplayName("应该设置启动状态")
        void shouldSetStartedStatus() {
            endpoint.setStarted(true);

            assertThat(endpoint.isStarted()).isTrue();
        }

        @Test
        @DisplayName("应该可以切换启动状态")
        void shouldToggleStartedStatus() {
            endpoint.setStarted(true);
            assertThat(endpoint.isStarted()).isTrue();

            endpoint.setStarted(false);
            assertThat(endpoint.isStarted()).isFalse();
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
