package io.github.afgprojects.framework.security.resource.permission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 权限校验切面。
 *
 * <p>拦截带有 @RequirePermission 或 @RequireRole 注解的方法进行校验。
 *
 * <h3>校验策略</h3>
 * <ul>
 *   <li>角色：直接从 JWT Token 获取，无需远程调用</li>
 *   <li>权限：优先从 JWT Token 获取，未命中时调用远程客户端（带缓存）</li>
 * </ul>
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class PermissionAspect {

    private final CachedPermissionChecker permissionChecker;

    @Around("@annotation(io.github.afgprojects.framework.security.resource.permission.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);

        String[] permissions = annotation.value();
        boolean hasPermission;

        if (annotation.logical() == Logical.AND) {
            hasPermission = checkAllPermissions(permissions);
        } else {
            hasPermission = checkAnyPermission(permissions);
        }

        if (!hasPermission) {
            log.warn("权限校验失败: permissions={}", Arrays.toString(permissions));
            throw new PermissionDeniedException(
                    String.format("缺少权限: %s", Arrays.toString(permissions)));
        }

        return joinPoint.proceed();
    }

    @Around("@annotation(io.github.afgprojects.framework.security.resource.permission.RequireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireRole annotation = method.getAnnotation(RequireRole.class);

        String[] roles = annotation.value();
        boolean hasRole;

        if (annotation.logical() == Logical.AND) {
            hasRole = checkAllRoles(roles);
        } else {
            hasRole = checkAnyRole(roles);
        }

        if (!hasRole) {
            log.warn("角色校验失败: roles={}", Arrays.toString(roles));
            throw new PermissionDeniedException(
                    String.format("缺少角色: %s", Arrays.toString(roles)));
        }

        return joinPoint.proceed();
    }

    private boolean checkAllPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (!permissionChecker.hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkAnyPermission(String[] permissions) {
        for (String permission : permissions) {
            if (permissionChecker.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkAllRoles(String[] roles) {
        for (String role : roles) {
            if (!permissionChecker.hasRole(role)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkAnyRole(String[] roles) {
        for (String role : roles) {
            if (permissionChecker.hasRole(role)) {
                return true;
            }
        }
        return false;
    }
}