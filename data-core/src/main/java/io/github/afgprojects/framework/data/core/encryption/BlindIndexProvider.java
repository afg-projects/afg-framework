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
 * 盲索引计算 SPI 接口
 * <p>
 * 为加密字段提供盲索引（Blind Index）计算能力。盲索引使用 HMAC-SHA256 对明文进行哈希，
 * 存储在伴随列中，用于加密字段的等值查询。
 *
 * <h3>工作原理</h3>
 * <p>
 * 当实体字段使用 {@code @EncryptedField} 标注且指定了盲索引列时：
 * <ul>
 *   <li>写入时：主列存 AES-GCM 密文，盲索引列存 HMAC-SHA256 哈希值</li>
 *   <li>查询时：条件值被转为 HMAC 哈希值，查询走盲索引列匹配</li>
 * </ul>
 *
 * <h3>安全性</h3>
 * <p>
 * 盲索引仅泄露等值信息（相同明文产生相同哈希），不支持 LIKE/范围查询。
 * HMAC 密钥与加密密钥分离，使用不同的派生后缀。
 *
 * @see io.github.afgprojects.framework.data.core.entity.FieldEncryptor
 * @see io.github.afgprojects.framework.data.core.entity.EncryptedFieldMetadata
 */
public interface BlindIndexProvider {

    /**
     * 计算盲索引值
     *
     * @param plaintext 明文值，不为 null
     * @param fieldName 字段名（Java 属性名），用于 HMAC 密钥派生
     * @param keyRef    密钥引用名称，来自 @EncryptedField 注解，可能为空字符串
     * @return 盲索引值的十六进制字符串
     */
    String computeBlindIndex(String plaintext, String fieldName, @Nullable String keyRef);
}
