package io.github.afgprojects.framework.security.casbin.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.afgprojects.framework.security.casbin.enforcer.CasbinAfgEnforcer;
import io.github.afgprojects.framework.security.casbin.model.AfgPolicyService;
import io.github.afgprojects.framework.security.casbin.model.InMemoryPolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CasbinAutoConfiguration 测试
 */
class CasbinAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CasbinAutoConfiguration.class));

    @Test
    void should_createCasbinAfgEnforcer_when_enabled() {
        contextRunner
                .withUserConfiguration(TestPolicyServiceConfig.class)
                .withPropertyValues("afg.security.casbin.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(CasbinAfgEnforcer.class);
                    assertThat(context).hasSingleBean(CasbinProperties.class);
                });
    }

    @Test
    void should_notCreateCasbinAfgEnforcer_when_disabled() {
        contextRunner
                .withUserConfiguration(TestPolicyServiceConfig.class)
                .withPropertyValues("afg.security.casbin.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CasbinAfgEnforcer.class);
                });
    }

    @Test
    void should_createDefaultPolicyService_when_notProvided() {
        contextRunner
                .withPropertyValues("afg.security.casbin.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(AfgPolicyService.class);
                    assertThat(context.getBean(AfgPolicyService.class))
                            .isInstanceOf(InMemoryPolicyService.class);
                });
    }

    @Test
    void should_useCustomPolicyService_when_provided() {
        contextRunner
                .withUserConfiguration(CustomPolicyServiceConfig.class)
                .withPropertyValues("afg.security.casbin.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(AfgPolicyService.class);
                    assertThat(context.getBean(AfgPolicyService.class))
                            .isInstanceOf(CustomPolicyService.class);
                });
    }

    @Test
    void should_bindPropertiesCorrectly() {
        contextRunner
                .withUserConfiguration(TestPolicyServiceConfig.class)
                .withPropertyValues(
                        "afg.security.casbin.enabled=true",
                        "afg.security.casbin.model-type=acl",
                        "afg.security.casbin.policy-adapter-type=jdbc",
                        "afg.security.casbin.auto-save=false")
                .run(context -> {
                    CasbinProperties properties = context.getBean(CasbinProperties.class);
                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.getModelType()).isEqualTo("acl");
                    assertThat(properties.getPolicyAdapterType()).isEqualTo("jdbc");
                    assertThat(properties.isAutoSave()).isFalse();
                });
    }

    @Configuration
    static class TestPolicyServiceConfig {
        @Bean
        AfgPolicyService afgPolicyService() {
            return new InMemoryPolicyService();
        }
    }

    @Configuration
    static class CustomPolicyServiceConfig {
        @Bean
        AfgPolicyService afgPolicyService() {
            return new CustomPolicyService();
        }
    }

    static class CustomPolicyService extends InMemoryPolicyService {
        // 自定义策略服务实现
    }
}
