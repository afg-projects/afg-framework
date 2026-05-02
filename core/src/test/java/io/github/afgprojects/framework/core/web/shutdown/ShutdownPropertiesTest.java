package io.github.afgprojects.framework.core.web.shutdown;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ShutdownProperties 测试
 */
@DisplayName("ShutdownProperties 测试")
class ShutdownPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("默认应该启用")
        void shouldBeEnabledByDefault() {
            ShutdownProperties properties = new ShutdownProperties();
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("默认超时应该是 30 秒")
        void shouldHaveDefaultTimeout() {
            ShutdownProperties properties = new ShutdownProperties();
            assertThat(properties.getTimeout()).isEqualTo(Duration.ofSeconds(30));
        }

        @Test
        @DisplayName("默认应该有 3 个阶段")
        void shouldHaveDefaultPhases() {
            ShutdownProperties properties = new ShutdownProperties();
            assertThat(properties.getPhases()).hasSize(3);
        }

        @Test
        @DisplayName("默认阶段应该是 drain, cleanup, force")
        void shouldHaveDefaultPhaseNames() {
            ShutdownProperties properties = new ShutdownProperties();

            assertThat(properties.getPhases().get(0).getName()).isEqualTo("drain");
            assertThat(properties.getPhases().get(1).getName()).isEqualTo("cleanup");
            assertThat(properties.getPhases().get(2).getName()).isEqualTo("force");
        }
    }

    @Nested
    @DisplayName("设置值测试")
    class SetValueTests {

        @Test
        @DisplayName("应该能够设置 enabled")
        void shouldSetEnabled() {
            ShutdownProperties properties = new ShutdownProperties();
            properties.setEnabled(false);

            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("应该能够设置 timeout")
        void shouldSetTimeout() {
            ShutdownProperties properties = new ShutdownProperties();
            properties.setTimeout(Duration.ofSeconds(60));

            assertThat(properties.getTimeout()).isEqualTo(Duration.ofSeconds(60));
        }

        @Test
        @DisplayName("应该能够设置并行执行")
        void shouldSetParallelExecutionEnabled() {
            ShutdownProperties properties = new ShutdownProperties();
            properties.setParallelExecutionEnabled(true);

            assertThat(properties.isParallelExecutionEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Phase 测试")
    class PhaseTests {

        @Test
        @DisplayName("应该创建 Phase")
        void shouldCreatePhase() {
            ShutdownProperties.Phase phase = new ShutdownProperties.Phase("test", Duration.ofSeconds(10));

            assertThat(phase.getName()).isEqualTo("test");
            assertThat(phase.getTimeout()).isEqualTo(Duration.ofSeconds(10));
        }

        @Test
        @DisplayName("应该能够设置 Phase 属性")
        void shouldSetPhaseProperties() {
            ShutdownProperties.Phase phase = new ShutdownProperties.Phase();
            phase.setName("custom");
            phase.setTimeout(Duration.ofSeconds(20));

            assertThat(phase.getName()).isEqualTo("custom");
            assertThat(phase.getTimeout()).isEqualTo(Duration.ofSeconds(20));
        }
    }
}
