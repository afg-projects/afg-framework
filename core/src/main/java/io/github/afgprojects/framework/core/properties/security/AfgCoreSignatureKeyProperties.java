package io.github.afgprojects.framework.core.properties.security;

import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.util.PathMatcher;

import lombok.Data;

/**
 * 签名密钥配置。
 */
@Data
public class AfgCoreSignatureKeyProperties {

    private String secret;
    private boolean enabled = true;
    private @Nullable String description;
    private Set<String> allowedPaths = new HashSet<>();
    private Set<String> allowedScopes = new HashSet<>();

    /**
     * 检查路径是否被允许
     *
     * @param path       请求路径
     * @param pathMatcher 路径匹配器
     * @return 是否允许
     */
    public boolean isPathAllowed(String path, PathMatcher pathMatcher) {
        if (allowedPaths.isEmpty()) {
            return true; // 未配置时默认允许所有路径
        }
        for (String allowedPath : allowedPaths) {
            if (pathMatcher.match(allowedPath, path)) {
                return true;
            }
        }
        return false;
    }
}
