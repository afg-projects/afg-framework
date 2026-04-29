package io.github.afgprojects.framework.core.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import io.github.afgprojects.framework.core.module.ModuleRegistry;
import io.github.afgprojects.framework.core.support.BaseIntegrationTest;
import io.github.afgprojects.framework.core.web.health.ModuleHealthIndicator;

/**
 * ModuleHealthIndicator 集成测试
 * 验证健康检查是否被 Actuator 识别
 */
class ModuleHealthIndicatorIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("应该成功注入 ModuleHealthIndicator Bean")
    void shouldInjectModuleHealthIndicator() {
        // when
        ModuleHealthIndicator indicator = getBean(ModuleHealthIndicator.class);

        // then
        assertThat(indicator).isNotNull();
    }

    @Test
    @DisplayName("健康检查应该返回 UP 状态")
    void healthCheckShouldReturnUp() {
        // given
        ModuleHealthIndicator indicator = getBean(ModuleHealthIndicator.class);

        // when
        Health health = indicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    @DisplayName("健康检查应该包含模块数量详情")
    void healthCheckShouldContainModuleCount() {
        // given
        ModuleHealthIndicator indicator = getBean(ModuleHealthIndicator.class);

        // when
        Health health = indicator.health();

        // then
        assertThat(health.getDetails()).containsKey("moduleCount");
    }

    @Test
    @DisplayName("健康检查应该包含模块列表详情")
    void healthCheckShouldContainModules() {
        // given
        ModuleHealthIndicator indicator = getBean(ModuleHealthIndicator.class);

        // when
        Health health = indicator.health();

        // then
        assertThat(health.getDetails()).containsKey("modules");
    }

    @Test
    @DisplayName("ModuleHealthIndicator 应该正确依赖 ModuleRegistry")
    void indicatorShouldDependOnModuleRegistry() {
        // given
        ModuleHealthIndicator indicator = getBean(ModuleHealthIndicator.class);
        ModuleRegistry registry = getBean(ModuleRegistry.class);

        // when
        Health health = indicator.health();
        int moduleCount = (int) health.getDetails().get("moduleCount");
        int registryCount = registry.getAllModules().size();

        // then
        assertThat(moduleCount).isEqualTo(registryCount);
    }
}
