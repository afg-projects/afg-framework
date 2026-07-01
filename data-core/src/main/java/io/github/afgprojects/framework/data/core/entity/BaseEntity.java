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

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 实体基类，提供 ID 和时间戳字段。
 *
 * <p>使用 {@link Instant} 替代 {@link java.time.LocalDateTime}，
 * 以确保在分布式系统中时间戳的一致性（Instant 包含 UTC 时区信息）。
 *
 * <p>ID 类型为 {@code String}，由 {@code IdGenerator.nextIdAsString()} 生成，
 * 解决前端 JavaScript 精度丢失问题（Long 超过 2^53-1 时精度丢失）。
 */
@Setter
@Getter
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
public abstract class BaseEntity {

    /**
     * 实体 ID（字符串类型，避免前端精度丢失）
     */
    protected String id;

    /**
     * 创建时间（UTC）
     */
    protected Instant createdAt;

    /**
     * 更新时间（UTC）
     */
    protected Instant updatedAt;
}
