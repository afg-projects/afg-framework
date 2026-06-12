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

import org.jspecify.annotations.Nullable;

/**
 * 字段加密 SPI 接口
 * <p>
 * 提供实体字段的加密和解密能力，配合 {@code @EncryptedField} 注解使用。
 * 框架在实体 INSERT 时自动加密标注了 {@code @EncryptedField} 的字段，
 * 在 SELECT（EntityMapper afterLoad）时自动解密。
 *
 * <h3>加密算法</h3>
 * <p>
 * 默认算法为 AES。{@code @EncryptedField} 注解的 {@code algorithm} 属性指定加密算法，
 * {@code keyRef} 属性指定密钥引用名称（用于从密钥管理系统中获取密钥）。
 *
 * <h3>自定义实现</h3>
 * <p>
 * 业务应用可以实现此接口，提供基于 KMS、HSM 或其他密钥管理系统的加密方案。
 * 实现类通过 Spring Bean 注册，框架自动发现并使用。
 * <pre>
 * &#064;Component
 * public class KmsFieldEncryptor implements FieldEncryptor {
 *     &#064;Override
 *     public String encrypt(String plaintext, String algorithm, String keyRef) {
 *         return kmsClient.encrypt(plaintext, keyRef);
 *     }
 *
 *     &#064;Override
 *     public String decrypt(String ciphertext, String algorithm, String keyRef) {
 *         return kmsClient.decrypt(ciphertext, keyRef);
 *     }
 * }
 * </pre>
 *
 * <h3>NoOp 降级</h3>
 * <p>
 * 框架内置 {@link NoOpFieldEncryptor} 作为默认降级实现，直接返回原文（不加密）。
 * 引入实际加密实现后，NoOp 自动被替换。
 *
 * @see NoOpFieldEncryptor
 * @see io.github.afgprojects.framework.apt.entity.EncryptedField
 */
public interface FieldEncryptor {

    /**
     * 加密字段值
     *
     * @param plaintext  明文值，不为 null
     * @param algorithm  加密算法（如 "AES"），来自 @EncryptedField 注解
     * @param keyRef     密钥引用名称，来自 @EncryptedField 注解，可能为空字符串
     * @return 加密后的密文
     */
    String encrypt(String plaintext, String algorithm, @Nullable String keyRef);

    /**
     * 解密字段值
     *
     * @param ciphertext 密文值，不为 null
     * @param algorithm  加密算法（如 "AES"），来自 @EncryptedField 注解
     * @param keyRef     密钥引用名称，来自 @EncryptedField 注解，可能为空字符串
     * @return 解密后的明文
     */
    String decrypt(String ciphertext, String algorithm, @Nullable String keyRef);
}
