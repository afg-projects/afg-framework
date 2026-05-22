package io.github.afgprojects.framework.core.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties.CloudNativeConfig.ProbeDetailConfig;

/**
 * {@link ReadinessProbeEndpoint} 的单元测试。
 * <p>
 * 测试 Kubernetes 就绪探针端点的初始化、路径获取和就绪状态管理功能。
 *
 * @see ReadinessProbeEndpoint
 * @see ProbeDetailConfig
 */
@DisplayName("ReadinessProbeEndpoint 测试")
class ReadinessProbeEndpointTest {

    private ReadinessProbeEndpoint endpoint;
    private ProbeDetailConfig config;

    @BeforeEach
    void setUp() {
        config = new ProbeDetailConfig();
        config.setPath("/health/readiness");
        endpoint = new ReadinessProbeEndpoint(config);
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
            assertThat(endpoint.getPath()).isEqualTo("/health/readiness");
        }
    }

    /**
     * isReady 测试分组。
     * <p>
     * 验证就绪状态的默认值。
     */
    @Nested
    @DisplayName("isReady 测试")
    class IsReadyTests {

        /**
         * 测试端点初始化后默认应为未就绪状态。
         */
        @Test
        @DisplayName("默认应该未就绪")
        void shouldNotBeReadyByDefault() {
            assertThat(endpoint.isReady()).isFalse();
        }
    }

    /**
     * setReady 测试分组。
     * <p>
     * 验证就绪状态的设置和切换功能。
     */
    @Nested
    @DisplayName("setReady 测试")
    class SetReadyTests {

        /**
         * 测试 setReady 方法应能设置就绪状态。
         */
        @Test
        @DisplayName("应该设置就绪状态")
        void shouldSetReadyStatus() {
            endpoint.setReady(false);

            assertThat(endpoint.isReady()).isFalse();
        }

        /**
         * 测试就绪状态应能多次切换。
         */
        @Test
        @DisplayName("应该可以切换就绪状态")
        void shouldToggleReadyStatus() {
            endpoint.setReady(false);
            assertThat(endpoint.isReady()).isFalse();

            endpoint.setReady(true);
            assertThat(endpoint.isReady()).isTrue();
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
