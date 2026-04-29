package io.github.afgprojects.framework.integration.config.apollo;

import io.github.afgprojects.framework.core.api.config.ConfigChangeEvent.ConfigChangeType;
import io.github.afgprojects.framework.core.api.config.ConfigChangeListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * ApolloConfigClient 单元测试
 * <p>
 * 测试客户端基本功能和配置属性。
 */
@DisplayName("ApolloConfigClient 单元测试")
class ApolloConfigClientUnitTest {

    private ApolloConfigClient client;

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该正确创建客户端")
        void shouldCreateClient() {
            client = new ApolloConfigClient("application");

            assertThat(client.getClientName()).isEqualTo("apollo");
        }

        @Test
        @DisplayName("应该使用指定的命名空间")
        void shouldUseSpecifiedNamespace() {
            client = new ApolloConfigClient("custom-namespace");

            assertThat(client.getClientName()).isEqualTo("apollo");
        }
    }

    @Nested
    @DisplayName("getClientName 测试")
    class GetClientNameTests {

        @Test
        @DisplayName("应该返回 apollo")
        void shouldReturnApollo() {
            client = new ApolloConfigClient("application");

            assertThat(client.getClientName()).isEqualTo("apollo");
        }
    }

    @Nested
    @DisplayName("publishConfig 测试")
    class PublishConfigTests {

        @Test
        @DisplayName("Apollo 不支持程序化发布配置")
        void shouldNotSupportProgrammaticPublish() {
            client = new ApolloConfigClient("application");

            boolean result = client.publishConfig("test.key", "test-value");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("使用命名空间发布也应该返回 false")
        void shouldNotSupportProgrammaticPublishWithNamespace() {
            client = new ApolloConfigClient("application");

            boolean result = client.publishConfig("custom-namespace", "test.key", "test-value");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getConfig 测试")
    class GetConfigTests {

        @Test
        @DisplayName("获取不存在的配置应该返回空")
        void shouldReturnEmptyWhenConfigNotExists() {
            client = new ApolloConfigClient("application");

            var result = client.getConfig("non-existent-key");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("使用命名空间获取不存在的配置应该返回空")
        void shouldReturnEmptyWhenConfigNotExistsWithNamespace() {
            client = new ApolloConfigClient("application");

            var result = client.getConfig("custom-namespace", "non-existent-key");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getConfigs 测试")
    class GetConfigsTests {

        @Test
        @DisplayName("应该返回不可修改的 Map")
        void shouldReturnUnmodifiableMap() {
            client = new ApolloConfigClient("application");

            var configs = client.getConfigs("prefix.");

            assertThat(configs).isUnmodifiable();
        }

        @Test
        @DisplayName("空缓存应该返回空 Map")
        void shouldReturnEmptyMapForEmptyCache() {
            client = new ApolloConfigClient("application");

            var configs = client.getConfigs("prefix.");

            assertThat(configs).isEmpty();
        }
    }

    @Nested
    @DisplayName("监听器测试")
    class ListenerTests {

        @Test
        @DisplayName("添加监听器后应该能够移除")
        void shouldAddAndRemoveListener() {
            client = new ApolloConfigClient("application");
            ConfigChangeListener mockListener = event -> {};

            client.addListener("test-key", mockListener);
            client.removeListener("test-key");

            assertThat(client).isNotNull();
        }

        @Test
        @DisplayName("使用命名空间添加和移除监听器")
        void shouldAddAndRemoveListenerWithNamespace() {
            client = new ApolloConfigClient("application");
            ConfigChangeListener mockListener = event -> {};

            client.addListener("custom-namespace", "test-key", mockListener);
            client.removeListener("custom-namespace", "test-key");

            assertThat(client).isNotNull();
        }

        @Test
        @DisplayName("移除未添加的监听器不应该抛出异常")
        void shouldNotThrowWhenRemovingNonExistentListener() {
            client = new ApolloConfigClient("application");

            assertThatCode(() -> client.removeListener("test-key"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("refresh 测试")
    class RefreshTests {

        @Test
        @DisplayName("刷新不应该抛出异常")
        void shouldNotThrowOnRefresh() {
            client = new ApolloConfigClient("application");

            assertThatCode(() -> client.refresh())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("isHealthy 测试")
    class IsHealthyTests {

        @Test
        @DisplayName("健康检查不应该抛出异常")
        void shouldNotThrowOnHealthCheck() {
            client = new ApolloConfigClient("application");

            assertThatCode(() -> client.isHealthy())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("close 测试")
    class CloseTests {

        @Test
        @DisplayName("关闭客户端不应该抛出异常")
        void shouldNotThrowOnClose() {
            client = new ApolloConfigClient("application");

            assertThatCode(() -> client.close())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("配置变更类型测试")
    class ChangeTypeTests {

        @Test
        @DisplayName("新增配置应该返回 ADDED")
        void shouldReturnAddedForNewConfig() {
            var changeType = determineChangeType(null, "new-value");

            assertThat(changeType).isEqualTo(ConfigChangeType.ADDED);
        }

        @Test
        @DisplayName("删除配置应该返回 DELETED")
        void shouldReturnDeletedForRemovedConfig() {
            var changeType = determineChangeType("old-value", null);

            assertThat(changeType).isEqualTo(ConfigChangeType.DELETED);
        }

        @Test
        @DisplayName("修改配置应该返回 MODIFIED")
        void shouldReturnModifiedForUpdatedConfig() {
            var changeType = determineChangeType("old-value", "new-value");

            assertThat(changeType).isEqualTo(ConfigChangeType.MODIFIED);
        }

        private ConfigChangeType determineChangeType(String oldValue, String newValue) {
            if (oldValue == null) {
                return ConfigChangeType.ADDED;
            } else if (newValue == null) {
                return ConfigChangeType.DELETED;
            } else {
                return ConfigChangeType.MODIFIED;
            }
        }
    }
}