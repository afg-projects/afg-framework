package io.github.afgprojects.framework.core.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ModuleStatus 测试
 */
@DisplayName("ModuleStatus 测试")
class ModuleStatusTest {

    @Nested
    @DisplayName("initial 测试")
    class InitialTests {

        @Test
        @DisplayName("应该创建初始状态")
        void shouldCreateInitialState() {
            // when
            ModuleStatus status = ModuleStatus.initial();

            // then
            assertThat(status.state()).isEqualTo(ModuleState.REGISTERED);
            assertThat(status.stateChangedAt()).isNotNull();
            assertThat(status.errorMessage()).isNull();
            assertThat(status.errorCause()).isNull();
            assertThat(status.metadata()).isEmpty();
            assertThat(status.isOperational()).isFalse();
        }
    }

    @Nested
    @DisplayName("failed 测试")
    class FailedTests {

        @Test
        @DisplayName("应该创建失败状态")
        void shouldCreateFailedState() {
            // given
            String errorMessage = "Initialization failed";
            Throwable cause = new RuntimeException("Root cause");

            // when
            ModuleStatus status = ModuleStatus.failed(errorMessage, cause);

            // then
            assertThat(status.state()).isEqualTo(ModuleState.FAILED);
            assertThat(status.stateChangedAt()).isNotNull();
            assertThat(status.errorMessage()).isEqualTo(errorMessage);
            assertThat(status.errorCause()).isEqualTo(cause);
            assertThat(status.isOperational()).isFalse();
        }

        @Test
        @DisplayName("应该支持 null 原因")
        void shouldSupportNullCause() {
            // when
            ModuleStatus status = ModuleStatus.failed("Error", null);

            // then
            assertThat(status.state()).isEqualTo(ModuleState.FAILED);
            assertThat(status.errorMessage()).isEqualTo("Error");
            assertThat(status.errorCause()).isNull();
        }
    }

    @Nested
    @DisplayName("transitionTo 测试")
    class TransitionToTests {

        @Test
        @DisplayName("应该成功转换到有效状态")
        void shouldTransitionToValidState() {
            // given
            ModuleStatus initial = ModuleStatus.initial();

            // when
            ModuleStatus result = initial.transitionTo(ModuleState.INITIALIZING);

            // then
            assertThat(result.state()).isEqualTo(ModuleState.INITIALIZING);
            assertThat(result.stateChangedAt()).isNotNull();
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        @DisplayName("转换时应该保留元数据")
        void shouldPreserveMetadata() {
            // given
            ModuleStatus status = ModuleStatus.initial();
            status.metadata().put("key", "value");

            // when
            ModuleStatus result = status.transitionTo(ModuleState.INITIALIZING);

            // then
            assertThat(result.metadata()).containsEntry("key", "value");
        }

        @Test
        @DisplayName("转换到无效状态应该抛出异常")
        void shouldThrowOnInvalidTransition() {
            // given
            ModuleStatus initial = ModuleStatus.initial();

            // when & then
            assertThatThrownBy(() -> initial.transitionTo(ModuleState.READY))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot transition");
        }

        @Test
        @DisplayName("应该支持完整的状态转换流程")
        void shouldSupportFullStateTransitionFlow() {
            // given
            ModuleStatus status = ModuleStatus.initial();

            // when - 完整的生命周期
            status = status.transitionTo(ModuleState.INITIALIZING);
            status = status.transitionTo(ModuleState.READY);
            status = status.transitionTo(ModuleState.PAUSED);
            status = status.transitionTo(ModuleState.READY);
            status = status.transitionTo(ModuleState.STOPPING);
            status = status.transitionTo(ModuleState.STOPPED);

            // then
            assertThat(status.state()).isEqualTo(ModuleState.STOPPED);
        }

        @Test
        @DisplayName("失败状态可以重新初始化")
        void canReinitializeFromFailed() {
            // given
            ModuleStatus failed = ModuleStatus.failed("Error", null);

            // when
            ModuleStatus result = failed.transitionTo(ModuleState.INITIALIZING);

            // then
            assertThat(result.state()).isEqualTo(ModuleState.INITIALIZING);
        }
    }

    @Nested
    @DisplayName("isOperational 测试")
    class IsOperationalTests {

        @Test
        @DisplayName("READY 状态应该可操作")
        void shouldBeOperationalWhenReady() {
            // given
            ModuleStatus status = ModuleStatus.initial()
                    .transitionTo(ModuleState.INITIALIZING)
                    .transitionTo(ModuleState.READY);

            // then
            assertThat(status.isOperational()).isTrue();
        }

        @Test
        @DisplayName("PAUSED 状态应该可操作")
        void shouldBeOperationalWhenPaused() {
            // given
            ModuleStatus status = ModuleStatus.initial()
                    .transitionTo(ModuleState.INITIALIZING)
                    .transitionTo(ModuleState.READY)
                    .transitionTo(ModuleState.PAUSED);

            // then
            assertThat(status.isOperational()).isTrue();
        }

        @Test
        @DisplayName("其他状态应该不可操作")
        void shouldNotBeOperationalInOtherStates() {
            assertThat(ModuleStatus.initial().isOperational()).isFalse();
            assertThat(ModuleStatus.failed("error", null).isOperational()).isFalse();
        }
    }

    @Nested
    @DisplayName("getMetadata 测试")
    class GetMetadataTests {

        @Test
        @DisplayName("应该返回不可修改的元数据视图")
        void shouldReturnUnmodifiableMetadata() {
            // given
            ModuleStatus status = ModuleStatus.initial();

            // when & then
            assertThatThrownBy(() -> status.getMetadata().put("key", "value"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("应该可以访问已设置的元数据")
        void shouldAccessSetMetadata() {
            // given
            ModuleStatus status = ModuleStatus.initial();
            status.metadata().put("key", "value");

            // when & then
            assertThat(status.getMetadata()).containsEntry("key", "value");
        }
    }

    @Nested
    @DisplayName("record 组件测试")
    class RecordComponentTests {

        @Test
        @DisplayName("应该正确创建 record 实例")
        void shouldCreateRecordInstance() {
            // given
            Instant now = Instant.now();
            RuntimeException cause = new RuntimeException("cause");

            // when
            ModuleStatus status = new ModuleStatus(
                    ModuleState.READY, now, null, cause, new java.util.concurrent.ConcurrentHashMap<>());

            // then
            assertThat(status.state()).isEqualTo(ModuleState.READY);
            assertThat(status.stateChangedAt()).isEqualTo(now);
            assertThat(status.errorMessage()).isNull();
            assertThat(status.errorCause()).isEqualTo(cause);
        }
    }
}
