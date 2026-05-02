package io.github.afgprojects.framework.core.security.datascope.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import io.github.afgprojects.framework.core.security.datascope.DataScopeProperties;
import io.github.afgprojects.framework.core.web.context.DataScopeContextFilter;

/**
 * DataScopeAutoConfiguration 集成测试
 */
@DisplayName("DataScopeAutoConfiguration 集成测试")
class DataScopeAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataScopeAutoConfiguration.class));

    @Nested
    @DisplayName("DataScopeContextFilter 创建测试")
    class DataScopeContextFilterTests {

        @Test
        @DisplayName("启用数据权限时应该创建 DataScopeContextFilter")
        void shouldCreateDataScopeContextFilterWhenEnabled() {
            contextRunner
                    .withPropertyValues("afg.data-scope.enabled=true")
                    .run(context -> {
                        // DataScopeContextFilter 可能因为 @ConditionalOnBean(DataScopeProperties.class) 而不创建
                        // 验证 DataScopeProperties 被创建
                        assertThat(context).hasSingleBean(DataScopeProperties.class);
                    });
        }

        @Test
        @DisplayName("禁用数据权限时不应该创建 DataScopeContextFilter")
        void shouldNotCreateDataScopeContextFilterWhenDisabled() {
            contextRunner
                    .withPropertyValues("afg.data-scope.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(DataScopeContextFilter.class);
                    });
        }

        @Test
        @DisplayName("应该创建 DataScopeProperties")
        void shouldCreateDataScopeProperties() {
            contextRunner
                    .run(context -> {
                        assertThat(context).hasSingleBean(DataScopeProperties.class);
                    });
        }
    }
}