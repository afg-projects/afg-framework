package io.github.afgprojects.framework.core.module;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * ModuleState 测试
 */
@DisplayName("ModuleState 测试")
class ModuleStateTest {

    @Nested
    @DisplayName("getOrder 测试")
    class GetOrderTests {

        @Test
        @DisplayName("应该返回正确的状态顺序")
        void shouldReturnCorrectOrder() {
            assertThat(ModuleState.REGISTERED.getOrder()).isEqualTo(0);
            assertThat(ModuleState.INITIALIZING.getOrder()).isEqualTo(1);
            assertThat(ModuleState.READY.getOrder()).isEqualTo(2);
            assertThat(ModuleState.PAUSED.getOrder()).isEqualTo(3);
            assertThat(ModuleState.STOPPING.getOrder()).isEqualTo(4);
            assertThat(ModuleState.STOPPED.getOrder()).isEqualTo(5);
            assertThat(ModuleState.FAILED.getOrder()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("isOperational 测试")
    class IsOperationalTests {

        @ParameterizedTest
        @EnumSource(
                value = ModuleState.class,
                names = {"READY", "PAUSED"})
        @DisplayName("可操作状态应该返回 true")
        void shouldReturnTrueForOperationalStates(ModuleState state) {
            assertThat(state.isOperational()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = ModuleState.class,
                names = {"REGISTERED", "INITIALIZING", "STOPPING", "STOPPED", "FAILED"})
        @DisplayName("不可操作状态应该返回 false")
        void shouldReturnFalseForNonOperationalStates(ModuleState state) {
            assertThat(state.isOperational()).isFalse();
        }
    }

    @Nested
    @DisplayName("canTransitionTo 测试")
    class CanTransitionToTests {

        @Test
        @DisplayName("REGISTERED 可以转换到 INITIALIZING 或 FAILED")
        void registeredCanTransitionTo() {
            assertThat(ModuleState.REGISTERED.canTransitionTo(ModuleState.INITIALIZING))
                    .isTrue();
            assertThat(ModuleState.REGISTERED.canTransitionTo(ModuleState.FAILED))
                    .isTrue();
            assertThat(ModuleState.REGISTERED.canTransitionTo(ModuleState.READY))
                    .isFalse();
            assertThat(ModuleState.REGISTERED.canTransitionTo(ModuleState.STOPPED))
                    .isFalse();
        }

        @Test
        @DisplayName("INITIALIZING 可以转换到 READY 或 FAILED")
        void initializingCanTransitionTo() {
            assertThat(ModuleState.INITIALIZING.canTransitionTo(ModuleState.READY))
                    .isTrue();
            assertThat(ModuleState.INITIALIZING.canTransitionTo(ModuleState.FAILED))
                    .isTrue();
            assertThat(ModuleState.INITIALIZING.canTransitionTo(ModuleState.STOPPED))
                    .isFalse();
            assertThat(ModuleState.INITIALIZING.canTransitionTo(ModuleState.REGISTERED))
                    .isFalse();
        }

        @Test
        @DisplayName("READY 可以转换到 PAUSED、STOPPING 或 FAILED")
        void readyCanTransitionTo() {
            assertThat(ModuleState.READY.canTransitionTo(ModuleState.PAUSED)).isTrue();
            assertThat(ModuleState.READY.canTransitionTo(ModuleState.STOPPING)).isTrue();
            assertThat(ModuleState.READY.canTransitionTo(ModuleState.FAILED)).isTrue();
            assertThat(ModuleState.READY.canTransitionTo(ModuleState.REGISTERED))
                    .isFalse();
            assertThat(ModuleState.READY.canTransitionTo(ModuleState.INITIALIZING))
                    .isFalse();
        }

        @Test
        @DisplayName("PAUSED 可以转换到 READY 或 STOPPING")
        void pausedCanTransitionTo() {
            assertThat(ModuleState.PAUSED.canTransitionTo(ModuleState.READY)).isTrue();
            assertThat(ModuleState.PAUSED.canTransitionTo(ModuleState.STOPPING)).isTrue();
            assertThat(ModuleState.PAUSED.canTransitionTo(ModuleState.FAILED)).isFalse();
            assertThat(ModuleState.PAUSED.canTransitionTo(ModuleState.STOPPED)).isFalse();
        }

        @Test
        @DisplayName("STOPPING 可以转换到 STOPPED 或 FAILED")
        void stoppingCanTransitionTo() {
            assertThat(ModuleState.STOPPING.canTransitionTo(ModuleState.STOPPED))
                    .isTrue();
            assertThat(ModuleState.STOPPING.canTransitionTo(ModuleState.FAILED)).isTrue();
            assertThat(ModuleState.STOPPING.canTransitionTo(ModuleState.READY)).isFalse();
            assertThat(ModuleState.STOPPING.canTransitionTo(ModuleState.PAUSED)).isFalse();
        }

        @Test
        @DisplayName("STOPPED 可以转换到 INITIALIZING")
        void stoppedCanTransitionTo() {
            assertThat(ModuleState.STOPPED.canTransitionTo(ModuleState.INITIALIZING))
                    .isTrue();
            assertThat(ModuleState.STOPPED.canTransitionTo(ModuleState.READY)).isFalse();
            assertThat(ModuleState.STOPPED.canTransitionTo(ModuleState.FAILED)).isFalse();
        }

        @Test
        @DisplayName("FAILED 可以转换到 INITIALIZING")
        void failedCanTransitionTo() {
            assertThat(ModuleState.FAILED.canTransitionTo(ModuleState.INITIALIZING))
                    .isTrue();
            assertThat(ModuleState.FAILED.canTransitionTo(ModuleState.READY)).isFalse();
            assertThat(ModuleState.FAILED.canTransitionTo(ModuleState.STOPPED)).isFalse();
        }

        @Test
        @DisplayName("相同状态之间的转换应该返回 false")
        void shouldNotTransitionToSameState() {
            for (ModuleState state : ModuleState.values()) {
                assertThat(state.canTransitionTo(state)).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("枚举完整性测试")
    class EnumCompletenessTests {

        @Test
        @DisplayName("应该包含所有预期的状态")
        void shouldContainAllExpectedStates() {
            ModuleState[] states = ModuleState.values();
            assertThat(states).hasSize(7);
            assertThat(states)
                    .containsExactlyInAnyOrder(
                            ModuleState.REGISTERED,
                            ModuleState.INITIALIZING,
                            ModuleState.READY,
                            ModuleState.PAUSED,
                            ModuleState.STOPPING,
                            ModuleState.STOPPED,
                            ModuleState.FAILED);
        }

        @Test
        @DisplayName("应该正确解析枚举名称")
        void shouldParseEnumNames() {
            assertThat(ModuleState.valueOf("REGISTERED")).isEqualTo(ModuleState.REGISTERED);
            assertThat(ModuleState.valueOf("READY")).isEqualTo(ModuleState.READY);
            assertThat(ModuleState.valueOf("FAILED")).isEqualTo(ModuleState.FAILED);
        }
    }
}
