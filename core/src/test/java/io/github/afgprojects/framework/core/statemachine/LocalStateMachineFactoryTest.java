package io.github.afgprojects.framework.core.statemachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.api.statemachine.LocalStateMachineFactory;
import io.github.afgprojects.framework.core.api.statemachine.StateMachineFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LocalStateMachineFactory")
class LocalStateMachineFactoryTest {

    enum TestStatus {
        PENDING, CONFIRMED, CANCELLED
    }

    private LocalStateMachineFactory factory;

    @BeforeEach
    void setUp() {
        factory = new LocalStateMachineFactory(true);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("注册状态机定义后可通过类型查找")
        void shouldRegisterAndFindByType() {
            var definition = StateMachineDefinition.<TestStatus>builder()
                    .name("TestStatus")
                    .entityType(Object.class)
                    .stateType(TestStatus.class)
                    .transition(new TransitionDefinition<>(TestStatus.PENDING, TestStatus.CONFIRMED, "confirm", "confirm"))
                    .build();

            factory.register(definition);

            var found = factory.getDefinition(TestStatus.class);
            assertThat(found).isNotNull();
            assertThat(found.getName()).isEqualTo("TestStatus");
        }

        @Test
        @DisplayName("注册状态机定义后可通过名称查找")
        void shouldRegisterAndFindByName() {
            var definition = StateMachineDefinition.<TestStatus>builder()
                    .name("TestStatus")
                    .entityType(Object.class)
                    .stateType(TestStatus.class)
                    .build();

            factory.register(definition);

            var found = factory.getDefinition("TestStatus");
            assertThat(found).isNotNull();
        }

        @Test
        @DisplayName("注册 null 定义静默忽略")
        void shouldIgnoreNullDefinition() {
            factory.register(null);

            assertThat(factory.getDefinition(TestStatus.class)).isNull();
        }

        @Test
        @DisplayName("重复注册覆盖旧定义")
        void shouldOverrideExistingDefinition() {
            var def1 = StateMachineDefinition.<TestStatus>builder()
                    .name("TestStatus-v1")
                    .entityType(Object.class)
                    .stateType(TestStatus.class)
                    .build();

            var def2 = StateMachineDefinition.<TestStatus>builder()
                    .name("TestStatus-v2")
                    .entityType(Object.class)
                    .stateType(TestStatus.class)
                    .build();

            factory.register(def1);
            factory.register(def2);

            var found = factory.getDefinition(TestStatus.class);
            assertThat(found.getName()).isEqualTo("TestStatus-v2");
        }
    }

    @Nested
    @DisplayName("getDefinition")
    class GetDefinition {

        @Test
        @DisplayName("未注册的类型返回 null")
        void shouldReturnNull_forUnregisteredType() {
            assertThat(factory.getDefinition(TestStatus.class)).isNull();
        }

        @Test
        @DisplayName("未注册的名称返回 null")
        void shouldReturnNull_forUnregisteredName() {
            assertThat(factory.getDefinition("nonExistent")).isNull();
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("创建已注册状态机的实例")
        void shouldCreateInstance_forRegisteredType() {
            var definition = StateMachineDefinition.<TestStatus>builder()
                    .name("TestStatus")
                    .entityType(Object.class)
                    .stateType(TestStatus.class)
                    .build();

            factory.register(definition);

            var instance = factory.create(TestStatus.class);
            assertThat(instance).isNotNull();
            assertThat(instance.getDefinition()).isEqualTo(definition);
        }

        @Test
        @DisplayName("未注册的类型抛出异常")
        void shouldThrowException_forUnregisteredType() {
            assertThatThrownBy(() -> factory.create(TestStatus.class))
                    .isInstanceOf(io.github.afgprojects.framework.commons.exception.BusinessException.class);
        }
    }

    @Nested
    @DisplayName("构造函数")
    class Constructor {

        @Test
        @DisplayName("默认严格模式为 true")
        void shouldDefaultToStrictMode() {
            var defaultFactory = new LocalStateMachineFactory();
            assertThat(defaultFactory.isStrictMode()).isTrue();
        }

        @Test
        @DisplayName("可配置非严格模式")
        void shouldSupportNonStrictMode() {
            var nonStrictFactory = new LocalStateMachineFactory(false);
            assertThat(nonStrictFactory.isStrictMode()).isFalse();
        }
    }
}
