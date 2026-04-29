package io.github.afgprojects.framework.core.support;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Arrays;

import io.github.afgprojects.framework.core.module.AfgModule;
import io.github.afgprojects.framework.core.module.ModuleDefinition;
import io.github.afgprojects.framework.core.module.ModuleState;
import io.github.afgprojects.framework.core.web.health.ModuleStatusProvider;

/**
 * 测试数据工厂
 * 提供统一的测试数据创建方法
 */
public final class TestDataFactory {

    private TestDataFactory() {
        // 工具类，禁止实例化
    }

    /**
     * 创建模块定义
     *
     * @param id           模块ID
     * @param name         模块名称
     * @param dependencies 依赖模块ID列表
     * @return ModuleDefinition 实例
     */
    public static ModuleDefinition createModuleDefinition(String id, String name, String... dependencies) {
        AfgModule healthyModule = createHealthyModule(id, name);
        return ModuleDefinition.builder()
                .id(id)
                .name(name)
                .dependencies(Arrays.asList(dependencies))
                .moduleInstance(healthyModule)
                .build();
    }

    /**
     * 创建健康的 Mock 模块（实现 ModuleStatusProvider 接口）
     *
     * @param id   模块ID
     * @param name 模块名称
     * @return Mock 的 AfgModule 实例，返回 READY 状态
     */
    private static AfgModule createHealthyModule(String id, String name) {
        AfgModule module = mock(AfgModule.class, withSettings().extraInterfaces(ModuleStatusProvider.class));
        when(module.getModuleId()).thenReturn(id);
        when(module.getModuleName()).thenReturn(name);
        ModuleStatusProvider statusProvider = (ModuleStatusProvider) module;
        when(statusProvider.getState()).thenReturn(ModuleState.READY);
        return module;
    }

    /**
     * 创建 Mock 模块
     *
     * @param id 模块ID
     * @return Mock 的 AfgModule 实例
     */
    public static AfgModule createMockModule(String id) {
        AfgModule module = mock(AfgModule.class);
        when(module.getModuleId()).thenReturn(id);
        return module;
    }

    /**
     * 创建 Mock 模块（带名称）
     *
     * @param id   模块ID
     * @param name 模块名称
     * @return Mock 的 AfgModule 实例
     */
    public static AfgModule createMockModule(String id, String name) {
        AfgModule module = mock(AfgModule.class);
        when(module.getModuleId()).thenReturn(id);
        when(module.getModuleName()).thenReturn(name);
        return module;
    }
}
