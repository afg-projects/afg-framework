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
package io.github.afgprojects.framework.data.jdbc.event;

import io.github.afgprojects.framework.data.core.event.EntityChangedEvent;
import io.github.afgprojects.framework.data.core.event.EntityChangedEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Spring 实体变更事件发布器
 * <p>
 * 通过 Spring 的 {@link ApplicationEventPublisher} 发布实体变更事件，
 * 支持使用 {@code @EventListener} 监听实体变更。
 *
 * <h3>使用示例</h3>
 * <pre>
 * &#064;Component
 * public class UserChangeHandler {
 *
 *     &#064;EventListener
 *     public void onUserChanged(EntityChangedEvent&lt;User&gt; event) {
 *         switch (event.getChangeType()) {
 *             case CREATED -&gt; handleUserCreated(event);
 *             case UPDATED -&gt; handleUserUpdated(event);
 *             case DELETED -&gt; handleUserDeleted(event);
 *             case RESTORED -&gt; handleUserRestored(event);
 *         }
 *     }
 * }
 * </pre>
 *
 * @see EntityChangedEventPublisher
 * @see EntityChangedEvent
 */
@Slf4j
@RequiredArgsConstructor
public class SpringEntityChangedEventPublisher implements EntityChangedEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public <T> void publish(@NonNull EntityChangedEvent<T> event) {
        try {
            applicationEventPublisher.publishEvent(event);
        } catch (Exception e) {
            // 事件发布失败不应影响业务操作
            log.warn("Failed to publish EntityChangedEvent for {}: {}",
                    event.getEntityType().getSimpleName(), e.getMessage());
        }
    }
}
