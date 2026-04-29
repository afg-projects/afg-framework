package io.github.afgprojects.framework.core.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ConfigRefresher 单元测试
 */
class ConfigRefresherTest {

    private AfgConfigRegistry registry;
    private ConfigRefresher refresher;

    @BeforeEach
    void setUp() {
        registry = new AfgConfigRegistry();
        refresher = new ConfigRefresher(registry);
    }

    @Nested
    @DisplayName("refresh 方法测试")
    class RefreshTests {

        @Test
        @DisplayName("应该刷新已存在的配置")
        void shouldRefreshExistingConfig() {
            // Given
            String prefix = "afg.test";
            TestConfig oldConfig = new TestConfig("old", 1);
            TestConfig newConfig = new TestConfig("new", 2);
            registry.register(prefix, oldConfig);

            // When
            refresher.refresh(prefix, newConfig);

            // Then
            assertEquals(newConfig, registry.getConfig(prefix));
        }

        @Test
        @DisplayName("刷新应该触发监听器")
        void shouldTriggerListenersOnRefresh() {
            // Given
            String prefix = "afg.test";
            TestConfig oldConfig = new TestConfig("old", 1);
            TestConfig newConfig = new TestConfig("new", 2);
            registry.register(prefix, oldConfig);

            ConfigChangeListener listener = mock(ConfigChangeListener.class);
            registry.addListener(prefix, listener);

            // When
            refresher.refresh(prefix, newConfig);

            // Then
            verify(listener)
                    .onConfigChange(argThat(event ->
                            event.prefix().equals(prefix) && event.newValue().equals(newConfig)));
        }

        @Test
        @DisplayName("刷新不存在的配置应该抛出异常")
        void shouldThrowExceptionWhenRefreshingNonExistentConfig() {
            assertThrows(IllegalArgumentException.class, () -> {
                refresher.refresh("non.existent", new TestConfig("value", 1));
            });
        }

        @Test
        @DisplayName("使用null前缀应该抛出异常")
        void shouldThrowExceptionWhenPrefixIsNull() {
            assertThrows(IllegalArgumentException.class, () -> {
                refresher.refresh(null, new TestConfig("value", 1));
            });
        }
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该接受AfgConfigRegistry")
        void shouldAcceptRegistry() {
            // When
            ConfigRefresher newRefresher = new ConfigRefresher(registry);

            // Then
            assertNotNull(newRefresher);
        }

        @Test
        @DisplayName("registry为null应该抛出异常")
        void shouldThrowExceptionWhenRegistryIsNull() {
            assertThrows(IllegalArgumentException.class, () -> {
                new ConfigRefresher(null);
            });
        }
    }

    @Nested
    @DisplayName("getRegistry 方法测试")
    class GetRegistryTests {

        @Test
        @DisplayName("应该返回关联的registry")
        void shouldReturnAssociatedRegistry() {
            assertEquals(registry, refresher.getRegistry());
        }
    }

    // 测试配置类
    static class TestConfig {
        private final String name;
        private final int value;

        TestConfig(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}
