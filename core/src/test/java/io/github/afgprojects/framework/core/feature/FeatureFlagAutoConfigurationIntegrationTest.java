package io.github.afgprojects.framework.core.feature;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.context.annotation.Configuration;

import io.github.afgprojects.framework.core.support.BaseIntegrationTest;

/**
 * FeatureFlagAutoConfiguration 集成测试
 */
@DisplayName("FeatureFlagAutoConfiguration 集成测试")
class FeatureFlagAutoConfigurationIntegrationTest extends BaseIntegrationTest {

    @Autowired(required = false)
    private FeatureFlagManager featureFlagManager;

    @Autowired(required = false)
    private FeatureToggleAspect featureToggleAspect;

    @Test
    @DisplayName("应自动配置 FeatureFlagManager")
    void shouldAutoConfigureFeatureFlagManager() {
        assertThat(featureFlagManager).isNotNull();
    }

    @Test
    @DisplayName("应自动配置 FeatureToggleAspect")
    void shouldAutoConfigureFeatureToggleAspect() {
        assertThat(featureToggleAspect).isNotNull();
    }

    @Test
    @DisplayName("FeatureFlagManager 应能正常工作")
    void featureFlagManagerShouldWork() {
        featureFlagManager.register(FeatureFlag.of("test-feature", true));
        assertThat(featureFlagManager.isEnabled("test-feature")).isTrue();

        featureFlagManager.disable("test-feature");
        assertThat(featureFlagManager.isEnabled("test-feature")).isFalse();
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestConfiguration {}
}