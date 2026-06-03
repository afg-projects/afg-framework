package io.github.afgprojects.framework.security.auth.properties.oauth2;

import java.util.Set;

import org.jspecify.annotations.NonNull;

/**
 * OAuth2 客户端配置。
 */
public record OAuth2ClientConfig(
        @NonNull String clientId,
        String clientSecret,
        @NonNull String clientName,
        @NonNull Set<String> redirectUris,
        @NonNull Set<String> scopes,
        @NonNull Set<String> grantTypes,
        boolean requirePkce
) {}
