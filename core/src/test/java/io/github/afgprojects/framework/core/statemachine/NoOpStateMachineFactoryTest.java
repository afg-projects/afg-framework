package io.github.afgprojects.framework.core.statemachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.api.statemachine.NoOpStateMachineFactory;
import io.github.afgprojects.framework.core.api.statemachine.StateMachineFactory;
import io.github.afgprojects.framework.core.api.statemachine.StateMachineInstance;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NoOpStateMachineFactory")
class NoOpStateMachineFactoryTest {

    private StateMachineFactory factory;

    @BeforeEach
    void setUp() {
        factory = new NoOpStateMachineFactory();
    }

    enum TestStatus {
        PENDING, CONFIRMED
    }

    @Nested
    @DisplayName("getDefinition")
    class GetDefinition {

        @Test
        @DisplayName("按类型查找返回 null")
        void shouldReturnNull_forTypeLookup() {
            assertThat(factory.getDefinition(TestStatus.class)).isNull();
        }

        @Test
        @DisplayName("按名称查找返回 null")
        void shouldReturnNull_forNameLookup() {
            assertThat(factory.getDefinition("TestStatus")).isNull();
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("创建 NoOp 实例")
        void shouldCreateNoOpInstance() {
            StateMachineInstance<Object, TestStatus> instance = factory.create(TestStatus.class);
            assertThat(instance).isNotNull();
        }

        @Test
        @DisplayName("NoOp 实例 getCurrentState 返回 null")
        void shouldReturnNull_forGetCurrentState() {
            StateMachineInstance<Object, TestStatus> instance = factory.create(TestStatus.class);
            assertThat(instance.getCurrentState(new Object())).isNull();
        }

        @Test
        @DisplayName("NoOp 实例 canTransit 返回 false")
        void shouldReturnFalse_forCanTransit() {
            StateMachineInstance<Object, TestStatus> instance = factory.create(TestStatus.class);
            assertThat(instance.canTransit(new Object(), TestStatus.CONFIRMED)).isFalse();
        }

        @Test
        @DisplayName("NoOp 实例 transit 静默忽略")
        void shouldIgnoreTransit() {
            StateMachineInstance<Object, TestStatus> instance = factory.create(TestStatus.class);
            instance.transit(new Object(), TestStatus.CONFIRMED);
            // 无异常，静默忽略
        }

        @Test
        @DisplayName("NoOp 实例 getAvailableTransitions 返回空列表")
        void shouldReturnEmptyList_forGetAvailableTransitions() {
            StateMachineInstance<Object, TestStatus> instance = factory.create(TestStatus.class);
            assertThat(instance.getAvailableTransitions(new Object())).isEmpty();
        }

        @Test
        @DisplayName("NoOp 实例 getDefinition 返回 null")
        void shouldReturnNull_forGetDefinition() {
            StateMachineInstance<Object, TestStatus> instance = factory.create(TestStatus.class);
            assertThat(instance.getDefinition()).isNull();
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("注册静默忽略")
        void shouldIgnoreRegistration() {
            var definition = StateMachineDefinition.<TestStatus>builder()
                    .name("TestStatus")
                    .entityType(Object.class)
                    .stateType(TestStatus.class)
                    .build();

            factory.register(definition);

            // 注册后仍然返回 null（NoOp 不存储）
            assertThat(factory.getDefinition(TestStatus.class)).isNull();
        }

        @Test
        @DisplayName("注册 null 静默忽略")
        void shouldIgnoreNullRegistration() {
            factory.register(null);
            // 无异常
        }
    }
}
