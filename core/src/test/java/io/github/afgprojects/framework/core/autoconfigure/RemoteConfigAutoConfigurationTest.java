package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.api.config.RemoteConfigClient;
import io.github.afgprojects.framework.core.api.config.ConfigChangeListener;

/**
 * RemoteConfigAutoConfiguration 单元测试。
 * 测试远程配置自动配置类的 NoOp 客户端实现。
 *
 * @see RemoteConfigAutoConfiguration
 * @see RemoteConfigClient
 */
@DisplayName("RemoteConfigAutoConfiguration 测试")
class RemoteConfigAutoConfigurationTest {

    private RemoteConfigAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new RemoteConfigAutoConfiguration();
    }

    /**
     * NoOp 远程配置客户端测试。
     * 验证 NoOp 实现的默认行为。
     */
    @Nested
    @DisplayName("noOpRemoteConfigClient 配置测试")
    class NoOpRemoteConfigClientTests {

        /**
         * 测试创建 NoOp 远程配置客户端。
         */
        @Test
        @DisplayName("应该创建 NoOp 远程配置客户端")
        void shouldCreateNoOpRemoteConfigClient() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            assertThat(client).isNotNull();
            assertThat(client.getClientName()).isEqualTo("no-op");
        }

        /**
         * 测试 getConfig 返回空值。
         */
        @Test
        @DisplayName("getConfig 应该返回空")
        void getConfigShouldReturnEmpty() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            assertThat(client.getConfig("any-key")).isEmpty();
        }

        /**
         * 测试带 group 的 getConfig 返回空值。
         */
        @Test
        @DisplayName("getConfig with group 应该返回空")
        void getConfigWithGroupShouldReturnEmpty() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            assertThat(client.getConfig("group", "key")).isEmpty();
        }

        /**
         * 测试 getConfigs 返回空 Map。
         */
        @Test
        @DisplayName("getConfigs 应该返回空 Map")
        void getConfigsShouldReturnEmptyMap() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            assertThat(client.getConfigs("prefix")).isEmpty();
        }

        /**
         * 测试 publishConfig 返回 false。
         */
        @Test
        @DisplayName("publishConfig 应该返回 false")
        void publishConfigShouldReturnFalse() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            assertThat(client.publishConfig("key", "value")).isFalse();
        }

        /**
         * 测试带 group 的 publishConfig 返回 false。
         */
        @Test
        @DisplayName("publishConfig with group 应该返回 false")
        void publishConfigWithGroupShouldReturnFalse() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            assertThat(client.publishConfig("group", "key", "value")).isFalse();
        }

        /**
         * 测试 addListener 不抛异常。
         */
        @Test
        @DisplayName("addListener 应该不抛异常")
        void addListenerShouldNotThrow() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();
            ConfigChangeListener listener = mock(ConfigChangeListener.class);

            client.addListener("key", listener);
            // 不抛异常即通过
        }

        /**
         * 测试带 group 的 addListener 不抛异常。
         */
        @Test
        @DisplayName("addListener with group 应该不抛异常")
        void addListenerWithGroupShouldNotThrow() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();
            ConfigChangeListener listener = mock(ConfigChangeListener.class);

            client.addListener("group", "key", listener);
            // 不抛异常即通过
        }

        /**
         * 测试 removeListener 不抛异常。
         */
        @Test
        @DisplayName("removeListener 应该不抛异常")
        void removeListenerShouldNotThrow() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            client.removeListener("key");
            // 不抛异常即通过
        }

        /**
         * 测试带 group 的 removeListener 不抛异常。
         */
        @Test
        @DisplayName("removeListener with group 应该不抛异常")
        void removeListenerWithGroupShouldNotThrow() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            client.removeListener("group", "key");
            // 不抛异常即通过
        }

        /**
         * 测试 refresh 不抛异常。
         */
        @Test
        @DisplayName("refresh 应该不抛异常")
        void refreshShouldNotThrow() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            client.refresh();
            // 不抛异常即通过
        }
    }
}
