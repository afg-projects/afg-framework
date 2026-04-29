package io.github.afgprojects.framework.core.web.security;

import java.util.Set;

/**
 * AFG 安全主体
 */
public interface AfgPrincipal {

    String getId();

    String getName();

    Set<String> getRoles();

    Set<String> getPermissions();
}
