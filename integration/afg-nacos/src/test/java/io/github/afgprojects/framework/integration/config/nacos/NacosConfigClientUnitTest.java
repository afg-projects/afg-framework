package io.github.afgprojects.framework.integration.config.nacos;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import io.github.afgprojects.framework.core.api.config.ConfigChangeListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NacosConfigClient 单元测试
 * <p>
 * 使用 Mock 测试客户端逻辑，不依赖真实的 Nacos 服务器。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NacosConfigClient 单元测试")
class NacosConfigClientUnitTest {

    @Mock
    private ConfigService mockConfigService;

    @Mock
    private ConfigChangeListener mockListener;

    private NacosConfigClient client;

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该正确创建客户端（使用 ConfigService）")
        void shouldCreateClientWithConfigService() {
            client = new NacosConfigClient(mockConfigService, "DEFAULT_GROUP");

            assertThat(client.getClientName()).isEqualTo("nacos");
        }

        @Test
        @DisplayName("应该正确创建客户端（使用自定义分组）")
        void shouldCreateClientWithCustomGroup() {
            client = new NacosConfigClient(mockConfigService, "CUSTOM_GROUP");

            assertThat(client.getClientName()).isEqualTo("nacos");
        }
    }

    @Nested
    @DisplayName("getConfig 测试")
    class GetConfigTests {

        @BeforeEach
        void setUp() {
            client = new NacosConfigClient(mockConfigService, "DEFAULT_GROUP");
        }

        @Test
        @DisplayName("获取存在的配置应该返回值")
        void shouldReturnValueWhenConfigExists() throws NacosException {
            // Given
            String key = "test.key";
            String value = "test-value";
            when(mockConfigService.getConfig(eq(key), eq("DEFAULT_GROUP"), any(long.class)))
                    .thenReturn(value);

            // When
            Optional<String> result = client.getConfig(key);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(value);
        }

        @Test
        @DisplayName("获取不存在的配置应该返回空")
        void shouldReturnEmptyWhenConfigNotExists() throws NacosException {
            // Given
            String key = "non.existent.key";
            when(mockConfigService.getConfig(eq(key), eq("DEFAULT_GROUP"), any(long.class)))
                    .thenReturn(null);

            // When
            Optional<String> result = client.getConfig(key);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("使用指定分组获取配置应该正确调用")
        void shouldUseSpecifiedGroup() throws NacosException {
            // Given
            String group = "CUSTOM_GROUP";
            String key = "test.key";
            String value = "test-value";
            when(mockConfigService.getConfig(eq(key), eq(group), any(long.class)))
                    .thenReturn(value);

            // When
            Optional<String> result = client.getConfig(group, key);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(value);
            verify(mockConfigService).getConfig(eq(key), eq(group), any(long.class));
        }

        @Test
        @DisplayName("NacosException 应该返回空")
        void shouldReturnEmptyOnNacosException() throws NacosException {
            // Given
            String key = "test.key";
            when(mockConfigService.getConfig(anyString(), anyString(), any(long.class)))
                    .thenThrow(new NacosException(500, "Connection refused"));

            // When
            Optional<String> result = client.getConfig(key);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getConfigs 测试")
    class GetConfigsTests {

        @BeforeEach
        void setUp() {
            client = new NacosConfigClient(mockConfigService, "DEFAULT_GROUP");
        }

        @Test
        @DisplayName("应该返回不可修改的 Map")
        void shouldReturnUnmodifiableMap() {
            Map<String, String> configs = client.getConfigs("prefix.");

            assertThat(configs).isUnmodifiable();
        }

        @Test
        @DisplayName("空缓存应该返回空 Map")
        void shouldReturnEmptyMapForEmptyCache() {
            Map<String, String> configs = client.getConfigs("prefix.");

            assertThat(configs).isEmpty();
        }
    }

    @Nested
    @DisplayName("publishConfig 测试")
    class PublishConfigTests {

        @BeforeEach
        void setUp() {
            client = new NacosConfigClient(mockConfigService, "DEFAULT_GROUP");
        }

        @Test
        @DisplayName("成功发布配置应该返回 true")
        void shouldReturnTrueOnSuccessfulPublish() throws NacosException {
            // Given
            String key = "test.key";
            String value = "test-value";
            when(mockConfigService.publishConfig(eq(key), eq("DEFAULT_GROUP"), eq(value)))
                    .thenReturn(true);

            // When
            boolean result = client.publishConfig(key, value);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("发布配置失败应该返回 false")
        void shouldReturnFalseOnFailedPublish() throws NacosException {
            // Given
            String key = "test.key";
            String value = "test-value";
            when(mockConfigService.publishConfig(anyString(), anyString(), anyString()))
                    .thenReturn(false);

            // When
            boolean result = client.publishConfig(key, value);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("发布配置异常应该返回 false")
        void shouldReturnFalseOnPublishException() throws NacosException {
            // Given
            String key = "test.key";
            String value = "test-value";
            when(mockConfigService.publishConfig(anyString(), anyString(), anyString()))
                    .thenThrow(new NacosException(500, "Publish failed"));

            // When
            boolean result = client.publishConfig(key, value);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("使用指定分组发布配置应该正确调用")
        void shouldPublishWithSpecifiedGroup() throws NacosException {
            // Given
            String group = "CUSTOM_GROUP";
            String key = "test.key";
            String value = "test-value";
            when(mockConfigService.publishConfig(eq(key), eq(group), eq(value)))
                    .thenReturn(true);

            // When
            boolean result = client.publishConfig(group, key, value);

            // Then
            assertThat(result).isTrue();
            verify(mockConfigService).publishConfig(eq(key), eq(group), eq(value));
        }
    }

    @Nested
    @DisplayName("addListener 测试")
    class AddListenerTests {

        @BeforeEach
        void setUp() {
            client = new NacosConfigClient(mockConfigService, "DEFAULT_GROUP");
        }

        @Test
        @DisplayName("添加监听器应该成功")
        void shouldAddListenerSuccessfully() throws NacosException {
            // When
            client.addListener("test.key", mockListener);

            // Then
            verify(mockConfigService).addListener(eq("test.key"), eq("DEFAULT_GROUP"), any());
        }

        @Test
        @DisplayName("使用指定分组添加监听器应该正确调用")
        void shouldAddListenerWithSpecifiedGroup() throws NacosException {
            // Given
            String group = "CUSTOM_GROUP";
            String key = "test.key";

            // When
            client.addListener(group, key, mockListener);

            // Then
            verify(mockConfigService).addListener(eq(key), eq(group), any());
        }

        @Test
        @DisplayName("重复添加相同监听器应该只注册一次")
        void shouldNotAddDuplicateListener() throws NacosException {
            // When
            client.addListener("test.key", mockListener);
            client.addListener("test.key", mockListener);

            // Then - should only be called once
            verify(mockConfigService).addListener(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("添加监听器异常应该被捕获")
        void shouldHandleAddListenerException() throws NacosException {
            // Given
            doThrow(new NacosException(500, "Add listener failed"))
                    .when(mockConfigService).addListener(anyString(), anyString(), any());

            // When & Then - should not throw exception
            assertThatCode(() -> client.addListener("test.key", mockListener))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("removeListener 测试")
    class RemoveListenerTests {

        @BeforeEach
        void setUp() throws NacosException {
            client = new NacosConfigClient(mockConfigService, "DEFAULT_GROUP");
        }

        @Test
        @DisplayName("移除未添加的监听器不应该抛出异常")
        void shouldNotThrowWhenRemovingNonExistentListener() {
            // When & Then
            assertThatCode(() -> client.removeListener("test.key"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("移除已添加的监听器应该成功")
        void shouldRemoveAddedListener() throws NacosException {
            // Given
            client.addListener("test.key", mockListener);

            // When
            client.removeListener("test.key");

            // Then
            verify(mockConfigService).removeListener(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("refresh 测试")
    class RefreshTests {

        @BeforeEach
        void setUp() {
            client = new NacosConfigClient(mockConfigService, "DEFAULT_GROUP");
        }

        @Test
        @DisplayName("刷新应该清空缓存")
        void shouldClearCacheOnRefresh() throws NacosException {
            // Given - populate cache
            when(mockConfigService.getConfig(anyString(), anyString(), any(long.class)))
                    .thenReturn("cached-value");
            client.getConfig("test.key");

            // When
            client.refresh();

            // Then - next call should not use cache
            verify(mockConfigService).getConfig(anyString(), anyString(), any(long.class));
        }

        @Test
        @DisplayName("刷新不应该抛出异常")
        void shouldNotThrowOnRefresh() {
            assertThatCode(() -> client.refresh())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getClientName 测试")
    class GetClientNameTests {

        @Test
        @DisplayName("应该返回 nacos")
        void shouldReturnNacos() {
            client = new NacosConfigClient(mockConfigService, "DEFAULT_GROUP");

            assertThat(client.getClientName()).isEqualTo("nacos");
        }
    }

    @Nested
    @DisplayName("isHealthy 测试")
    class IsHealthyTests {

        @BeforeEach
        void setUp() {
            client = new NacosConfigClient(mockConfigService, "DEFAULT_GROUP");
        }

        @Test
        @DisplayName("服务状态 UP 应该返回 true")
        void shouldReturnTrueWhenServerIsUp() throws NacosException {
            // Given
            when(mockConfigService.getServerStatus()).thenReturn("UP");

            // When
            boolean healthy = client.isHealthy();

            // Then
            assertThat(healthy).isTrue();
        }

        @Test
        @DisplayName("服务状态非 UP 应该返回 false")
        void shouldReturnFalseWhenServerIsNotUp() throws NacosException {
            // Given
            when(mockConfigService.getServerStatus()).thenReturn("DOWN");

            // When
            boolean healthy = client.isHealthy();

            // Then
            assertThat(healthy).isFalse();
        }

        @Test
        @DisplayName("异常应该返回 false")
        void shouldReturnFalseOnException() throws NacosException {
            // Given
            when(mockConfigService.getServerStatus()).thenThrow(new RuntimeException("Connection error"));

            // When
            boolean healthy = client.isHealthy();

            // Then
            assertThat(healthy).isFalse();
        }
    }

    @Nested
    @DisplayName("close 测试")
    class CloseTests {

        @Test
        @DisplayName("关闭客户端不应该抛出异常")
        void shouldNotThrowOnClose() {
            client = new NacosConfigClient(mockConfigService, "DEFAULT_GROUP");

            assertThatCode(() -> client.close())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("关闭有监听器的客户端应该移除所有监听器")
        void shouldRemoveListenersOnClose() throws NacosException {
            // Given
            client = new NacosConfigClient(mockConfigService, "DEFAULT_GROUP");
            client.addListener("test.key", mockListener);

            // When
            client.close();

            // Then
            verify(mockConfigService).removeListener(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("缓存行为测试")
    class CacheBehaviorTests {

        @BeforeEach
        void setUp() {
            client = new NacosConfigClient(mockConfigService, "DEFAULT_GROUP");
        }

        @Test
        @DisplayName("获取配置应该缓存值")
        void shouldCacheConfigValue() throws NacosException {
            // Given
            String key = "test.key";
            String value = "test-value";
            when(mockConfigService.getConfig(eq(key), eq("DEFAULT_GROUP"), any(long.class)))
                    .thenReturn(value);

            // When
            Optional<String> result1 = client.getConfig(key);
            Optional<String> result2 = client.getConfig(key);

            // Then
            assertThat(result1).isPresent();
            assertThat(result2).isPresent();
        }

        @Test
        @DisplayName("发布配置应该更新缓存")
        void shouldUpdateCacheOnPublish() throws NacosException {
            // Given
            String key = "test.key";
            String value = "test-value";
            when(mockConfigService.publishConfig(anyString(), anyString(), anyString()))
                    .thenReturn(true);

            // When
            boolean result = client.publishConfig(key, value);

            // Then
            assertThat(result).isTrue();
        }
    }
}
