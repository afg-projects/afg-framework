package io.github.afgprojects.framework.data.core.scope;

/**
 * 租户作用域
 * <p>
 * 实现 AutoCloseable 支持 try-with-resources 自动恢复
 */
public interface TenantScope extends AutoCloseable {

    /**
     * 获取租户ID
     */
    String getTenantId();

    /**
     * 关闭作用域（恢复之前的租户上下文）
     */
    @Override
    void close();
}
