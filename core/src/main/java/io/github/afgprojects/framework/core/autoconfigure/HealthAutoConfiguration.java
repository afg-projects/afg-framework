package io.github.afgprojects.framework.core.autoconfigure;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;

import com.zaxxer.hikari.HikariDataSource;

import io.github.afgprojects.framework.core.module.ModuleRegistry;
import io.github.afgprojects.framework.core.web.health.DataSourceHealthIndicator;
import io.github.afgprojects.framework.core.web.health.DataSourceHealthProperties;
import io.github.afgprojects.framework.core.web.health.HealthCheckProperties;
import io.github.afgprojects.framework.core.web.health.LivenessHealthIndicator;
import io.github.afgprojects.framework.core.web.health.ModuleHealthIndicator;
import io.github.afgprojects.framework.core.web.health.ReadinessHealthIndicator;

/**
 * 健康检查自动配置类
 * 自动注册各类健康指示器
 *
 * <p>配置属性前缀：
 * <ul>
 *   <li>afg.health - 通用健康检查配置</li>
 *   <li>afg.health.datasource - 数据源健康检查配置</li>
 * </ul>
 *
 * <p>提供的健康指示器：
 * <ul>
 *   <li>{@link ModuleHealthIndicator} - 模块健康检查</li>
 *   <li>{@link DataSourceHealthIndicator} - 数据源健康检查</li>
 *   <li>{@link LivenessHealthIndicator} - 存活探针</li>
 *   <li>{@link ReadinessHealthIndicator} - 就绪探针</li>
 * </ul>
 *
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(HealthIndicator.class)
@EnableConfigurationProperties({HealthCheckProperties.class, DataSourceHealthProperties.class})
public class HealthAutoConfiguration {

    /**
     * 模块健康指示器
     */
    @Bean
    @ConditionalOnBean(ModuleRegistry.class)
    @ConditionalOnMissingBean
    public ModuleHealthIndicator moduleHealthIndicator(ModuleRegistry moduleRegistry) {
        return new ModuleHealthIndicator(moduleRegistry);
    }

    /**
     * 数据源健康指示器
     * 当存在 DataSource 时自动配置
     */
    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "afg.health.datasource", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DataSourceHealthIndicator dataSourceHealthIndicator(DataSource dataSource, DataSourceHealthProperties properties) {
        return new DataSourceHealthIndicator(dataSource, properties);
    }

    /**
     * 存活探针健康指示器
     * 用于 Kubernetes Liveness Probe
     * 注意：Bean 名称使用 afgLiveness 避免与 Spring Boot 4.0 内置 liveness 组冲突
     */
    @Bean("afgLiveness")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "afg.health", name = "livenessEnabled", havingValue = "true", matchIfMissing = true)
    public LivenessHealthIndicator livenessHealthIndicator(HealthCheckProperties properties) {
        return new LivenessHealthIndicator(properties);
    }

    /**
     * 就绪探针健康指示器
     * 用于 Kubernetes Readiness Probe
     * 注意：Bean 名称使用 afgReadiness 避免与 Spring Boot 4.0 内置 readiness 组冲突
     */
    @Bean("afgReadiness")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "afg.health", name = "readinessEnabled", havingValue = "true", matchIfMissing = true)
    public ReadinessHealthIndicator readinessHealthIndicator(
            HealthCheckProperties properties,
            @Nullable DataSource dataSource,
            @Nullable RedissonClient redissonClient,
            @Nullable ModuleRegistry moduleRegistry) {
        return new ReadinessHealthIndicator(properties, dataSource, redissonClient, moduleRegistry);
    }
}
