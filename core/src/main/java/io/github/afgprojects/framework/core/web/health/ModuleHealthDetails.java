package io.github.afgprojects.framework.core.web.health;

import java.util.List;

/**
 * 模块健康详情
 *
 * @param id           模块ID
 * @param name         模块名称
 * @param dependencies 依赖模块ID列表
 * @param status       健康状态
 */
@SuppressWarnings("PMD.UnusedAssignment")
public record ModuleHealthDetails(String id, String name, List<String> dependencies, String status) {

    // NOPMD - record 紧凑构造器中重新赋值参数是正常行为
    public ModuleHealthDetails {
        dependencies = dependencies != null ? List.copyOf(dependencies) : List.of();
    }
}
