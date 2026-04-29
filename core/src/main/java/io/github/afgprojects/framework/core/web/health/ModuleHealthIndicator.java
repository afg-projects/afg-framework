package io.github.afgprojects.framework.core.web.health;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;

import io.github.afgprojects.framework.core.module.ModuleDefinition;
import io.github.afgprojects.framework.core.module.ModuleRegistry;
import io.github.afgprojects.framework.core.module.ModuleState;

/**
 * 模块健康检查指示器
 * 基于 ModuleRegistry 提供模块健康状态
 *
 * <p>支持健康检查分级，通过 {@link HealthCheckLevel} 指定检查深度：
 * <ul>
 *   <li>{@link HealthCheckLevel#LIVENESS} - 仅检查模块是否存在</li>
 *   <li>{@link HealthCheckLevel#READINESS} - 检查模块状态是否可操作</li>
 *   <li>{@link HealthCheckLevel#DEEP} - 包含详细依赖关系检查</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class ModuleHealthIndicator implements HealthIndicator {

    private final ModuleRegistry moduleRegistry;
    /**
     * -- GETTER --
     *  获取当前健康检查级别
     *
     * @return 健康检查级别
     */
    @Getter
    private HealthCheckLevel checkLevel = HealthCheckLevel.READINESS;

    /**
     * 构造函数，默认使用 READINESS 级别
     *
     * @param moduleRegistry 模块注册表
     */
    public ModuleHealthIndicator(@NonNull ModuleRegistry moduleRegistry) {
        this.moduleRegistry = moduleRegistry;
    }

    /**
     * 构造函数，指定检查级别
     *
     * @param moduleRegistry 模块注册表
     * @param checkLevel     健康检查级别
     */
    public ModuleHealthIndicator(@NonNull ModuleRegistry moduleRegistry, @NonNull HealthCheckLevel checkLevel) {
        this.moduleRegistry = moduleRegistry;
        this.checkLevel = checkLevel;
    }

    /**
     * 设置健康检查级别
     *
     * @param checkLevel 健康检查级别
     */
    public void setCheckLevel(@NonNull HealthCheckLevel checkLevel) {
        this.checkLevel = checkLevel;
    }

    @Override
    public Health health() {
        List<ModuleDefinition> modules = moduleRegistry.getAllModules();

        // 根据检查级别获取模块详情
        List<ModuleHealthDetails> details = modules.stream()
                .map(def -> buildModuleHealthDetails(def, checkLevel))
                .collect(Collectors.toList());

        // 构建健康状态
        boolean allHealthy = details.stream()
                .allMatch(d -> "UP".equals(d.status()));

        Health.Builder builder = allHealthy ? Health.up() : Health.down();
        builder.withDetail("moduleCount", modules.size())
                .withDetail("checkLevel", checkLevel.name())
                .withDetail("modules", details);

        // 深度检查时添加额外信息
        if (checkLevel == HealthCheckLevel.DEEP) {
            addDeepCheckDetails(builder, modules);
        }

        return builder.build();
    }

    /**
     * 构建模块健康详情
     */
    private ModuleHealthDetails buildModuleHealthDetails(ModuleDefinition definition, HealthCheckLevel level) {
        String status = determineModuleStatus(definition, level);
        return new ModuleHealthDetails(definition.id(), definition.name(), definition.dependencies(), status);
    }

    /**
     * 确定模块状态
     */
    private String determineModuleStatus(ModuleDefinition definition, HealthCheckLevel level) {
        // LIVENESS 级别：仅检查模块是否存在
        if (level == HealthCheckLevel.LIVENESS) {
            return "UP";
        }

        // READINESS 和 DEEP 级别：检查模块是否可操作
        boolean operational = isModuleOperational(definition);
        return operational ? "UP" : "DOWN";
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

    /**
     * 添加深度检查详情
     */
    private void addDeepCheckDetails(Health.Builder builder, List<ModuleDefinition> modules) {
        // 统计各状态模块数量
        long upCount = modules.stream()
                .filter(this::isModuleOperational)
                .count();
        long downCount = modules.size() - upCount;

        builder.withDetail("upModules", upCount)
                .withDetail("downModules", downCount);

        // 检查依赖关系
        long modulesWithDependencies = modules.stream()
                .filter(m -> !m.dependencies().isEmpty())
                .count();
        builder.withDetail("modulesWithDependencies", modulesWithDependencies);
    }
}
