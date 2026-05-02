package io.github.afgprojects.framework.core.api.storage.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * StorageMetadata 测试
 */
@DisplayName("StorageMetadata 测试")
class StorageMetadataTest {

    @Nested
    @DisplayName("构造方法测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该创建空元数据")
        void shouldCreateEmptyMetadata() {
            StorageMetadata metadata = new StorageMetadata();

            assertThat(metadata.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("应该从 Map 创建元数据")
        void shouldCreateFromMap() {
            Map<String, String> data = new HashMap<>();
            data.put("author", "test");
            data.put("version", "1.0");

            StorageMetadata metadata = new StorageMetadata(data);

            assertThat(metadata.get("author")).isEqualTo("test");
            assertThat(metadata.get("version")).isEqualTo("1.0");
            assertThat(metadata.isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("操作方法测试")
    class OperationTests {

        private StorageMetadata metadata;

        @BeforeEach
        void setUp() {
            metadata = new StorageMetadata();
        }

        @Test
        @DisplayName("应该正确设置和获取值")
        void shouldPutAndGet() {
            metadata.put("key1", "value1");

            assertThat(metadata.get("key1")).isEqualTo("value1");
            assertThat(metadata.containsKey("key1")).isTrue();
        }

        @Test
        @DisplayName("应该对不存在的键返回 null")
        void shouldReturnNullForMissingKey() {
            assertThat(metadata.get("missing")).isNull();
            assertThat(metadata.containsKey("missing")).isFalse();
        }

        @Test
        @DisplayName("应该返回不可变的元数据 Map")
        void shouldReturnUnmodifiableMap() {
            metadata.put("key1", "value1");
            Map<String, String> all = metadata.getAll();

            assertThat(all).containsEntry("key1", "value1");
            assertThatThrownBy(() -> all.put("key2", "value2"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("应该正确判断是否为空")
        void shouldCheckIsEmpty() {
            assertThat(metadata.isEmpty()).isTrue();

            metadata.put("key", "value");
            assertThat(metadata.isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("Builder 测试")
    class BuilderTests {

        @Test
        @DisplayName("应该使用 Builder 构建元数据")
        void shouldBuildWithBuilder() {
            StorageMetadata metadata = StorageMetadata.builder()
                    .put("author", "test")
                    .put("version", "1.0")
                    .build();

            assertThat(metadata.get("author")).isEqualTo("test");
            assertThat(metadata.get("version")).isEqualTo("1.0");
        }

        @Test
        @DisplayName("应该支持 putAll")
        void shouldSupportPutAll() {
            Map<String, String> data = new HashMap<>();
            data.put("key1", "value1");
            data.put("key2", "value2");

            StorageMetadata metadata = StorageMetadata.builder()
                    .putAll(data)
                    .build();

            assertThat(metadata.get("key1")).isEqualTo("value1");
            assertThat(metadata.get("key2")).isEqualTo("value2");
        }
    }

}