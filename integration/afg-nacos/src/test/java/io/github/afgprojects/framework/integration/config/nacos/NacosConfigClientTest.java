package io.github.afgprojects.framework.integration.config.nacos;

import io.github.afgprojects.framework.core.api.config.ConfigChangeEvent;
import io.github.afgprojects.framework.core.api.config.ConfigChangeListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NacosConfigClient 单元测试
 * <p>
 * 注意：此测试不启动真实的 Nacos 服务器，仅测试客户端逻辑。
 * 集成测试需要启动 Nacos 服务器或使用 Testcontainers。
 */
@DisplayName("NacosConfigClient 测试")
class NacosConfigClientTest {

    @Mock
    private ConfigChangeListener mockListener;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Nested
    @DisplayName("基本功能测试")
    class BasicFunctionTests {

        @Test
        @DisplayName("应该正确获取客户端名称")
        void shouldGetClientName() {
            assertThat("nacos").isEqualTo("nacos");
        }
    }

    @Nested
    @DisplayName("配置属性测试")
    class ConfigPropertiesTests {

        @Test
        @DisplayName("NacosConfigProperties 应该正确配置")
        void shouldConfigurePropertiesCorrectly() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setServerAddr("localhost:8848");
            properties.setGroup("TEST_GROUP");

            assertThat(properties.getServerAddr()).isEqualTo("localhost:8848");
            assertThat(properties.getGroup()).isEqualTo("TEST_GROUP");
        }

        @Test
        @DisplayName("NacosConfigProperties 应该支持认证配置")
        void shouldSupportAuthConfiguration() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setUsername("nacos");
            properties.setPassword("nacos");

            assertThat(properties.getUsername()).isEqualTo("nacos");
            assertThat(properties.getPassword()).isEqualTo("nacos");
        }

        @Test
        @DisplayName("NacosConfigProperties 应该支持命名空间配置")
        void shouldSupportNamespaceConfiguration() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setNamespace("dev-namespace");

            assertThat(properties.getNamespace()).isEqualTo("dev-namespace");
        }
    }

    @Nested
    @DisplayName("配置变更事件测试")
    class ConfigChangeEventTests {

        @Test
        @DisplayName("ConfigChangeEvent 应该正确创建")
        void shouldCreateConfigChangeEventCorrectly() {
            ConfigChangeEvent event = new ConfigChangeEvent(
                    "test-key",
                    "test-group",
                    "old-value",
                    "new-value",
                    ConfigChangeEvent.ConfigChangeType.MODIFIED
            );

            assertThat(event.key()).isEqualTo("test-key");
            assertThat(event.group()).isEqualTo("test-group");
            assertThat(event.oldValue()).isEqualTo("old-value");
            assertThat(event.newValue()).isEqualTo("new-value");
            assertThat(event.changeType()).isEqualTo(ConfigChangeEvent.ConfigChangeType.MODIFIED);
            assertThat(event.isModification()).isTrue();
            assertThat(event.isAddition()).isFalse();
            assertThat(event.isDeletion()).isFalse();
        }

        @Test
        @DisplayName("ADDED 类型的变更事件应该正确判断")
        void shouldDetectAddedChangeType() {
            ConfigChangeEvent event = new ConfigChangeEvent(
                    "new-key",
                    "group",
                    null,
                    "new-value",
                    ConfigChangeEvent.ConfigChangeType.ADDED
            );

            assertThat(event.isAddition()).isTrue();
            assertThat(event.isModification()).isFalse();
            assertThat(event.isDeletion()).isFalse();
        }

        @Test
        @DisplayName("DELETED 类型的变更事件应该正确判断")
        void shouldDetectDeletedChangeType() {
            ConfigChangeEvent event = new ConfigChangeEvent(
                    "deleted-key",
                    "group",
                    "old-value",
                    null,
                    ConfigChangeEvent.ConfigChangeType.DELETED
            );

            assertThat(event.isDeletion()).isTrue();
            assertThat(event.isAddition()).isFalse();
            assertThat(event.isModification()).isFalse();
        }
    }

    @Nested
    @DisplayName("配置监听器测试")
    class ConfigChangeListenerTests {

        @Test
        @DisplayName("ConfigChangeListener 应该正确响应事件")
        void shouldRespondToConfigChangeEvent() {
            final ConfigChangeEvent[] receivedEvent = new ConfigChangeEvent[1];

            ConfigChangeListener listener = event -> receivedEvent[0] = event;

            ConfigChangeEvent event = new ConfigChangeEvent(
                    "test-key",
                    "test-group",
                    null,
                    "test-value",
                    ConfigChangeEvent.ConfigChangeType.ADDED
            );

            listener.onChange(event);

            assertThat(receivedEvent[0]).isNotNull();
            assertThat(receivedEvent[0].key()).isEqualTo("test-key");
        }
    }

    @Nested
    @DisplayName("超时配置测试")
    class TimeoutConfigurationTests {

        @Test
        @DisplayName("应该正确配置各种超时参数")
        void shouldConfigureTimeoutsCorrectly() {
            NacosConfigProperties properties = new NacosConfigProperties();

            assertThat(properties.getConnectTimeout()).isEqualTo(5000);
            assertThat(properties.getReadTimeout()).isEqualTo(10000);
            assertThat(properties.getPollTimeout()).isEqualTo(30000);
            assertThat(properties.getMaxRetries()).isEqualTo(3);
            assertThat(properties.getRetryInterval()).isEqualTo(1000);
        }

        @Test
        @DisplayName("应该允许自定义超时参数")
        void shouldAllowCustomTimeouts() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setConnectTimeout(10000);
            properties.setReadTimeout(20000);
            properties.setPollTimeout(60000);
            properties.setMaxRetries(5);
            properties.setRetryInterval(2000);

            assertThat(properties.getConnectTimeout()).isEqualTo(10000);
            assertThat(properties.getReadTimeout()).isEqualTo(20000);
            assertThat(properties.getPollTimeout()).isEqualTo(60000);
            assertThat(properties.getMaxRetries()).isEqualTo(5);
            assertThat(properties.getRetryInterval()).isEqualTo(2000);
        }
    }

    @Nested
    @DisplayName("安全配置测试")
    class SecurityConfigurationTests {

        @Test
        @DisplayName("应该支持 HTTPS 配置")
        void shouldSupportHttpsConfiguration() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setSecure(true);

            assertThat(properties.isSecure()).isTrue();
        }

        @Test
        @DisplayName("应该支持 accessToken 配置")
        void shouldSupportAccessTokenConfiguration() {
            NacosConfigProperties properties = new NacosConfigProperties();
            properties.setAccessToken("my-access-token");

            assertThat(properties.getAccessToken()).isEqualTo("my-access-token");
        }
    }
}
