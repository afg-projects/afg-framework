package io.github.afgprojects.framework.core.web.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AFG 安全配置收集器
 *
 * 声明式定义安全规则，由各模块通过 AfgSecurityConfigurer 贡献。
 * patterns 为 Ant 路径模式，如 /api/data/**
 */
public class AfgSecurityConfiguration {

    private final List<String> permitPatterns = new ArrayList<>();
    private final List<String> denyPatterns = new ArrayList<>();
    private final List<RoleRule> roleRules = new ArrayList<>();
    private final List<PermissionRule> permissionRules = new ArrayList<>();

    public void permit(String... patterns) {
        Collections.addAll(permitPatterns, patterns);
    }

    public void deny(String... patterns) {
        Collections.addAll(denyPatterns, patterns);
    }

    public void requireRole(String role, String... patterns) {
        roleRules.add(new RoleRule(role, List.of(patterns)));
    }

    public void requirePermission(String permission, String... patterns) {
        permissionRules.add(new PermissionRule(permission, List.of(patterns)));
    }

    public List<String> getPermitPatterns() {
        return Collections.unmodifiableList(permitPatterns);
    }

    public List<String> getDenyPatterns() {
        return Collections.unmodifiableList(denyPatterns);
    }

    public List<RoleRule> getRoleRules() {
        return Collections.unmodifiableList(roleRules);
    }

    public List<PermissionRule> getPermissionRules() {
        return Collections.unmodifiableList(permissionRules);
    }

    public record RoleRule(String role, List<String> patterns) {}

    public record PermissionRule(String permission, List<String> patterns) {}
}
