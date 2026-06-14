package io.github.afgprojects.framework.integration.storage;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.afgprojects.framework.core.api.storage.FileStorage;
import io.github.afgprojects.framework.core.api.storage.FileStorageFactory;
import io.github.afgprojects.framework.core.api.storage.model.StorageType;
import io.github.afgprojects.framework.integration.storage.autoconfigure.DefaultFileStorageFactory;
import io.github.afgprojects.framework.integration.storage.local.LocalFileStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DefaultFileStorageFactory 测试
 *
 * <p>测试文件存储工厂的注册、获取、注销操作
 */
@DisplayName("DefaultFileStorageFactory 文件存储工厂测试")
class DefaultFileStorageFactoryTest {

    @TempDir
    Path tempDir;

    private DefaultFileStorageFactory factory;
    private FileStorage localStorage;

    @BeforeEach
    void setUp() {
        factory = new DefaultFileStorageFactory("default");
        localStorage = new LocalFileStorage("test-bucket", tempDir.toString(), null);
    }

    @Nested
    @DisplayName("register / getDefaultStorage 操作")
    class RegisterAndGetDefault {

        @Test
        @DisplayName("register 后 getDefaultStorage 应返回注册的存储")
        void shouldReturnRegisteredStorage() {
            factory.register("default", localStorage);

            FileStorage result = factory.getDefaultStorage();
            assertThat(result).isNotNull();
            assertThat(result.getStorageType()).isEqualTo(StorageType.LOCAL);
            assertThat(result.getBucket()).isEqualTo("test-bucket");
        }

        @Test
        @DisplayName("getDefaultStorage 未配置时应抛出 IllegalStateException")
        void shouldThrowException_whenDefaultNotConfigured() {
            assertThatThrownBy(() -> factory.getDefaultStorage())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("getStorage 操作")
    class GetStorage {

        @Test
        @DisplayName("getStorage 应按名称返回注册的存储")
        void shouldReturnStorageByName() {
            factory.register("my-storage", localStorage);

            FileStorage result = factory.getStorage("my-storage");
            assertThat(result).isNotNull();
            assertThat(result.getBucket()).isEqualTo("test-bucket");
        }

        @Test
        @DisplayName("getStorage 不存在的名称应返回 null")
        void shouldReturnNull_whenNameNotExists() {
            FileStorage result = factory.getStorage("nonexistent");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("getStorage 按类型和名称应返回匹配的存储")
        void shouldReturnStorageByTypeAndName() {
            factory.register("typed-storage", localStorage);

            FileStorage result = factory.getStorage(StorageType.LOCAL, "typed-storage");
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("getStorage 类型不匹配应返回 null")
        void shouldReturnNull_whenTypeMismatch() {
            factory.register("mismatched", localStorage);

            FileStorage result = factory.getStorage(StorageType.S3, "mismatched");
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("unregister 操作")
    class Unregister {

        @Test
        @DisplayName("unregister 应注销存储并返回它")
        void shouldUnregisterStorage() {
            factory.register("to-remove", localStorage);

            FileStorage removed = factory.unregister("to-remove");
            assertThat(removed).isNotNull();
            assertThat(factory.hasStorage("to-remove")).isFalse();
        }

        @Test
        @DisplayName("unregister 不存在的名称应返回 null")
        void shouldReturnNull_whenUnregisterNonExistent() {
            FileStorage removed = factory.unregister("nonexistent");
            assertThat(removed).isNull();
        }
    }

    @Nested
    @DisplayName("hasStorage 操作")
    class HasStorage {

        @Test
        @DisplayName("hasStorage 应正确判断存储是否存在")
        void shouldDetectStorageExistence() {
            assertThat(factory.hasStorage("check")).isFalse();

            factory.register("check", localStorage);
            assertThat(factory.hasStorage("check")).isTrue();
        }
    }

    @Nested
    @DisplayName("size 操作")
    class Size {

        @Test
        @DisplayName("size 应返回注册的存储数量")
        void shouldReturnStorageCount() {
            assertThat(factory.size()).isEqualTo(0);

            factory.register("storage-1", localStorage);
            assertThat(factory.size()).isEqualTo(1);

            FileStorage anotherStorage = new LocalFileStorage("another-bucket", tempDir.toString(), null);
            factory.register("storage-2", anotherStorage);
            assertThat(factory.size()).isEqualTo(2);
        }
    }
}
