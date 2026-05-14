package io.github.afgprojects.framework.core.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.cloud.KubernetesProbeProperties.ProbeConfig;

/**
 * {@link StartupProbeEndpoint} 的单元测试。
 * <p>
 * 测试 Kubernetes 启动探针端点的初始化、路径获取和启动状态管理功能。
 *
 * @see StartupProbeEndpoint
 * @see ProbeConfig
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

    /**
     * 构造函数测试分组。
     * <p>
     * 验证端点实例的正确初始化。
     */
    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        /**
         * 测试构造函数应正确初始化端点实例。
         */
        @Test
        @DisplayName("应该正确初始化")
        void shouldInitialize() {
            assertThat(endpoint).isNotNull();
        }
    }

    /**
     * getPath 测试分组。
     * <p>
     * 验证探针路径的获取功能。
     */
    @Nested
    @DisplayName("getPath 测试")
    class GetPathTests {

        /**
         * 测试 getPath 方法应返回配置中指定的探针路径。
         */
        @Test
        @DisplayName("应该返回探针路径")
        void shouldReturnPath() {
            assertThat(endpoint.getPath()).isEqualTo("/health/startup");
        }
    }

    /**
     * isStarted 测试分组。
     * <p>
     * 验证启动状态的默认值。
     */
    @Nested
    @DisplayName("isStarted 测试")
    class IsStartedTests {

        /**
         * 测试端点初始化后默认应为未启动状态。
         */
        @Test
        @DisplayName("默认应该未启动")
        void shouldNotBeStartedByDefault() {
            assertThat(endpoint.isStarted()).isFalse();
        }
    }

    /**
     * setStarted 测试分组。
     * <p>
     * 验证启动状态的设置和切换功能。
     */
    @Nested
    @DisplayName("setStarted 测试")
    class SetStartedTests {

        /**
         * 测试 setStarted 方法应能设置启动状态。
         */
        @Test
        @DisplayName("应该设置启动状态")
        void shouldSetStartedStatus() {
            endpoint.setStarted(true);

            assertThat(endpoint.isStarted()).isTrue();
        }

        /**
         * 测试启动状态应能多次切换。
         */
        @Test
        @DisplayName("应该可以切换启动状态")
        void shouldToggleStartedStatus() {
            endpoint.setStarted(true);
            assertThat(endpoint.isStarted()).isTrue();

            endpoint.setStarted(false);
            assertThat(endpoint.isStarted()).isFalse();
        }
    }

    /**
     * getConfig 测试分组。
     * <p>
     * 验证探针配置的获取功能。
     */
    @Nested
    @DisplayName("getConfig 测试")
    class GetConfigTests {

        /**
         * 测试 getConfig 方法应返回构造时传入的配置对象。
         */
        @Test
        @DisplayName("应该返回探针配置")
        void shouldReturnConfig() {
            assertThat(endpoint.getConfig()).isSameAs(config);
        }
    }
}
