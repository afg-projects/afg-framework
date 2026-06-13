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
package io.github.afgprojects.framework.data.core.event;

import lombok.Builder;
import lombok.Data;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 实体变更事件
 * <p>
 * 当 DataManager 执行保存、更新、删除、恢复操作时自动发布此事件。
 * 携带变更类型、实体数据、变更前数据（仅更新时有值）等信息。
 *
 * <h3>事件类型</h3>
 * <ul>
 *   <li>{@link ChangeType#CREATED} — 实体新增</li>
 *   <li>{@link ChangeType#UPDATED} — 实体更新</li>
 *   <li>{@link ChangeType#DELETED} — 实体删除（含软删除）</li>
 *   <li>{@link ChangeType#RESTORED} — 软删除恢复</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * &#064;EventListener
 * public void onEntityChanged(EntityChangedEvent&lt;User&gt; event) {
 *     if (event.getChangeType() == ChangeType.CREATED) {
 *         log.info("New user created: {}", event.getEntity().getUsername());
 *     }
 * }
 * </pre>
 *
 * @param <T> 实体类型
 * @see EntityChangedEventPublisher
 * @see ChangeType
 */
@Data
@Builder
public class EntityChangedEvent<T> {

    /**
     * 实体类型
     */
    private @NonNull Class<T> entityType;

    /**
     * 变更后的实体数据
     */
    private @NonNull T entity;

    /**
     * 变更前的实体数据（仅 UPDATE 时有值，其他类型为 null）
     */
    private @Nullable T oldEntity;

    /**
     * 变更类型
     */
    private @NonNull ChangeType changeType;

    /**
     * 事件发生时间
     */
    private @NonNull Instant timestamp;

    /**
     * 实体变更类型枚举
     */
    public enum ChangeType {
        /**
         * 实体新增
         */
        CREATED,

        /**
         * 实体更新
         */
        UPDATED,

        /**
         * 实体删除（含软删除）
         */
        DELETED,

        /**
         * 软删除恢复
         */
        RESTORED
    }
}
