package io.github.afgprojects.framework.security.auth.tenant.resolver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.security.auth.tenant.validator.TenantValidator;
import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import io.github.afgprojects.framework.security.core.tenant.TenantException;
import io.github.afgprojects.framework.security.core.tenant.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;
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
 * @since 1.0.0
 */
@Slf4j
public class TenantResolverChain implements TenantResolver {

    private final List<TenantResolver> resolvers;
    private final TenantValidator validator;
    private final boolean failIfUnresolved;

    /**
     * 构造租户解析器链。
     *
     * @param resolvers 解析器列表（可为空列表）
     * @param validator 租户验证器（可为 null 表示不验证）
     * @param failIfUnresolved 无法解析时是否抛出异常
     */
    public TenantResolverChain(@NonNull List<TenantResolver> resolvers,
                               @Nullable TenantValidator validator,
                               boolean failIfUnresolved) {
        // 按 Order 排序（升序，数值越小优先级越高）
        this.resolvers = new ArrayList<>(resolvers);
        this.resolvers.sort(Comparator.comparingInt(TenantResolver::getOrder));

        this.validator = validator;
        this.failIfUnresolved = failIfUnresolved;

        log.debug("创建租户解析器链: resolvers={}, failIfUnresolved={}",
                this.resolvers.size(), failIfUnresolved);
    }

    @Override
    @Nullable
    public TenantContext resolve(@NonNull HttpServletRequest request) {
        log.debug("开始解析租户: request={}", request.getRequestURI());

        // 按优先级顺序调用解析器
        for (TenantResolver resolver : resolvers) {
            log.debug("调用解析器: resolver={}, order={}",
                    resolver.getClass().getSimpleName(), resolver.getOrder());

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
        }

        // 所有解析器都无法解析
        log.warn("无法解析租户: request={}", request.getRequestURI());

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
}