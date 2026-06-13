package io.github.afgprojects.framework.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.api.enummanagement.EnumManagementEndpoint;
import io.github.afgprojects.framework.core.api.enummanagement.EnumRegistry;
import io.github.afgprojects.framework.core.api.enummanagement.LocalEnumRegistry;
import io.github.afgprojects.framework.core.api.enummanagement.NoOpEnumRegistry;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * 枚举管理自动配置。
 * <p>
 * 自动配置枚举元数据管理功能，包括：
 * <ul>
 *   <li>{@link LocalEnumRegistry} — 本地内存枚举注册表（默认实现）</li>
 *   <li>{@link EnumManagementEndpoint} — REST 端点（仅在 Web 环境下注册）</li>
 *   <li>{@link NoOpEnumRegistry} — NoOp 降级实现</li>
 * </ul>
 * 分布式注册表由集成模块提供，通过 {@code @ConditionalOnMissingBean} 自动升级。
 * </p>
 * <p>
 * 配置示例：
 * <pre>
 * afg:
 *   core:
 *     enum-management:
 *       enabled: true
 *       expose-endpoint: true
 *       endpoint-path: /afg/enums
 * </pre>
 * </p>
 *
 * @since 1.0.0
 */
@AutoConfiguration(after = AfgAutoConfiguration.class)
@ConditionalOnProperty(prefix = "afg.core.enum-management", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class EnumManagementAutoConfiguration {

    /**
     * 本地内存枚举注册表。
     * <p>
     * 使用 ConcurrentHashMap 存储枚举元数据。
     * 分布式注册表由集成模块提供，通过 {@code @ConditionalOnMissingBean} 自动升级。
     *
     * @return 本地枚举注册表实例
     */
    @Bean
    @ConditionalOnMissingBean(EnumRegistry.class)
    public EnumRegistry localEnumRegistry() {
        return new LocalEnumRegistry();
    }

    /**
     * 枚举管理 REST 端点。
     * <p>
     * 仅在 SERVLET Web 环境下且 exposeEndpoint=true 时注册。
     * 暴露枚举元数据给前端，供下拉框等 UI 组件使用。
     *
     * @param enumRegistry 枚举注册表
     * @return 枚举管理端点实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "afg.core.enum-management", name = "expose-endpoint", havingValue = "true", matchIfMissing = true)
    public EnumManagementEndpoint enumManagementEndpoint(EnumRegistry enumRegistry) {
        return new EnumManagementEndpoint(enumRegistry);
    }

    /**
     * NoOp 枚举注册表降级实现。
     * <p>
     * 当本地注册表也不满足条件时使用。
     * 所有注册和查询操作均为空操作。
     *
     * @return NoOp 枚举注册表实例
     */
    @Bean
    @ConditionalOnMissingBean(EnumRegistry.class)
    public EnumRegistry noOpEnumRegistry() {
        return new NoOpEnumRegistry();
    }
}
