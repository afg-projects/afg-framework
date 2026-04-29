package io.github.afgprojects.framework.integration.config.apollo;

import io.github.afgprojects.framework.core.api.config.ConfigChangeListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * ApolloConfigClient 单元测试
 * <p>
 * 注意：此测试不启动真实的 Apollo 服务器，仅测试客户端逻辑。
 * 集成测试需要启动 Apollo 服务器或使用 Mock 服务器。
 */
@DisplayName("ApolloConfigClient 测试")
class ApolloConfigClientTest {

    private ApolloConfigClient client;

    @Mock
    private ConfigChangeListener mockListener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 创建客户端，使用默认命名空间
        client = new ApolloConfigClient("application");
    }

    @Nested
    @DisplayName("基本功能测试")
    class BasicFunctionTests {

        @Test
        @DisplayName("应该正确获取客户端名称")
        void shouldGetClientName() {
            assertThat(client.getClientName()).isEqualTo("apollo");
        }

        @Test
        @DisplayName("应该正确获取默认命名空间")
        void shouldGetDefaultNamespace() {
            // 通过 getConfig 方法验证默认命名空间的使用
            // 由于没有真实的 Apollo 服务器，getConfig 会返回空
            Optional<String> result = client.getConfig("test-key");
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("配置获取测试")
    class GetConfigTests {

        @Test
        @DisplayName("获取不存在的配置应该返回空")
        void shouldReturnEmptyWhenConfigNotExists() {
            Optional<String> result = client.getConfig("non-existent-key");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("使用命名空间获取不存在的配置应该返回空")
        void shouldReturnEmptyWhenConfigNotExistsWithNamespace() {
            Optional<String> result = client.getConfig("custom-namespace", "non-existent-key");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getConfigs 应该返回不可修改的 Map")
        void shouldReturnUnmodifiableMap() {
            Map<String, String> configs = client.getConfigs("prefix.");

            assertThat(configs).isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("配置发布测试")
    class PublishConfigTests {

        @Test
        @DisplayName("Apollo 不支持程序化发布配置")
        void shouldNotSupportProgrammaticPublish() {
            boolean result = client.publishConfig("test-key", "test-value");

            // Apollo 不支持通过客户端程序化发布配置
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("使用命名空间发布配置也应该返回 false")
        void shouldNotSupportProgrammaticPublishWithNamespace() {
            boolean result = client.publishConfig("custom-namespace", "test-key", "test-value");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("监听器测试")
    class ListenerTests {

        @Test
        @DisplayName("添加监听器后应该能够移除")
        void shouldAddAndRemoveListener() {
            // 添加监听器（注意：由于没有真实的 Apollo 连接，此操作会失败但不应抛出异常）
            client.addListener("test-key", mockListener);

            // 移除监听器
            client.removeListener("test-key");

            // 验证没有异常发生
            assertThat(client).isNotNull();
        }

        @Test
        @DisplayName("使用命名空间添加和移除监听器")
        void shouldAddAndRemoveListenerWithNamespace() {
            client.addListener("custom-namespace", "test-key", mockListener);
            client.removeListener("custom-namespace", "test-key");

            assertThat(client).isNotNull();
        }
    }

    @Nested
    @DisplayName("刷新测试")
    class RefreshTests {

        @Test
        @DisplayName("刷新操作不应该抛出异常")
        void shouldRefreshWithoutException() {
            client.refresh();

            assertThat(client).isNotNull();
        }
    }

    @Nested
    @DisplayName("健康检查测试")
    class HealthCheckTests {

        @Test
        @DisplayName("健康检查不应该抛出异常")
        void shouldNotThrowExceptionOnHealthCheck() {
            // Apollo 客户端的 isHealthy() 方法不应该抛出异常
            // 注意：即使没有连接，Apollo ConfigService.getConfig() 也会返回非空的 Config 对象
            // 因此 isHealthy() 可能返回 true
            assertThatCode(() -> client.isHealthy()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("关闭测试")
    class CloseTests {

        @Test
        @DisplayName("关闭客户端不应该抛出异常")
        void shouldCloseWithoutException() {
            client.close();

            assertThat(client).isNotNull();
        }
    }
}
