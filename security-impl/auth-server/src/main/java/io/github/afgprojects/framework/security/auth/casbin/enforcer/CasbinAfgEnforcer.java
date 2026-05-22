package io.github.afgprojects.framework.security.auth.casbin.enforcer;

import io.github.afgprojects.framework.core.web.security.AfgEnforcer;
import io.github.afgprojects.framework.security.auth.autoconfigure.AuthSecurityProperties;
import io.github.afgprojects.framework.security.auth.casbin.model.AfgPolicyService;
import io.github.afgprojects.framework.security.auth.casbin.model.CasbinRule;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.jcasbin.persist.file_adapter.FileAdapter;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Casbin 权限执行器
 *
 * <p>实现 {@link AfgEnforcer} 接口，使用 JCasbin 进行权限判断。
 *
 * <p>支持 RBAC with domains 模型：
 * <ul>
 *   <li>sub - 主体（用户/角色）</li>
 *   <li>dom - 域（租户）</li>
 *   <li>obj - 对象（资源）</li>
 *   <li>act - 动作（操作）</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class CasbinAfgEnforcer implements AfgEnforcer {

    private final AuthSecurityProperties.CasbinConfig casbinConfig;
    private final AfgPolicyService policyService;
    private final Enforcer enforcer;

    /**
     * 构造函数
     *
     * @param casbinConfig Casbin 配置属性
     * @param policyService 策略服务
     */
    public CasbinAfgEnforcer(AuthSecurityProperties.CasbinConfig casbinConfig, AfgPolicyService policyService) {
        this.casbinConfig = casbinConfig;
        this.policyService = policyService;
        this.enforcer = createEnforcer();
    }

    /**
     * 创建 Casbin Enforcer
     *
     * @return Enforcer 实例
     */
    private Enforcer createEnforcer() {
        // 创建模型
        Model model = new Model();
        String modelText = casbinConfig.getModelText() != null
                ? casbinConfig.getModelText()
                : casbinConfig.getDefaultModelText();

        // 从模型文本加载
        model.loadModelFromText(modelText);

        // 创建适配器
        Adapter adapter = createAdapter();

        // 创建 Enforcer
        Enforcer enf = new Enforcer(model, adapter);
        enf.enableAutoSave(casbinConfig.isAutoSave());
        enf.enableAutoBuildRoleLinks(casbinConfig.isAutoBuildRoleLinks());

        // 加载策略
        loadPoliciesFromService(enf);

        return enf;
    }

    /**
     * 创建适配器
     *
     * @return 适配器实例
     */
    private Adapter createAdapter() {
        // 使用空的文件适配器，策略从 AfgPolicyService 加载
        String emptyPolicy = "";
        return new FileAdapter(new ByteArrayInputStream(emptyPolicy.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * 从策略服务加载策略
     *
     * @param enf Enforcer 实例
     */
    private void loadPoliciesFromService(Enforcer enf) {
        List<CasbinRule> rules = policyService.loadPolicies();
        for (CasbinRule rule : rules) {
            String[] policy = rule.toPolicy();
            if (policy.length > 0) {
                if ("p".equals(rule.getPtype())) {
                    enf.addPolicy(policy);
                } else if ("g".equals(rule.getPtype())) {
                    enf.addGroupingPolicy(policy);
                }
            }
        }
    }

    @Override
    public boolean enforce(String subject, String resource, String action) {
        if (subject == null) {
            return false;
        }
        // 使用默认域（空字符串）的两参数版本
        // 实际应用中，域（租户）应该从安全上下文获取
        return enforcer.enforce(subject, "", resource, action);
    }

    /**
     * 执行权限检查（带域参数）
     *
     * @param subject  主体（用户）
     * @param domain   域（租户）
     * @param resource 资源
     * @param action   动作
     * @return 是否允许
     */
    public boolean enforce(String subject, String domain, String resource, String action) {
        if (subject == null) {
            return false;
        }
        return enforcer.enforce(subject, domain, resource, action);
    }

    /**
     * 添加策略
     *
     * @param subject  主体
     * @param domain   域
     * @param resource 资源
     * @param action   动作
     */
    public void addPolicy(String subject, String domain, String resource, String action) {
        String[] policy = {subject, domain, resource, action};
        enforcer.addPolicy(policy);
        policyService.savePolicy(CasbinRule.createPolicy(subject, domain, resource, action));
    }

    /**
     * 删除策略
     *
     * @param subject  主体
     * @param domain   域
     * @param resource 资源
     * @param action   动作
     */
    public void removePolicy(String subject, String domain, String resource, String action) {
        String[] policy = {subject, domain, resource, action};
        enforcer.removePolicy(policy);
        policyService.removePolicy(CasbinRule.createPolicy(subject, domain, resource, action));
    }

    /**
     * 为用户添加角色
     *
     * @param user   用户
     * @param domain 域
     * @param role   角色
     */
    public void addRoleForUser(String user, String domain, String role) {
        String[] groupingPolicy = {user, domain, role};
        enforcer.addGroupingPolicy(groupingPolicy);
        policyService.savePolicy(CasbinRule.createRole(user, domain, role));
    }

    /**
     * 删除用户的角色
     *
     * @param user   用户
     * @param domain 域
     * @param role   角色
     */
    public void deleteRoleForUser(String user, String domain, String role) {
        String[] groupingPolicy = {user, domain, role};
        enforcer.removeGroupingPolicy(groupingPolicy);
        policyService.removePolicy(CasbinRule.createRole(user, domain, role));
    }

    /**
     * 重新加载策略
     */
    public void reloadPolicies() {
        enforcer.clearPolicy();
        loadPoliciesFromService(enforcer);
    }

    /**
     * 清空所有策略
     */
    public void clearPolicies() {
        enforcer.clearPolicy();
        policyService.clearPolicies();
    }

    /**
     * 获取底层 Casbin Enforcer
     *
     * @return Casbin Enforcer
     */
    public Enforcer getEnforcer() {
        return enforcer;
    }
}
