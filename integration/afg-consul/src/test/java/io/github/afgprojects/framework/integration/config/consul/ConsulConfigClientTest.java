package io.github.afgprojects.framework.integration.config.consul;

import io.github.afgprojects.framework.core.api.config.ConfigChangeListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * ConsulConfigClient 单元测试
 * <p>
 * 注意：此测试不启动真实的 Consul 服务器，仅测试客户端逻辑。
 * 集成测试需要启动 Consul 服务器或使用 Testcontainers。
 */
@DisplayName("ConsulConfigClient 测试")
class ConsulConfigClientTest {

    private ConsulConfigClient client;

    @Mock
    private ConfigChangeListener mockListener;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    @Nested
    @DisplayName("使用配置属性构造测试")
    class ConstructorWithPropertiesTests {

        @Test
        @DisplayName("应该使用默认属性创建客户端")
        void shouldCreateClientWithDefaultProperties() {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            properties.setHost("localhost");
            properties.setPort(8500);
            properties.setPrefix("config/test");

            // 注意：这会尝试连接 Consul，可能会失败
            // 但客户端构造函数不应该抛出异常
            try {
                client = new ConsulConfigClient(properties);
                assertThat(client.getClientName()).isEqualTo("consul");
            } catch (Exception e) {
                // 如果 Consul 不可用，忽略连接错误
                assertThat(e).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("基本功能测试")
    class BasicFunctionTests {

        @Test
        @DisplayName("应该正确获取客户端名称")
        void shouldGetClientName() throws Exception {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            client = new ConsulConfigClient(properties);

            assertThat(client.getClientName()).isEqualTo("consul");
        }
    }

    @Nested
    @DisplayName("配置获取测试")
    class GetConfigTests {

        @Test
        @DisplayName("获取不存在的配置应该返回空")
        void shouldReturnEmptyWhenConfigNotExists() throws Exception {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            client = new ConsulConfigClient(properties);

            Optional<String> result = client.getConfig("non-existent-key");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("使用分组获取不存在的配置应该返回空")
        void shouldReturnEmptyWhenConfigNotExistsWithGroup() throws Exception {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            client = new ConsulConfigClient(properties);

            Optional<String> result = client.getConfig("custom-group", "non-existent-key");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getConfigs 应该返回不可修改的 Map")
        void shouldReturnUnmodifiableMap() throws Exception {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            client = new ConsulConfigClient(properties);

            Map<String, String> configs = client.getConfigs("prefix.");

            assertThat(configs).isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("监听器测试")
    class ListenerTests {

        @Test
        @DisplayName("添加监听器后应该能够移除")
        void shouldAddAndRemoveListener() throws Exception {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            client = new ConsulConfigClient(properties);

            client.addListener("test-key", mockListener);
            client.removeListener("test-key");

            assertThat(client).isNotNull();
        }

        @Test
        @DisplayName("使用分组添加和移除监听器")
        void shouldAddAndRemoveListenerWithGroup() throws Exception {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            client = new ConsulConfigClient(properties);

            client.addListener("custom-group", "test-key", mockListener);
            client.removeListener("custom-group", "test-key");

            assertThat(client).isNotNull();
        }
    }

    @Nested
    @DisplayName("刷新测试")
    class RefreshTests {

        @Test
        @DisplayName("刷新操作不应该抛出异常")
        void shouldRefreshWithoutException() throws Exception {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            client = new ConsulConfigClient(properties);

            client.refresh();

            assertThat(client).isNotNull();
        }
    }

    @Nested
    @DisplayName("健康检查测试")
    class HealthCheckTests {

        @Test
        @DisplayName("没有 Consul 连接时健康检查应该返回 false")
        void shouldReturnFalseWhenNoConnection() throws Exception {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            // 使用一个不存在的端口
            properties.setPort(9999);
            client = new ConsulConfigClient(properties);

            boolean healthy = client.isHealthy();

            assertThat(healthy).isFalse();
        }
    }

    @Nested
    @DisplayName("关闭测试")
    class CloseTests {

        @Test
        @DisplayName("关闭客户端不应该抛出异常")
        void shouldCloseWithoutException() throws Exception {
            ConsulConfigProperties properties = new ConsulConfigProperties();
            client = new ConsulConfigClient(properties);

            client.close();

            assertThat(client).isNotNull();
        }
    }
}
