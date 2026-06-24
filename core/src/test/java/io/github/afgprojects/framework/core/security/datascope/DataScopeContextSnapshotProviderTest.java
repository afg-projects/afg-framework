package io.github.afgprojects.framework.core.security.datascope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DataScopeContextSnapshotProvider")
class DataScopeContextSnapshotProviderTest {

    private DataScopeContextSnapshotProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DataScopeContextSnapshotProvider();
        DataScopeContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        DataScopeContextHolder.clear();
    }

    @Nested
    @DisplayName("capture")
    class Capture {

        @Test
        @DisplayName("should capture context when set")
        void shouldCaptureContext_whenSet() {
            DataScopeContext context = DataScopeContext.builder()
                    .userId("1")
                    .deptId("10")
                    .build();
            DataScopeContextHolder.setContext(context);

            Map<String, Object> snapshot = new HashMap<>();
            provider.capture(snapshot);

            assertThat(snapshot).containsKey(DataScopeContextSnapshotProvider.KEY);
            assertThat(snapshot.get(DataScopeContextSnapshotProvider.KEY)).isSameAs(context);
        }

        @Test
        @DisplayName("should not capture when context is null")
        void shouldNotCapture_whenContextIsNull() {
            Map<String, Object> snapshot = new HashMap<>();
            provider.capture(snapshot);

            assertThat(snapshot).doesNotContainKey(DataScopeContextSnapshotProvider.KEY);
        }
    }

    @Nested
    @DisplayName("restore")
    class Restore {

        @Test
        @DisplayName("should restore context from snapshot")
        void shouldRestoreContext_fromSnapshot() {
            DataScopeContext context = DataScopeContext.builder()
                    .userId("1")
                    .deptId("10")
                    .build();

            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put(DataScopeContextSnapshotProvider.KEY, context);

            provider.restore(snapshot);

            assertThat(DataScopeContextHolder.getContext()).isSameAs(context);
        }

        @Test
        @DisplayName("should clear context when snapshot does not contain key")
        void shouldClearContext_whenSnapshotDoesNotContainKey() {
            DataScopeContextHolder.setContext(DataScopeContext.builder().userId("1").build());

            Map<String, Object> snapshot = new HashMap<>();
            provider.restore(snapshot);

            assertThat(DataScopeContextHolder.getContext()).isNull();
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("should clear context")
        void shouldClearContext() {
            DataScopeContextHolder.setContext(DataScopeContext.builder().userId("1").build());

            provider.clear();

            assertThat(DataScopeContextHolder.getContext()).isNull();
        }
    }

    @Nested
    @DisplayName("end-to-end: capture -> restore in another thread")
    class EndToEnd {

        @Test
        @DisplayName("should propagate DataScopeContext across threads")
        void shouldPropagateDataScopeContext_acrossThreads() throws InterruptedException {
            DataScopeContext context = DataScopeContext.builder()
                    .userId("42")
                    .deptId("100")
                    .build();
            DataScopeContextHolder.setContext(context);

            // Capture in main thread
            Map<String, Object> snapshot = new HashMap<>();
            provider.capture(snapshot);

            // Restore in another thread
            AtomicReference<DataScopeContext> captured = new AtomicReference<>();
            Thread thread = new Thread(() -> {
                provider.restore(snapshot);
                captured.set(DataScopeContextHolder.getContext());
                provider.clear();
            });
            thread.start();
            thread.join();

            assertThat(captured.get()).isNotNull();
            assertThat(captured.get().getUserId()).isEqualTo("42");
            assertThat(captured.get().getDeptId()).isEqualTo("100");

            // Main thread context unchanged
            assertThat(DataScopeContextHolder.getContext()).isSameAs(context);
        }
    }
}
