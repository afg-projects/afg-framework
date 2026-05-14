package io.github.afgprojects.framework.core.api.storage.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link StorageType} 存储类型枚举测试
 *
 * <p>测试存储类型枚举：
 * <ul>
 *   <li>LOCAL - 本地存储</li>
 *   <li>MINIO - MinIO 存储</li>
 *   <li>OSS - 阿里云 OSS</li>
 *   <li>S3 - AWS S3</li>
 * </ul>
 *
 * @see StorageType
 */
@DisplayName("StorageType 测试")
class StorageTypeTest {

    /**
     * 枚举值测试
     */
    @Nested
    @DisplayName("枚举值测试")
    class EnumValuesTests {

        /**
         * 测试包含所有存储类型
         */
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

        /**
         * 测试返回正确的代码
         */
        @Test
        @DisplayName("应该返回正确的代码")
        void shouldReturnCorrectCode() {
            assertThat(StorageType.LOCAL.getCode()).isEqualTo("local");
            assertThat(StorageType.MINIO.getCode()).isEqualTo("minio");
            assertThat(StorageType.OSS.getCode()).isEqualTo("oss");
            assertThat(StorageType.S3.getCode()).isEqualTo("s3");
        }
    }

    /**
     * fromCode 方法测试
     */
    @Nested
    @DisplayName("fromCode 测试")
    class FromCodeTests {

        /**
         * 测试根据代码返回正确的类型
         */
        @Test
        @DisplayName("应该根据代码返回正确的类型")
        void shouldReturnCorrectTypeFromCode() {
            assertThat(StorageType.fromCode("local")).isEqualTo(StorageType.LOCAL);
            assertThat(StorageType.fromCode("minio")).isEqualTo(StorageType.MINIO);
            assertThat(StorageType.fromCode("oss")).isEqualTo(StorageType.OSS);
            assertThat(StorageType.fromCode("s3")).isEqualTo(StorageType.S3);
        }

        /**
         * 测试支持大写代码
         */
        @Test
        @DisplayName("应该支持大写代码")
        void shouldSupportUpperCaseCode() {
            assertThat(StorageType.fromCode("LOCAL")).isEqualTo(StorageType.LOCAL);
            assertThat(StorageType.fromCode("MINIO")).isEqualTo(StorageType.MINIO);
        }

        /**
         * 测试支持混合大小写代码
         */
        @Test
        @DisplayName("应该支持混合大小写代码")
        void shouldSupportMixedCaseCode() {
            assertThat(StorageType.fromCode("Local")).isEqualTo(StorageType.LOCAL);
            assertThat(StorageType.fromCode("MinIO")).isEqualTo(StorageType.MINIO);
        }

        /**
         * 测试对无效代码返回 null
         */
        @Test
        @DisplayName("应该对无效代码返回 null")
        void shouldReturnNullForInvalidCode() {
            assertThat(StorageType.fromCode("invalid")).isNull();
            assertThat(StorageType.fromCode("")).isNull();
        }
    }
}