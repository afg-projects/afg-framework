package io.github.afgprojects.framework.core.web.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RequestContextSnapshotProvider")
class RequestContextSnapshotProviderTest {

    private RequestContextSnapshotProvider provider;

    @BeforeEach
    void setUp() {
        provider = new RequestContextSnapshotProvider();
    }

    @AfterEach
    void tearDown() {
        provider.clear();
    }

    @Nested
    @DisplayName("capture")
    class Capture {

        @Test
        @DisplayName("should not capture when no request context available")
        void shouldNotCapture_whenNoRequestContextAvailable() {
            Map<String, Object> snapshot = new HashMap<>();
            provider.capture(snapshot);

            assertThat(snapshot).doesNotContainKey(RequestContextSnapshotProvider.KEY);
        }
    }

    @Nested
    @DisplayName("restore and getAsyncContext")
    class RestoreAndGetAsyncContext {

        @Test
        @DisplayName("should restore context and make it available via getAsyncContext")
        void shouldRestoreContext_andMakeAvailableViaGetAsyncContext() {
            RequestContext context = RequestContext.builder()
                    .traceId("trace-123")
                    .userId("1")
                    .build();

            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put(RequestContextSnapshotProvider.KEY, context);

            provider.restore(snapshot);

            RequestContext restored = RequestContextSnapshotProvider.getAsyncContext();
            assertThat(restored).isNotNull();
            assertThat(restored.getTraceId()).isEqualTo("trace-123");
            assertThat(restored.getUserId()).isEqualTo("1");
        }

        @Test
        @DisplayName("should clear async context when snapshot does not contain key")
        void shouldClearAsyncContext_whenSnapshotDoesNotContainKey() {
            // First set up a context
            RequestContext context = RequestContext.builder()
                    .traceId("trace-123")
                    .build();
            Map<String, Object> snapshotWithKey = new HashMap<>();
            snapshotWithKey.put(RequestContextSnapshotProvider.KEY, context);
            provider.restore(snapshotWithKey);

            // Then restore without the key
            Map<String, Object> emptySnapshot = new HashMap<>();
            provider.restore(emptySnapshot);

            assertThat(RequestContextSnapshotProvider.getAsyncContext()).isNull();
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("should clear async context")
        void shouldClearAsyncContext() {
            RequestContext context = RequestContext.builder()
                    .traceId("trace-123")
                    .build();

            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put(RequestContextSnapshotProvider.KEY, context);
            provider.restore(snapshot);

            provider.clear();

            assertThat(RequestContextSnapshotProvider.getAsyncContext()).isNull();
        }
    }

    @Nested
    @DisplayName("end-to-end: capture -> restore in another thread")
    class EndToEnd {

        @Test
        @DisplayName("should propagate RequestContext across threads via async context")
        void shouldPropagateRequestContext_acrossThreads() throws InterruptedException {
            RequestContext context = RequestContext.builder()
                    .traceId("trace-abc")
                    .userId("42")
                    .build();

            // Manually put context in snapshot (AfgRequestContextHolder needs HTTP request binding)
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put(RequestContextSnapshotProvider.KEY, context);

            // Restore in another thread
            AtomicReference<RequestContext> captured = new AtomicReference<>();
            Thread thread = new Thread(() -> {
                provider.restore(snapshot);
                captured.set(RequestContextSnapshotProvider.getAsyncContext());
                provider.clear();
            });
            thread.start();
            thread.join();

            assertThat(captured.get()).isNotNull();
            assertThat(captured.get().getTraceId()).isEqualTo("trace-abc");
            assertThat(captured.get().getUserId()).isEqualTo("42");
        }
    }
}
