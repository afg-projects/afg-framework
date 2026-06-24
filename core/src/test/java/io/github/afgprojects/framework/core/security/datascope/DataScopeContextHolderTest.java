package io.github.afgprojects.framework.core.security.datascope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DataScopeContextHolder")
class DataScopeContextHolderTest {

    @BeforeEach
    @AfterEach
    void cleanUp() {
        DataScopeContextHolder.clear();
    }

    @Nested
    @DisplayName("setContext / getContext")
    class SetAndGetContext {

        @Test
        @DisplayName("should return null when no context set")
        void shouldReturnNull_whenNoContextSet() {
            assertThat(DataScopeContextHolder.getContext()).isNull();
        }

        @Test
        @DisplayName("should set and get context")
        void shouldSetAndGetContext() {
            DataScopeContext context = DataScopeContext.builder()
                    .userId("1")
                    .deptId("10")
                    .build();

            DataScopeContextHolder.setContext(context);

            assertThat(DataScopeContextHolder.getContext()).isSameAs(context);
            assertThat(DataScopeContextHolder.getContext().getUserId()).isEqualTo("1");
            assertThat(DataScopeContextHolder.getContext().getDeptId()).isEqualTo("10");
        }

        @Test
        @DisplayName("should clear context when set to null")
        void shouldClearContext_whenSetToNull() {
            DataScopeContext context = DataScopeContext.builder().userId("1").build();
            DataScopeContextHolder.setContext(context);
            assertThat(DataScopeContextHolder.getContext()).isNotNull();

            DataScopeContextHolder.setContext(null);
            assertThat(DataScopeContextHolder.getContext()).isNull();
        }
    }

    @Nested
    @DisplayName("getRequiredContext")
    class GetRequiredContext {

        @Test
        @DisplayName("should return context when set")
        void shouldReturnContext_whenSet() {
            DataScopeContext context = DataScopeContext.builder()
                    .userId("1")
                    .deptId("10")
                    .build();
            DataScopeContextHolder.setContext(context);

            DataScopeContext required = DataScopeContextHolder.getRequiredContext();

            assertThat(required).isNotNull();
            assertThat(required.getUserId()).isEqualTo("1");
        }

        @Test
        @DisplayName("should return empty context when not set")
        void shouldReturnEmptyContext_whenNotSet() {
            DataScopeContext required = DataScopeContextHolder.getRequiredContext();

            assertThat(required).isNotNull();
            assertThat(required.getUserId()).isNull();
            assertThat(required.getDeptId()).isNull();
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("should clear context")
        void shouldClearContext() {
            DataScopeContextHolder.setContext(DataScopeContext.builder().userId("1").build());
            assertThat(DataScopeContextHolder.getContext()).isNotNull();

            DataScopeContextHolder.clear();

            assertThat(DataScopeContextHolder.getContext()).isNull();
        }

        @Test
        @DisplayName("should be idempotent")
        void shouldBeIdempotent() {
            DataScopeContextHolder.clear();
            DataScopeContextHolder.clear();
            assertThat(DataScopeContextHolder.getContext()).isNull();
        }
    }

    @Nested
    @DisplayName("runWithContext")
    class RunWithContext {

        @Test
        @DisplayName("should execute with specified context")
        void shouldExecuteWithSpecifiedContext() {
            AtomicReference<DataScopeContext> captured = new AtomicReference<>();

            DataScopeContext context = DataScopeContext.builder()
                    .userId("1")
                    .deptId("10")
                    .build();

            DataScopeContextHolder.runWithContext(context, () -> {
                captured.set(DataScopeContextHolder.getContext());
            });

            assertThat(captured.get()).isSameAs(context);
        }

        @Test
        @DisplayName("should restore previous context after execution")
        void shouldRestorePreviousContext_afterExecution() {
            DataScopeContext original = DataScopeContext.builder().userId("1").build();
            DataScopeContextHolder.setContext(original);

            DataScopeContext temporary = DataScopeContext.builder().userId("2").build();
            DataScopeContextHolder.runWithContext(temporary, () -> {
                // Inside: temporary context
            });

            // After: original context restored
            assertThat(DataScopeContextHolder.getContext()).isSameAs(original);
        }

        @Test
        @DisplayName("should restore null context after execution")
        void shouldRestoreNullContext_afterExecution() {
            DataScopeContext temporary = DataScopeContext.builder().userId("1").build();
            DataScopeContextHolder.runWithContext(temporary, () -> {
                // Inside: temporary context
            });

            // After: null restored (no context was set before)
            assertThat(DataScopeContextHolder.getContext()).isNull();
        }

        @Test
        @DisplayName("should restore previous context even when exception occurs")
        void shouldRestorePreviousContext_evenWhenExceptionOccurs() {
            DataScopeContext original = DataScopeContext.builder().userId("1").build();
            DataScopeContextHolder.setContext(original);

            DataScopeContext temporary = DataScopeContext.builder().userId("2").build();
            try {
                DataScopeContextHolder.runWithContext(temporary, () -> {
                    throw new RuntimeException("test exception");
                });
            } catch (RuntimeException ignored) {
                // Expected
            }

            assertThat(DataScopeContextHolder.getContext()).isSameAs(original);
        }
    }

    @Nested
    @DisplayName("runWithoutDataScope")
    class RunWithoutDataScope {

        @Test
        @DisplayName("should execute with ignoreDataScope=true")
        void shouldExecuteWithIgnoreDataScopeTrue() {
            AtomicReference<DataScopeContext> captured = new AtomicReference<>();

            DataScopeContextHolder.runWithoutDataScope(() -> {
                captured.set(DataScopeContextHolder.getContext());
            });

            assertThat(captured.get()).isNotNull();
            assertThat(captured.get().isIgnoreDataScope()).isTrue();
        }

        @Test
        @DisplayName("should preserve userId from current context")
        void shouldPreserveUserId_fromCurrentContext() {
            DataScopeContextHolder.setContext(DataScopeContext.builder().userId("42").build());

            AtomicReference<DataScopeContext> captured = new AtomicReference<>();
            DataScopeContextHolder.runWithoutDataScope(() -> {
                captured.set(DataScopeContextHolder.getContext());
            });

            assertThat(captured.get().getUserId()).isEqualTo("42");
            assertThat(captured.get().isIgnoreDataScope()).isTrue();
        }

        @Test
        @DisplayName("should restore previous context after execution")
        void shouldRestorePreviousContext_afterExecution() {
            DataScopeContext original = DataScopeContext.builder()
                    .userId("1")
                    .ignoreDataScope(false)
                    .build();
            DataScopeContextHolder.setContext(original);

            DataScopeContextHolder.runWithoutDataScope(() -> {
                // Inside: ignoreDataScope=true
            });

            DataScopeContext restored = DataScopeContextHolder.getContext();
            assertThat(restored).isNotNull();
            assertThat(restored.isIgnoreDataScope()).isFalse();
        }
    }

    @Nested
    @DisplayName("ThreadLocal isolation")
    class ThreadLocalIsolation {

        @Test
        @DisplayName("should isolate context between threads")
        void shouldIsolateContextBetweenThreads() throws InterruptedException {
            DataScopeContextHolder.setContext(DataScopeContext.builder().userId("1").build());

            AtomicReference<DataScopeContext> otherThreadContext = new AtomicReference<>();
            Thread thread = new Thread(() -> {
                otherThreadContext.set(DataScopeContextHolder.getContext());
            });
            thread.start();
            thread.join();

            // Other thread should not see the main thread's context
            assertThat(otherThreadContext.get()).isNull();
            // Main thread still has its context
            assertThat(DataScopeContextHolder.getContext()).isNotNull();
            assertThat(DataScopeContextHolder.getContext().getUserId()).isEqualTo("1");
        }
    }
}
