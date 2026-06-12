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

/**
 * 加密字段元数据
 * <p>
 * 记录标注了 {@code @EncryptedField} 注解的字段信息，
 * 包括字段名、加密算法和密钥引用名称。
 *
 * @param fieldName  字段名（Java 属性名）
 * @param algorithm  加密算法（默认 "AES"）
 * @param keyRef     密钥引用名称（默认 ""）
 */
public record EncryptedFieldMetadata(
    String fieldName,
    String algorithm,
    String keyRef
) {}
