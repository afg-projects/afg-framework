package io.github.afgprojects.framework.core.web.shutdown;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * ShutdownConfig 测试
 */
@DisplayName("ShutdownConfig 测试")
class ShutdownConfigTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("默认应该启用")
        void shouldBeEnabledByDefault() {
            AfgCoreProperties.ShutdownConfig config = new AfgCoreProperties.ShutdownConfig();
            assertThat(config.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("默认超时应该是 30 秒")
        void shouldHaveDefaultTimeout() {
            AfgCoreProperties.ShutdownConfig config = new AfgCoreProperties.ShutdownConfig();
            assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(30));
        }

        @Test
        @DisplayName("默认阶段列表应该为空")
        void shouldHaveEmptyPhasesByDefault() {
            AfgCoreProperties.ShutdownConfig config = new AfgCoreProperties.ShutdownConfig();
            assertThat(config.getPhases()).isEmpty();
        }
    }

    @Nested
    @DisplayName("设置值测试")
    class SetValueTests {

        @Test
        @DisplayName("应该能够设置 enabled")
        void shouldSetEnabled() {
            AfgCoreProperties.ShutdownConfig config = new AfgCoreProperties.ShutdownConfig();
            config.setEnabled(false);

            assertThat(config.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("应该能够设置 timeout")
        void shouldSetTimeout() {
            AfgCoreProperties.ShutdownConfig config = new AfgCoreProperties.ShutdownConfig();
            config.setTimeout(Duration.ofSeconds(60));

            assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(60));
        }

        @Test
        @DisplayName("应该能够设置并行执行")
        void shouldSetParallelExecutionEnabled() {
            AfgCoreProperties.ShutdownConfig config = new AfgCoreProperties.ShutdownConfig();
            config.setParallelExecutionEnabled(true);

            assertThat(config.isParallelExecutionEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("ShutdownPhase 测试")
    class ShutdownPhaseTests {

        @Test
        @DisplayName("应该创建 ShutdownPhase")
        void shouldCreatePhase() {
            AfgCoreProperties.ShutdownConfig.ShutdownPhase phase = new AfgCoreProperties.ShutdownConfig.ShutdownPhase("test", Duration.ofSeconds(10));

            assertThat(phase.getName()).isEqualTo("test");
            assertThat(phase.getTimeout()).isEqualTo(Duration.ofSeconds(10));
        }

        @Test
        @DisplayName("应该能够设置 ShutdownPhase 属性")
        void shouldSetPhaseProperties() {
            AfgCoreProperties.ShutdownConfig.ShutdownPhase phase = new AfgCoreProperties.ShutdownConfig.ShutdownPhase();
            phase.setName("custom");
            phase.setTimeout(Duration.ofSeconds(20));

            assertThat(phase.getName()).isEqualTo("custom");
            assertThat(phase.getTimeout()).isEqualTo(Duration.ofSeconds(20));
        }
    }
}