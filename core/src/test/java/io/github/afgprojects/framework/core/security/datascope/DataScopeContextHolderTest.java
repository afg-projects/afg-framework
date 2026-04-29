package io.github.afgprojects.framework.core.security.datascope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DataScopeContextHolder 测试
 */
@DisplayName("DataScopeContextHolder 测试")
class DataScopeContextHolderTest {

    @AfterEach
    void tearDown() {
        DataScopeContextHolder.clear();
    }

    @Nested
    @DisplayName("基本操作测试")
    class BasicOperationTests {

        @Test
        @DisplayName("初始上下文应该为 null")
        void shouldBeNullInitially() {
            assertThat(DataScopeContextHolder.getContext()).isNull();
        }

        @Test
        @DisplayName("应该设置和获取上下文")
        void shouldSetAndGetContext() {
            // given
            DataScopeContext context = DataScopeContext.builder()
                    .userId(100L)
                    .deptId(10L)
                    .build();

            // when
            DataScopeContextHolder.setContext(context);

            // then
            assertThat(DataScopeContextHolder.getContext()).isEqualTo(context);
        }

        @Test
        @DisplayName("设置 null 应该清除上下文")
        void shouldClearWhenSetNull() {
            // given
            DataScopeContextHolder.setContext(DataScopeContext.allPermission(100L));

            // when
            DataScopeContextHolder.setContext(null);

            // then
            assertThat(DataScopeContextHolder.getContext()).isNull();
        }

        @Test
        @DisplayName("应该清除上下文")
        void shouldClearContext() {
            // given
            DataScopeContextHolder.setContext(DataScopeContext.allPermission(100L));

            // when
            DataScopeContextHolder.clear();

            // then
            assertThat(DataScopeContextHolder.getContext()).isNull();
        }
    }

    @Nested
    @DisplayName("getRequiredContext 测试")
    class RequiredContextTests {

        @Test
        @DisplayName("无上下文时应该返回空上下文")
        void shouldReturnEmptyWhenNoContext() {
            // when
            DataScopeContext context = DataScopeContextHolder.getRequiredContext();

            // then
            assertThat(context).isNotNull();
            assertThat(context.getUserId()).isNull();
        }

        @Test
        @DisplayName("有上下文时应该返回设置值")
        void shouldReturnContextWhenSet() {
            // given
            DataScopeContext original = DataScopeContext.builder()
                    .userId(100L)
                    .build();
            DataScopeContextHolder.setContext(original);

            // when
            DataScopeContext context = DataScopeContextHolder.getRequiredContext();

            // then
            assertThat(context).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("runWithContext 测试")
    class RunWithContextTests {

        @Test
        @DisplayName("应该在指定上下文中执行")
        void shouldRunInSpecifiedContext() {
            // given
            DataScopeContext context = DataScopeContext.builder()
                    .userId(100L)
                    .deptId(10L)
                    .build();

            // when
            DataScopeContextHolder.runWithContext(context, () -> {
                // then
                assertThat(DataScopeContextHolder.getContext()).isEqualTo(context);
            });
        }

        @Test
        @DisplayName("执行完成后应该恢复原上下文")
        void shouldRestoreOriginalContext() {
            // given
            DataScopeContext original = DataScopeContext.builder()
                    .userId(200L)
                    .build();
            DataScopeContextHolder.setContext(original);

            DataScopeContext newContext = DataScopeContext.builder()
                    .userId(100L)
                    .build();

            // when
            DataScopeContextHolder.runWithContext(newContext, () -> {
                // 执行中的上下文应该是新的
                assertThat(DataScopeContextHolder.getContext().getUserId()).isEqualTo(100L);
            });

            // then
            assertThat(DataScopeContextHolder.getContext()).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("runWithoutDataScope 测试")
    class RunWithoutDataScopeTests {

        @Test
        @DisplayName("应该在忽略权限模式下执行")
        void shouldRunInIgnoreMode() {
            // given
            DataScopeContext context = DataScopeContext.builder()
                    .userId(100L)
                    .deptId(10L)
                    .build();
            DataScopeContextHolder.setContext(context);

            // when
            DataScopeContextHolder.runWithoutDataScope(() -> {
                // then
                DataScopeContext currentContext = DataScopeContextHolder.getContext();
                assertThat(currentContext).isNotNull();
                assertThat(currentContext.isIgnoreDataScope()).isTrue();
                assertThat(currentContext.getUserId()).isEqualTo(100L);
            });
        }

        @Test
        @DisplayName("无上下文时也应该正常执行")
        void shouldRunWithoutExistingContext() {
            // when/then
            assertThatCode(() -> {
                DataScopeContextHolder.runWithoutDataScope(() -> {
                    DataScopeContext currentContext = DataScopeContextHolder.getContext();
                    assertThat(currentContext).isNotNull();
                    assertThat(currentContext.isIgnoreDataScope()).isTrue();
                });
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("执行完成后应该恢复原上下文")
        void shouldRestoreOriginalContext() {
            // given
            DataScopeContext original = DataScopeContext.builder()
                    .userId(100L)
                    .deptId(10L)
                    .build();
            DataScopeContextHolder.setContext(original);

            // when
            DataScopeContextHolder.runWithoutDataScope(() -> {
                // 执行中应该忽略权限
                assertThat(DataScopeContextHolder.getContext().isIgnoreDataScope()).isTrue();
            });

            // then
            assertThat(DataScopeContextHolder.getContext().isIgnoreDataScope()).isFalse();
            assertThat(DataScopeContextHolder.getContext().getUserId()).isEqualTo(100L);
        }
    }
}