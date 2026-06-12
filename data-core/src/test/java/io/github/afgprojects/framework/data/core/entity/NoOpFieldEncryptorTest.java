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
 * NoOpFieldEncryptor 测试
 */
@DisplayName("NoOpFieldEncryptor 测试")
class NoOpFieldEncryptorTest {

    private final NoOpFieldEncryptor encryptor = new NoOpFieldEncryptor();

    @Nested
    @DisplayName("encrypt 方法")
    class EncryptTests {

        @Test
        @DisplayName("应返回原文不进行加密")
        void shouldReturnPlaintextUnchanged() {
            String plaintext = "sensitive-data";
            assertThat(encryptor.encrypt(plaintext, "AES", "key1")).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("不同算法应都返回原文")
        void shouldReturnPlaintextWithAnyAlgorithm() {
            String plaintext = "test-value";
            assertThat(encryptor.encrypt(plaintext, "AES", "key1")).isEqualTo(plaintext);
            assertThat(encryptor.encrypt(plaintext, "RSA", "key2")).isEqualTo(plaintext);
            assertThat(encryptor.encrypt(plaintext, "SM4", "key3")).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("keyRef 为 null 时应正常返回原文")
        void shouldReturnPlaintext_whenKeyRefIsNull() {
            String plaintext = "test-data";
            assertThat(encryptor.encrypt(plaintext, "AES", null)).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("keyRef 为空字符串时应正常返回原文")
        void shouldReturnPlaintext_whenKeyRefIsEmpty() {
            String plaintext = "test-data";
            assertThat(encryptor.encrypt(plaintext, "AES", "")).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("空字符串值应原样返回")
        void shouldReturnEmptyString_whenPlaintextIsEmpty() {
            assertThat(encryptor.encrypt("", "AES", "key1")).isEmpty();
        }
    }

    @Nested
    @DisplayName("decrypt 方法")
    class DecryptTests {

        @Test
        @DisplayName("应返回密文不进行解密")
        void shouldReturnCiphertextUnchanged() {
            String ciphertext = "encrypted-data";
            assertThat(encryptor.decrypt(ciphertext, "AES", "key1")).isEqualTo(ciphertext);
        }

        @Test
        @DisplayName("不同算法应都返回密文原文")
        void shouldReturnCiphertextWithAnyAlgorithm() {
            String ciphertext = "encrypted-value";
            assertThat(encryptor.decrypt(ciphertext, "AES", "key1")).isEqualTo(ciphertext);
            assertThat(encryptor.decrypt(ciphertext, "RSA", "key2")).isEqualTo(ciphertext);
            assertThat(encryptor.decrypt(ciphertext, "SM4", "key3")).isEqualTo(ciphertext);
        }

        @Test
        @DisplayName("keyRef 为 null 时应正常返回密文")
        void shouldReturnCiphertext_whenKeyRefIsNull() {
            String ciphertext = "encrypted-data";
            assertThat(encryptor.decrypt(ciphertext, "AES", null)).isEqualTo(ciphertext);
        }

        @Test
        @DisplayName("keyRef 为空字符串时应正常返回密文")
        void shouldReturnCiphertext_whenKeyRefIsEmpty() {
            String ciphertext = "encrypted-data";
            assertThat(encryptor.decrypt(ciphertext, "AES", "")).isEqualTo(ciphertext);
        }

        @Test
        @DisplayName("空字符串值应原样返回")
        void shouldReturnEmptyString_whenCiphertextIsEmpty() {
            assertThat(encryptor.decrypt("", "AES", "key1")).isEmpty();
        }
    }

    @Nested
    @DisplayName("FieldEncryptor 接口契约")
    class InterfaceContractTests {

        @Test
        @DisplayName("NoOpFieldEncryptor 应实现 FieldEncryptor 接口")
        void shouldImplementFieldEncryptorInterface() {
            assertThat(encryptor).isInstanceOf(FieldEncryptor.class);
        }

        @Test
        @DisplayName("encrypt 和 decrypt 对同一值应返回相同结果")
        void shouldReturnSameResultForEncryptAndDecrypt() {
            String value = "hello-world";
            String encrypted = encryptor.encrypt(value, "AES", "key1");
            String decrypted = encryptor.decrypt(encrypted, "AES", "key1");
            assertThat(decrypted).isEqualTo(value);
        }
    }
}
