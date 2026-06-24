package io.github.afgprojects.framework.core.api.ratelimit;

import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;

/**
 * 默认维度解析实现
 * <p>
 * 从请求上下文中解析限流维度值。
 * </p>
 */
public class DefaultDimensionResolver implements DimensionResolver {

    @Override
    public String resolve(RateLimitDimension dimension) {
        // API 维度不需要上下文
        if (dimension == RateLimitDimension.API) {
            return "global";
        }

        RequestContext context = AfgRequestContextHolder.getContext();

        if (context == null) {
            return "unknown";
        }

        return switch (dimension) {
            case IP -> {
                String clientIp = context.getClientIp();
                yield clientIp != null ? clientIp : "unknown";
            }
            case USER -> {
                String userId = context.getUserId();
                yield userId != null ? userId : "anonymous";
            }
            case TENANT -> {
                String tenantId = context.getTenantId();
                yield tenantId != null ? tenantId : "default";
            }
            case API -> "global";
        };
    }
}
