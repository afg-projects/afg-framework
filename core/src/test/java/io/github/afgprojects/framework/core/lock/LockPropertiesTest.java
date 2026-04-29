package io.github.afgprojects.framework.core.lock;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * LockProperties 测试
 */
@DisplayName("LockProperties 测试")
class LockPropertiesTest {

    @Test
    @DisplayName("应该使用默认值")
    void shouldUseDefaultValues() {
        // given
        LockProperties properties = new LockProperties();

        // then
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getKeyPrefix()).isEqualTo("afg:lock");
        assertThat(properties.getDefaultWaitTime()).isEqualTo(5000);
        assertThat(properties.getDefaultLeaseTime()).isEqualTo(-1);
        assertThat(properties.getAnnotations().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("应该正确设置和获取属性")
    void shouldSetAndGetProperties() {
        // given
        LockProperties properties = new LockProperties();

        // when
        properties.setEnabled(false);
        properties.setKeyPrefix("myapp:lock");
        properties.setDefaultWaitTime(10000);
        properties.setDefaultLeaseTime(30000);
        properties.getAnnotations().setEnabled(false);

        // then
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getKeyPrefix()).isEqualTo("myapp:lock");
        assertThat(properties.getDefaultWaitTime()).isEqualTo(10000);
        assertThat(properties.getDefaultLeaseTime()).isEqualTo(30000);
        assertThat(properties.getAnnotations().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("AnnotationConfig 应该正确初始化")
    void shouldInitializeAnnotationConfig() {
        // given
        LockProperties properties = new LockProperties();

        // then
        assertThat(properties.getAnnotations()).isNotNull();
        assertThat(properties.getAnnotations().isEnabled()).isTrue();
    }
}
