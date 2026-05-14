package io.github.afgprojects.framework.core.autoconfigure.condition;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 条件装配测试配置类。
 * 提供各种条件注解的测试 Bean。
 *
 * @see ConditionalOnFeature
 * @see ConditionalOnTenant
 * @see ConditionalOnPropertyNotEmpty
 */
@Configuration
public class ConditionTestConfiguration {

    /**
     * 功能开关测试：cache 功能启用时装配。
     *
     * @return cache 服务字符串
     */
    @Bean
    @ConditionalOnFeature(feature = "cache", enabled = true)
    public String cacheService() {
        return "cacheService";
    }

    /**
     * 功能开关测试：search 功能启用时装配（配置为禁用）。
     *
     * @return search 服务字符串
     */
    @Bean
    @ConditionalOnFeature(feature = "search", enabled = true)
    public String searchService() {
        return "searchService";
    }

    /**
     * 多租户测试：tenant-001 匹配时装配。
     *
     * @return 租户特定服务字符串
     */
    @Bean
    @ConditionalOnTenant(tenantId = "tenant-001")
    public String tenantSpecificService() {
        return "tenantSpecificService";
    }

    /**
     * 多租户测试：tenant-999 不匹配（当前租户为 tenant-001）。
     *
     * @return 其他租户服务字符串
     */
    @Bean
    @ConditionalOnTenant(tenantId = "tenant-999")
    public String otherTenantService() {
        return "otherTenantService";
    }

    /**
     * 属性非空测试：数据库 URL 存在时装配。
     *
     * @return 数据库服务字符串
     */
    @Bean
    @ConditionalOnPropertyNotEmpty("afg.database.url")
    public String databaseService() {
        return "databaseService";
    }

    /**
     * 属性非空测试：属性不存在时不装配。
     *
     * @return 缺失属性服务字符串
     */
    @Bean
    @ConditionalOnPropertyNotEmpty("afg.missing.property")
    public String missingPropertyService() {
        return "missingPropertyService";
    }
}