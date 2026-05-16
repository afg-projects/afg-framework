package io.github.afgprojects.framework.core.module;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

import io.github.afgprojects.framework.core.event.ModuleEvent;

/**
 * 模块上下文
 * 管理模块生命周期和事件分发
 */
@Slf4j
public class ModuleContext {

    private final ModuleRegistry registry;
    private final ApplicationEventPublisher eventPublisher;

    public ModuleContext(ModuleRegistry registry, ApplicationEventPublisher eventPublisher) {
        this.registry = registry;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 获取模块注册表
     */
    public ModuleRegistry getRegistry() {
        return registry;
    }

    /**
     * 发布事件
     * 使用 Spring 事件机制，开发者应使用 @EventListener 监听事件
     *
     * @param event 模块事件
     */
    public void publishEvent(ModuleEvent event) {
        if (event == null) {
            return;
        }

        String eventType = event.eventType();

        // 发布到 Spring 事件机制
        if (eventPublisher != null) {
            eventPublisher.publishEvent(event);
        }

        log.debug("Module event published: type={}, module={}", eventType, event.moduleId());
    }
}
