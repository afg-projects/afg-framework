package io.github.afgprojects.framework.core.invocation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MethodKeyTest {

    @Test
    void resolve_shouldFindPublicMethod() {
        MethodKey key = new MethodKey("getName", List.of());
        Method method = key.resolve(SampleService.class);
        assertEquals("getName", method.getName());
        assertEquals(0, method.getParameterCount());
    }

    @Test
    void resolve_shouldFindMethodWithParameters() {
        MethodKey key = new MethodKey("greet", List.of("java.lang.String"));
        Method method = key.resolve(SampleService.class);
        assertEquals("greet", method.getName());
        assertEquals(1, method.getParameterCount());
        assertEquals(String.class, method.getParameterTypes()[0]);
    }

    @Test
    void resolve_shouldThrowWhenMethodNotFound() {
        MethodKey key = new MethodKey("nonExistent", List.of());
        assertThrows(ServiceInvocationException.class, () -> key.resolve(SampleService.class));
    }

    @Test
    void resolve_shouldCacheMethod() {
        MethodKey key = new MethodKey("getName", List.of());
        Method first = key.resolve(SampleService.class);
        Method second = key.resolve(SampleService.class);
        assertSame(first, second);
    }

    @Test
    void equalsAndHashCode_shouldWork() {
        MethodKey key1 = new MethodKey("greet", List.of("java.lang.String"));
        MethodKey key2 = new MethodKey("greet", List.of("java.lang.String"));
        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    static class SampleService {
        public String getName() { return "test"; }
        public String greet(String name) { return "Hello " + name; }
    }
}
