package io.github.afgprojects.framework.security.auth.casbin.config;

import io.github.afgprojects.framework.security.auth.autoconfigure.AuthSecurityProperties;
import io.github.afgprojects.framework.security.auth.autoconfigure.CasbinAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.afgprojects.framework.security.auth.casbin.enforcer.CasbinAfgEnforcer;
import io.github.afgprojects.framework.security.auth.casbin.model.AfgPolicyService;
import io.github.afgprojects.framework.security.auth.casbin.model.InMemoryPolicyService;
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
                .withPropertyValues("afg.security.auth-server.casbin.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(CasbinAfgEnforcer.class);
                    assertThat(context).hasSingleBean(AuthSecurityProperties.class);
                });
    }

    @Test
    void should_notCreateCasbinAfgEnforcer_when_disabled() {
        contextRunner
                .withUserConfiguration(TestPolicyServiceConfig.class)
                .withPropertyValues("afg.security.auth-server.casbin.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CasbinAfgEnforcer.class);
                });
    }

    @Test
    void should_createDefaultPolicyService_when_notProvided() {
        contextRunner
                .withPropertyValues("afg.security.auth-server.casbin.enabled=true")
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
                .withPropertyValues("afg.security.auth-server.casbin.enabled=true")
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
                        "afg.security.auth-server.casbin.enabled=true",
                        "afg.security.auth-server.casbin.model-type=acl",
                        "afg.security.auth-server.casbin.policy-adapter-type=jdbc",
                        "afg.security.auth-server.casbin.auto-save=false")
                .run(context -> {
                    AuthSecurityProperties properties = context.getBean(AuthSecurityProperties.class);
                    AuthSecurityProperties.CasbinConfig casbinConfig = properties.getCasbin();
                    assertThat(casbinConfig.isEnabled()).isTrue();
                    assertThat(casbinConfig.getModelType()).isEqualTo("acl");
                    assertThat(casbinConfig.getPolicyAdapterType()).isEqualTo("jdbc");
                    assertThat(casbinConfig.isAutoSave()).isFalse();
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
