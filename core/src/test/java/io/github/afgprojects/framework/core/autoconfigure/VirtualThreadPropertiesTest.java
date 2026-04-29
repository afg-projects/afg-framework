package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * VirtualThreadProperties 单元测试
 */
class VirtualThreadPropertiesTest extends BaseUnitTest {

    @Test
    @DisplayName("默认配置应该正确初始化")
    void defaultPropertiesShouldBeCorrect() {
        // given
        VirtualThreadProperties properties = new VirtualThreadProperties();

        // then
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getNamePrefix()).isEqualTo("afg-vt-");
    }

    @Test
    @DisplayName("应该能够修改配置属性")
    void shouldAllowModifyingProperties() {
        // given
        VirtualThreadProperties properties = new VirtualThreadProperties();

        // when
        properties.setEnabled(false);
        properties.setNamePrefix("custom-vt-");

        // then
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getNamePrefix()).isEqualTo("custom-vt-");
    }
}
