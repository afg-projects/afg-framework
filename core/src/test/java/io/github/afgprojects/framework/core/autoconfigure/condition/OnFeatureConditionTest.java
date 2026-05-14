package io.github.afgprojects.framework.core.autoconfigure.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * OnFeatureCondition 单元测试。
 * 测试功能开关条件注解的匹配逻辑。
 *
 * @see OnFeatureCondition
 * @see ConditionalOnFeature
 */
class OnFeatureConditionTest {

    private OnFeatureCondition condition;
    private ConditionContext context;
    private Environment environment;
    private AnnotatedTypeMetadata metadata;

    @BeforeEach
    void setUp() {
        condition = new OnFeatureCondition();
        context = mock(ConditionContext.class);
        environment = mock(Environment.class);
        metadata = mock(AnnotatedTypeMetadata.class);
        when(context.getEnvironment()).thenReturn(environment);
    }

    /**
     * 测试功能启用时匹配。
     */
    @Test
    @DisplayName("功能启用时应该匹配")
    void shouldMatchWhenFeatureEnabled() {
        // given
        when(environment.getProperty("afg.feature.cache.enabled")).thenReturn("true");
        mockAnnotationAttributes("cache", true);

        // when
        boolean matches = condition.matches(context, metadata);

        // then
        assertThat(matches).isTrue();
    }

    /**
     * 测试功能禁用且期望禁用时匹配。
     */
    @Test
    @DisplayName("功能禁用且期望禁用时应该匹配")
    void shouldMatchWhenFeatureDisabledAndExpectedDisabled() {
        // given
        when(environment.getProperty("afg.feature.cache.enabled")).thenReturn("false");
        mockAnnotationAttributes("cache", false);

        // when
        boolean matches = condition.matches(context, metadata);

        // then
        assertThat(matches).isTrue();
    }

    /**
     * 测试功能启用但期望禁用时不匹配。
     */
    @Test
    @DisplayName("功能启用但期望禁用时应该不匹配")
    void shouldNotMatchWhenFeatureEnabledButExpectedDisabled() {
        // given
        when(environment.getProperty("afg.feature.cache.enabled")).thenReturn("true");
        mockAnnotationAttributes("cache", false);

        // when
        boolean matches = condition.matches(context, metadata);

        // then
        assertThat(matches).isFalse();
    }

    /**
     * 测试功能配置缺失时不匹配。
     */
    @Test
    @DisplayName("功能配置缺失时应该不匹配")
    void shouldNotMatchWhenFeatureConfigMissing() {
        // given
        when(environment.getProperty("afg.feature.cache.enabled")).thenReturn(null);
        mockAnnotationAttributes("cache", true);

        // when
        boolean matches = condition.matches(context, metadata);

        // then
        assertThat(matches).isFalse();
    }

    /**
     * 测试功能配置为空字符串时不匹配。
     */
    @Test
    @DisplayName("功能配置为空字符串时应该不匹配")
    void shouldNotMatchWhenFeatureConfigEmpty() {
        // given
        when(environment.getProperty("afg.feature.cache.enabled")).thenReturn("");
        mockAnnotationAttributes("cache", true);

        // when
        boolean matches = condition.matches(context, metadata);

        // then
        assertThat(matches).isFalse();
    }

    /**
     * 测试注解属性为 null 时不匹配。
     */
    @Test
    @DisplayName("注解属性为 null 时应该不匹配")
    void shouldNotMatchWhenAnnotationAttributesNull() {
        // given
        when(metadata.getAnnotationAttributes(ConditionalOnFeature.class.getName())).thenReturn(null);

        // when
        boolean matches = condition.matches(context, metadata);

        // then
        assertThat(matches).isFalse();
    }

    /**
     * 测试功能名称为空时不匹配。
     */
    @Test
    @DisplayName("功能名称为空时应该不匹配")
    void shouldNotMatchWhenFeatureNameEmpty() {
        // given
        when(environment.getProperty("afg.feature..enabled")).thenReturn("true");
        mockAnnotationAttributes("", true);

        // when
        boolean matches = condition.matches(context, metadata);

        // then
        assertThat(matches).isFalse();
    }

    /**
     * 测试布尔值大小写不敏感。
     */
    @Test
    @DisplayName("布尔值大小写不敏感")
    void shouldBeCaseInsensitiveForBoolean() {
        // given
        when(environment.getProperty("afg.feature.cache.enabled")).thenReturn("TRUE");
        mockAnnotationAttributes("cache", true);

        // when
        boolean matches = condition.matches(context, metadata);

        // then
        assertThat(matches).isTrue();
    }

    private void mockAnnotationAttributes(String feature, boolean enabled) {
        java.util.Map<String, Object> attrs = new java.util.HashMap<>();
        attrs.put("feature", feature);
        attrs.put("enabled", enabled);
        when(metadata.getAnnotationAttributes(ConditionalOnFeature.class.getName())).thenReturn(attrs);
    }
}