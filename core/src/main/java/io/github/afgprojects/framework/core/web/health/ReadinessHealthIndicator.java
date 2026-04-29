package io.github.afgprojects.framework.core.web.health;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;

import io.github.afgprojects.framework.core.module.ModuleDefinition;
import io.github.afgprojects.framework.core.module.ModuleRegistry;
import io.github.afgprojects.framework.core.module.ModuleState;

/**
 * 就绪探针健康指示器
 * 用于 Kubernetes Readiness Probe，检查应用是否可对外提供服务
 *
 * <p>检查内容：
 * <ul>
 *   <li>数据库连接</li>
 *   <li>Redis 连接</li>
 *   <li>模块状态</li>
 * </ul>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class ReadinessHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(ReadinessHealthIndicator.class);

    private final HealthCheckProperties properties;
    private final @Nullable DataSource dataSource;
    private final @Nullable RedissonClient redissonClient;
    private final @Nullable ModuleRegistry moduleRegistry;

    /**
     * 构造函数
     *
     * @param properties     健康检查配置
     * @param dataSource     数据源（可为 null）
     * @param redissonClient Redis 客户端（可为 null）
     * @param moduleRegistry 模块注册表（可为 null）
     */
    public ReadinessHealthIndicator(
            @NonNull HealthCheckProperties properties,
            @Nullable DataSource dataSource,
            @Nullable RedissonClient redissonClient,
            @Nullable ModuleRegistry moduleRegistry) {
        this.properties = properties;
        this.dataSource = dataSource;
        this.redissonClient = redissonClient;
        this.moduleRegistry = moduleRegistry;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        boolean allHealthy = true;

        // 数据库连接检查
        if (properties.getReadiness().isDatabaseCheckEnabled()) {
            allHealthy &= checkDatabase(builder);
        }

        // Redis 连接检查
        if (properties.getReadiness().isRedisCheckEnabled()) {
            allHealthy &= checkRedis(builder);
        }

        // 模块状态检查
        if (properties.getReadiness().isModuleCheckEnabled()) {
            allHealthy &= checkModules(builder);
        }

        if (!allHealthy) {
            builder.status(Status.DOWN);
        }

        return builder.build();
    }

    /**
     * 检查数据库连接
     *
     * @return true 表示健康，false 表示不健康
     */
    private boolean checkDatabase(Health.Builder builder) {
        if (dataSource == null) {
            builder.withDetail("database", "NOT_CONFIGURED");
            return true;
        }

        long startTime = System.currentTimeMillis();
        long timeoutMs = properties.getReadiness().getDatabaseCheckTimeout().toMillis();

        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid((int) TimeUnit.MILLISECONDS.toSeconds(timeoutMs));
            long duration = System.currentTimeMillis() - startTime;

            if (valid) {
                builder.withDetail("database", "UP")
                        .withDetail("databaseResponseTime", duration + "ms");
                return true;
            } else {
                log.error("数据库连接验证失败");
                builder.withDetail("database", "DOWN")
                        .withDetail("databaseError", "Connection validation failed");
                return false;
            }
        } catch (SQLException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("数据库连接检查失败: {}", e.getMessage(), e);
            builder.withDetail("database", "DOWN")
                    .withDetail("databaseError", e.getMessage())
                    .withDetail("databaseResponseTime", duration + "ms");
            return false;
        }
    }

    /**
     * 检查 Redis 连接
     *
     * @return true 表示健康，false 表示不健康
     */
    private boolean checkRedis(Health.Builder builder) {
        if (redissonClient == null) {
            builder.withDetail("redis", "NOT_CONFIGURED");
            return true;
        }

        long startTime = System.currentTimeMillis();
        long timeoutMs = properties.getReadiness().getRedisCheckTimeout().toMillis();

        try {
            // 使用 ping 命令检查连接
            long remainingTime = timeoutMs;
            boolean connected = redissonClient.getNodesGroup().pingAll(remainingTime, TimeUnit.MILLISECONDS);
            long duration = System.currentTimeMillis() - startTime;

            if (connected) {
                builder.withDetail("redis", "UP")
                        .withDetail("redisResponseTime", duration + "ms");
                return true;
            } else {
                log.error("Redis ping 失败");
                builder.withDetail("redis", "DOWN")
                        .withDetail("redisError", "Ping failed");
                return false;
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Redis 连接检查失败: {}", e.getMessage(), e);
            builder.withDetail("redis", "DOWN")
                    .withDetail("redisError", e.getMessage())
                    .withDetail("redisResponseTime", duration + "ms");
            return false;
        }
    }

    /**
     * 检查模块状态
     *
     * @return true 表示健康，false 表示不健康
     */
    private boolean checkModules(Health.Builder builder) {
        if (moduleRegistry == null) {
            builder.withDetail("modules", "NOT_CONFIGURED");
            return true;
        }

        List<ModuleDefinition> modules = moduleRegistry.getAllModules();
        Set<String> requiredModules = properties.getReadiness().getRequiredModules();

        // 过滤需要检查的模块
        Stream<ModuleDefinition> moduleStream = modules.stream();
        if (!requiredModules.isEmpty()) {
            moduleStream = moduleStream.filter(m -> requiredModules.contains(m.id()));
        }

        List<ModuleHealthDetails> moduleDetails = moduleStream
                .map(this::checkModuleHealth)
                .collect(Collectors.toList());

        // 检查是否有失败的模块
        boolean hasFailedModules = moduleDetails.stream()
                .anyMatch(d -> "DOWN".equals(d.status()));

        int totalModules = moduleDetails.size();
        int upModules = (int) moduleDetails.stream().filter(d -> "UP".equals(d.status())).count();
        int downModules = totalModules - upModules;

        builder.withDetail("modules", moduleDetails)
                .withDetail("totalModules", totalModules)
                .withDetail("upModules", upModules)
                .withDetail("downModules", downModules);

        if (hasFailedModules) {
            log.error("存在不健康的模块: {}/{} 个模块状态异常", downModules, totalModules);
            return false;
        }

        return true;
    }

    /**
     * 检查单个模块健康状态
     */
    private ModuleHealthDetails checkModuleHealth(ModuleDefinition definition) {
        // 获取模块状态（这里假设模块实例有状态）
        // 由于 ModuleDefinition 中没有直接的状态字段，我们使用模块注册表检查
        boolean operational = isModuleOperational(definition);
        String status = operational ? "UP" : "DOWN";
        return new ModuleHealthDetails(definition.id(), definition.name(), definition.dependencies(), status);
    }

    /**
     * 检查模块是否可操作
     */
    private boolean isModuleOperational(ModuleDefinition definition) {
        // 检查模块实例是否存在
        if (definition.moduleInstance() == null) {
            return false;
        }

        // 如果模块实现了 ModuleStatusProvider 接口，获取其状态
        if (definition.moduleInstance() instanceof ModuleStatusProvider statusProvider) {
            ModuleState state = statusProvider.getState();
            return state.isOperational();
        }

        // 默认认为模块是健康的
        return true;
    }
}
