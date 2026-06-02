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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注软删除字段的注解。
 *
 * <p>用于自定义软删除字段的配置，支持不同类型的软删除策略：
 * <ul>
 *   <li>布尔型：{@code deleted = true/false}，对应 {@link SoftDeletable}</li>
 *   <li>时间戳型：{@code deletedAt = <timestamp>/null}，对应 {@link TimestampSoftDeletable}</li>
 * </ul>
 *
 * <p>{@code deletedValue} 和 {@code notDeletedValue} 使用 String 类型以适应不同策略：
 * <ul>
 *   <li>布尔型策略："true" / "false"</li>
 *   <li>时间戳型策略：实际时间戳值 / "null"（表示 NULL）</li>
 * </ul>
 *
 * @see SoftDeletable
 * @see TimestampSoftDeletable
 * @see SoftDeleteStrategy
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SoftDeleteField {

    /**
     * 软删除策略
     *
     * @return 删除策略
     */
    SoftDeleteStrategy strategy() default SoftDeleteStrategy.BOOLEAN;

    /**
     * 表示"已删除"的值。
     *
     * <p>对于布尔型策略，默认为 "true"；
     * 对于时间戳型策略，运行时使用当前时间，此值通常不需要设置。
     *
     * @return 已删除的值
     */
    String deletedValue() default "true";

    /**
     * 表示"未删除"的值。
     *
     * <p>对于布尔型策略，默认为 "false"；
     * 对于时间戳型策略，默认为 "null"（表示数据库 NULL）。
     *
     * @return 未删除的值
     */
    String notDeletedValue() default "false";
}
