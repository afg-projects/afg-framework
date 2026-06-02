package io.github.afgprojects.framework.ai.core.api.tool.

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ToolContext} 测试
 */
@DisplayName("ToolContext")
class DefaultToolContextTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("应构建空上下文")
        void shouldBuildEmpty() {
            ToolContext context = ToolContext.builder().build();
            assertThat(context.getUserId()).isNull();
            assertThat(context.getTenantId()).isNull();
            assertThat(context.getSessionId()).isNull();
        }

        @Test
        @DisplayName("应构建完整上下文")
        void shouldBuildFull() {
            ToolContext context = ToolContext.builder()
                    .userId("user1")
                    .tenantId("tenant1")
                    .sessionId("session1")
                    .build();

            assertThat(context.getUserId()).isEqualTo("user1");
            assertThat(context.getTenantId()).isEqualTo("tenant1");
            assertThat(context.getSessionId()).isEqualTo("session1");
        }
    }

    @Nested
    @DisplayName("属性")
    class PropertiesTest {

        @Test
        @DisplayName("empty 返回空上下文")
        void shouldReturnEmptyContext() {
            ToolContext context = ToolContext.empty();
            assertThat(context.getUserId()).isNull();
            assertThat(context.getTenantId()).isNull();
        }

        @Test
        @DisplayName("应支持额外的属性")
        void shouldSupportAdditionalProperties() {
            ToolContext context = ToolContext.builder()
                    .userId("user1")
                    .attribute("key1", "value1")
                    .build();

            assertThat(context.getAttributes().get("key1")).isEqualTo("value1");
        }
    }
}