package io.github.afgprojects.framework.core.statemachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StateMachineDefinition")
class StateMachineDefinitionTest {

    enum TestStatus {
        PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }

    private StateMachineDefinition<TestStatus> definition;

    @BeforeEach
    void setUp() {
        definition = StateMachineDefinition.<TestStatus>builder()
                .name("TestStatus")
                .entityType(Object.class)
                .stateType(TestStatus.class)
                .transition(new TransitionDefinition<>(TestStatus.PENDING, TestStatus.CONFIRMED, "confirm", "confirm"))
                .transition(new TransitionDefinition<>(TestStatus.CONFIRMED, TestStatus.SHIPPED, "ship", "ship"))
                .transition(new TransitionDefinition<>(TestStatus.SHIPPED, TestStatus.DELIVERED, "deliver", "deliver"))
                .transition(new TransitionDefinition<>(TestStatus.CONFIRMED, TestStatus.CANCELLED, "cancel", "cancel"))
                .transition(new TransitionDefinition<>(TestStatus.PENDING, TestStatus.CANCELLED, "cancel", "cancel"))
                .build();
    }

    @Nested
    @DisplayName("canTransit")
    class CanTransit {

        @Test
        @DisplayName("合法转换返回 true")
        void shouldReturnTrue_whenTransitionIsValid() {
            assertThat(definition.canTransit(TestStatus.PENDING, TestStatus.CONFIRMED)).isTrue();
            assertThat(definition.canTransit(TestStatus.CONFIRMED, TestStatus.SHIPPED)).isTrue();
            assertThat(definition.canTransit(TestStatus.SHIPPED, TestStatus.DELIVERED)).isTrue();
        }

        @Test
        @DisplayName("非法转换返回 false")
        void shouldReturnFalse_whenTransitionIsInvalid() {
            assertThat(definition.canTransit(TestStatus.PENDING, TestStatus.DELIVERED)).isFalse();
            assertThat(definition.canTransit(TestStatus.DELIVERED, TestStatus.PENDING)).isFalse();
            assertThat(definition.canTransit(TestStatus.CANCELLED, TestStatus.CONFIRMED)).isFalse();
        }

        @Test
        @DisplayName("源状态为 null 时返回 false")
        void shouldReturnFalse_whenFromStateIsNull() {
            assertThat(definition.canTransit(null, TestStatus.CONFIRMED)).isFalse();
        }

        @Test
        @DisplayName("目标状态为 null 时返回 false")
        void shouldReturnFalse_whenToStateIsNull() {
            assertThat(definition.canTransit(TestStatus.PENDING, null)).isFalse();
        }

        @Test
        @DisplayName("多源状态转换")
        void shouldSupportMultipleFromStates() {
            // CONFIRMED -> CANCELLED 和 PENDING -> CANCELLED 都应该合法
            assertThat(definition.canTransit(TestStatus.CONFIRMED, TestStatus.CANCELLED)).isTrue();
            assertThat(definition.canTransit(TestStatus.PENDING, TestStatus.CANCELLED)).isTrue();
        }
    }

    @Nested
    @DisplayName("findTransitions")
    class FindTransitions {

        @Test
        @DisplayName("查找从指定状态出发的所有转换")
        void shouldFindAllTransitionsFromGivenState() {
            var transitions = definition.findTransitions(TestStatus.PENDING);

            assertThat(transitions).hasSize(2);
            assertThat(transitions.stream().map(TransitionDefinition::getTo).toList())
                    .containsExactlyInAnyOrder(TestStatus.CONFIRMED, TestStatus.CANCELLED);
        }

        @Test
        @DisplayName("没有转换的状态返回空列表")
        void shouldReturnEmptyList_whenNoTransitionsFromState() {
            var transitions = definition.findTransitions(TestStatus.DELIVERED);

            assertThat(transitions).isEmpty();
        }

        @Test
        @DisplayName("源状态为 null 时返回空列表")
        void shouldReturnEmptyList_whenFromStateIsNull() {
            var transitions = definition.findTransitions(null);

            assertThat(transitions).isEmpty();
        }
    }

    @Nested
    @DisplayName("findReachableStates")
    class FindReachableStates {

        @Test
        @DisplayName("查找从指定状态可达的所有目标状态")
        void shouldFindAllReachableStates() {
            var reachable = definition.findReachableStates(TestStatus.PENDING);

            assertThat(reachable).containsExactlyInAnyOrder(TestStatus.CONFIRMED, TestStatus.CANCELLED);
        }

        @Test
        @DisplayName("终态无可达状态")
        void shouldReturnEmptySet_forTerminalState() {
            var reachable = definition.findReachableStates(TestStatus.DELIVERED);

            assertThat(reachable).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEvent")
    class FindByEvent {

        @Test
        @DisplayName("按事件名查找转换")
        void shouldFindTransitionByEvent() {
            var transitions = definition.findByEvent("cancel");

            assertThat(transitions).hasSize(2);
            assertThat(transitions.stream().map(TransitionDefinition::getFrom).toList())
                    .containsExactlyInAnyOrder(TestStatus.CONFIRMED, TestStatus.PENDING);
        }

        @Test
        @DisplayName("不存在的事件名返回空列表")
        void shouldReturnEmptyList_forNonExistentEvent() {
            var transitions = definition.findByEvent("nonExistent");

            assertThat(transitions).isEmpty();
        }
    }

    @Nested
    @DisplayName("states")
    class States {

        @Test
        @DisplayName("自动收集所有状态")
        void shouldCollectAllStatesFromTransitions() {
            assertThat(definition.getStates()).containsExactlyInAnyOrder(
                    TestStatus.PENDING, TestStatus.CONFIRMED, TestStatus.SHIPPED,
                    TestStatus.DELIVERED, TestStatus.CANCELLED);
        }

        @Test
        @DisplayName("手动添加的状态也包含在内")
        void shouldIncludeManuallyAddedStates() {
            var def = StateMachineDefinition.<TestStatus>builder()
                    .name("Test")
                    .entityType(Object.class)
                    .stateType(TestStatus.class)
                    .state(TestStatus.DELIVERED)
                    .build();

            assertThat(def.getStates()).contains(TestStatus.DELIVERED);
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("Builder 构建完整定义")
        void shouldBuildCompleteDefinition() {
            var def = StateMachineDefinition.<TestStatus>builder()
                    .name("OrderStatus")
                    .entityType(String.class)
                    .stateType(TestStatus.class)
                    .transition(new TransitionDefinition<>(TestStatus.PENDING, TestStatus.CONFIRMED, "confirm", "confirm"))
                    .build();

            assertThat(def.getName()).isEqualTo("OrderStatus");
            assertThat(def.getEntityType()).isEqualTo(String.class);
            assertThat(def.getStateType()).isEqualTo(TestStatus.class);
            assertThat(def.getTransitions()).hasSize(1);
            assertThat(def.getStates()).containsExactlyInAnyOrder(TestStatus.PENDING, TestStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Builder transitions 批量添加")
        void shouldSupportBatchTransitions() {
            var transitions = java.util.List.of(
                    new TransitionDefinition<>(TestStatus.PENDING, TestStatus.CONFIRMED, "confirm", "confirm"),
                    new TransitionDefinition<>(TestStatus.CONFIRMED, TestStatus.SHIPPED, "ship", "ship")
            );

            var def = StateMachineDefinition.<TestStatus>builder()
                    .name("Batch")
                    .entityType(Object.class)
                    .stateType(TestStatus.class)
                    .transitions(transitions)
                    .build();

            assertThat(def.getTransitions()).hasSize(2);
            assertThat(def.getStates()).hasSize(3);
        }
    }
}
