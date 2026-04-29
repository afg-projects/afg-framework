package io.github.afgprojects.framework.data.core.context;

import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import org.jspecify.annotations.Nullable;

/**
 * 实体上下文
 */
public interface EntityContext {

    /**
     * 获取实体对象
     */
    Object getEntity();

    /**
     * 获取实体元数据
     */
    EntityMetadata<?> getMetadata();

    /**
     * 获取属性
     */
    @Nullable Object getAttribute(String key);

    /**
     * 设置属性
     */
    void setAttribute(String key, Object value);

    /**
     * 获取审计上下文
     */
    AuditContext getAuditContext();
}
