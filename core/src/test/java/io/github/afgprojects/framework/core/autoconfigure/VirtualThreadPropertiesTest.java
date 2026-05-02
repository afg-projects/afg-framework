package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * VirtualThreadProperties 测试
 */
@DisplayName("VirtualThreadProperties 测试")
class VirtualThreadPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            VirtualThreadProperties props = new VirtualThreadProperties();

            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getNamePrefix()).isEqualTo("afg-vt-");
        }
    }

    @Nested
    @DisplayName("设置属性测试")
    class SetPropertiesTests {

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            VirtualThreadProperties props = new VirtualThreadProperties();
            props.setEnabled(false);
            props.setNamePrefix("custom-vt-");

            assertThat(props.isEnabled()).isFalse();
            assertThat(props.getNamePrefix()).isEqualTo("custom-vt-");
        }
    }
}
