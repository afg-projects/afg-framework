package io.github.afgprojects.framework.security.auth.properties.casbin;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * Casbin 配置。
 */
@Data
public class CasbinConfig {

    /**
     * 是否启用 Casbin 权限控制。
     */
    private boolean enabled = true;

    /**
     * 模型类型：rbac-domain, acl, rbac 等。
     */
    private String modelType = "rbac-domain";

    /**
     * 策略适配器类型：memory, jdbc, redis 等。
     */
    private String policyAdapterType = "memory";

    /**
     * 是否自动保存策略。
     */
    private boolean autoSave = true;

    /**
     * 是否自动构建角色链接。
     */
    private boolean autoBuildRoleLinks = true;

    /**
     * 自定义模型文本（可选，默认使用 rbac-domain 模型）。
     */
    @Nullable
    private String modelText;

    /**
     * Casbin 模型文件路径。
     */
    private String modelPath = "casbin/rbac_model.conf";

    /**
     * 获取默认的 RBAC with domains 模型文本。
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
