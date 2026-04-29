package io.github.afgprojects.framework.core.autoconfigure.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * OnPropertyNotEmptyCondition 测试类
 */
class OnPropertyNotEmptyConditionTest {

    private OnPropertyNotEmptyCondition condition;
    private ConditionContext context;
    private Environment environment;
    private AnnotatedTypeMetadata metadata;

    @BeforeEach
    void setUp() {
        condition = new OnPropertyNotEmptyCondition();
        context = mock(ConditionContext.class);
        environment = mock(Environment.class);
        metadata = mock(AnnotatedTypeMetadata.class);
        when(context.getEnvironment()).thenReturn(environment);
    }

    @Test
    @DisplayName("属性存在且非空时应该匹配")
    void shouldMatchWhenPropertyExistsAndNotEmpty() {
        // 准备
        when(environment.getProperty("afg.database.url")).thenReturn("jdbc:mysql://localhost:3306/db");
        mockAnnotationAttributes("afg.database.url", "");

        // 执行
        boolean matches = condition.matches(context, metadata);

        // 验证
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("属性不存在时应该不匹配")
    void shouldNotMatchWhenPropertyNotExists() {
        // 准备
        when(environment.getProperty("afg.database.url")).thenReturn(null);
        mockAnnotationAttributes("afg.database.url", "");

        // 执行
        boolean matches = condition.matches(context, metadata);

        // 验证
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("属性为空字符串时应该不匹配")
    void shouldNotMatchWhenPropertyIsEmptyString() {
        // 准备
        when(environment.getProperty("afg.database.url")).thenReturn("");
        mockAnnotationAttributes("afg.database.url", "");

        // 执行
        boolean matches = condition.matches(context, metadata);

        // 验证
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("带前缀的属性应该正确拼接")
    void shouldBuildPropertyNameWithPrefix() {
        // 准备
        when(environment.getProperty("afg.database.url")).thenReturn("jdbc:mysql://localhost:3306/db");
        mockAnnotationAttributes("url", "afg.database");

        // 执行
        boolean matches = condition.matches(context, metadata);

        // 验证
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("前缀以点结尾时应该正确拼接")
    void shouldBuildPropertyNameWhenPrefixEndsWithDot() {
        // 准备
        when(environment.getProperty("afg.database.url")).thenReturn("jdbc:mysql://localhost:3306/db");
        mockAnnotationAttributes("url", "afg.database.");

        // 执行
        boolean matches = condition.matches(context, metadata);

        // 验证
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("空前缀时应该只使用 value")
    void shouldUseValueOnlyWhenPrefixEmpty() {
        // 准备
        when(environment.getProperty("my.property")).thenReturn("some-value");
        mockAnnotationAttributes("my.property", "");

        // 执行
        boolean matches = condition.matches(context, metadata);

        // 验证
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("注解属性为 null 时应该不匹配")
    void shouldNotMatchWhenAnnotationAttributesNull() {
        // 准备
        when(metadata.getAnnotationAttributes(ConditionalOnPropertyNotEmpty.class.getName())).thenReturn(null);

        // 执行
        boolean matches = condition.matches(context, metadata);

        // 验证
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("value 为空时应该不匹配")
    void shouldNotMatchWhenValueEmpty() {
        // 准备
        when(environment.getProperty("")).thenReturn(null);
        mockAnnotationAttributes("", "");

        // 执行
        boolean matches = condition.matches(context, metadata);

        // 验证
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("null 前缀应该被视为空字符串")
    void shouldTreatNullPrefixAsEmpty() {
        // 准备
        when(environment.getProperty("my.property")).thenReturn("some-value");
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("value", "my.property");
        attrs.put("prefix", null);
        when(metadata.getAnnotationAttributes(ConditionalOnPropertyNotEmpty.class.getName())).thenReturn(attrs);

        // 执行
        boolean matches = condition.matches(context, metadata);

        // 验证
        assertThat(matches).isTrue();
    }

    private void mockAnnotationAttributes(String value, String prefix) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("value", value);
        attrs.put("prefix", prefix);
        when(metadata.getAnnotationAttributes(ConditionalOnPropertyNotEmpty.class.getName())).thenReturn(attrs);
    }
}