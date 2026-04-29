package io.github.afgprojects.framework.core.event;

import java.time.Instant;

/**
 * 模块事件基类
 * 用于模块间的事件通信
 */
public record ModuleEvent(String moduleId, String eventType, long timestamp) {

    public ModuleEvent(String moduleId, String eventType) {
        this(moduleId, eventType, Instant.now().toEpochMilli());
    }
}
