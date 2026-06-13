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

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

/**
 * NoOp 实体变更事件发布器（降级实现）
 * <p>
 * 当 Spring ApplicationEventPublisher 不可用时使用此降级实现，
 * 不发布任何事件，仅记录 debug 日志。
 *
 * @see EntityChangedEventPublisher
 */
@Slf4j
public class NoOpEntityChangedEventPublisher implements EntityChangedEventPublisher {

    @Override
    public <T> void publish(@NonNull EntityChangedEvent<T> event) {
        log.debug("EntityChangedEvent published (NoOp): type={}, entityType={}, changeType={}",
                event.getEntityType().getSimpleName(),
                event.getEntityType().getName(),
                event.getChangeType());
    }
}
