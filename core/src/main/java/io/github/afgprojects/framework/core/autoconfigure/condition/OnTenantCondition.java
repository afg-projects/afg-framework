package io.github.afgprojects.framework.core.autoconfigure.condition;

import java.util.Arrays;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 多租户条件判断实现
 *
 * <p>检查当前租户 ID 是否在注解指定的租户列表中
 * <p>租户 ID 来源优先级:
 * <ol>
 *   <li>配置项 afg.tenant.id</li>
 *   <li>环境变量 AFG_TENANT_ID</li>
 * </ol>
 *
 * @see ConditionalOnTenant
 */
public class OnTenantCondition implements Condition {

    /**
     * 租户 ID 配置属性名
     */
    private static final String TENANT_ID_PROPERTY = "afg.tenant.id";

    /**
     * 租户 ID 环境变量名
     */
    private static final String TENANT_ID_ENV = "AFG_TENANT_ID";

    @Override
    public boolean matches(@NonNull ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
        // 获取注解属性
        var attributes = metadata.getAnnotationAttributes(ConditionalOnTenant.class.getName());
        if (attributes == null) {
            return false;
        }

        String[] tenantIds = (String[]) attributes.get("tenantId");
        boolean matchIfMissing = (Boolean) attributes.get("matchIfMissing");

        if (tenantIds == null || tenantIds.length == 0) {
            return false;
        }

        // 获取当前租户 ID
        String currentTenantId = getCurrentTenantId(context.getEnvironment());

        // 租户 ID 缺失时根据 matchIfMissing 决定
        if (currentTenantId == null || currentTenantId.isEmpty()) {
            return matchIfMissing;
        }

        // 检查是否匹配任一租户 ID
        return Arrays.asList(tenantIds).contains(currentTenantId);
    }

    /**
     * 获取当前租户 ID
     *
     * <p>按优先级从配置属性和环境变量中获取
     *
     * @param environment Spring 环境对象
     * @return 租户 ID，可能为 null
     */
    private @Nullable String getCurrentTenantId(Environment environment) {
        // 优先从配置属性获取
        String tenantId = environment.getProperty(TENANT_ID_PROPERTY);
        if (tenantId != null && !tenantId.isEmpty()) {
            return tenantId;
        }

        // 从环境变量获取
        tenantId = environment.getProperty(TENANT_ID_ENV);
        return (tenantId != null && !tenantId.isEmpty()) ? tenantId : null;
    }
}