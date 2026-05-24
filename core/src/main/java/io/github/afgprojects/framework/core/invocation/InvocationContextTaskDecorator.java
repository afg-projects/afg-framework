package io.github.afgprojects.framework.core.invocation;

import org.springframework.core.task.TaskDecorator;
import java.util.HashMap;
import java.util.Map;

public class InvocationContextTaskDecorator implements TaskDecorator {
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

    static class ContextSnapshot {
        static Map<String, Object> capture() { return new HashMap<>(); }
        static void restore(Map<String, Object> snapshot) {}
    }
}
