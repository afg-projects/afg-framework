package io.github.afgprojects.framework.integration.storage;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.api.storage.model.StorageMetadata;
import io.github.afgprojects.framework.core.api.storage.model.StorageObject;

/**
 * 存储模型测试
 */
class StorageModelTest {

    @Test
    void shouldCreateStorageObject() {
        StorageObject obj = StorageObject.of("test/file.txt", 1024, "text/plain");

        assertThat(obj.key()).isEqualTo("test/file.txt");
        assertThat(obj.size()).isEqualTo(1024);
        assertThat(obj.contentType()).isEqualTo("text/plain");
        assertThat(obj.getFileName()).isEqualTo("file.txt");
        assertThat(obj.getExtension()).isEqualTo("txt");
    }

    @Test
    void shouldExtractFileNameFromKey() {
        StorageObject obj1 = StorageObject.of("file.txt", 0, null);
        assertThat(obj1.getFileName()).isEqualTo("file.txt");

        StorageObject obj2 = StorageObject.of("path/to/file.txt", 0, null);
        assertThat(obj2.getFileName()).isEqualTo("file.txt");

        StorageObject obj3 = StorageObject.of("no-extension", 0, null);
        assertThat(obj3.getFileName()).isEqualTo("no-extension");
        assertThat(obj3.getExtension()).isNull();
    }

    @Test
    void shouldHandleMetadata() {
        StorageMetadata metadata = StorageMetadata.builder()
                .put("author", "test")
                .put("version", "1.0")
                .build();

        assertThat(metadata.get("author")).isEqualTo("test");
        assertThat(metadata.get("version")).isEqualTo("1.0");
        assertThat(metadata.containsKey("author")).isTrue();
        assertThat(metadata.isEmpty()).isFalse();

        Map<String, String> all = metadata.getAll();
        assertThat(all).hasSize(2);
    }
}
