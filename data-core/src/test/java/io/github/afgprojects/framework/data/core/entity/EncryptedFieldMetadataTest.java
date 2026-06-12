/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.afgprojects.framework.data.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EncryptedFieldMetadata 测试
 */
@DisplayName("EncryptedFieldMetadata 测试")
class EncryptedFieldMetadataTest {

    @Nested
    @DisplayName("Record 构造与访问器")
    class ConstructionTests {

        @Test
        @DisplayName("应正确构造并返回所有字段值")
        void shouldConstructWithAllFields() {
            EncryptedFieldMetadata metadata = new EncryptedFieldMetadata("idCard", "AES", "user-key");

            assertThat(metadata.fieldName()).isEqualTo("idCard");
            assertThat(metadata.algorithm()).isEqualTo("AES");
            assertThat(metadata.keyRef()).isEqualTo("user-key");
        }

        @Test
        @DisplayName("默认算法为 AES、keyRef 为空字符串时应正确存储")
        void shouldStoreDefaultAlgorithmAndEmptyKeyRef() {
            EncryptedFieldMetadata metadata = new EncryptedFieldMetadata("phone", "AES", "");

            assertThat(metadata.fieldName()).isEqualTo("phone");
            assertThat(metadata.algorithm()).isEqualTo("AES");
            assertThat(metadata.keyRef()).isEmpty();
        }

        @Test
        @DisplayName("keyRef 为 null 时应正确存储")
        void shouldStoreNullKeyRef() {
            EncryptedFieldMetadata metadata = new EncryptedFieldMetadata("bankAccount", "AES", null);

            assertThat(metadata.fieldName()).isEqualTo("bankAccount");
            assertThat(metadata.algorithm()).isEqualTo("AES");
            assertThat(metadata.keyRef()).isNull();
        }
    }

    @Nested
    @DisplayName("Record 相等性")
    class EqualityTests {

        @Test
        @DisplayName("相同字段值的两个实例应相等")
        void shouldBeEqual_whenSameFieldValues() {
            EncryptedFieldMetadata meta1 = new EncryptedFieldMetadata("idCard", "AES", "key1");
            EncryptedFieldMetadata meta2 = new EncryptedFieldMetadata("idCard", "AES", "key1");

            assertThat(meta1).isEqualTo(meta2);
            assertThat(meta1.hashCode()).isEqualTo(meta2.hashCode());
        }

        @Test
        @DisplayName("不同字段名的两个实例应不相等")
        void shouldNotBeEqual_whenDifferentFieldName() {
            EncryptedFieldMetadata meta1 = new EncryptedFieldMetadata("idCard", "AES", "key1");
            EncryptedFieldMetadata meta2 = new EncryptedFieldMetadata("phone", "AES", "key1");

            assertThat(meta1).isNotEqualTo(meta2);
        }

        @Test
        @DisplayName("不同算法的两个实例应不相等")
        void shouldNotBeEqual_whenDifferentAlgorithm() {
            EncryptedFieldMetadata meta1 = new EncryptedFieldMetadata("idCard", "AES", "key1");
            EncryptedFieldMetadata meta2 = new EncryptedFieldMetadata("idCard", "RSA", "key1");

            assertThat(meta1).isNotEqualTo(meta2);
        }

        @Test
        @DisplayName("不同 keyRef 的两个实例应不相等")
        void shouldNotBeEqual_whenDifferentKeyRef() {
            EncryptedFieldMetadata meta1 = new EncryptedFieldMetadata("idCard", "AES", "key1");
            EncryptedFieldMetadata meta2 = new EncryptedFieldMetadata("idCard", "AES", "key2");

            assertThat(meta1).isNotEqualTo(meta2);
        }
    }

    @Nested
    @DisplayName("toString 方法")
    class ToStringTests {

        @Test
        @DisplayName("toString 应包含所有字段值")
        void shouldContainAllFieldValues() {
            EncryptedFieldMetadata metadata = new EncryptedFieldMetadata("idCard", "AES", "user-key");
            String str = metadata.toString();

            assertThat(str).contains("idCard");
            assertThat(str).contains("AES");
            assertThat(str).contains("user-key");
        }
    }
}
