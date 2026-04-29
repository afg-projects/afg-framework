package io.github.afgprojects.framework.core.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * 基于多租户的条件装配注解
 *
 * <p>根据当前租户 ID 决定是否装配 Bean
 * <p>租户 ID 从配置项 afg.tenant.id 或环境变量 AFG_TENANT_ID 读取
 *
 * <h3>使用示例:</h3>
 * <pre>{@code
 * // 单一租户
 * @Bean
 * @ConditionalOnTenant(tenantId = "tenant-001")
 * public TenantSpecificService tenant001Service() {
 *     return new TenantSpecificService();
 * }
 *
 * // 多个租户（任一匹配即生效）
 * @Bean
 * @ConditionalOnTenant(tenantId = {"tenant-001", "tenant-002"})
 * public MultiTenantService multiTenantService() {
 *     return new MultiTenantService();
 * }
 * }</pre>
 *
 * @see OnTenantCondition
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnTenantCondition.class)
public @interface ConditionalOnTenant {

    /**
     * 租户 ID（支持多个）
     * <p>
     * 任一租户 ID 匹配即生效
     *
     * @return 租户 ID 数组
     */
    String[] tenantId();

    /**
     * 当租户 ID 配置缺失时是否匹配
     * <p>
     * 默认为 false，即缺失时不匹配
     *
     * @return 缺失时是否匹配
     */
    boolean matchIfMissing() default false;
}
