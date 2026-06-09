package io.github.afgprojects.framework.core.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CompositeContextTaskDecorator")
class CompositeContextTaskDecoratorTest {

    @Nested
    @DisplayName("decorate")
    class Decorate {

        @Test
        @DisplayName("should propagate context to decorated runnable")
        void shouldPropagateContext_toDecoratedRunnable() throws InterruptedException {
            // Set up propagator with a test provider
            ThreadLocalContextPropagator propagator = new ThreadLocalContextPropagator();
            TestContextProvider provider = new TestContextProvider();
            propagator.register(provider);

            CompositeContextTaskDecorator decorator = new CompositeContextTaskDecorator(propagator);

            // Set context in current thread
            TestContextHolder.setValue("main-thread-value");

            // Create and decorate a runnable
            AtomicReference<String> capturedValue = new AtomicReference<>();
            Runnable task = () -> capturedValue.set(TestContextHolder.getValue());
            Runnable decorated = decorator.decorate(task);

            // Run in another thread
            Thread thread = new Thread(decorated);
            thread.start();
            thread.join();

            // The decorated runnable should have the context from the main thread
            assertThat(capturedValue.get()).isEqualTo("main-thread-value");
        }

        @Test
        @DisplayName("should clear context after runnable completes")
        void shouldClearContext_afterRunnableCompletes() throws InterruptedException {
            ThreadLocalContextPropagator propagator = new ThreadLocalContextPropagator();
            TestContextProvider provider = new TestContextProvider();
            propagator.register(provider);

            CompositeContextTaskDecorator decorator = new CompositeContextTaskDecorator(propagator);

            TestContextHolder.setValue("main-thread-value");

            AtomicReference<String> valueAfterRun = new AtomicReference<>();
            Runnable task = () -> {};
            Runnable decorated = decorator.decorate(task);

            // Run in another thread
            Thread thread = new Thread(() -> {
                decorated.run();
                // After the decorated runnable completes, context should be cleared
                valueAfterRun.set(TestContextHolder.getValue());
            });
            thread.start();
            thread.join();

            // Context should be cleared in the worker thread after execution
            assertThat(valueAfterRun.get()).isNull();
        }

        @Test
        @DisplayName("should clear context even when runnable throws exception")
        void shouldClearContext_evenWhenRunnableThrowsException() throws InterruptedException {
            ThreadLocalContextPropagator propagator = new ThreadLocalContextPropagator();
            TestContextProvider provider = new TestContextProvider();
            propagator.register(provider);

            CompositeContextTaskDecorator decorator = new CompositeContextTaskDecorator(propagator);

            TestContextHolder.setValue("main-thread-value");

            AtomicReference<String> valueAfterException = new AtomicReference<>();
            Runnable task = () -> { throw new RuntimeException("test"); };
            Runnable decorated = decorator.decorate(task);

            Thread thread = new Thread(() -> {
                try {
                    decorated.run();
                } catch (RuntimeException ignored) {
                    // Expected
                }
                valueAfterException.set(TestContextHolder.getValue());
            });
            thread.start();
            thread.join();

            // Context should be cleared even after exception
            assertThat(valueAfterException.get()).isNull();
        }

        @Test
        @DisplayName("should not affect main thread context")
        void shouldNotAffectMainThreadContext() throws InterruptedException {
            ThreadLocalContextPropagator propagator = new ThreadLocalContextPropagator();
            TestContextProvider provider = new TestContextProvider();
            propagator.register(provider);

            CompositeContextTaskDecorator decorator = new CompositeContextTaskDecorator(propagator);

            TestContextHolder.setValue("main-thread-value");

            Runnable task = () -> {};
            Runnable decorated = decorator.decorate(task);

            Thread thread = new Thread(decorated);
            thread.start();
            thread.join();

            // Main thread context should be unchanged
            assertThat(TestContextHolder.getValue()).isEqualTo("main-thread-value");
        }
    }

    /**
     * Test ThreadLocal holder for verifying context propagation.
     */
    private static class TestContextHolder {
        private static final ThreadLocal<String> VALUE = new ThreadLocal<>();

        static void setValue(String value) {
            VALUE.set(value);
        }

        static String getValue() {
            return VALUE.get();
        }

        static void clear() {
            VALUE.remove();
        }
    }

    /**
     * Test provider that captures/restores TestContextHolder.
     */
    private static class TestContextProvider implements ContextSnapshotProvider {

        @Override
        public void capture(Map<String, Object> snapshot) {
            String value = TestContextHolder.getValue();
            if (value != null) {
                snapshot.put("testContext", value);
            }
        }

        @Override
        public void restore(Map<String, Object> snapshot) {
            Object value = snapshot.get("testContext");
            if (value instanceof String s) {
                TestContextHolder.setValue(s);
            } else {
                TestContextHolder.clear();
            }
        }

        @Override
        public void clear() {
            TestContextHolder.clear();
        }
    }
}
