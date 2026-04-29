package io.github.afgprojects.framework.core.event;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * ModuleEvent 单元测试
 */
@DisplayName("ModuleEvent 测试")
class ModuleEventTest extends BaseUnitTest {

    @Nested
    @DisplayName("构造器测试")
    class ConstructorTest {

        @Test
        @DisplayName("应该成功创建 ModuleEvent")
        void shouldCreateModuleEvent() {
            // given
            String moduleId = "test-module";
            String eventType = "START";

            // when
            ModuleEvent event = new ModuleEvent(moduleId, eventType);

            // then
            assertNotNull(event);
            assertEquals(moduleId, event.moduleId());
            assertEquals(eventType, event.eventType());
        }

        @Test
        @DisplayName("时间戳应该为正数")
        void timestampShouldBePositive() {
            // given
            ModuleEvent event = new ModuleEvent("test-module", "START");

            // when
            long timestamp = event.timestamp();

            // then
            assertTrue(timestamp > 0);
        }

        @Test
        @DisplayName("时间戳应该是当前时间")
        void timestampShouldBeCurrentTime() {
            // given
            long before = System.currentTimeMillis();

            // when
            ModuleEvent event = new ModuleEvent("test-module", "START");

            // then
            long after = System.currentTimeMillis();
            assertTrue(event.timestamp() >= before);
            assertTrue(event.timestamp() <= after);
        }
    }

    @Nested
    @DisplayName("Getter 测试")
    class GetterTest {

        @Test
        @DisplayName("getModuleId 应该返回正确的模块ID")
        void getModuleIdShouldReturnCorrectId() {
            // given
            ModuleEvent event = new ModuleEvent("my-module", "STOP");

            // when & then
            assertEquals("my-module", event.moduleId());
        }

        @Test
        @DisplayName("getEventType 应该返回正确的事件类型")
        void getEventTypeShouldReturnCorrectType() {
            // given
            ModuleEvent event = new ModuleEvent("my-module", "STOP");

            // when & then
            assertEquals("STOP", event.eventType());
        }

        @Test
        @DisplayName("getTimestamp 应该返回时间戳")
        void getTimestampShouldReturnTimestamp() {
            // given
            ModuleEvent event = new ModuleEvent("my-module", "EVENT");

            // when & then
            assertTrue(event.timestamp() > 0);
        }
    }
}
