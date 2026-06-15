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
package io.github.afgprojects.framework.data.core.encryption;

import org.jspecify.annotations.Nullable;

/**
 * 字段加密密钥提供者 SPI 接口
 * <p>
 * 为 {@link io.github.afgprojects.framework.data.core.entity.FieldEncryptor} 提供加密密钥和盲索引密钥。
 * 密钥通过 {@code @EncryptedField(keyRef)} 注解的 keyRef 属性引用。
 *
 * <h3>内置实现</h3>
 * <p>
 * 框架提供 {@code ConfigFieldEncryptionKeyProvider}，从 Spring 配置
 * {@code afg.data.encryption.keys.<keyRef>} 读取 Base64 编码的密钥。
 *
 * <h3>自定义实现</h3>
 * <p>
 * 业务应用可以实现此接口，提供基于 Vault、KMS 或 HSM 的密钥管理方案：
 * <pre>
 * &#064;Component
 * public class VaultFieldEncryptionKeyProvider implements FieldEncryptionKeyProvider {
 *     &#064;Override
 *     public byte[] getEncryptionKey(String keyRef) {
 *         return vaultClient.getSecret("encryption/" + keyRef);
 *     }
 *
 *     &#064;Override
 *     public byte[] getBlindIndexKey(String keyRef) {
 *         return vaultClient.getSecret("blind-index/" + keyRef);
 *     }
 * }
 * </pre>
 *
 * @see io.github.afgprojects.framework.data.core.entity.FieldEncryptor
 * @see BlindIndexProvider
 */
public interface FieldEncryptionKeyProvider {

    /**
     * 获取加密密钥（AES-256）
     *
     * @param keyRef 密钥引用名称，来自 @EncryptedField(keyRef) 注解，可能为空字符串
     * @return 加密密钥字节数组（32 字节 for AES-256）
     * @throws io.github.afgprojects.framework.commons.exception.BusinessException
     *         如果密钥不存在或长度不合法
     */
    byte[] getEncryptionKey(@Nullable String keyRef);

    /**
     * 获取盲索引密钥（HMAC-SHA256）
     *
     * @param keyRef 密钥引用名称，来自 @EncryptedField(keyRef) 注解，可能为空字符串
     * @return HMAC 密钥字节数组（至少 32 字节）
     * @throws io.github.afgprojects.framework.commons.exception.BusinessException
     *         如果密钥不存在或长度不合法
     */
    byte[] getBlindIndexKey(@Nullable String keyRef);
}
