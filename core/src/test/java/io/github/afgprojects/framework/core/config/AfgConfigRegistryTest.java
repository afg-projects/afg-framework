package io.github.afgprojects.framework.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

@DisplayName("AfgConfigRegistry 测试")
class AfgConfigRegistryTest extends BaseUnitTest {

    private AfgConfigRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AfgConfigRegistry();
    }

    @Nested
    @DisplayName("register 测试")
    class RegisterTests {

        @Test
        @DisplayName("应该成功注册配置（使用默认配置源）")
        void shouldRegisterConfigWithDefaultSource() {
            // given
            TestConfig config = new TestConfig("value1", 100);

            // when
            registry.register("test", config);

            // then
            assertThat(registry.contains("test")).isTrue();
            assertThat(registry.getConfig("test")).isEqualTo(config);
        }

        @Test
        @DisplayName("应该成功注册配置（使用指定配置源）")
        void shouldRegisterConfigWithSpecifiedSource() {
            // given
            TestConfig config = new TestConfig("value1", 100);

            // when
            registry.register("test", config, ConfigSource.CONFIG_CENTER);

            // then
            assertThat(registry.contains("test")).isTrue();
            assertThat(registry.getConfig("test")).isEqualTo(config);
        }

        @Test
        @DisplayName("prefix 为 null 时应该抛出异常")
        void shouldThrowWhenPrefixIsNull() {
            // when & then
            assertThatThrownBy(() -> registry.register(null, new TestConfig("v", 1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("prefix 为空白时应该抛出异常")
        void shouldThrowWhenPrefixIsBlank() {
            // when & then
            assertThatThrownBy(() -> registry.register("  ", new TestConfig("v", 1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("config 为 null 时应该抛出异常")
        void shouldThrowWhenConfigIsNull() {
            // when & then
            assertThatThrownBy(() -> registry.register("test", null)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("source 为 null 时应该抛出异常")
        void shouldThrowWhenSourceIsNull() {
            // when & then
            assertThatThrownBy(() -> registry.register("test", new TestConfig("v", 1), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("高优先级配置源应该覆盖低优先级")
        void shouldOverrideWhenHigherPriority() {
            // given
            TestConfig moduleConfig = new TestConfig("module", 1);
            TestConfig centerConfig = new TestConfig("center", 2);

            // when
            registry.register("test", moduleConfig, ConfigSource.MODULE_DEFAULT);
            registry.register("test", centerConfig, ConfigSource.CONFIG_CENTER);

            // then
            assertThat(registry.getConfig("test")).isEqualTo(centerConfig);
        }

        @Test
        @DisplayName("低优先级配置源不应该覆盖高优先级")
        void shouldNotOverrideWhenLowerPriority() {
            // given
            TestConfig centerConfig = new TestConfig("center", 2);
            TestConfig moduleConfig = new TestConfig("module", 1);

            // when
            registry.register("test", centerConfig, ConfigSource.CONFIG_CENTER);
            registry.register("test", moduleConfig, ConfigSource.MODULE_DEFAULT);

            // then
            assertThat(registry.getConfig("test")).isEqualTo(centerConfig);
        }
    }

    @Nested
    @DisplayName("getConfig 测试")
    class GetConfigTests {

        @Test
        @DisplayName("获取不存在的配置应该返回 null")
        void shouldReturnNullForNonExistent() {
            // when
            Object result = registry.getConfig("nonexistent");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("应该获取指定类型的配置")
        void shouldGetTypedConfig() {
            // given
            TestConfig config = new TestConfig("value1", 100);
            registry.register("test", config);

            // when
            TestConfig result = registry.getConfig("test", TestConfig.class);

            // then
            assertThat(result).isEqualTo(config);
        }

        @Test
        @DisplayName("类型不匹配时应该抛出 ClassCastException")
        void shouldThrowWhenTypeMismatch() {
            // given
            registry.register("test", new TestConfig("value1", 100));

            // when & then
            assertThatThrownBy(() -> registry.getConfig("test", String.class)).isInstanceOf(ClassCastException.class);
        }

        @Test
        @DisplayName("获取不存在的类型配置应该返回 null")
        void shouldReturnNullWhenTypedConfigNotExist() {
            // when
            TestConfig result = registry.getConfig("nonexistent", TestConfig.class);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("updateConfig 测试")
    class UpdateConfigTests {

        @Test
        @DisplayName("应该成功更新已存在的配置")
        void shouldUpdateExistingConfig() {
            // given
            TestConfig original = new TestConfig("original", 1);
            TestConfig updated = new TestConfig("updated", 2);
            registry.register("test", original);

            // when
            registry.updateConfig("test", updated);

            // then
            assertThat(registry.getConfig("test")).isEqualTo(updated);
        }

        @Test
        @DisplayName("更新不存在的配置应该抛出异常")
        void shouldThrowWhenUpdatingNonExistent() {
            // when & then
            assertThatThrownBy(() -> registry.updateConfig("nonexistent", new TestConfig("v", 1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("更新配置时应该通知监听器")
        void shouldNotifyListenersOnUpdate() {
            // given
            TestConfig original = new TestConfig("original", 1);
            TestConfig updated = new TestConfig("updated", 2);
            registry.register("test", original);

            boolean[] notified = {false};
            registry.addListener("test", newConfig -> notified[0] = true);

            // when
            registry.updateConfig("test", updated);

            // then
            assertThat(notified[0]).isTrue();
        }
    }

    @Nested
    @DisplayName("unregister 测试")
    class UnregisterTests {

        @Test
        @DisplayName("应该成功注销配置")
        void shouldUnregisterConfig() {
            // given
            registry.register("test", new TestConfig("v", 1));

            // when
            registry.unregister("test");

            // then
            assertThat(registry.contains("test")).isFalse();
        }

        @Test
        @DisplayName("注销不存在的配置不应该抛出异常")
        void shouldHandleUnregisteringNonExistent() {
            // when & then
            assertThatCode(() -> registry.unregister("nonexistent")).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("监听器测试")
    class ListenerTests {

        @Test
        @DisplayName("应该成功添加和检查监听器")
        void shouldAddAndCheckListeners() {
            // given
            registry.register("test", new TestConfig("v", 1));

            // when & then
            assertThat(registry.hasListeners("test")).isFalse();

            registry.addListener("test", newConfig -> {});

            assertThat(registry.hasListeners("test")).isTrue();
        }

        @Test
        @DisplayName("应该成功移除监听器")
        void shouldRemoveListener() {
            // given
            registry.register("test", new TestConfig("v", 1));
            ConfigChangeListener listener = newConfig -> {};
            registry.addListener("test", listener);

            // when
            registry.removeListener("test", listener);

            // then
            assertThat(registry.hasListeners("test")).isFalse();
        }
    }

    @Nested
    @DisplayName("配置源测试")
    class ConfigSourceTests {

        @Test
        @DisplayName("应该按优先级排序获取配置源")
        void shouldGetConfigSourcesSortedByPriority() {
            // given
            TestConfig moduleConfig = new TestConfig("module", 1);
            TestConfig centerConfig = new TestConfig("center", 2);
            registry.register("test", moduleConfig, ConfigSource.MODULE_DEFAULT);
            registry.register("test", centerConfig, ConfigSource.CONFIG_CENTER);

            // when
            List<ConfigEntry> sources = registry.getConfigSources("test");

            // then
            assertThat(sources).hasSize(2);
            assertThat(sources.get(0).source()).isEqualTo(ConfigSource.CONFIG_CENTER);
            assertThat(sources.get(1).source()).isEqualTo(ConfigSource.MODULE_DEFAULT);
        }

        @Test
        @DisplayName("获取不存在配置的配置源应该返回空列表")
        void shouldReturnEmptyListForNonExistent() {
            // when
            List<ConfigEntry> sources = registry.getConfigSources("nonexistent");

            // then
            assertThat(sources).isEmpty();
        }

        @Test
        @DisplayName("应该获取最终配置条目")
        void shouldGetFinalConfigEntry() {
            // given
            TestConfig moduleConfig = new TestConfig("module", 1);
            TestConfig centerConfig = new TestConfig("center", 2);
            registry.register("test", moduleConfig, ConfigSource.MODULE_DEFAULT);
            registry.register("test", centerConfig, ConfigSource.CONFIG_CENTER);

            // when
            ConfigEntry entry = registry.getFinalConfigEntry("test");

            // then
            assertThat(entry).isNotNull();
            assertThat(entry.source()).isEqualTo(ConfigSource.CONFIG_CENTER);
            assertThat(entry.value()).isEqualTo(centerConfig);
        }

        @Test
        @DisplayName("应该获取活跃配置源")
        void shouldGetActiveSource() {
            // given
            TestConfig centerConfig = new TestConfig("center", 2);
            registry.register("test", centerConfig, ConfigSource.CONFIG_CENTER);

            // when & then
            assertThat(registry.getActiveSource("test")).isEqualTo(ConfigSource.CONFIG_CENTER);
        }

        @Test
        @DisplayName("获取不存在配置的活跃配置源应该返回 null")
        void shouldReturnNullActiveSourceForNonExistent() {
            // when & then
            assertThat(registry.getActiveSource("nonexistent")).isNull();
        }
    }

    @Nested
    @DisplayName("环境测试")
    class EnvironmentTests {

        @Test
        @DisplayName("应该设置和获取活跃环境")
        void shouldSetAndGetActiveEnvironment() {
            // when
            registry.setActiveEnvironment("production");

            // then
            assertThat(registry.getActiveEnvironment()).isEqualTo("production");
        }

        @Test
        @DisplayName("未设置环境时应该返回 null")
        void shouldReturnNullWhenNoEnvironmentSet() {
            // when & then
            assertThat(registry.getActiveEnvironment()).isNull();
        }
    }

    @Nested
    @DisplayName("配置中心刷新测试")
    class RefreshFromConfigCenterTests {

        @Test
        @DisplayName("应该从配置中心刷新配置")
        void shouldRefreshFromConfigCenter() {
            // given
            TestConfig original = new TestConfig("original", 1);
            TestConfig refreshed = new TestConfig("refreshed", 2);
            registry.register("test", original, ConfigSource.CONFIG_CENTER);

            // when
            registry.refreshFromConfigCenter("test", refreshed);

            // then
            assertThat(registry.getConfig("test")).isEqualTo(refreshed);
        }

        @Test
        @DisplayName("刷新不存在的配置应该抛出异常")
        void shouldThrowWhenRefreshingNonExistent() {
            // when & then
            assertThatThrownBy(() -> registry.refreshFromConfigCenter("nonexistent", new TestConfig("v", 1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("刷新非配置中心来源的配置应该抛出异常")
        void shouldThrowWhenRefreshingNonConfigCenterSource() {
            // given
            registry.register("test", new TestConfig("v", 1), ConfigSource.MODULE_DEFAULT);

            // when & then
            assertThatThrownBy(() -> registry.refreshFromConfigCenter("test", new TestConfig("v2", 2)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("刷新配置时应该通知监听器")
        void shouldNotifyListenersOnRefresh() {
            // given
            TestConfig original = new TestConfig("original", 1);
            TestConfig refreshed = new TestConfig("refreshed", 2);
            registry.register("test", original, ConfigSource.CONFIG_CENTER);

            Object[] received = {null};
            registry.addListener("test", event -> received[0] = event.newValue());

            // when
            registry.refreshFromConfigCenter("test", refreshed);

            // then
            assertThat(received[0]).isEqualTo(refreshed);
        }
    }

    @Nested
    @DisplayName("getAllConfigs 测试")
    class GetAllConfigsTests {

        @Test
        @DisplayName("应该返回不可修改的配置 Map")
        void shouldReturnAllConfigsAsUnmodifiableMap() {
            // given
            registry.register("test1", new TestConfig("v1", 1));
            registry.register("test2", new TestConfig("v2", 2));

            // when
            Map<String, Object> allConfigs = registry.getAllConfigs();

            // then
            assertThat(allConfigs).hasSize(2);
            assertThatThrownBy(() -> allConfigs.put("test3", new TestConfig("v3", 3)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("线程安全测试")
    class ThreadSafetyTests {

        @Test
        @DisplayName("应该安全处理并发注册")
        void shouldHandleConcurrentRegistrations() throws InterruptedException {
            // given
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];

            // when
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> registry.register("config-" + index, new TestConfig("v" + index, index)));
            }

            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }

            // then
            for (int i = 0; i < threadCount; i++) {
                assertThat(registry.contains("config-" + i)).isTrue();
            }
        }
    }

    record TestConfig(String name, int value) {}
}
