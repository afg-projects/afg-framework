package io.github.afgprojects.framework.security.core.invocation;

import io.github.afgprojects.framework.core.invocation.*;
import io.github.afgprojects.framework.security.core.permission.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Slf4j
public class SecurityInvocationInterceptor implements InvocationInterceptor {

    private final PermissionService permissionService;

    public SecurityInvocationInterceptor(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public SecurityInvocationInterceptor() {
        this.permissionService = null;
    }

    @Override
    public int order() { return 100; }

    @Override
    public boolean before(InvocationContext context) {
        OperationMetadata op = context.operationMetadata();

        if (!op.requiredRoles().isEmpty()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new ServiceAccessDeniedException("Authentication required");
            }
            boolean hasRole = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority -> op.requiredRoles().contains(authority.replace("ROLE_", "")));
            if (!hasRole) {
                throw new ServiceAccessDeniedException("Missing required role for " + op.name());
            }
        }

        if (!op.permission().isEmpty() && permissionService != null) {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getName() != null) {
                    boolean permitted = permissionService.hasPermission(auth.getName(), op.permission());
                    if (!permitted) {
                        throw new ServiceAccessDeniedException(op.permission());
                    }
                }
            } catch (ServiceAccessDeniedException e) {
                throw e;
            } catch (Exception e) {
                ServiceAccessDeniedException denied = new ServiceAccessDeniedException(op.permission());
                denied.initCause(e);
                throw denied;
            }
        }

        return true;
    }

    @Override
    public Object after(InvocationContext context, Object result) { return result; }

    @Override
    public void onError(InvocationContext context, Exception exception) {}
}
