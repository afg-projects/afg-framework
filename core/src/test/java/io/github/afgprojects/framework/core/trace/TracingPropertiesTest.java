package io.github.afgprojects.framework.core.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * TracingProperties 测试
 */
@DisplayName("TracingProperties 测试")
class TracingPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            TracingProperties props = new TracingProperties();

            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getAnnotations()).isNotNull();
            assertThat(props.getSampling()).isNotNull();
            assertThat(props.getBaggage()).isNotNull();
            assertThat(props.getPropagation()).isNotNull();
            assertThat(props.getZipkin()).isNotNull();
            assertThat(props.getJaeger()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Annotations 测试")
    class AnnotationsTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            TracingProperties.Annotations annotations = new TracingProperties.Annotations();

            assertThat(annotations.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            TracingProperties.Annotations annotations = new TracingProperties.Annotations();
            annotations.setEnabled(false);

            assertThat(annotations.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Sampling 测试")
    class SamplingTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            TracingProperties.Sampling sampling = new TracingProperties.Sampling();

            assertThat(sampling.getStrategy()).isEqualTo(SamplingStrategy.PROBABILITY);
            assertThat(sampling.getProbability()).isEqualTo(1.0);
            assertThat(sampling.getRate()).isEqualTo(100);
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            TracingProperties.Sampling sampling = new TracingProperties.Sampling();
            sampling.setStrategy(SamplingStrategy.RATE_LIMITING);
            sampling.setProbability(0.5);
            sampling.setRate(50);

            assertThat(sampling.getStrategy()).isEqualTo(SamplingStrategy.RATE_LIMITING);
            assertThat(sampling.getProbability()).isEqualTo(0.5);
            assertThat(sampling.getRate()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("Baggage 测试")
    class BaggageTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            TracingProperties.Baggage baggage = new TracingProperties.Baggage();

            assertThat(baggage.isEnabled()).isTrue();
            assertThat(baggage.getRemoteFields()).contains("tenantId", "userId", "traceId");
            assertThat(baggage.getLocalFields()).isEmpty();
            assertThat(baggage.getFieldMappings()).isEmpty();
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            TracingProperties.Baggage baggage = new TracingProperties.Baggage();
            baggage.setEnabled(false);
            baggage.setRemoteFields(List.of("customField"));
            baggage.setLocalFields(List.of("localField"));

            Map<String, String> mappings = new HashMap<>();
            mappings.put("key", "header");
            baggage.setFieldMappings(mappings);

            assertThat(baggage.isEnabled()).isFalse();
            assertThat(baggage.getRemoteFields()).contains("customField");
            assertThat(baggage.getLocalFields()).contains("localField");
            assertThat(baggage.getFieldMappings()).containsEntry("key", "header");
        }
    }

    @Nested
    @DisplayName("Propagation 测试")
    class PropagationTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            TracingProperties.Propagation propagation = new TracingProperties.Propagation();

            assertThat(propagation.isEnabled()).isTrue();
            assertThat(propagation.isThreadPoolEnabled()).isTrue();
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            TracingProperties.Propagation propagation = new TracingProperties.Propagation();
            propagation.setEnabled(false);
            propagation.setThreadPoolEnabled(false);

            assertThat(propagation.isEnabled()).isFalse();
            assertThat(propagation.isThreadPoolEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Zipkin 测试")
    class ZipkinTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            TracingProperties.Zipkin zipkin = new TracingProperties.Zipkin();

            assertThat(zipkin.isEnabled()).isFalse();
            assertThat(zipkin.getEndpoint()).isEqualTo("http://localhost:9411/api/v2/spans");
            assertThat(zipkin.getConnectTimeout()).isEqualTo(5000);
            assertThat(zipkin.getReadTimeout()).isEqualTo(10000);
            assertThat(zipkin.isCompressionEnabled()).isTrue();
            assertThat(zipkin.getSendInterval()).isEqualTo(5000);
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            TracingProperties.Zipkin zipkin = new TracingProperties.Zipkin();
            zipkin.setEnabled(true);
            zipkin.setEndpoint("http://custom:9411/api/v2/spans");
            zipkin.setConnectTimeout(10000);
            zipkin.setReadTimeout(20000);
            zipkin.setCompressionEnabled(false);
            zipkin.setSendInterval(10000);

            assertThat(zipkin.isEnabled()).isTrue();
            assertThat(zipkin.getEndpoint()).isEqualTo("http://custom:9411/api/v2/spans");
            assertThat(zipkin.getConnectTimeout()).isEqualTo(10000);
            assertThat(zipkin.getReadTimeout()).isEqualTo(20000);
            assertThat(zipkin.isCompressionEnabled()).isFalse();
            assertThat(zipkin.getSendInterval()).isEqualTo(10000);
        }
    }

    @Nested
    @DisplayName("Jaeger 测试")
    class JaegerTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            TracingProperties.Jaeger jaeger = new TracingProperties.Jaeger();

            assertThat(jaeger.isEnabled()).isFalse();
            assertThat(jaeger.getEndpoint()).isEqualTo("http://localhost:14268/api/traces");
            assertThat(jaeger.getOtlpEndpoint()).isNull();
            assertThat(jaeger.isUseOtlp()).isTrue();
            assertThat(jaeger.getConnectTimeout()).isEqualTo(5000);
            assertThat(jaeger.getReadTimeout()).isEqualTo(10000);
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            TracingProperties.Jaeger jaeger = new TracingProperties.Jaeger();
            jaeger.setEnabled(true);
            jaeger.setEndpoint("http://custom:14268/api/traces");
            jaeger.setOtlpEndpoint("http://custom:4317");
            jaeger.setUseOtlp(false);
            jaeger.setConnectTimeout(10000);
            jaeger.setReadTimeout(20000);

            assertThat(jaeger.isEnabled()).isTrue();
            assertThat(jaeger.getEndpoint()).isEqualTo("http://custom:14268/api/traces");
            assertThat(jaeger.getOtlpEndpoint()).isEqualTo("http://custom:4317");
            assertThat(jaeger.isUseOtlp()).isFalse();
            assertThat(jaeger.getConnectTimeout()).isEqualTo(10000);
            assertThat(jaeger.getReadTimeout()).isEqualTo(20000);
        }
    }
}
