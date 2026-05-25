package io.github.afgprojects.framework.core.invocation;

import org.springframework.core.task.TaskDecorator;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class InvocationContextTaskDecorator implements TaskDecorator, ThreadFactory {

    private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, Object> contextSnapshot = ContextSnapshot.capture();
        return () -> {
            Map<String, Object> previous = ContextSnapshot.capture();
            try {
                ContextSnapshot.restore(contextSnapshot);
                runnable.run();
            } finally {
                ContextSnapshot.restore(previous);
            }
        };
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Runnable decorated = decorate(runnable);
        Thread thread = defaultThreadFactory.newThread(decorated);
        thread.setName("afg-invocation-" + thread.getId());
        return thread;
    }

    static class ContextSnapshot {
        static Map<String, Object> capture() { return new HashMap<>(); }
        static void restore(Map<String, Object> snapshot) {}
    }
}
