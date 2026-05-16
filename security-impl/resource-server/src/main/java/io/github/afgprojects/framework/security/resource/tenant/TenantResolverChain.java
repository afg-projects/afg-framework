package io.github.afgprojects.framework.security.resource.tenant;

import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import io.github.afgprojects.framework.security.core.tenant.TenantException;
import io.github.afgprojects.framework.security.core.tenant.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

/**
 * 租户解析链。
 *
 * <p>按照优先级顺序遍历所有租户解析器，返回第一个成功解析的结果。
 *
 * <p>使用示例：
 * <pre>{@code
 * TenantResolverChain chain = new TenantResolverChain();
 * chain.addResolver(new TokenTenantResolver());
 * chain.addResolver(new HeaderTenantResolver());
 *
 * TenantContext context = chain.resolve(request);
 * }</pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class TenantResolverChain implements TenantResolver {

    private final List<TenantResolver> resolvers = new ArrayList<>();
    private boolean failIfUnresolved = true;

    /**
     * 创建空的租户解析链。
     */
    public TenantResolverChain() {
    }

    /**
     * 使用指定解析器创建租户解析链。
     *
     * @param resolvers 租户解析器列表
     */
    public TenantResolverChain(@NonNull List<TenantResolver> resolvers) {
        Objects.requireNonNull(resolvers, "resolvers must not be null");
        this.resolvers.addAll(resolvers);
        sortResolvers();
    }

    /**
     * 添加租户解析器。
     *
     * @param resolver 租户解析器
     * @return 当前解析链
     */
    @NonNull
    public TenantResolverChain addResolver(@NonNull TenantResolver resolver) {
        Objects.requireNonNull(resolver, "resolver must not be null");
        this.resolvers.add(resolver);
        sortResolvers();
        return this;
    }

    /**
     * 添加多个租户解析器。
     *
     * @param resolvers 租户解析器列表
     * @return 当前解析链
     */
    @NonNull
    public TenantResolverChain addResolvers(@NonNull List<TenantResolver> resolvers) {
        Objects.requireNonNull(resolvers, "resolvers must not be null");
        this.resolvers.addAll(resolvers);
        sortResolvers();
        return this;
    }

    /**
     * 按优先级排序解析器。
     */
    private void sortResolvers() {
        this.resolvers.sort(Comparator.comparingInt(TenantResolver::getOrder));
    }

    @Override
    @Nullable
    public TenantContext resolve(@NonNull HttpServletRequest request) {
        log.debug("Resolving tenant from request, {} resolvers available", resolvers.size());

        for (TenantResolver resolver : resolvers) {
            try {
                TenantContext context = resolver.resolve(request);
                if (context != null) {
                    log.debug("Tenant resolved by {}: {}", resolver.getClass().getSimpleName(), context.getTenantId());
                    return context;
                }
            } catch (Exception e) {
                log.warn("Tenant resolver {} failed: {}",
                        resolver.getClass().getSimpleName(), e.getMessage());
                // 继续尝试下一个解析器
            }
        }

        if (failIfUnresolved) {
            throw TenantException.unresolved();
        }

        log.debug("No tenant could be resolved from request");
        return null;
    }

    @Override
    @Nullable
    public TenantContext resolveFromToken(@NonNull String token) {
        for (TenantResolver resolver : resolvers) {
            try {
                TenantContext context = resolver.resolveFromToken(token);
                if (context != null) {
                    log.debug("Tenant resolved from token by {}: {}",
                            resolver.getClass().getSimpleName(), context.getTenantId());
                    return context;
                }
            } catch (Exception e) {
                log.warn("Token tenant resolver {} failed: {}",
                        resolver.getClass().getSimpleName(), e.getMessage());
            }
        }

        if (failIfUnresolved) {
            throw TenantException.unresolved();
        }

        return null;
    }

    @Override
    public int getOrder() {
        return 0; // 解析链本身没有优先级
    }

    /**
     * 获取所有解析器。
     *
     * @return 解析器列表
     */
    @NonNull
    public List<TenantResolver> getResolvers() {
        return new ArrayList<>(resolvers);
    }

    /**
     * 设置是否在无法解析时抛出异常。
     *
     * @param failIfUnresolved true 表示抛出异常，false 表示返回 null
     */
    public void setFailIfUnresolved(boolean failIfUnresolved) {
        this.failIfUnresolved = failIfUnresolved;
    }

    /**
     * 判断是否在无法解析时抛出异常。
     *
     * @return 如果抛出异常则返回 true
     */
    public boolean isFailIfUnresolved() {
        return failIfUnresolved;
    }

    /**
     * 判断解析链是否为空。
     *
     * @return 如果没有任何解析器则返回 true
     */
    public boolean isEmpty() {
        return resolvers.isEmpty();
    }

    /**
     * 清空所有解析器。
     */
    public void clear() {
        resolvers.clear();
    }
}