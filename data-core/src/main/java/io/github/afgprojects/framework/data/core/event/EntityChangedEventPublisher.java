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

import org.jspecify.annotations.NonNull;

/**
 * 实体变更事件发布器接口
 * <p>
 * 定义实体变更事件的发布能力。JdbcDataManager 在执行保存、更新、删除、恢复操作后
 * 调用此接口发布事件。
 * <p>
 * 默认使用 {@link NoOpEntityChangedEventPublisher}（不发布任何事件），
 * Spring 环境下使用 {@code SpringEntityChangedEventPublisher}（通过 Spring
 * ApplicationEventPublisher 发布事件）。
 *
 * @see EntityChangedEvent
 * @see NoOpEntityChangedEventPublisher
 */
public interface EntityChangedEventPublisher {

    /**
     * 发布实体变更事件
     *
     * @param event 实体变更事件
     * @param <T>   实体类型
     */
    <T> void publish(@NonNull EntityChangedEvent<T> event);
}
