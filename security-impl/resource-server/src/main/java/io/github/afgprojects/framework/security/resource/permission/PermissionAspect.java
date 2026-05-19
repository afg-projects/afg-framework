package io.github.afgprojects.framework.security.resource.permission;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 权限校验切面。
 *
 * <p>拦截带有 @RequirePermission 或 @RequireRole 注解的方法进行校验。
 *
 * <h3>校验策略</h3>
 * <ul>
 *   <li>如果动态权限拦截器已校验通过，跳过注解校验</li>
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
        // 检查动态权限是否已校验通过
        if (isDynamicPermissionChecked()) {
            log.debug("Dynamic permission already checked, skipping @RequirePermission");
            return joinPoint.proceed();
        }

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
        // 检查动态权限是否已校验通过
        if (isDynamicPermissionChecked()) {
            log.debug("Dynamic permission already checked, skipping @RequireRole");
            return joinPoint.proceed();
        }

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

    /**
     * 检查动态权限拦截器是否已校验通过。
     */
    private boolean isDynamicPermissionChecked() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return false;
        }
        HttpServletRequest request = attrs.getRequest();
        Object checked = request.getAttribute(DynamicApiPermissionInterceptor.PERMISSION_CHECKED_ATTR);
        return Boolean.TRUE.equals(checked);
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