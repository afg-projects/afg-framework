package io.github.afgprojects.framework.security.core.tenant;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

/**
 * 租户解析器链。
 *
 * <p>按优先级顺序调用多个 {@link TenantResolver}，直到找到租户上下文。
 * 支持可选的租户验证和失败处理策略。
 *
 * <p>解析流程：
 * <ol>
 *   <li>按 Order 值升序排列解析器</li>
 *   <li>依次调用每个解析器的 {@link TenantResolver#resolve(HttpServletRequest)}</li>
 *   <li>找到第一个非 null 的租户上下文后停止</li>
 *   <li>如果配置了验证，调用 {@link TenantValidator#validate(String)}</li>
 *   <li>如果无法解析且 failIfUnresolved=true，抛出 {@link TenantException#unresolved()}</li>
 * </ol>
 *
 * <p>使用示例：
 * <pre>{@code
 * TenantResolverChain chain = new TenantResolverChain();
 * chain.addResolver(new HeaderTenantResolver());
 * chain.addResolver(new TokenTenantResolver(tokenService));
 *
 * TenantContext context = chain.resolve(request);
 * }</pre>
 *
 * @since 1.0.0
 */
@Slf4j
public class TenantResolverChain implements TenantResolver {

    private final List<TenantResolver> resolvers = new ArrayList<>();
    private final TenantValidator validator;
    private boolean failIfUnresolved = true;

    /**
     * 创建空的租户解析链（无验证器）。
     */
    public TenantResolverChain() {
        this.validator = null;
    }

    /**
     * 使用指定解析器创建租户解析链（无验证器）。
     *
     * @param resolvers 租户解析器列表
     */
    public TenantResolverChain(@NonNull List<TenantResolver> resolvers) {
        Objects.requireNonNull(resolvers, "resolvers must not be null");
        this.resolvers.addAll(resolvers);
        sortResolvers();
        this.validator = null;
    }

    /**
     * 构造租户解析器链（带验证器）。
     *
     * @param resolvers 解析器列表（可为空列表）
     * @param validator 租户验证器（可为 null 表示不验证）
     * @param failIfUnresolved 无法解析时是否抛出异常
     */
    public TenantResolverChain(@NonNull List<TenantResolver> resolvers,
                               @Nullable TenantValidator validator,
                               boolean failIfUnresolved) {
        Objects.requireNonNull(resolvers, "resolvers must not be null");
        this.resolvers.addAll(resolvers);
        sortResolvers();
        this.validator = validator;
        this.failIfUnresolved = failIfUnresolved;

        log.debug("创建租户解析器链: resolvers={}, failIfUnresolved={}",
                this.resolvers.size(), failIfUnresolved);
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
        log.debug("开始解析租户: request={}", request.getRequestURI());

        // 按优先级顺序调用解析器
        for (TenantResolver resolver : resolvers) {
            log.debug("调用解析器: resolver={}, order={}",
                    resolver.getClass().getSimpleName(), resolver.getOrder());

            try {
                TenantContext context = resolver.resolve(request);

                if (context != null) {
                    log.info("租户解析成功: tenantId={}, resolver={}",
                            context.getTenantId(), resolver.getClass().getSimpleName());

                    // 验证租户（如果配置了验证器）
                    if (validator != null) {
                        try {
                            validator.validate(context.getTenantId());
                            log.debug("租户验证通过: tenantId={}", context.getTenantId());
                        } catch (TenantException e) {
                            log.warn("租户验证失败: tenantId={}, error={}",
                                    context.getTenantId(), e.getMessage());
                            throw e;
                        }
                    }

                    return context;
                }
            } catch (TenantException e) {
                // TenantException 直接抛出（如验证失败）
                throw e;
            } catch (Exception e) {
                log.warn("租户解析器 {} 失败: {}",
                        resolver.getClass().getSimpleName(), e.getMessage());
                // 继续尝试下一个解析器
            }
        }

        // 所有解析器都无法解析
        log.warn("无法解析租户: request={}", request.getRequestURI());

        if (failIfUnresolved) {
            throw TenantException.unresolved();
        }

        return null;
    }

    @Override
    @Nullable
    public TenantContext resolveFromToken(@NonNull String token) {
        for (TenantResolver resolver : resolvers) {
            try {
                TenantContext context = resolver.resolveFromToken(token);
                if (context != null) {
                    log.debug("从 Token 解析租户成功: resolver={}, tenantId={}",
                            resolver.getClass().getSimpleName(), context.getTenantId());

                    // 验证租户（如果配置了验证器）
                    if (validator != null) {
                        validator.validate(context.getTenantId());
                    }

                    return context;
                }
            } catch (TenantException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Token 租户解析器 {} 失败: {}",
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
        // TenantResolverChain 本身不参与排序，总是返回最低优先级
        return Integer.MAX_VALUE;
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
