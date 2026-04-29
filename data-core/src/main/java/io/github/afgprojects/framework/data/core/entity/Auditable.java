package io.github.afgprojects.framework.data.core.entity;

import io.github.afgprojects.framework.data.core.context.AuditContext;

/**
 * 可审计实体接口
 * <p>
 * 实现此接口的实体在保存/更新时，DataManager 会自动调用回调方法填充审计字段
 */
public interface Auditable {

    /**
     * 创建时回调
     *
     * @param context 审计上下文
     */
    void onCreate(AuditContext context);

    /**
     * 更新时回调
     *
     * @param context 审计上下文
     */
    void onUpdate(AuditContext context);
}
