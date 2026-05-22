package io.github.afgprojects.framework.security.auth.api;

import io.github.afgprojects.framework.core.web.security.signature.SignatureRequired;
import io.github.afgprojects.framework.security.core.permission.RbacService;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 权限查询 API（供资源服务器调用）。
 *
 * <p>内部服务接口，需要签名验证以确保服务间调用的安全性。
 * 调用方需要在请求头中提供：
 * <ul>
 *   <li>X-Signature - 签名值</li>
 *   <li>X-Timestamp - 时间戳</li>
 *   <li>X-Nonce - 随机数（防重放）</li>
 *   <li>X-Key-Id - 密钥标识（可选，使用默认密钥时可不传）</li>
 * </ul>
 */
@ResponseBody
@RequestMapping("/internal/permissions")
public class PermissionQueryController {

    private final RbacService rbacService;

    public PermissionQueryController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    /**
     * 检查用户是否具有指定权限。
     */
    @GetMapping("/check")
    @SignatureRequired
    public boolean hasPermission(
            @RequestParam String userId,
            @RequestParam String permission,
            @RequestParam @Nullable String tenantId) {
        return rbacService.hasPermission(userId, permission, tenantId);
    }

    /**
     * 检查用户是否具有指定角色。
     */
    @GetMapping("/check-role")
    @SignatureRequired
    public boolean hasRole(
            @RequestParam String userId,
            @RequestParam String role,
            @RequestParam @Nullable String tenantId) {
        return rbacService.hasRole(userId, role, tenantId);
    }

    /**
     * 获取用户的所有权限。
     */
    @GetMapping("/{userId}")
    @SignatureRequired
    public Set<String> getPermissions(
            @PathVariable String userId,
            @RequestParam @Nullable String tenantId) {
        return rbacService.getPermissions(userId, tenantId);
    }

    /**
     * 获取用户的所有角色。
     */
    @GetMapping("/{userId}/roles")
    @SignatureRequired
    public Set<String> getRoles(
            @PathVariable String userId,
            @RequestParam @Nullable String tenantId) {
        return rbacService.getRoles(userId, tenantId);
    }
}