package io.github.afgprojects.framework.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ConfigChangeEvent 测试
 */
@DisplayName("ConfigChangeEvent 测试")
class ConfigChangeEventTest {

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该创建配置变更事件")
        void shouldCreateConfigChangeEvent() {
            // given
            String prefix = "app.config";
            Object oldValue = Map.of("key", "old");
            Object newValue = Map.of("key", "new");
            ConfigDiff diff = ConfigDiff.compute(oldValue, newValue);
            Instant now = Instant.now();

            // when
            ConfigChangeEvent event =
                    new ConfigChangeEvent(prefix, oldValue, newValue, diff, now, ConfigSource.CONFIG_CENTER);

            // then
            assertThat(event.prefix()).isEqualTo(prefix);
            assertThat(event.oldValue()).isEqualTo(oldValue);
            assertThat(event.newValue()).isEqualTo(newValue);
            assertThat(event.diff()).isEqualTo(diff);
            assertThat(event.changedAt()).isEqualTo(now);
            assertThat(event.source()).isEqualTo(ConfigSource.CONFIG_CENTER);
        }

        @Test
        @DisplayName("null changedAt 应该自动设置为当前时间")
        void shouldSetCurrentTimeWhenChangedAtIsNull() {
            // given
            Instant before = Instant.now();

            // when
            ConfigChangeEvent event = new ConfigChangeEvent(
                    "prefix", null, "value", ConfigDiff.empty(), null, ConfigSource.MODULE_DEFAULT);
            Instant after = Instant.now();

            // then
            assertThat(event.changedAt()).isBetween(before, after);
        }
    }

    @Nested
    @DisplayName("hasChanges 测试")
    class HasChangesTests {

        @Test
        @DisplayName("有差异时应该返回 true")
        void shouldReturnTrueWhenHasDiff() {
            // given
            ConfigChangeEvent event = ConfigChangeEvent.update(
                    "prefix", Map.of("key", "old"), Map.of("key", "new"), ConfigSource.CONFIG_CENTER);

            // then
            assertThat(event.hasChanges()).isTrue();
        }

        @Test
        @DisplayName("无差异时应该返回 false")
        void shouldReturnFalseWhenNoDiff() {
            // given
            ConfigChangeEvent event = new ConfigChangeEvent(
                    "prefix", "value", "value", ConfigDiff.empty(), Instant.now(), ConfigSource.MODULE_DEFAULT);

            // then
            assertThat(event.hasChanges()).isFalse();
        }
    }

    @Nested
    @DisplayName("isAddition 测试")
    class IsAdditionTests {

        @Test
        @DisplayName("新增配置应该返回 true")
        void shouldReturnTrueForAddition() {
            // given
            ConfigChangeEvent event =
                    ConfigChangeEvent.addition("prefix", Map.of("key", "value"), ConfigSource.CONFIG_CENTER);

            // then
            assertThat(event.isAddition()).isTrue();
        }

        @Test
        @DisplayName("更新配置应该返回 false")
        void shouldReturnFalseForUpdate() {
            // given
            ConfigChangeEvent event = ConfigChangeEvent.update("prefix", "old", "new", ConfigSource.CONFIG_CENTER);

            // then
            assertThat(event.isAddition()).isFalse();
        }

        @Test
        @DisplayName("删除配置应该返回 false")
        void shouldReturnFalseForRemoval() {
            // given
            ConfigChangeEvent event = ConfigChangeEvent.removal("prefix", "value", ConfigSource.CONFIG_CENTER);

            // then
            assertThat(event.isAddition()).isFalse();
        }
    }

    @Nested
    @DisplayName("isRemoval 测试")
    class IsRemovalTests {

        @Test
        @DisplayName("删除配置应该返回 true")
        void shouldReturnTrueForRemoval() {
            // given
            ConfigChangeEvent event =
                    ConfigChangeEvent.removal("prefix", Map.of("key", "value"), ConfigSource.CONFIG_CENTER);

            // then
            assertThat(event.isRemoval()).isTrue();
        }

        @Test
        @DisplayName("新增配置应该返回 false")
        void shouldReturnFalseForAddition() {
            // given
            ConfigChangeEvent event = ConfigChangeEvent.addition("prefix", "value", ConfigSource.CONFIG_CENTER);

            // then
            assertThat(event.isRemoval()).isFalse();
        }

        @Test
        @DisplayName("更新配置应该返回 false")
        void shouldReturnFalseForUpdate() {
            // given
            ConfigChangeEvent event = ConfigChangeEvent.update("prefix", "old", "new", ConfigSource.CONFIG_CENTER);

            // then
            assertThat(event.isRemoval()).isFalse();
        }
    }

    @Nested
    @DisplayName("isUpdate 测试")
    class IsUpdateTests {

        @Test
        @DisplayName("更新配置应该返回 true")
        void shouldReturnTrueForUpdate() {
            // given
            ConfigChangeEvent event = ConfigChangeEvent.update("prefix", "old", "new", ConfigSource.CONFIG_CENTER);

            // then
            assertThat(event.isUpdate()).isTrue();
        }

        @Test
        @DisplayName("新增配置应该返回 false")
        void shouldReturnFalseForAddition() {
            // given
            ConfigChangeEvent event = ConfigChangeEvent.addition("prefix", "value", ConfigSource.CONFIG_CENTER);

            // then
            assertThat(event.isUpdate()).isFalse();
        }

        @Test
        @DisplayName("删除配置应该返回 false")
        void shouldReturnFalseForRemoval() {
            // given
            ConfigChangeEvent event = ConfigChangeEvent.removal("prefix", "value", ConfigSource.CONFIG_CENTER);

            // then
            assertThat(event.isUpdate()).isFalse();
        }
    }

    @Nested
    @DisplayName("静态工厂方法测试")
    class FactoryMethodTests {

        @Test
        @DisplayName("addition 应该创建正确的事件")
        void shouldCreateCorrectAdditionEvent() {
            // given
            String prefix = "app.config";
            Object newValue = Map.of("key", "value");

            // when
            ConfigChangeEvent event = ConfigChangeEvent.addition(prefix, newValue, ConfigSource.ENVIRONMENT);

            // then
            assertThat(event.prefix()).isEqualTo(prefix);
            assertThat(event.oldValue()).isNull();
            assertThat(event.newValue()).isEqualTo(newValue);
            assertThat(event.source()).isEqualTo(ConfigSource.ENVIRONMENT);
            assertThat(event.isAddition()).isTrue();
        }

        @Test
        @DisplayName("removal 应该创建正确的事件")
        void shouldCreateCorrectRemovalEvent() {
            // given
            String prefix = "app.config";
            Object oldValue = Map.of("key", "value");

            // when
            ConfigChangeEvent event = ConfigChangeEvent.removal(prefix, oldValue, ConfigSource.CONFIG_CENTER);

            // then
            assertThat(event.prefix()).isEqualTo(prefix);
            assertThat(event.oldValue()).isEqualTo(oldValue);
            assertThat(event.newValue()).isNull();
            assertThat(event.source()).isEqualTo(ConfigSource.CONFIG_CENTER);
            assertThat(event.isRemoval()).isTrue();
        }

        @Test
        @DisplayName("update 应该创建正确的事件")
        void shouldCreateCorrectUpdateEvent() {
            // given
            String prefix = "app.config";
            Object oldValue = Map.of("key", "old");
            Object newValue = Map.of("key", "new");

            // when
            ConfigChangeEvent event = ConfigChangeEvent.update(prefix, oldValue, newValue, ConfigSource.CURRENT_CONFIG);

            // then
            assertThat(event.prefix()).isEqualTo(prefix);
            assertThat(event.oldValue()).isEqualTo(oldValue);
            assertThat(event.newValue()).isEqualTo(newValue);
            assertThat(event.source()).isEqualTo(ConfigSource.CURRENT_CONFIG);
            assertThat(event.isUpdate()).isTrue();
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("应该处理 null 新值")
        void shouldHandleNullNewValue() {
            // when
            ConfigChangeEvent event = ConfigChangeEvent.removal("prefix", "value", ConfigSource.MODULE_DEFAULT);

            // then
            assertThat(event.newValue()).isNull();
            assertThat(event.isRemoval()).isTrue();
        }

        @Test
        @DisplayName("应该处理 null 旧值")
        void shouldHandleNullOldValue() {
            // when
            ConfigChangeEvent event = ConfigChangeEvent.addition("prefix", "value", ConfigSource.MODULE_DEFAULT);

            // then
            assertThat(event.oldValue()).isNull();
            assertThat(event.isAddition()).isTrue();
        }

        @Test
        @DisplayName("应该处理相同值")
        void shouldHandleSameValue() {
            // given
            Object value = Map.of("key", "value");

            // when
            ConfigChangeEvent event = ConfigChangeEvent.update("prefix", value, value, ConfigSource.MODULE_DEFAULT);

            // then
            assertThat(event.hasChanges()).isFalse();
        }
    }
}
