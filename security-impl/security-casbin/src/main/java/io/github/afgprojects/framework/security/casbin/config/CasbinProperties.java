package io.github.afgprojects.framework.security.casbin.config;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Casbin 配置属性
 *
 * <p>配置示例：
 * <pre>
 * afg:
 *   security:
 *     casbin:
 *       enabled: true
 *       model-type: rbac-domain
 *       policy-adapter-type: jdbc
 *       auto-save: true
 *       auto-build-role-links: true
 *       model-text: |
 *         [request_definition]
 *         r = sub, dom, obj, act
 * </pre>
 *
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.security.casbin")
public class CasbinProperties {

    /**
     * 是否启用 Casbin 权限控制
     */
    private boolean enabled = true;

    /**
     * 模型类型：rbac-domain, acl, rbac 等
     */
    private String modelType = "rbac-domain";

    /**
     * 策略适配器类型：memory, jdbc, redis 等
     */
    private String policyAdapterType = "memory";

    /**
     * 是否自动保存策略
     */
    private boolean autoSave = true;

    /**
     * 是否自动构建角色链接
     */
    private boolean autoBuildRoleLinks = true;

    /**
     * 自定义模型文本（可选，默认使用 rbac-domain 模型）
     */
    @Nullable
    private String modelText;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public String getPolicyAdapterType() {
        return policyAdapterType;
    }

    public void setPolicyAdapterType(String policyAdapterType) {
        this.policyAdapterType = policyAdapterType;
    }

    public boolean isAutoSave() {
        return autoSave;
    }

    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }

    public boolean isAutoBuildRoleLinks() {
        return autoBuildRoleLinks;
    }

    public void setAutoBuildRoleLinks(boolean autoBuildRoleLinks) {
        this.autoBuildRoleLinks = autoBuildRoleLinks;
    }

    @Nullable
    public String getModelText() {
        return modelText;
    }

    public void setModelText(@Nullable String modelText) {
        this.modelText = modelText;
    }

    /**
     * 获取默认的 RBAC with domains 模型文本
     *
     * @return 模型文本
     */
    public String getDefaultModelText() {
        return """
                [request_definition]
                r = sub, dom, obj, act

                [policy_definition]
                p = sub, dom, obj, act

                [role_definition]
                g = _, _, _

                [policy_effect]
                e = some(where (p.eft == allow))

                [matchers]
                m = g(r.sub, r.dom, p.sub) && r.dom == p.dom && keyMatch2(r.obj, p.obj) && r.act == p.act
                """;
    }
}
