package io.github.afgprojects.framework.core.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * NullValue 测试
 */
@DisplayName("NullValue 测试")
class NullValueTest {

    @Test
    @DisplayName("应该返回单例实例")
    void shouldReturnSingletonInstance() {
        assertThat(NullValue.INSTANCE).isSameAs(NullValue.INSTANCE);
    }

    @Test
    @DisplayName("应该输出有意义的字符串表示")
    void shouldOutputMeaningfulString() {
        assertThat(NullValue.INSTANCE.toString()).isEqualTo("NullValue.INSTANCE");
    }
}