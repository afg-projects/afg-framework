package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * VirtualThreadProperties 单元测试。
 * 测试虚拟线程配置属性类的默认值和属性设置。
 *
 * @see VirtualThreadProperties
 */
@DisplayName("VirtualThreadProperties 测试")
class VirtualThreadPropertiesTest {

    /**
     * 默认值测试。
     * 验证配置属性的默认初始化值。
     */
    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        /**
         * 测试 VirtualThreadProperties 的默认值。
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            VirtualThreadProperties props = new VirtualThreadProperties();

            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getNamePrefix()).isEqualTo("afg-vt-");
        }
    }

    /**
     * 设置属性测试。
     * 验证配置属性的设置功能。
     */
    @Nested
    @DisplayName("设置属性测试")
    class SetPropertiesTests {

        /**
         * 测试 VirtualThreadProperties 的属性设置。
         */
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
