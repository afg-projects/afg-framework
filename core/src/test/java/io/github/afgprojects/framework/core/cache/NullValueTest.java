package io.github.afgprojects.framework.core.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * NullValue 单元测试。
 * <p>
 * 测试 NullValue 单例模式的实现，包括单例实例获取和字符串表示。
 * </p>
 *
 * @see NullValue
 */
@DisplayName("NullValue 测试")
class NullValueTest {

    /**
     * 测试返回单例实例。
     */
    @Test
    @DisplayName("应该返回单例实例")
    void shouldReturnSingletonInstance() {
        assertThat(NullValue.INSTANCE).isSameAs(NullValue.INSTANCE);
    }

    /**
     * 测试输出有意义的字符串表示。
     */
    @Test
    @DisplayName("应该输出有意义的字符串表示")
    void shouldOutputMeaningfulString() {
        assertThat(NullValue.INSTANCE.toString()).isEqualTo("NullValue.INSTANCE");
    }
}