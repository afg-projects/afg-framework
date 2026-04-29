package io.github.afgprojects.framework.core.module;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ModuleStateEvent 测试
 */
@DisplayName("ModuleStateEvent 测试")
class ModuleStateEventTest {

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该创建模块状态变更事件")
        void shouldCreateModuleStateEvent() {
            // given
            Object source = new Object();
            String moduleId = "test-module";
            ModuleState previousState = ModuleState.REGISTERED;
            ModuleState newState = ModuleState.INITIALIZING;
            String reason = "Starting initialization";

            // when
            ModuleStateEvent event = new ModuleStateEvent(source, moduleId, previousState, newState, reason);

            // then
            assertThat(event.getSource()).isEqualTo(source);
            assertThat(event.getModuleId()).isEqualTo(moduleId);
            assertThat(event.getPreviousState()).isEqualTo(previousState);
            assertThat(event.getNewState()).isEqualTo(newState);
            assertThat(event.getReason()).isEqualTo(reason);
        }

        @Test
        @DisplayName("应该支持 null 原因")
        void shouldSupportNullReason() {
            // when
            ModuleStateEvent event =
                    new ModuleStateEvent(new Object(), "module", ModuleState.READY, ModuleState.STOPPING, null);

            // then
            assertThat(event.getReason()).isNull();
        }
    }

    @Nested
    @DisplayName("getter 测试")
    class GetterTests {

        @Test
        @DisplayName("应该返回正确的模块ID")
        void shouldReturnCorrectModuleId() {
            // given
            ModuleStateEvent event =
                    new ModuleStateEvent(new Object(), "my-module", ModuleState.READY, ModuleState.PAUSED, null);

            // then
            assertThat(event.getModuleId()).isEqualTo("my-module");
        }

        @Test
        @DisplayName("应该返回正确的之前状态")
        void shouldReturnCorrectPreviousState() {
            // given
            ModuleStateEvent event =
                    new ModuleStateEvent(new Object(), "module", ModuleState.INITIALIZING, ModuleState.READY, null);

            // then
            assertThat(event.getPreviousState()).isEqualTo(ModuleState.INITIALIZING);
        }

        @Test
        @DisplayName("应该返回正确的新状态")
        void shouldReturnCorrectNewState() {
            // given
            ModuleStateEvent event = new ModuleStateEvent(
                    new Object(), "module", ModuleState.REGISTERED, ModuleState.FAILED, "Error occurred");

            // then
            assertThat(event.getNewState()).isEqualTo(ModuleState.FAILED);
        }

        @Test
        @DisplayName("应该返回正确的原因")
        void shouldReturnCorrectReason() {
            // given
            ModuleStateEvent event = new ModuleStateEvent(
                    new Object(), "module", ModuleState.READY, ModuleState.STOPPING, "Shutdown requested");

            // then
            assertThat(event.getReason()).isEqualTo("Shutdown requested");
        }

        @Test
        @DisplayName("应该返回事件时间戳")
        void shouldReturnEventTimestamp() {
            // given
            Instant before = Instant.now();
            ModuleStateEvent event =
                    new ModuleStateEvent(new Object(), "module", ModuleState.READY, ModuleState.PAUSED, null);
            Instant after = Instant.now();

            // then
            assertThat(event.getEventTimestamp()).isBetween(before, after);
        }
    }

    @Nested
    @DisplayName("toString 测试")
    class ToStringTests {

        @Test
        @DisplayName("应该包含所有信息")
        void shouldContainAllInformation() {
            // given
            ModuleStateEvent event = new ModuleStateEvent(
                    new Object(), "test-module", ModuleState.READY, ModuleState.STOPPING, "Shutdown");

            // when
            String result = event.toString();

            // then
            assertThat(result).contains("test-module");
            assertThat(result).contains("READY");
            assertThat(result).contains("STOPPING");
            assertThat(result).contains("Shutdown");
            assertThat(result).contains("timestamp");
        }

        @Test
        @DisplayName("null 原因应该正确显示")
        void shouldHandleNullReason() {
            // given
            ModuleStateEvent event = new ModuleStateEvent(
                    new Object(), "module", ModuleState.REGISTERED, ModuleState.INITIALIZING, null);

            // when
            String result = event.toString();

            // then
            assertThat(result).contains("reason='null'");
        }
    }

    @Nested
    @DisplayName("ApplicationEvent 继承测试")
    class ApplicationEventTests {

        @Test
        @DisplayName("应该是 ApplicationEvent 的子类")
        void shouldBeSubclassOfApplicationEvent() {
            // given
            ModuleStateEvent event =
                    new ModuleStateEvent(new Object(), "module", ModuleState.READY, ModuleState.PAUSED, null);

            // then
            assertThat(event).isInstanceOf(org.springframework.context.ApplicationEvent.class);
        }

        @Test
        @DisplayName("应该返回正确的事件源")
        void shouldReturnCorrectSource() {
            // given
            Object source = new Object();
            ModuleStateEvent event =
                    new ModuleStateEvent(source, "module", ModuleState.READY, ModuleState.PAUSED, null);

            // then
            assertThat(event.getSource()).isEqualTo(source);
        }
    }
}
