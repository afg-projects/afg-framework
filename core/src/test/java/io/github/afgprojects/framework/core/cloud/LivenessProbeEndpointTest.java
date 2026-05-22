package io.github.afgprojects.framework.core.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties.CloudNativeConfig.ProbeDetailConfig;

/**
 * {@link LivenessProbeEndpoint} 的单元测试。
 * <p>
 * 测试 Kubernetes 存活探针端点的初始化、路径获取和健康状态管理功能。
 *
 * @see LivenessProbeEndpoint
 * @see ProbeDetailConfig
 */
@DisplayName("LivenessProbeEndpoint 测试")
class LivenessProbeEndpointTest {

    private LivenessProbeEndpoint endpoint;
    private ProbeDetailConfig config;

    @BeforeEach
    void setUp() {
        config = new ProbeDetailConfig();
        config.setPath("/health/liveness");
        endpoint = new LivenessProbeEndpoint(config);
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
            assertThat(endpoint.getPath()).isEqualTo("/health/liveness");
        }
    }

    /**
     * isHealthy 测试分组。
     * <p>
     * 验证健康状态的默认值。
     */
    @Nested
    @DisplayName("isHealthy 测试")
    class IsHealthyTests {

        /**
         * 测试端点初始化后默认应为健康状态。
         */
        @Test
        @DisplayName("默认应该健康")
        void shouldBeHealthyByDefault() {
            assertThat(endpoint.isHealthy()).isTrue();
        }
    }

    /**
     * setHealthy 测试分组。
     * <p>
     * 验证健康状态的设置和切换功能。
     */
    @Nested
    @DisplayName("setHealthy 测试")
    class SetHealthyTests {

        /**
         * 测试 setHealthy 方法应能设置健康状态。
         */
        @Test
        @DisplayName("应该设置健康状态")
        void shouldSetHealthyStatus() {
            endpoint.setHealthy(false);

            assertThat(endpoint.isHealthy()).isFalse();
        }

        /**
         * 测试健康状态应能多次切换。
         */
        @Test
        @DisplayName("应该可以切换健康状态")
        void shouldToggleHealthyStatus() {
            endpoint.setHealthy(false);
            assertThat(endpoint.isHealthy()).isFalse();

            endpoint.setHealthy(true);
            assertThat(endpoint.isHealthy()).isTrue();
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
