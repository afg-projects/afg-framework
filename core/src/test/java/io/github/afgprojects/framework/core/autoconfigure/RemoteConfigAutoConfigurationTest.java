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
 * RemoteConfigAutoConfiguration 测试
 */
@DisplayName("RemoteConfigAutoConfiguration 测试")
class RemoteConfigAutoConfigurationTest {

    private RemoteConfigAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new RemoteConfigAutoConfiguration();
    }

    @Nested
    @DisplayName("noOpRemoteConfigClient 配置测试")
    class NoOpRemoteConfigClientTests {

        @Test
        @DisplayName("应该创建 NoOp 远程配置客户端")
        void shouldCreateNoOpRemoteConfigClient() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            assertThat(client).isNotNull();
            assertThat(client.getClientName()).isEqualTo("no-op");
        }

        @Test
        @DisplayName("getConfig 应该返回空")
        void getConfigShouldReturnEmpty() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            assertThat(client.getConfig("any-key")).isEmpty();
        }

        @Test
        @DisplayName("getConfig with group 应该返回空")
        void getConfigWithGroupShouldReturnEmpty() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            assertThat(client.getConfig("group", "key")).isEmpty();
        }

        @Test
        @DisplayName("getConfigs 应该返回空 Map")
        void getConfigsShouldReturnEmptyMap() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            assertThat(client.getConfigs("prefix")).isEmpty();
        }

        @Test
        @DisplayName("publishConfig 应该返回 false")
        void publishConfigShouldReturnFalse() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            assertThat(client.publishConfig("key", "value")).isFalse();
        }

        @Test
        @DisplayName("publishConfig with group 应该返回 false")
        void publishConfigWithGroupShouldReturnFalse() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            assertThat(client.publishConfig("group", "key", "value")).isFalse();
        }

        @Test
        @DisplayName("addListener 应该不抛异常")
        void addListenerShouldNotThrow() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();
            ConfigChangeListener listener = mock(ConfigChangeListener.class);

            client.addListener("key", listener);
            // 不抛异常即通过
        }

        @Test
        @DisplayName("addListener with group 应该不抛异常")
        void addListenerWithGroupShouldNotThrow() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();
            ConfigChangeListener listener = mock(ConfigChangeListener.class);

            client.addListener("group", "key", listener);
            // 不抛异常即通过
        }

        @Test
        @DisplayName("removeListener 应该不抛异常")
        void removeListenerShouldNotThrow() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            client.removeListener("key");
            // 不抛异常即通过
        }

        @Test
        @DisplayName("removeListener with group 应该不抛异常")
        void removeListenerWithGroupShouldNotThrow() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            client.removeListener("group", "key");
            // 不抛异常即通过
        }

        @Test
        @DisplayName("refresh 应该不抛异常")
        void refreshShouldNotThrow() {
            RemoteConfigClient client = configuration.noOpRemoteConfigClient();

            client.refresh();
            // 不抛异常即通过
        }
    }
}
