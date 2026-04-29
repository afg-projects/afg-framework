package io.github.afgprojects.framework.core.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * FeatureToggleAspect 测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FeatureToggleAspect 测试")
class FeatureToggleAspectTest {

    private FeatureFlagManager featureFlagManager;
    private FeatureToggleAspect aspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @BeforeEach
    void setUp() {
        FeatureFlagProperties properties = new FeatureFlagProperties();
        featureFlagManager = new FeatureFlagManager(properties);
        aspect = new FeatureToggleAspect(featureFlagManager);
    }

    @Test
    @DisplayName("功能启用时应执行原方法")
    void around_enabledFeature_shouldProceed() throws Throwable {
        // 准备测试数据
        FeatureToggle annotation = createAnnotation("enabled-feature", true, "");
        setupJoinPoint("testMethod");
        when(joinPoint.proceed()).thenReturn("result");

        // 启用功能
        featureFlagManager.enable("enabled-feature");

        // 执行
        Object result = aspect.around(joinPoint, annotation);

        // 验证
        assertThat(result).isEqualTo("result");
    }

    @Test
    @DisplayName("功能禁用且无回退方法时应抛出异常")
    void around_disabledFeature_shouldThrowException() throws Throwable {
        // 准备测试数据
        FeatureToggle annotation = createAnnotation("disabled-feature", true, "");
        setupJoinPoint("testMethod");

        // 禁用功能
        featureFlagManager.disable("disabled-feature");

        // 执行并验证
        assertThatThrownBy(() -> aspect.around(joinPoint, annotation))
                .isInstanceOf(FeatureDisabledException.class)
                .extracting("featureName")
                .isEqualTo("disabled-feature");
    }

    @Test
    @DisplayName("功能禁用且有回退方法时应执行回退")
    void around_disabledFeatureWithFallback_shouldExecuteFallback() throws Throwable {
        // 准备测试数据
        FeatureToggle annotation = createAnnotation("fallback-feature", true, "fallbackMethod");
        TestService target = new TestService();
        setupJoinPointWithTarget(target, "originalMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[] {"input"});

        // 禁用功能
        featureFlagManager.disable("fallback-feature");

        // 执行
        Object result = aspect.around(joinPoint, annotation);

        // 验证
        assertThat(result).isEqualTo("fallback: input");
    }

    @Test
    @DisplayName("功能未配置时使用默认值")
    void around_unconfiguredFeature_shouldUseDefault() throws Throwable {
        // 准备测试数据
        FeatureToggle annotation = createAnnotation("default-true", true, "");
        setupJoinPoint("testMethod");
        when(joinPoint.proceed()).thenReturn("result");

        // 功能未配置，默认启用

        // 执行
        Object result = aspect.around(joinPoint, annotation);

        // 验证
        assertThat(result).isEqualTo("result");
    }

    @Test
    @DisplayName("功能未配置且默认禁用时应抛出异常")
    void around_unconfiguredFeatureDefaultFalse_shouldThrowException() throws Throwable {
        // 准备测试数据
        FeatureToggle annotation = createAnnotation("default-false", false, "");
        setupJoinPoint("testMethod");

        // 功能未配置，默认禁用

        // 执行并验证
        assertThatThrownBy(() -> aspect.around(joinPoint, annotation))
                .isInstanceOf(FeatureDisabledException.class)
                .extracting("featureName")
                .isEqualTo("default-false");
    }

    @Test
    @DisplayName("回退方法不存在时应抛出异常")
    void around_fallbackNotFound_shouldThrowException() throws Throwable {
        // 准备测试数据
        FeatureToggle annotation = createAnnotation("missing-fallback", true, "nonExistentMethod");
        TestService target = new TestService();
        setupJoinPointWithTarget(target, "originalMethod");

        // 禁用功能
        featureFlagManager.disable("missing-fallback");

        // 执行并验证
        assertThatThrownBy(() -> aspect.around(joinPoint, annotation))
                .isInstanceOf(FeatureDisabledException.class)
                .hasMessageContaining("回退方法不存在");
    }

    @Test
    @DisplayName("灰度规则应根据上下文判断")
    void around_grayscaleRule_shouldCheckContext() throws Throwable {
        // 准备测试数据
        FeatureToggle annotation = createAnnotation("grayscale-feature", true, "");
        TestService target = new TestService();
        setupJoinPointWithTarget(target, "originalMethod");
        when(joinPoint.proceed()).thenReturn("original");

        // 设置灰度规则：只有用户 123 可访问
        featureFlagManager.setGrayscaleRule(
                "grayscale-feature", GrayscaleRule.ofUserWhitelist(java.util.Set.of(123L)));

        // 用户 123 应能访问
        // 用户 456 应不能访问
        // 由于我们无法在测试中模拟 RequestContext，这里测试 FeatureFlagManager
        assertThat(featureFlagManager.isEnabled("grayscale-feature", GrayscaleContext.fromUserId(123L)))
                .isTrue();
        assertThat(featureFlagManager.isEnabled("grayscale-feature", GrayscaleContext.fromUserId(456L)))
                .isFalse();
    }

    private void setupJoinPoint(String methodName) {
        lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(methodSignature.getName()).thenReturn(methodName);
        lenient().when(methodSignature.getDeclaringType()).thenReturn(Object.class);
        lenient().when(joinPoint.getTarget()).thenReturn(new Object());
    }

    private void setupJoinPointWithTarget(Object target, String methodName) {
        lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(methodSignature.getName()).thenReturn(methodName);
        lenient().when(methodSignature.getDeclaringType()).thenReturn(target.getClass());
        lenient().when(methodSignature.getParameterTypes()).thenReturn(new Class<?>[] {String.class});
        lenient().when(joinPoint.getTarget()).thenReturn(target);
    }

    private FeatureToggle createAnnotation(String feature, boolean enabledByDefault, String fallbackMethod) {
        return new FeatureToggle() {
            @Override
            public String feature() {
                return feature;
            }

            @Override
            public boolean enabledByDefault() {
                return enabledByDefault;
            }

            @Override
            public String fallbackMethod() {
                return fallbackMethod;
            }

            @Override
            public Class<FeatureToggle> annotationType() {
                return FeatureToggle.class;
            }
        };
    }

    /**
     * 测试服务类
     */
    static class TestService {

        public String originalMethod(String input) {
            return "original: " + input;
        }

        public String fallbackMethod(String input) {
            return "fallback: " + input;
        }
    }
}