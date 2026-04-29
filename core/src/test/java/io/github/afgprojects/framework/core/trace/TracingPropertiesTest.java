package io.github.afgprojects.framework.core.trace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TracingProperties 测试类
 */
@DisplayName("TracingProperties 测试")
class TracingPropertiesTest {

    @Test
    @DisplayName("默认配置正确")
    void testDefaultConfiguration() {
        TracingProperties properties = new TracingProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getAnnotations().isEnabled()).isTrue();
        assertThat(properties.getSampling().getStrategy()).isEqualTo(SamplingStrategy.PROBABILITY);
        assertThat(properties.getSampling().getProbability()).isEqualTo(1.0);
        assertThat(properties.getSampling().getRate()).isEqualTo(100);
        assertThat(properties.getBaggage().isEnabled()).isTrue();
        assertThat(properties.getBaggage().getRemoteFields()).contains("tenantId", "userId", "traceId");
        assertThat(properties.getPropagation().isEnabled()).isTrue();
        assertThat(properties.getPropagation().isThreadPoolEnabled()).isTrue();
    }

    @Test
    @DisplayName("可以修改配置")
    void testModifyConfiguration() {
        TracingProperties properties = new TracingProperties();

        properties.setEnabled(false);
        properties.getAnnotations().setEnabled(false);
        properties.getSampling().setStrategy(SamplingStrategy.RATE_LIMITING);
        properties.getSampling().setProbability(0.5);
        properties.getSampling().setRate(50);
        properties.getBaggage().setEnabled(false);
        properties.getBaggage().setRemoteFields(java.util.List.of("customField"));
        properties.getPropagation().setEnabled(false);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getAnnotations().isEnabled()).isFalse();
        assertThat(properties.getSampling().getStrategy()).isEqualTo(SamplingStrategy.RATE_LIMITING);
        assertThat(properties.getSampling().getProbability()).isEqualTo(0.5);
        assertThat(properties.getSampling().getRate()).isEqualTo(50);
        assertThat(properties.getBaggage().isEnabled()).isFalse();
        assertThat(properties.getBaggage().getRemoteFields()).containsExactly("customField");
        assertThat(properties.getPropagation().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Baggage 字段映射支持自定义")
    void testBaggageFieldMappings() {
        TracingProperties properties = new TracingProperties();

        properties.getBaggage().getFieldMappings().put("tenantId", "X-Custom-Tenant");
        properties.getBaggage().getFieldMappings().put("userId", "X-Custom-User");

        assertThat(properties.getBaggage().getFieldMappings())
                .containsEntry("tenantId", "X-Custom-Tenant")
                .containsEntry("userId", "X-Custom-User");
    }
}