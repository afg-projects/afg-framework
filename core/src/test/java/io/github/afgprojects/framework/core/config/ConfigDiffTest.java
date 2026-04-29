package io.github.afgprojects.framework.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ConfigDiff 测试
 */
@DisplayName("ConfigDiff 测试")
class ConfigDiffTest {

    @Nested
    @DisplayName("empty 测试")
    class EmptyTests {

        @Test
        @DisplayName("应该创建空的差异")
        void shouldCreateEmptyDiff() {
            // when
            ConfigDiff diff = ConfigDiff.empty();

            // then
            assertThat(diff.isEmpty()).isTrue();
            assertThat(diff.addedKeys()).isEmpty();
            assertThat(diff.removedKeys()).isEmpty();
            assertThat(diff.changedKeys()).isEmpty();
            assertThat(diff.valueChanges()).isEmpty();
            assertThat(diff.totalChanges()).isZero();
        }
    }

    @Nested
    @DisplayName("addition 测试")
    class AdditionTests {

        @Test
        @DisplayName("应该创建 Map 类型的新增差异")
        void shouldCreateAdditionForMap() {
            // given
            Map<String, Object> newValue = Map.of("key1", "value1", "key2", "value2");

            // when
            ConfigDiff diff = ConfigDiff.addition(newValue);

            // then
            assertThat(diff.addedKeys()).containsExactlyInAnyOrder("key1", "key2");
            assertThat(diff.removedKeys()).isEmpty();
            assertThat(diff.changedKeys()).isEmpty();
            assertThat(diff.isEmpty()).isFalse();
            assertThat(diff.totalChanges()).isEqualTo(2);
        }

        @Test
        @DisplayName("应该创建非 Map 类型的新增差异")
        void shouldCreateAdditionForNonMap() {
            // when
            ConfigDiff diff = ConfigDiff.addition("simple-value");

            // then
            assertThat(diff.addedKeys()).containsExactly("value");
            assertThat(diff.removedKeys()).isEmpty();
            assertThat(diff.changedKeys()).isEmpty();
        }

        @Test
        @DisplayName("Map 中的 null key 应该被忽略")
        void shouldIgnoreNullKeyInMap() {
            // given
            Map<String, Object> map = new HashMap<>();
            map.put("valid", "value");
            map.put(null, "ignored");

            // when
            ConfigDiff diff = ConfigDiff.addition(map);

            // then
            assertThat(diff.addedKeys()).containsExactly("valid");
        }
    }

    @Nested
    @DisplayName("removal 测试")
    class RemovalTests {

        @Test
        @DisplayName("应该创建 Map 类型的删除差异")
        void shouldCreateRemovalForMap() {
            // given
            Map<String, Object> oldValue = Map.of("key1", "value1", "key2", "value2");

            // when
            ConfigDiff diff = ConfigDiff.removal(oldValue);

            // then
            assertThat(diff.removedKeys()).containsExactlyInAnyOrder("key1", "key2");
            assertThat(diff.addedKeys()).isEmpty();
            assertThat(diff.changedKeys()).isEmpty();
        }

        @Test
        @DisplayName("应该创建非 Map 类型的删除差异")
        void shouldCreateRemovalForNonMap() {
            // when
            ConfigDiff diff = ConfigDiff.removal("simple-value");

            // then
            assertThat(diff.removedKeys()).containsExactly("value");
            assertThat(diff.addedKeys()).isEmpty();
            assertThat(diff.changedKeys()).isEmpty();
        }
    }

    @Nested
    @DisplayName("compute 测试")
    class ComputeTests {

        @Test
        @DisplayName("两个 null 应该返回空差异")
        void shouldReturnEmptyForBothNull() {
            // when
            ConfigDiff diff = ConfigDiff.compute(null, null);

            // then
            assertThat(diff.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("null 到值应该是新增")
        void shouldBeAdditionFromNull() {
            // when
            ConfigDiff diff = ConfigDiff.compute(null, Map.of("key", "value"));

            // then
            assertThat(diff.addedKeys()).containsExactly("key");
            assertThat(diff.removedKeys()).isEmpty();
        }

        @Test
        @DisplayName("值到 null 应该是删除")
        void shouldBeRemovalToNull() {
            // when
            ConfigDiff diff = ConfigDiff.compute(Map.of("key", "value"), null);

            // then
            assertThat(diff.removedKeys()).containsExactly("key");
            assertThat(diff.addedKeys()).isEmpty();
        }

        @Test
        @DisplayName("应该正确计算变更")
        void shouldComputeChanges() {
            // given
            Map<String, Object> oldValue = Map.of("key1", "value1", "key2", "same");
            Map<String, Object> newValue = Map.of("key1", "changed", "key2", "same", "key3", "new");

            // when
            ConfigDiff diff = ConfigDiff.compute(oldValue, newValue);

            // then
            assertThat(diff.addedKeys()).containsExactly("key3");
            assertThat(diff.removedKeys()).isEmpty();
            assertThat(diff.changedKeys()).containsExactly("key1");
            assertThat(diff.totalChanges()).isEqualTo(2);
        }

        @Test
        @DisplayName("应该正确计算删除和新增")
        void shouldComputeRemovalsAndAdditions() {
            // given
            Map<String, Object> oldValue = Map.of("removed", "value", "kept", "same");
            Map<String, Object> newValue = Map.of("added", "new", "kept", "same");

            // when
            ConfigDiff diff = ConfigDiff.compute(oldValue, newValue);

            // then
            assertThat(diff.addedKeys()).containsExactly("added");
            assertThat(diff.removedKeys()).containsExactly("removed");
            assertThat(diff.changedKeys()).isEmpty();
        }

        @Test
        @DisplayName("非 Map 值应该转换为单 key Map")
        void shouldConvertNonMapToSingleKeyMap() {
            // when
            ConfigDiff diff = ConfigDiff.compute("old", "new");

            // then
            assertThat(diff.changedKeys()).containsExactly("value");
            assertThat(diff.valueChanges()).containsKey("value");
        }

        @Test
        @DisplayName("相同值应该返回空差异")
        void shouldReturnEmptyForSameValues() {
            // given
            Map<String, Object> value = Map.of("key", "value");

            // when
            ConfigDiff diff = ConfigDiff.compute(value, value);

            // then
            assertThat(diff.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("ValueChange 测试")
    class ValueChangeTests {

        @Test
        @DisplayName("两个 null 应该没有实际变化")
        void shouldNotHaveActualChangeWhenBothNull() {
            // given
            ConfigDiff.ValueChange change = new ConfigDiff.ValueChange(null, null);

            // then
            assertThat(change.hasActualChange()).isFalse();
        }

        @Test
        @DisplayName("null 到值应该有实际变化")
        void shouldHaveActualChangeFromNull() {
            // given
            ConfigDiff.ValueChange change = new ConfigDiff.ValueChange(null, "value");

            // then
            assertThat(change.hasActualChange()).isTrue();
        }

        @Test
        @DisplayName("值到 null 应该有实际变化")
        void shouldHaveActualChangeToNull() {
            // given
            ConfigDiff.ValueChange change = new ConfigDiff.ValueChange("value", null);

            // then
            assertThat(change.hasActualChange()).isTrue();
        }

        @Test
        @DisplayName("相同值应该没有实际变化")
        void shouldNotHaveActualChangeForSameValue() {
            // given
            ConfigDiff.ValueChange change = new ConfigDiff.ValueChange("same", "same");

            // then
            assertThat(change.hasActualChange()).isFalse();
        }

        @Test
        @DisplayName("不同值应该有实际变化")
        void shouldHaveActualChangeForDifferentValues() {
            // given
            ConfigDiff.ValueChange change = new ConfigDiff.ValueChange("old", "new");

            // then
            assertThat(change.hasActualChange()).isTrue();
        }
    }

    @Nested
    @DisplayName("totalChanges 测试")
    class TotalChangesTests {

        @Test
        @DisplayName("应该返回正确的变更总数")
        void shouldReturnCorrectTotalChanges() {
            // given
            Map<String, Object> oldValue = Map.of("removed", "v", "changed", "old");
            Map<String, Object> newValue = Map.of("added", "v", "changed", "new");

            // when
            ConfigDiff diff = ConfigDiff.compute(oldValue, newValue);

            // then
            assertThat(diff.totalChanges()).isEqualTo(3); // 1 removed + 1 added + 1 changed
        }
    }

    @Nested
    @DisplayName("valueChanges 测试")
    class ValueChangesTests {

        @Test
        @DisplayName("应该记录所有变更的详细信息")
        void shouldRecordAllValueChanges() {
            // given
            Map<String, Object> oldValue = Map.of("key1", "old1", "key2", "same");
            Map<String, Object> newValue = Map.of("key1", "new1", "key2", "same", "key3", "new3");

            // when
            ConfigDiff diff = ConfigDiff.compute(oldValue, newValue);

            // then
            assertThat(diff.valueChanges()).hasSize(2);
            assertThat(diff.valueChanges().get("key1").oldValue()).isEqualTo("old1");
            assertThat(diff.valueChanges().get("key1").newValue()).isEqualTo("new1");
            assertThat(diff.valueChanges().get("key3").oldValue()).isNull();
            assertThat(diff.valueChanges().get("key3").newValue()).isEqualTo("new3");
        }
    }

    @Nested
    @DisplayName("不可变性测试")
    class ImmutabilityTests {

        @Test
        @DisplayName("返回的集合应该是不可修改的")
        void shouldReturnUnmodifiableCollections() {
            // given
            ConfigDiff diff = ConfigDiff.compute(Map.of("k", "v"), Map.of("k", "v2"));

            // when & then
            assertThatThrownBy(() -> diff.addedKeys().add("test")).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> diff.removedKeys().add("test")).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> diff.changedKeys().add("test")).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> diff.valueChanges().put("test", null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
