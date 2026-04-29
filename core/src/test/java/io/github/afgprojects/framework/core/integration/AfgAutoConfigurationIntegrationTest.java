package io.github.afgprojects.framework.core.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration;
import io.github.afgprojects.framework.core.config.AfgConfigRegistry;
import io.github.afgprojects.framework.core.config.ConfigRefresher;
import io.github.afgprojects.framework.core.module.ModuleContext;
import io.github.afgprojects.framework.core.module.ModuleRegistry;
import io.github.afgprojects.framework.core.support.BaseIntegrationTest;

/**
 * AfgAutoConfiguration 集成测试
 * 验证自动配置是否正确注入所有 Bean
 */
class AfgAutoConfigurationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("应该成功注入 ModuleRegistry Bean")
    void shouldInjectModuleRegistry() {
        // when
        ModuleRegistry registry = getBean(ModuleRegistry.class);

        // then
        assertThat(registry).isNotNull();
    }

    @Test
    @DisplayName("应该成功注入 AfgConfigRegistry Bean")
    void shouldInjectAfgConfigRegistry() {
        // when
        AfgConfigRegistry configRegistry = getBean(AfgConfigRegistry.class);

        // then
        assertThat(configRegistry).isNotNull();
    }

    @Test
    @DisplayName("应该成功注入 ConfigRefresher Bean")
    void shouldInjectConfigRefresher() {
        // when
        ConfigRefresher refresher = getBean(ConfigRefresher.class);

        // then
        assertThat(refresher).isNotNull();
    }

    @Test
    @DisplayName("应该成功注入 ModuleContext Bean")
    void shouldInjectModuleContext() {
        // when
        ModuleContext moduleContext = getBean(ModuleContext.class);

        // then
        assertThat(moduleContext).isNotNull();
    }

    @Test
    @DisplayName("应该成功注入 ObjectMapper Bean")
    void shouldInjectObjectMapper() {
        // when
        ObjectMapper objectMapper = getBean(ObjectMapper.class);

        // then
        assertThat(objectMapper).isNotNull();
    }

    @Test
    @DisplayName("应该成功注入 AfgAutoConfiguration Bean")
    void shouldInjectAfgAutoConfiguration() {
        // when
        AfgAutoConfiguration autoConfig = getBean(AfgAutoConfiguration.class);

        // then
        assertThat(autoConfig).isNotNull();
    }

    @Test
    @DisplayName("所有核心 Bean 应该在 ApplicationContext 中可用")
    void allCoreBeansShouldBeAvailable() {
        // when & then
        assertThat(applicationContext.containsBean("moduleRegistry")).isTrue();
        assertThat(applicationContext.containsBean("afgConfigRegistry")).isTrue();
        assertThat(applicationContext.containsBean("configRefresher")).isTrue();
        assertThat(applicationContext.containsBean("moduleContext")).isTrue();
        assertThat(applicationContext.containsBean("objectMapper")).isTrue();
    }
}
