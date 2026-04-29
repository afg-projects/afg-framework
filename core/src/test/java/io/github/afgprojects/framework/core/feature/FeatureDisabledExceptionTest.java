package io.github.afgprojects.framework.core.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;
import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * FeatureDisabledException 测试
 */
@DisplayName("FeatureDisabledException 测试")
class FeatureDisabledExceptionTest extends BaseUnitTest {

    @Test
    @DisplayName("构造函数应正确设置属性")
    void constructor_shouldSetProperties() {
        FeatureDisabledException ex = new FeatureDisabledException("test-feature");

        assertThat(ex.getFeatureName()).isEqualTo("test-feature");
        assertThat(ex.getMessage()).contains("功能已禁用");
        assertThat(ex.getCode()).isEqualTo(CommonErrorCode.FEATURE_DISABLED.getCode());
    }

    @Test
    @DisplayName("带自定义消息的构造函数应正确设置属性")
    void constructor_withMessage_shouldSetProperties() {
        FeatureDisabledException ex = new FeatureDisabledException("test-feature", "自定义错误消息");

        assertThat(ex.getFeatureName()).isEqualTo("test-feature");
        assertThat(ex.getMessage()).isEqualTo("自定义错误消息");
        assertThat(ex.getCode()).isEqualTo(CommonErrorCode.FEATURE_DISABLED.getCode());
    }

    @Test
    @DisplayName("异常应可抛出并捕获")
    void exception_shouldBeThrowable() {
        assertThatThrownBy(() -> {
            throw new FeatureDisabledException("my-feature");
        })
                .isInstanceOf(FeatureDisabledException.class)
                .hasMessageContaining("功能已禁用")
                .extracting("featureName")
                .isEqualTo("my-feature");
    }
}