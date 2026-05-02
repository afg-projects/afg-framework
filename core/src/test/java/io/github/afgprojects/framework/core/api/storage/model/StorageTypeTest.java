package io.github.afgprojects.framework.core.api.storage.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * StorageType 测试
 */
@DisplayName("StorageType 测试")
class StorageTypeTest {

    @Nested
    @DisplayName("枚举值测试")
    class EnumValuesTests {

        @Test
        @DisplayName("应该包含所有存储类型")
        void shouldContainAllTypes() {
            assertThat(StorageType.values()).hasSize(4);
            assertThat(StorageType.values()).contains(
                    StorageType.LOCAL,
                    StorageType.MINIO,
                    StorageType.OSS,
                    StorageType.S3
            );
        }

        @Test
        @DisplayName("应该返回正确的代码")
        void shouldReturnCorrectCode() {
            assertThat(StorageType.LOCAL.getCode()).isEqualTo("local");
            assertThat(StorageType.MINIO.getCode()).isEqualTo("minio");
            assertThat(StorageType.OSS.getCode()).isEqualTo("oss");
            assertThat(StorageType.S3.getCode()).isEqualTo("s3");
        }
    }

    @Nested
    @DisplayName("fromCode 测试")
    class FromCodeTests {

        @Test
        @DisplayName("应该根据代码返回正确的类型")
        void shouldReturnCorrectTypeFromCode() {
            assertThat(StorageType.fromCode("local")).isEqualTo(StorageType.LOCAL);
            assertThat(StorageType.fromCode("minio")).isEqualTo(StorageType.MINIO);
            assertThat(StorageType.fromCode("oss")).isEqualTo(StorageType.OSS);
            assertThat(StorageType.fromCode("s3")).isEqualTo(StorageType.S3);
        }

        @Test
        @DisplayName("应该支持大写代码")
        void shouldSupportUpperCaseCode() {
            assertThat(StorageType.fromCode("LOCAL")).isEqualTo(StorageType.LOCAL);
            assertThat(StorageType.fromCode("MINIO")).isEqualTo(StorageType.MINIO);
        }

        @Test
        @DisplayName("应该支持混合大小写代码")
        void shouldSupportMixedCaseCode() {
            assertThat(StorageType.fromCode("Local")).isEqualTo(StorageType.LOCAL);
            assertThat(StorageType.fromCode("MinIO")).isEqualTo(StorageType.MINIO);
        }

        @Test
        @DisplayName("应该对无效代码返回 null")
        void shouldReturnNullForInvalidCode() {
            assertThat(StorageType.fromCode("invalid")).isNull();
            assertThat(StorageType.fromCode("")).isNull();
        }
    }
}