package io.github.afgprojects.framework.core.trace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.github.afgprojects.framework.core.support.TestApplication;

/**
 * Tracing 集成测试
 */
@DisplayName("Tracing 集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.tracing.enabled=true",
                "afg.tracing.sampling-rate=1.0"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TracingIntegrationTest {

    @Autowired(required = false)
    private TracingProperties tracingProperties;

    @Autowired(required = false)
    private TracingSampler tracingSampler;

    @Nested
    @DisplayName("Tracing 配置测试")
    class TracingConfigTests {

        @Test
        @DisplayName("应该正确配置 TracingProperties")
        void shouldConfigureTracingProperties() {
            assertThat(tracingProperties).isNotNull();
            assertThat(tracingProperties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("应该正确配置 TracingSampler")
        void shouldConfigureTracingSampler() {
            assertThat(tracingSampler).isNotNull();
        }
    }

    @Nested
    @DisplayName("采样测试")
    class SamplingTests {

        @Test
        @DisplayName("应该能够判断是否采样")
        void shouldDetermineSampling() {
            assertThat(tracingSampler.shouldSample()).isTrue();
        }
    }
}
