package io.github.afgprojects.framework.core.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ThreadLocalContextPropagator")
class ThreadLocalContextPropagatorTest {

    private ThreadLocalContextPropagator propagator;

    @BeforeEach
    void setUp() {
        propagator = new ThreadLocalContextPropagator();
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should register provider and increase count")
        void shouldRegisterProvider_andIncreaseCount() {
            assertThat(propagator.getProviderCount()).isZero();

            propagator.register(new StubProvider("p1"));

            assertThat(propagator.getProviderCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should register multiple providers")
        void shouldRegisterMultipleProviders() {
            propagator.register(new StubProvider("p1"));
            propagator.register(new StubProvider("p2"));

            assertThat(propagator.getProviderCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("captureAll / restoreAll / clearAll")
    class CaptureRestoreClear {

        @Test
        @DisplayName("should capture data from all providers")
        void shouldCaptureData_fromAllProviders() {
            propagator.register(new StubProvider("scope1"));
            propagator.register(new StubProvider("scope2"));

            Map<String, Object> snapshot = propagator.captureAll();

            assertThat(snapshot).containsEntry("scope1.value", "captured");
            assertThat(snapshot).containsEntry("scope2.value", "captured");
        }

        @Test
        @DisplayName("should return empty map when no providers registered")
        void shouldReturnEmptyMap_whenNoProviders() {
            Map<String, Object> snapshot = propagator.captureAll();

            assertThat(snapshot).isEmpty();
        }

        @Test
        @DisplayName("should restore data to all providers")
        void shouldRestoreData_toAllProviders() {
            StubProvider provider1 = new StubProvider("scope1");
            StubProvider provider2 = new StubProvider("scope2");
            propagator.register(provider1);
            propagator.register(provider2);

            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("scope1.value", "data1");
            snapshot.put("scope2.value", "data2");

            propagator.restoreAll(snapshot);

            assertThat(provider1.restoredValue).isEqualTo("data1");
            assertThat(provider2.restoredValue).isEqualTo("data2");
        }

        @Test
        @DisplayName("should clear all providers")
        void shouldClearAllProviders() {
            StubProvider provider1 = new StubProvider("scope1");
            StubProvider provider2 = new StubProvider("scope2");
            propagator.register(provider1);
            propagator.register(provider2);

            propagator.clearAll();

            assertThat(provider1.cleared).isTrue();
            assertThat(provider2.cleared).isTrue();
        }
    }

    @Nested
    @DisplayName("end-to-end async propagation")
    class EndToEndAsyncPropagation {

        @Test
        @DisplayName("should propagate context across threads")
        void shouldPropagateContext_acrossThreads() throws InterruptedException {
            StubProvider provider = new StubProvider("test");
            propagator.register(provider);

            // Capture context in main thread
            Map<String, Object> snapshot = propagator.captureAll();
            assertThat(snapshot).containsEntry("test.value", "captured");

            // Verify context is available in another thread
            AtomicReference<Boolean> restored = new AtomicReference<>(false);
            Thread thread = new Thread(() -> {
                propagator.restoreAll(snapshot);
                restored.set(provider.restoredValue != null);
                propagator.clearAll();
            });
            thread.start();
            thread.join();

            assertThat(restored.get()).isTrue();
        }
    }

    /**
     * Stub provider for testing.
     */
    private static class StubProvider implements ContextSnapshotProvider {

        private final String prefix;
        private String restoredValue;
        private boolean cleared;

        StubProvider(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void capture(Map<String, Object> snapshot) {
            snapshot.put(prefix + ".value", "captured");
        }

        @Override
        public void restore(Map<String, Object> snapshot) {
            this.restoredValue = (String) snapshot.get(prefix + ".value");
        }

        @Override
        public void clear() {
            this.cleared = true;
            this.restoredValue = null;
        }
    }
}
