package io.github.afgprojects.framework.core.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BaggageContextSnapshotProvider")
class BaggageContextSnapshotProviderTest {

    private BaggageContextSnapshotProvider provider;

    @BeforeEach
    void setUp() {
        provider = new BaggageContextSnapshotProvider();
        BaggageContext.clearLocalBaggage();
    }

    @AfterEach
    void tearDown() {
        BaggageContext.clearLocalBaggage();
    }

    @Nested
    @DisplayName("capture")
    class Capture {

        @Test
        @DisplayName("should capture baggage when set")
        void shouldCaptureBaggage_whenSet() {
            BaggageContext.set("key1", "value1");
            BaggageContext.set("key2", "value2");

            Map<String, Object> snapshot = new HashMap<>();
            provider.capture(snapshot);

            assertThat(snapshot).containsKey(BaggageContextSnapshotProvider.KEY);
            @SuppressWarnings("unchecked")
            Map<String, String> captured = (Map<String, String>) snapshot.get(BaggageContextSnapshotProvider.KEY);
            assertThat(captured).containsEntry("key1", "value1");
            assertThat(captured).containsEntry("key2", "value2");
        }

        @Test
        @DisplayName("should not capture when baggage is empty")
        void shouldNotCapture_whenBaggageIsEmpty() {
            Map<String, Object> snapshot = new HashMap<>();
            provider.capture(snapshot);

            assertThat(snapshot).doesNotContainKey(BaggageContextSnapshotProvider.KEY);
        }
    }

    @Nested
    @DisplayName("restore")
    class Restore {

        @Test
        @DisplayName("should restore baggage from snapshot")
        void shouldRestoreBaggage_fromSnapshot() {
            Map<String, Object> snapshot = new HashMap<>();
            Map<String, String> baggage = new HashMap<>();
            baggage.put("key1", "value1");
            baggage.put("key2", "value2");
            snapshot.put(BaggageContextSnapshotProvider.KEY, baggage);

            provider.restore(snapshot);

            assertThat(BaggageContext.getLocalBaggage()).containsEntry("key1", "value1");
            assertThat(BaggageContext.getLocalBaggage()).containsEntry("key2", "value2");
        }

        @Test
        @DisplayName("should clear baggage when snapshot does not contain key")
        void shouldClearBaggage_whenSnapshotDoesNotContainKey() {
            BaggageContext.set("key1", "value1");

            Map<String, Object> snapshot = new HashMap<>();
            provider.restore(snapshot);

            assertThat(BaggageContext.getLocalBaggage()).isEmpty();
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("should clear baggage")
        void shouldClearBaggage() {
            BaggageContext.set("key1", "value1");

            provider.clear();

            assertThat(BaggageContext.getLocalBaggage()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ThreadLocal isolation")
    class ThreadLocalIsolation {

        @Test
        @DisplayName("should isolate baggage between threads")
        void shouldIsolateBaggage_betweenThreads() throws InterruptedException {
            BaggageContext.set("mainKey", "mainValue");

            AtomicReference<Map<String, String>> otherThreadBaggage = new AtomicReference<>();
            Thread thread = new Thread(() -> {
                otherThreadBaggage.set(BaggageContext.getLocalBaggage());
            });
            thread.start();
            thread.join();

            assertThat(otherThreadBaggage.get()).isEmpty();
            assertThat(BaggageContext.getLocalBaggage()).containsEntry("mainKey", "mainValue");
        }

        @Test
        @DisplayName("should not share clear() across threads")
        void shouldNotShareClearAcrossThreads() throws InterruptedException {
            BaggageContext.set("mainKey", "mainValue");

            Thread thread = new Thread(() -> {
                BaggageContext.set("otherKey", "otherValue");
                BaggageContext.clearLocalBaggage();
            });
            thread.start();
            thread.join();

            // Main thread should still have its data
            assertThat(BaggageContext.getLocalBaggage()).containsEntry("mainKey", "mainValue");
        }
    }

    @Nested
    @DisplayName("end-to-end: capture -> restore in another thread")
    class EndToEnd {

        @Test
        @DisplayName("should propagate BaggageContext across threads")
        void shouldPropagateBaggageContext_acrossThreads() throws InterruptedException {
            BaggageContext.set("tenantId", "t-123");
            BaggageContext.set("userId", "u-456");

            // Capture in main thread
            Map<String, Object> snapshot = new HashMap<>();
            provider.capture(snapshot);

            // Restore in another thread
            AtomicReference<Map<String, String>> captured = new AtomicReference<>();
            Thread thread = new Thread(() -> {
                provider.restore(snapshot);
                captured.set(BaggageContext.getLocalBaggage());
                provider.clear();
            });
            thread.start();
            thread.join();

            assertThat(captured.get()).containsEntry("tenantId", "t-123");
            assertThat(captured.get()).containsEntry("userId", "u-456");

            // Main thread context unchanged
            assertThat(BaggageContext.getLocalBaggage()).containsEntry("tenantId", "t-123");
        }
    }
}
