package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.api.agent.AgentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * AgentStatus 枚举纯单元测试
 */
@DisplayName("AgentStatus")
class AgentStatusTest {

    @Nested
    @DisplayName("枚举值")
    class Values {

        @Test
        @DisplayName("应有 4 个状态值")
        void shouldHaveFourStatusValues() {
            assertThat(AgentStatus.values()).hasSize(4);
        }

        @Test
        @DisplayName("应包含所有预期状态")
        void shouldContainAllExpectedStatuses() {
            assertThat(AgentStatus.values())
                    .containsExactlyInAnyOrder(
                            AgentStatus.COMPLETED,
                            AgentStatus.NEEDS_INPUT,
                            AgentStatus.TOOL_CALLING,
                            AgentStatus.ERROR
                    );
        }
    }

    @Nested
    @DisplayName("valueOf")
    class ValueOf {

        @Test
        @DisplayName("应按名称获取枚举值")
        void shouldGetByName() {
            assertThat(AgentStatus.valueOf("COMPLETED")).isEqualTo(AgentStatus.COMPLETED);
            assertThat(AgentStatus.valueOf("NEEDS_INPUT")).isEqualTo(AgentStatus.NEEDS_INPUT);
            assertThat(AgentStatus.valueOf("TOOL_CALLING")).isEqualTo(AgentStatus.TOOL_CALLING);
            assertThat(AgentStatus.valueOf("ERROR")).isEqualTo(AgentStatus.ERROR);
        }

        @Test
        @DisplayName("无效名称应抛异常")
        void shouldThrow_whenInvalidName() {
            assertThatThrownBy(() -> AgentStatus.valueOf("INVALID"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
